package com.biyao.moses.match2.service.impl;

import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.common.constant.AlgorithmRedisKeyConstants;
import com.biyao.moses.common.constant.MatchRedisKeyConstant;
import com.biyao.moses.match2.constants.MatchStrategyConst;
import com.biyao.moses.match2.param.MatchParam;
import com.biyao.moses.match2.service.Match2;
import com.biyao.moses.model.match2.MatchItem2;
import com.biyao.moses.params.ProductInfo;
import com.biyao.moses.params.ProductScoreInfo;
import com.biyao.moses.util.AlgorithmRedisUtil;
import com.biyao.moses.util.FilterUtil;
import com.biyao.moses.util.MatchRedisUtil;
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
 * @date 2020/2/14
 * acrec基础召回源
 **/
@Slf4j
@Component(value = MatchStrategyConst.ACREC)
public class AcrecMatchImpl implements Match2 {
    @Autowired
    private AlgorithmRedisUtil algorithmRedisUtil;

    private static  final int PID_NUM_MAX_LIMIT = 500;

    @Autowired
    private ProductDetailCache productDetailCache;

    @BProfiler(key = "AcrecMatchImpl.match",
            monitorType = {MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR})
    @Override
    public List<MatchItem2> match(MatchParam matchParam) {

        String uuid = matchParam.getUuid();
        Integer uid = matchParam.getUid();
        List<MatchItem2> result = new ArrayList<>();
        //通过uid从redis中获取召回的商品信息
        String productIds = null;
        if(uid != null && uid > 0){
            String redisKey = AlgorithmRedisKeyConstants.MOSES_ACREC_PREFIX + uid;
            productIds = algorithmRedisUtil.getString(redisKey);
        }

        //如果uuid没有获取到数据，则使用uuid查询
        if(StringUtils.isBlank(productIds)) {
            String redisKey = AlgorithmRedisKeyConstants.MOSES_ACREC_PREFIX + uuid;
            productIds = algorithmRedisUtil.getString(redisKey);
        }

        if(StringUtils.isBlank(productIds)){
            log.error("[一般异常][召回源]acrec召回源商品数据为空， uid {}， uuid {}", uid, uuid);
            return result;
        }
        List<ProductScoreInfo> productScoreInfos = StringUtil.parseProductStr(productIds, PID_NUM_MAX_LIMIT, MatchStrategyConst.ACREC);
        if(CollectionUtils.isEmpty(productScoreInfos)){
            log.error("[严重异常][召回源]解析召回源{}商品数据为空，uid {}, uuid {}", MatchStrategyConst.ACREC, matchParam.getUid(), matchParam.getUuid());
            return result;
        }

        for (ProductScoreInfo p : productScoreInfos) {
            ProductInfo productInfo = productDetailCache.getProductInfo(p.getProductId());
            if(FilterUtil.isCommonFilter(productInfo)){
                continue;
            }
            MatchItem2 matchItem2 = new MatchItem2();
            matchItem2.setProductId(p.getProductId());
            matchItem2.setScore(p.getScore());
            matchItem2.setSource(MatchStrategyConst.ACREC);
            result.add(matchItem2);
        }

        return result;
    }
}
