package com.biyao.moses.match2.service.impl;

import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.cache.ProductSeasonCache;
import com.biyao.moses.common.constant.CacheRedisKeyConstant;
import com.biyao.moses.common.constant.MatchRedisKeyConstant;
import com.biyao.moses.match2.constants.MatchStrategyConst;
import com.biyao.moses.match2.param.MatchParam;
import com.biyao.moses.match2.service.Match2;
import com.biyao.moses.match2.util.MatchUtil;
import com.biyao.moses.model.match2.MatchItem2;
import com.biyao.moses.params.ProductInfo;
import com.biyao.moses.util.CacheRedisUtil;
import com.biyao.moses.util.FilterUtil;
import com.biyao.moses.util.MatchRedisUtil;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import com.uc.domain.bean.User;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @ClassName WeatherMatchImpl
 * @Description 天气基础召回源
 * @Author xiaojiankai
 * @Date 2020/7/9 15:15
 * @Version 1.0
 **/
@Slf4j
@Component(value = MatchStrategyConst.WEATHER)
public class WeatherMatchImpl implements Match2 {
    @Autowired
    private MatchRedisUtil matchRedisUtil;

    @Autowired
    private CacheRedisUtil cacheRedisUtil;

    @Autowired
    private ProductDetailCache productDetailCache;

    @Autowired
    private ProductSeasonCache productSeasonCache;

    private static final String weatherExposure24h = "weatherExposure24h";
    private static final String weatherExposure72h = "weatherExposure72h";
    private static final long oneDay2Ms = 86400000L; //24*3600*1000
    private static final long threeDay2Ms = 259200000L;//3*24*3600*1000
    private static final int expPidNum24hMaxLimit = 9;
    private static final int expPidNum72hMaxLimit = 19;
    private static final int eachWeatherPidNumMaxLimit = 5;
    private static final int expectProductNumMaxLimit = 2;

    @BProfiler(key = "WeatherMatchImpl.match",
            monitorType = {MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR})
    @Override
    public List<MatchItem2> match(MatchParam matchParam) {
        String uuid = matchParam.getUuid();
        User ucUser = matchParam.getUcUser();
        List<MatchItem2> result = new ArrayList<>();
        if(ucUser == null){
            return result;
        }

        //从ucUser中获取用户天气
        List<String> userWeather = ucUser.getWeatherSevenDay();
        if(CollectionUtils.isEmpty(userWeather)){
            //如果没有获取到用户天气，则直接返回空集合
            return result;
        }

        //获取天气召回源曝光商品
        Map<String, Set<Long>> weatherExposure = getWeatherExposure(uuid);
        Set<Long> expPid24hSet = weatherExposure.get(weatherExposure24h);
        Set<Long> expPid72hSet = weatherExposure.get(weatherExposure72h);

        if((expPid24hSet != null && expPid24hSet.size() >= expPidNum24hMaxLimit)
            || (expPid72hSet != null && expPid72hSet.size() >= expPidNum72hMaxLimit)){
            //天气召回曝光商品24小时内达到9个或者72小时内达到19个，则熔断，直接返回空集合
            return result;
        }

        //根据用户天气查询对应商品
        Map<String, List<MatchItem2>> weatherMatchItem2Map = getProductInfo(userWeather);
        if(weatherMatchItem2Map == null || weatherMatchItem2Map.isEmpty()){
            log.error("[一般异常][天气召回源]获取到用户天气商品为空，uuid {}", uuid);
            return result;
        }

        List<MatchItem2> candidateList = getCandidateList(weatherMatchItem2Map, expPid72hSet, ucUser.getSeason(), matchParam);
        if(CollectionUtils.isEmpty(candidateList)){
            log.error("[一般异常][天气召回源]过滤后用户天气候选商品为空，uuid {}", uuid);
            return  result;
        }

        Collections.shuffle(candidateList);
        Set<Long> pidSet = new HashSet<>();
        Random random = new Random();
        for(MatchItem2 matchItem2 : candidateList){
            if(matchItem2 == null || pidSet.contains(matchItem2.getProductId())){
                continue;
            }
            double score = (10 - random.nextInt(6))/10d;
            matchItem2.setScore(score);
            matchItem2.setSource(MatchStrategyConst.WEATHER);
            result.add(matchItem2);
            pidSet.add(matchItem2.getProductId());
            if(result.size() >= expectProductNumMaxLimit){
                break;
            }
        }
        return result;
    }

    /**
     * 从各个用户天气的商品集合中获取天气召回候选商品集合
     * @param weatherMatchItem2Map
     * @param expPid72hSet
     * @param usrSeason
     * @param matchParam
     * @return
     */
    private List<MatchItem2> getCandidateList(Map<String, List<MatchItem2>> weatherMatchItem2Map, Set<Long> expPid72hSet, String usrSeason, MatchParam matchParam){
        List<MatchItem2> candidateList = new ArrayList<>();
        int usrSeasonValue = MatchUtil.convertSeason2int(usrSeason);
        for (Map.Entry<String, List<MatchItem2>> entry : weatherMatchItem2Map.entrySet()){
            List<MatchItem2> matchItem2List = entry.getValue();
            if(CollectionUtils.isEmpty(matchItem2List)){
                continue;
            }
            Iterator<MatchItem2> iterator = matchItem2List.iterator();
            while (iterator.hasNext()){
                MatchItem2 matchItem2 = iterator.next();
                //通用过滤
                ProductInfo productInfo = productDetailCache.getProductInfo(matchItem2.getProductId());
                if(FilterUtil.isCommonFilter(productInfo)){
                    iterator.remove();
                    continue;
                }

                //过滤72小时内已曝光商品
                if(expPid72hSet != null && expPid72hSet.contains(matchItem2.getProductId())){
                    iterator.remove();
                    continue;
                }

                //季节过滤
                int productSeasonValue = productSeasonCache.getProductSeasonValue(matchItem2.getProductId().toString());
                if(MatchUtil.isFilterByUserSeason(productSeasonValue, usrSeasonValue)){
                    iterator.remove();
                    continue;
                }

                //性别过滤
                if(MatchUtil.isFilterBySex(productInfo, matchParam.getUserSex())){
                    iterator.remove();
                    continue;
                }
            }

            if(CollectionUtils.isEmpty(matchItem2List)){
                continue;
            }

            if(matchItem2List.size() > eachWeatherPidNumMaxLimit){
                Collections.shuffle(matchItem2List);
                candidateList.addAll(matchItem2List.subList(0, eachWeatherPidNumMaxLimit));
            }else{
                candidateList.addAll(matchItem2List);
            }
        }
        return candidateList;
    }

    /**
     * 从redis中获取商品信息
     * @param userWeatherList
     * @return key为用户天气，value为该天气对应的商品信息集合
     */
    private Map<String, List<MatchItem2>> getProductInfo(List<String> userWeatherList){
        Map<String, List<MatchItem2>> result = new HashMap<>();
        if(CollectionUtils.isEmpty(userWeatherList)){
            return result;
        }
        String[] userWeatherArray = userWeatherList.toArray(new String[0]);
        List<String> productStrList = matchRedisUtil.hmget(MatchRedisKeyConstant.MOSES_WEATHER_PRODUCT, userWeatherArray);
        if(CollectionUtils.isEmpty(productStrList)){
            return result;
        }
        //解析并根据通用规则过滤
        for(int i = 0; i < userWeatherArray.length; i++){
            String userWeather = userWeatherArray[i];
            String productInfoStr = productStrList.get(i);
            if(StringUtils.isBlank(productInfoStr)){
                continue;
            }
            List<MatchItem2> matchItem2List = new ArrayList<>();
            String[] productInfoArray = productInfoStr.trim().split(",");
            for(String productStr : productInfoArray){
                try {
                    if(StringUtils.isBlank(productStr)){
                        continue;
                    }
                    String[] productTagArray = productStr.trim().split(":");
                    String pidStr = productTagArray[0];
                    if(StringUtils.isBlank(pidStr)){
                        continue;
                    }
                    Long pid = Long.valueOf(pidStr);
                    ProductInfo productInfo = productDetailCache.getProductInfo(pid);
                    if(FilterUtil.isCommonFilter(productInfo)){
                        continue;
                    }
                    MatchItem2 matchItem2 = new MatchItem2();
                    matchItem2.setProductId(pid);
                    if(productTagArray.length > 1 && StringUtils.isNotBlank(productTagArray[1])){
                        matchItem2.setLabelContent(productTagArray[1]);
                    }
                    matchItem2List.add(matchItem2);
                }catch (Exception e){
                    log.error("[严重异常][天气召回源]解析商品信息出现异常 天气 {}，商品信息 {}，e ", userWeather, productInfoStr, e);
                }
            }
            if(CollectionUtils.isNotEmpty(matchItem2List)){
                result.put(userWeather, matchItem2List);
            }
        }
        return result;
    }
    /**
     * 从用户曝光信息中获取天气召回源24小时和72小时曝光商品ID
     * @param uuid
     * @return
     */
    private Map<String, Set<Long>> getWeatherExposure(String uuid){
        Map<String, Set<Long>> result = new HashMap<>();
        //获取曝光商品 格式pid:time:source
        List<String> expPidInfoList = cacheRedisUtil.lrange(CacheRedisKeyConstant.NEW_HOME_FEED_EXPOSURE + uuid, 0, -1);
        if(CollectionUtils.isEmpty(expPidInfoList)){
            return result;
        }
        Set<Long> expPid24hSet = new HashSet<>();
        Set<Long> expPid72hSet = new HashSet<>();
        long currentTime = System.currentTimeMillis();
        for(String expPidInfoStr : expPidInfoList){
            try {
                if(StringUtils.isBlank(expPidInfoStr)){
                    continue;
                }
                String[] pidInfoArray = expPidInfoStr.split(":");
                if(pidInfoArray.length < 3){
                    continue;
                }
                long pid = Long.valueOf(pidInfoArray[0]);
                long time = Long.valueOf(pidInfoArray[1]);
                String source = pidInfoArray[2];
                if(StringUtils.isBlank(source)){
                    continue;
                }
                Set<String> sourceSet = new HashSet<>(Arrays.asList(source.split("_")));
                if(!sourceSet.contains(MatchStrategyConst.WEATHER)){
                    continue;
                }

                if(currentTime - time > threeDay2Ms){
                    continue;
                }
                expPid72hSet.add(pid);
                if(currentTime - time <= oneDay2Ms){
                    expPid24hSet.add(pid);
                }
            }catch (Exception e){
                log.error("[严重异常][天气召回源]解析用户曝光信息发生异常 uuid {}， expInfo {}， e", uuid, expPidInfoStr, e);
            }
        }
        result.put(weatherExposure24h, expPid24hSet);
        result.put(weatherExposure72h, expPid72hSet);
        return result;
    }

}
