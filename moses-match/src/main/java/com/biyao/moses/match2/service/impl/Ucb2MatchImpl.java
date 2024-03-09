package com.biyao.moses.match2.service.impl;

import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.cache.UcbProductCache;
import com.biyao.moses.common.constant.AlgorithmRedisKeyConstants;
import com.biyao.moses.common.constant.CommonConstants;
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

import static org.bouncycastle.asn1.x500.style.RFC4519Style.uid;

/**
 * @author xiaojiankai@idstaff.com
 * @date 2020/2/14
 * ucb2（多臂赌博机）召回源优化后的召回源
 **/
@Slf4j
@Component(value = MatchStrategyConst.UCB2)
public class Ucb2MatchImpl implements Match2 {
    @Autowired
    private AlgorithmRedisUtil algorithmRedisUtil;

    @Autowired
    private UcbProductCache ucbProductCache;

    @Autowired
    private ProductDetailCache productDetailCache;

    private static  final int PID_NUM_MAX_LIMIT = 500;

    @BProfiler(key = "Ucb2MatchImpl.match",
            monitorType = {MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR})
    @Override
    public List<MatchItem2> match(MatchParam matchParam) {
        List<MatchItem2> result = new ArrayList<>();
        try {

            Integer uid = matchParam.getUid();
            //通过uid从redis中获取召回的商品信息
            String productIds = null;
            if(uid != null && uid > 0){
                String redisKey = AlgorithmRedisKeyConstants.MOSES_MAB_UCB2_PREFIX + uid;
                productIds = algorithmRedisUtil.getString(redisKey);
            }

            List<ProductScoreInfo> productScoreInfo = new ArrayList<>();
            if(StringUtils.isNotBlank(productIds)){
                productScoreInfo = StringUtil.parseProductStr(productIds, PID_NUM_MAX_LIMIT, MatchStrategyConst.UCB2);
                if(CollectionUtils.isEmpty(productScoreInfo)){
                    log.error("[一般异常][召回源]解析召回源{}商品数据为空，uid {}, uuid {}", MatchStrategyConst.UCB2, matchParam.getUid(), matchParam.getUuid());
                }
            }

            if(CollectionUtils.isEmpty(productScoreInfo)) {
                String userSex;
                if (matchParam.getUserSex() == null) {
                    userSex = CommonConstants.UNKNOWN_SEX;
                } else {
                    userSex = matchParam.getUserSex().toString();
                }
                productScoreInfo = ucbProductCache.getProductScoreInfoForUcb2BySex(userSex);
                if (CollectionUtils.isEmpty(productScoreInfo)) {
                    log.error("[严重异常][召回源]从缓存中获取ucb2召回源数据为空， uuid {}, uid {}", matchParam.getUuid(), matchParam.getUid());
                    return result;
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
                matchItem2.setSource(MatchStrategyConst.UCB2);
                result.add(matchItem2);
            }
        } catch (Exception e) {
            log.error("[严重异常][召回源]获取ucb2召回源数据异常， uuid {}, uid {}, e ", matchParam.getUuid(), matchParam.getUid(), e);
        }
        return result;
    }
}
