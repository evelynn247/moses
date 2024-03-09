package com.biyao.moses.match2.service.impl;

import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.cache.UcbProductCache;
import com.biyao.moses.common.constant.AlgorithmRedisKeyConstants;
import com.biyao.moses.common.constant.CommonConstants;
import com.biyao.moses.common.constant.ExpFlagsConstants;
import com.biyao.moses.match2.constants.MatchStrategyConst;
import com.biyao.moses.match2.param.MatchParam;
import com.biyao.moses.match2.service.Match2;
import com.biyao.moses.model.match2.MatchItem2;
import com.biyao.moses.params.ProductInfo;
import com.biyao.moses.params.ProductScoreInfo;
import com.biyao.moses.util.AlgorithmRedisUtil;
import com.biyao.moses.util.FilterUtil;
import com.biyao.moses.util.StringUtil;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author xiaojiankai@idstaff.com
 * @date 2019/12/23
 * ucb（多臂赌博机）召回源
 **/
@Slf4j
@Component(value = MatchStrategyConst.UCB)
public class UcbMatchImpl implements Match2 {
    @Autowired
    private UcbProductCache ucbProductCache;

    @Autowired
    private ProductDetailCache productDetailCache;

    @Autowired
    private AlgorithmRedisUtil algorithmRedisUtil;

    private static  final int PID_NUM_MAX_LIMIT = 500;

    @BProfiler(key = "UcbMatchImpl.match",
            monitorType = {MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR})
    @Override
    public List<MatchItem2> match(MatchParam matchParam) {
        String ucbDataNum = matchParam.getUcbDataNum();
        //如果UCBDataNum值不为空，且其值不为DEFAULT，则按通用ucb match处理。
        if(StringUtils.isNotBlank(ucbDataNum) && !ExpFlagsConstants.VALUE_DEFAULT.equals(ucbDataNum)){
            return ucbCommonMatch(matchParam);
        }
        return ucbMatch(matchParam);
    }

    /**
     * ucb 召回源
     * @param matchParam
     * @return
     */
    private List<MatchItem2> ucbMatch(MatchParam matchParam){
        List<MatchItem2> result = new ArrayList<>();
        try {
            String userSex;
            if(matchParam.getUserSex() == null){
                userSex = CommonConstants.UNKNOWN_SEX;
            }else{
                userSex = matchParam.getUserSex().toString();
            }
            List<ProductScoreInfo> productScoreInfo = ucbProductCache.getProductScoreInfoBySex(userSex);
            if(CollectionUtils.isEmpty(productScoreInfo)){
                log.error("[严重异常][召回源]获取ucb召回源数据为空， uuid {}, uid {}", matchParam.getUuid(), matchParam.getUid());
                return result;
            }

            for (ProductScoreInfo p : productScoreInfo) {
                ProductInfo productInfo = productDetailCache.getProductInfo(p.getProductId());
                if(FilterUtil.isCommonFilter(productInfo)){
                    continue;
                }
                MatchItem2 matchItem2 = new MatchItem2();
                matchItem2.setProductId(p.getProductId());
                matchItem2.setScore(p.getScore());
                matchItem2.setSource(MatchStrategyConst.UCB);
                result.add(matchItem2);
            }
        } catch (Exception e) {
            log.error("[严重异常][召回源]获取ucb召回源数据异常， uuid {}, uid {}, e ", matchParam.getUuid(), matchParam.getUid(), e);
        }
        return result;
    }

    /**
     * ucb通用召回源处理逻辑
     * @param matchParam
     * @return
     */
    private List<MatchItem2> ucbCommonMatch(MatchParam matchParam){
        List<MatchItem2> result = new ArrayList<>();
        String ucbDataNum = matchParam.getUcbDataNum();
        if(StringUtils.isBlank(ucbDataNum) || ExpFlagsConstants.VALUE_DEFAULT.equals(ucbDataNum)){
            log.error("[严重异常][召回源]ucb通用召回错误，入参UCBDataNum值不能为空或 DEFAULT， uuid {}", matchParam.getUuid());
            return result;
        }

        String[] dataNumArray = ucbDataNum.trim().split(",");
        String dataNum1 = dataNumArray[0];
        String dataNum2 = dataNumArray[0];
        if(dataNumArray.length > 1){
            dataNum2 = dataNumArray[1];
        }
        String source = "ucb"+dataNum1;
        try {
            Integer uid = matchParam.getUid();
            //通过uid从redis中获取召回的商品信息
            String productIds = null;
            if(uid != null && uid > 0){
                String redisKey = AlgorithmRedisKeyConstants.MOSES_MAB_UCB_PREFIX + dataNum1 + "_"+ uid;
                productIds = algorithmRedisUtil.getString(redisKey);
            }

            List<ProductScoreInfo> productScoreInfo = new ArrayList<>();
            if(StringUtils.isNotBlank(productIds)){
                productScoreInfo = StringUtil.parseProductStr(productIds, PID_NUM_MAX_LIMIT, source);
                if(CollectionUtils.isEmpty(productScoreInfo)){
                    log.error("[严重异常][召回源]解析个性化召回源{}商品数据为空，uid {}, uuid {}", source, matchParam.getUid(), matchParam.getUuid());
                }
            }

            if(CollectionUtils.isEmpty(productScoreInfo)) {
                String redisKey;
                String userSex = matchParam.getUserSex() == null ? CommonConstants.UNKNOWN_SEX : matchParam.getUserSex().toString();
                if (CommonConstants.MALE_SEX.equals(userSex)) {
                    redisKey = AlgorithmRedisKeyConstants.MOSES_MAB_UCB_PREFIX + dataNum2+ AlgorithmRedisKeyConstants.MOSES_MALE_SUFFIX;
                } else if(CommonConstants.FEMALE_SEX.equals(userSex)) {
                    redisKey = AlgorithmRedisKeyConstants.MOSES_MAB_UCB_PREFIX + dataNum2+ AlgorithmRedisKeyConstants.MOSES_FEMALE_SUFFIX;
                }else{
                    redisKey = AlgorithmRedisKeyConstants.MOSES_MAB_UCB_PREFIX + dataNum2+ AlgorithmRedisKeyConstants.MOSES_COMMON_SUFFIX;
                }
                String productScoreStr = algorithmRedisUtil.getString(redisKey);
                if(StringUtils.isNotBlank(productScoreStr)){
                    productScoreInfo = StringUtil.parseProductStr(productScoreStr, PID_NUM_MAX_LIMIT, source);
                    if(CollectionUtils.isEmpty(productScoreInfo)){
                        log.error("[严重异常][召回源]解析兜底召回源{}商品数据为空，uid {}, uuid {}", source, matchParam.getUid(), matchParam.getUuid());
                    }
                }
            }

            for (ProductScoreInfo p : productScoreInfo) {
                ProductInfo productInfo = productDetailCache.getProductInfo(p.getProductId());
                if(FilterUtil.isCommonFilter(productInfo)){
                    continue;
                }
                MatchItem2 matchItem2 = new MatchItem2();
                matchItem2.setProductId(p.getProductId());
                matchItem2.setScore(p.getScore());
                matchItem2.setSource(source);
                result.add(matchItem2);
            }
        } catch (Exception e) {
            log.error("[严重异常][召回源]获取召回源{}数据异常， uuid {}, uid {}, e ", source, matchParam.getUuid(), matchParam.getUid(), e);
        }

        return result;
    }
}
