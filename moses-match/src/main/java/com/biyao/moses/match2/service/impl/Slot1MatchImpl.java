package com.biyao.moses.match2.service.impl;

import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.common.constant.MatchRedisKeyConstant;
import com.biyao.moses.match2.constants.MatchStrategyConst;
import com.biyao.moses.match2.param.MatchParam;
import com.biyao.moses.match2.service.Match2;
import com.biyao.moses.model.match2.MatchItem2;
import com.biyao.moses.params.ProductInfo;
import com.biyao.moses.params.ProductScoreInfo;
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
 * @date 2019/12/31
 * slot1实验槽match
 **/
@Slf4j
@Component(value = MatchStrategyConst.SLOT1)
public class Slot1MatchImpl implements Match2 {
    @Autowired
    private MatchRedisUtil matchRedisUtil;

    private static  final int PID_NUM_MAX_LIMIT = 500;

    @Autowired
    private ProductDetailCache productDetailCache;

    @BProfiler(key = "Slot1MatchImpl.match",
            monitorType = {MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR})
    @Override
    public List<MatchItem2> match(MatchParam matchParam) {

        String uuid = matchParam.getUuid();
        Integer uid = matchParam.getUid();
        List<MatchItem2> result = new ArrayList<>();
        //通过uuid从redis中获取召回的商品信息
        String productIds = null;
        if(uid != null && uid > 0){
            String redisKey = MatchRedisKeyConstant.MOSES_SLOT1_PREFIX + uid;
            productIds = matchRedisUtil.getString(redisKey);
        }

        //如果uid没有获取到数据，则使用uuid查询
        if(StringUtils.isBlank(productIds)) {
            String redisKey = MatchRedisKeyConstant.MOSES_SLOT1_PREFIX + uuid;
            productIds = matchRedisUtil.getString(redisKey);
        }

        if(StringUtils.isBlank(productIds)){
            log.error("[一般异常][召回源]slot1召回源商品数据为空， uid {}， uuid {}", uid, uuid);
            return result;
        }
        List<ProductScoreInfo> productScoreInfos = StringUtil.parseProductStr(productIds, PID_NUM_MAX_LIMIT, MatchStrategyConst.SLOT1);
        if(CollectionUtils.isEmpty(productScoreInfos)){
            log.error("[严重异常][召回源]解析召回源{}商品数据为空，uid {}, uuid {}", MatchStrategyConst.SLOT1, matchParam.getUid(), matchParam.getUuid());
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
            matchItem2.setSource(MatchStrategyConst.SLOT1);
            result.add(matchItem2);
        }

        return result;
    }
}
