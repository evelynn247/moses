package com.biyao.moses.match2.service.impl;

import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.cache.RecommendManualSourceConfigCache;
import com.biyao.moses.common.constant.CommonConstants;
import com.biyao.moses.common.enums.MatchSourceEnum;
import com.biyao.moses.match2.constants.MatchSourceDataStrategyConst;
import com.biyao.moses.match2.constants.MatchSourceRedisConst;
import com.biyao.moses.match2.constants.MatchStrategyConst;
import com.biyao.moses.match2.param.MatchParam;
import com.biyao.moses.match2.service.Match2;
import com.biyao.moses.model.match2.MatchItem2;
import com.biyao.moses.params.MatchSourceData;
import com.biyao.moses.params.ProductInfo;
import com.biyao.moses.util.AlgorithmRedisUtil;
import com.biyao.moses.util.FilterUtil;
import com.biyao.moses.util.MatchRedisUtil;
import com.biyao.moses.util.StringUtil;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @ClassName CommonMatchImpl
 * @Description 通用match召回
 * @Author xiaojiankai
 * @Date 2020/4/20 11:36
 * @Version 1.0
 **/
@Slf4j
@Component(value = MatchStrategyConst.COMMON_MATCH)
public class CommonMatchImpl implements Match2 {
    @Autowired
    private MatchRedisUtil matchRedisUtil;

    @Autowired
    private AlgorithmRedisUtil algorithmRedisUtil;

    @Autowired
    private ProductDetailCache productDetailCache;
    @Autowired
    RecommendManualSourceConfigCache recommendManualSourceConfigCache;

    @BProfiler(key = "CommonMatchImpl.match",
            monitorType = {MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR})
    @Override
    public List<MatchItem2> match(MatchParam matchParam) {
        String source = matchParam.getSource();
        int uid = matchParam.getUid() == null ? 0 : matchParam.getUid();
        String uuid = matchParam.getUuid();

        List<MatchItem2> result = new ArrayList<>();
        String matchSourceDataStr;
        if(matchParam.getIsMunmanualSource()){
            matchSourceDataStr = getDataBySourceId(source);
        }else {
            // 常规从redis中获取数据
            matchSourceDataStr= getDataByStrategyAndRedis(matchParam);
        }
        if(StringUtils.isBlank(matchSourceDataStr)){
            log.error("[一般异常][召回源]{}召回源商品数据为空， uid {}， uuid {}", source, uid, uuid);
            return result;
        }
        String bizName = source + "_" + uuid;
        if(uid > 0){
            bizName = bizName + "_" + uid;
        }
        Map<String, MatchSourceData> matchSourceDataMap = StringUtil.parseMatchSourceDataStr(matchSourceDataStr, bizName);
        if(matchSourceDataMap == null || matchSourceDataMap.size() <= 0){
            log.error("[严重异常][召回源]解析召回源{}商品数据为空，uid {}, uuid {}", source, matchParam.getUid(), matchParam.getUuid());
            return result;
        }
        boolean isNormalProduct = true;
        if(MatchSourceEnum.isNotNormalProduct(source)){
            isNormalProduct = false;
        }

        for (Map.Entry<String, MatchSourceData> entry : matchSourceDataMap.entrySet()) {
            try {
                String id = entry.getKey();
                MatchSourceData matchSourceData = entry.getValue();
                MatchItem2 matchItem2 = new MatchItem2();
                if (isNormalProduct) {
                    Long pid = Long.valueOf(id);
                    ProductInfo productInfo = productDetailCache.getProductInfo(pid);
                    if (FilterUtil.isCommonFilter(productInfo)) {
                        continue;
                    }
                    // 过滤必要造物商品
                    if("1".equals(matchParam.getIsFilterByZw() )&& (byte)1 == productInfo.getIsCreator()){
                        continue;
                    }
                    matchItem2.setProductId(pid);
                }else{
                    matchItem2.setId(id);
                }
                matchItem2.setScore(matchSourceData.getScore());

                //如果有真实召回源，则召回源信息为配置的召回源名称+真实召回源名称
                String realSourceName = matchSourceData.getRealSourceName();
                if(StringUtils.isBlank(realSourceName)){
                    realSourceName = source;
                }else{
                    realSourceName = source + "_" + realSourceName;
                }
                matchItem2.setSource(realSourceName);
                result.add(matchItem2);
            }catch (Exception e){
                log.error("[严重异常][召回源]填入召回源{}商品数据时发生异常，uid {}, uuid {}", source, matchParam.getUid(), matchParam.getUuid());
            }
        }
        return result;
    }

    /**
     * 通过个性化数据获取策略到指定的redis中获取数据并返回
     * 默认策略：有uid，则使用uid获取，没有uid，则使用uuid获取
     * @param matchParam
     * @param redisKeyPrefix
     * @param personDataStrategy
     * @return
     */
    private String getDataByPersonStrategyAndRedis(MatchParam matchParam, String redisKeyPrefix, String personDataStrategy){
        String result = null;
        String redis = matchParam.getRedis();
        int uid = matchParam.getUid() == null ? 0 : matchParam.getUid();
        String uuid = matchParam.getUuid();

        //如果个性化数据策略和兜底数据获取策略都存在时，则只有在个性化数据策略没有获取到数据时，才会按照兜底数据获取策略获取数据
        if(MatchSourceDataStrategyConst.DATA_STATEGY_UID.equals(personDataStrategy)){
            if(uid > 0){
                result = getDataByRedis(redisKeyPrefix + uid, redis);
            }
        }else if(MatchSourceDataStrategyConst.DATA_STATEGY_UUID.equals(personDataStrategy)){
            result = getDataByRedis(redisKeyPrefix + uuid, redis);
        }else if(MatchSourceDataStrategyConst.DATA_STATEGY_UID_NODATA_UUID.equals(personDataStrategy)){
            if(uid > 0){
                result = getDataByRedis(redisKeyPrefix + uid, redis);
            }
            if(StringUtils.isBlank(result)){
                result = getDataByRedis(redisKeyPrefix + uuid, redis);
            }
        }else{
            if(uid > 0){
                result = getDataByRedis(redisKeyPrefix + uid, redis);
            }else{
                result = getDataByRedis(redisKeyPrefix + uuid, redis);
            }
        }
        return result;
    }

    /**
     * 通过兜底数据获取策略到指定的redis中获取数据并返回
     * @param matchParam
     * @param redisKeyPrefix
     * @param commonDataStrategy
     * @return
     */
    private String getDataByCommonStrategyAndRedis(MatchParam matchParam, String redisKeyPrefix, String commonDataStrategy){
        String result = "";
        String redis = matchParam.getRedis();
        //如果个性化数据策略和兜底数据获取策略都存在时，则只有在个性化数据策略没有获取到数据时，才会按照兜底数据获取策略获取数据
        if(MatchSourceDataStrategyConst.DATA_STATEGY_COMMON.equals(commonDataStrategy)){
            result = getDataByRedis(redisKeyPrefix + "common", redis);
        }else if(MatchSourceDataStrategyConst.DATA_STATEGY_COMMON_SEX.equals(commonDataStrategy)){
            String userSex = matchParam.getUserSex() == null ? CommonConstants.UNKNOWN_SEX : matchParam.getUserSex().toString();
            String rediskey;
            if(CommonConstants.MALE_SEX.equals(userSex)){
                rediskey = redisKeyPrefix + "male";
            }else if(CommonConstants.FEMALE_SEX.equals(userSex)){
                rediskey = redisKeyPrefix + "female";
            }else{
                rediskey = redisKeyPrefix + "common";
            }
            result = getDataByRedis(rediskey, redis);
        }
        return result;
    }
    /**
     * 通过数据获取策略到指定的redis中获取数据并返回
     * 默认策略：有uid，则使用uid获取，没有uid，则使用uuid获取
     * @param matchParam
     * @return
     */
    private String getDataByStrategyAndRedis(MatchParam matchParam){
        String result = null;
        String dataStrategy = matchParam.getDataStrategy();
        //组装redis key前缀
        String redisKeyPrefix = "moses:"+matchParam.getSource()+"_";
        //个性化数据获取策略
        String personStrategy = null;
        //兜底数据获取策略
        String commonStrategy = null;
        //如果没有配置召回源策略则默认个性化数据策略为4，无兜底数据获取策略
        if(StringUtils.isBlank(dataStrategy)){
            personStrategy = MatchSourceDataStrategyConst.DATA_STATEGY_UID_NO_UUID;
        }else{
            String[] split = dataStrategy.split("-");
            personStrategy = split[0];
            if(split.length > 1){
                commonStrategy = split[1];
            }
        }
        //如果存在个性化数据策略，则根据个性化数据策略获取数据
        if(StringUtils.isNotBlank(personStrategy)){
            result = getDataByPersonStrategyAndRedis(matchParam, redisKeyPrefix, personStrategy);
        }

        //如果存在兜底数据策略并且没有通过个性化数据策略获取到数据，则根据兜底数据策略获取数据
        if(StringUtils.isNotBlank(commonStrategy) && StringUtils.isBlank(result)){
            result = getDataByCommonStrategyAndRedis(matchParam, redisKeyPrefix, commonStrategy);
        }

        return result;
    }

    /**
     * 根据redis信息 从指定的redis中获取数据
     * @param key
     * @param redis
     * @return
     */
    private String getDataByRedis(String key, String redis){
        String result;
        if(MatchSourceRedisConst.REDIS_MATCH.equals(redis)){
            result = matchRedisUtil.getString(key);
        }else{
            result = algorithmRedisUtil.getString(key);
        }
        return result;
    }

    /**
     * 数据源为人工配置时 商品召回
     * @param sourceId
     * @return
     */
    private String getDataBySourceId(String sourceId){

        String result="";
        if(!StringUtil.isBlank(sourceId)){
            result= recommendManualSourceConfigCache.getManualSourcePidBySourceId(sourceId);
        }
       return result;
    }
}
