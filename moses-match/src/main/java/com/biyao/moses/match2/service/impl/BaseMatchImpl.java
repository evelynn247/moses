package com.biyao.moses.match2.service.impl;

import com.alibaba.fastjson.JSON;
import com.biyao.moses.cache.*;
import com.biyao.moses.common.enums.MatchConfigInfoEnum;
import com.biyao.moses.match2.constants.MatchStrategyConst;
import com.biyao.moses.match2.param.MatchParam;
import com.biyao.moses.match2.service.Match2;
import com.biyao.moses.match2.util.MatchUtil;
import com.biyao.moses.model.match2.MatchItem2;
import com.biyao.moses.params.ProductInfo;
import com.biyao.moses.util.CacheRedisUtil;
import com.biyao.moses.util.FilterUtil;
import com.uc.domain.bean.User;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * @ClassName BaseMatchImpl
 * @Description 基础流量召回源实现类
 * @Author xiaojiankai
 * @Date 2020/7/28 14:08
 * @Version 1.0
 **/
@Slf4j
@Component(value = MatchStrategyConst.BASE)
public class BaseMatchImpl implements Match2 {

    @Autowired
    private CacheRedisUtil cacheRedisUtil;

    @Autowired
    private ProductDetailCache productDetailCache;

    @Autowired
    private ProductSeasonCache productSeasonCache;

    @Autowired
    private BaseSourceProductCache baseSourceProductCache;

    @Autowired
    private SimilarCategory3IdCache similarCategory3IdCache;

    @Autowired
    private RedisCache redisCache;

    private static final long oneDay2Ms = 86400000L; //24*3600*1000
    private static final double level3HobbyScoreMinLimit = 5d;
    private static final int hobbyCate3PidNumMaxLimit = 5;
    private static final int noHobbyCate3PidNumMaxLimit = 2;

    @Override
    public List<MatchItem2> match(MatchParam matchParam) {
        List<MatchItem2> result = new ArrayList<>();
        String uuid = matchParam.getUuid();
        String source = matchParam.getSource();

        //获取配置值
        String expPidNum24hMaxLimitStr = redisCache.getMatchConfigValue(MatchConfigInfoEnum.BaseSource24hPidNumMaxLimit);
        String expectPidNumLimitStr = redisCache.getMatchConfigValue(MatchConfigInfoEnum.BaseSourceExpectPidNunMaxLimit);
        int expPidNum24hMaxLimit;
        int expectPidNumLimit;
        try{
            expPidNum24hMaxLimit = Integer.valueOf(expPidNum24hMaxLimitStr.trim());
            expectPidNumLimit = Integer.valueOf(expectPidNumLimitStr.trim());
        }catch (Exception e){
            expPidNum24hMaxLimit = Integer.valueOf(MatchConfigInfoEnum.BaseSource24hPidNumMaxLimit.getDefaultValue());
            expectPidNumLimit = Integer.valueOf(MatchConfigInfoEnum.BaseSourceExpectPidNunMaxLimit.getDefaultValue());
        }

        try {
            if (expPidNum24hMaxLimit <= 0 || expectPidNumLimit <= 0) {
                log.error("[严重异常][基础流量召回源]设置的配置上限不大于0， expPidNum24hMaxLimit {}， expectPidNumLimit {}", expPidNum24hMaxLimit, expectPidNumLimit);
                return result;
            }

            User ucUser = matchParam.getUcUser();
            String userSeason = null;
            Map<String, BigDecimal> level3Hobby = null;
            if (ucUser != null) {
                userSeason = ucUser.getSeason();
                level3Hobby = ucUser.getLevel3Hobby();
            }

            Set<Long> expectTimeSet = new HashSet<>();
            expectTimeSet.add(oneDay2Ms);
            Map<Long, List<Long>> sourceExposurePidMap = MatchUtil.getSourceExposurePid(uuid, source, cacheRedisUtil, expectTimeSet);
            List<Long> expPid24hList = sourceExposurePidMap.get(oneDay2Ms);
            Set<Long> expPid24hSet = new HashSet<>();
            int exposurePidNum = 0;
            //熔断机制 24小时内该召回源对该用户曝光商品超过40，则进行熔断
            if (CollectionUtils.isNotEmpty(expPid24hList)) {
                exposurePidNum = expPid24hList.size();
                if (exposurePidNum >= expPidNum24hMaxLimit) {
                    return result;
                }
                expPid24hSet.addAll(expPid24hList);
            }

            Map<Long, List<Long>> baseSourceProductMap = baseSourceProductCache.getBaseSourceProductMap();
            Map<Long, List<Long>> filterBaseSourceProductMap = filterCandidateProduct(baseSourceProductMap, expPid24hSet, userSeason, matchParam);
            if (filterBaseSourceProductMap == null || filterBaseSourceProductMap.isEmpty()) {
                log.error("[一般异常][基础流量召回源]过滤后基础流量候选商品为空，uuid {}", uuid);
                return result;
            }

            int expectNum = Math.min(expPidNum24hMaxLimit - exposurePidNum, expectPidNumLimit);
            List<Long> pidList = new ArrayList<>();
            //从用户感兴趣的相似三级类目中选择商品
            List<Long> hobbySimilarCate3IdList = getHobbySimilarCate3Id(level3Hobby);
            selectProduct(hobbySimilarCate3IdList, filterBaseSourceProductMap, hobbyCate3PidNumMaxLimit, expectNum, pidList);

            //从用户不感兴趣的相似三级类目中选择商品
            List<Long> noHobbySimilarCate3IdList = new ArrayList<>(filterBaseSourceProductMap.keySet());
            noHobbySimilarCate3IdList.removeAll(hobbySimilarCate3IdList);
            selectProduct(noHobbySimilarCate3IdList, filterBaseSourceProductMap, noHobbyCate3PidNumMaxLimit, expectNum, pidList);

            if (CollectionUtils.isEmpty(pidList)) {
                log.error("[一般异常][基础流量召回源]基础流量召回源商品为空，uuid {}", uuid);
                return result;
            }

            int sortNum = 1;
            for (Long pid : pidList) {
                MatchItem2 matchItem2 = new MatchItem2();
                matchItem2.setSource(MatchStrategyConst.BASE);
                matchItem2.setProductId(pid);
                matchItem2.setScore((21 - sortNum) / 20d);
                sortNum++;
                result.add(matchItem2);
            }
        }catch (Exception e){
            log.error("[严重异常][基础流量召回源]获取召回源数据时出现异常，uuid {}，", uuid, e);
        }
        return result;
    }

    /**
     * 通过规则选择商品
     * @param similarCate3List 相似三级类目集合
     * @param filterBaseSourceProductMap 候选商品集合
     * @param cate3PidNumMaxLimit 每个相似三级类目选择的商品数量上限
     * @param expectNum 期望获取的商品数量，即pidList的最大长度
     * @param pidList 选择的商品存储在该List中
     */
    private void selectProduct(List<Long> similarCate3List, Map<Long, List<Long>> filterBaseSourceProductMap,
                               int cate3PidNumMaxLimit, int expectNum, List<Long> pidList){
        if(CollectionUtils.isEmpty(similarCate3List) || pidList == null || pidList.size() >= expectNum){
            return;
        }

        int currentExpectNum = expectNum - pidList.size();
        for(Long similarCate3Id : similarCate3List){
            if(!filterBaseSourceProductMap.containsKey(similarCate3Id)){
                continue;
            }
            List<Long> similarCate3IdPidList = filterBaseSourceProductMap.get(similarCate3Id);
            if(CollectionUtils.isEmpty(similarCate3IdPidList)){
                continue;
            }
            Collections.shuffle(similarCate3IdPidList);
            int count = 0;
            for(int i = 0; i < similarCate3IdPidList.size(); i++){
                if(currentExpectNum <= 0 || count >= cate3PidNumMaxLimit){
                    break;
                }
                Long pid = similarCate3IdPidList.get(i);
                if(pidList.contains(pid)){
                    continue;
                }
                pidList.add(pid);
                currentExpectNum--;
                count++;
            }
        }
    }
    /**
     * 通过用户感兴趣后台三级类目，获取用户感兴趣相似三级类目，按类目兴趣分降序排序
     * @param level3Hobby
     * @return
     */
    private List<Long> getHobbySimilarCate3Id(Map<String, BigDecimal> level3Hobby){
        List<Long> result = new ArrayList<>();
        if(level3Hobby == null || level3Hobby.isEmpty()){
            return result;
        }
        List<Map.Entry<String, BigDecimal>> mapList = new ArrayList<>();
        try {
            //根据用户喜爱的三级类目的浏览商品次数降序排序
            mapList = new ArrayList<>(level3Hobby.entrySet());
            Collections.sort(mapList, (o1, o2) -> {
                BigDecimal v1 = o1.getValue() == null ? new BigDecimal(0) : o1.getValue();
                BigDecimal v2 = o2.getValue() == null ? new BigDecimal(0) : o2.getValue();
                return -v1.compareTo(v2);
            });
        }catch (Exception e){
            log.error("[严重异常][基础流量召回源]将用户后台三级类目转化为相似三级类目偏好时出现异常， level3Hobby {}，", JSON.toJSONString(level3Hobby), e);
            return result;
        }

        //遍历按分值降序排序的后台三级类目集合，按分值高低获取相似三级类目
        for(Map.Entry<String, BigDecimal> entry : mapList){
            try {
                String cate3IdStr = entry.getKey();
                if(StringUtils.isBlank(cate3IdStr)){
                    continue;
                }
                BigDecimal score = entry.getValue();
                if (score.doubleValue() < level3HobbyScoreMinLimit) {
                    break;
                }
                Long similarCate3Id = similarCategory3IdCache.getSimilarCate3Id(Long.valueOf(cate3IdStr));
                if(!result.contains(similarCate3Id)){
                    result.add(similarCate3Id);
                }
            }catch (Exception e){
                log.error("[严重异常][基础流量召回源]处理用户感兴趣后台类目出现异常 level3Hobby {}， e ", JSON.toJSONString(level3Hobby), e);
            }
        }

        return result;
    }

    /**
     * 根据用户信息对候选商品进行过滤
     * @param baseSourceProductMap
     * @param expPid24hSet
     * @param usrSeason
     * @param matchParam
     * @return
     */
    private Map<Long, List<Long>> filterCandidateProduct(Map<Long, List<Long>> baseSourceProductMap, Set<Long> expPid24hSet, String usrSeason, MatchParam matchParam){
        Map<Long, List<Long>> result = new HashMap<>();
        if(baseSourceProductMap == null || baseSourceProductMap.isEmpty()){
            return result;
        }

        int usrSeasonValue = MatchUtil.convertSeason2int(usrSeason);
        for(Map.Entry<Long, List<Long>> entry : baseSourceProductMap.entrySet()){
            Long similarCate3 = entry.getKey();
            List<Long> pidList = entry.getValue();
            if(CollectionUtils.isEmpty(pidList)){
                continue;
            }
            for(Long pid : pidList){
                //通用过滤
                ProductInfo productInfo = productDetailCache.getProductInfo(pid);
                if(FilterUtil.isCommonFilter(productInfo)){
                    continue;
                }

                //过滤24小时内已曝光商品
                if(expPid24hSet != null && expPid24hSet.contains(pid)){
                    continue;
                }

                //季节过滤
                int productSeasonValue = productSeasonCache.getProductSeasonValue(pid.toString());
                if(MatchUtil.isFilterByUserSeason(productSeasonValue, usrSeasonValue)){
                    continue;
                }

                //性别过滤
                if(MatchUtil.isFilterBySex(productInfo, matchParam.getUserSex())){
                    continue;
                }

                if(result.containsKey(similarCate3)){
                    result.get(similarCate3).add(pid);
                }else{
                    List<Long> list = new ArrayList<>();
                    list.add(pid);
                    result.put(similarCate3, list);
                }
            }
        }


        return result;
    }
}
