package com.biyao.moses.match2.service.impl;

import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.common.constant.AlgorithmRedisKeyConstants;
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
 * 双向编码推荐bert召回源
 **/
@Slf4j
@Component(value = MatchStrategyConst.BERT)
public class BertMatchImpl implements Match2 {
    @Autowired
    private AlgorithmRedisUtil algorithmRedisUtil;

    private static  final int PID_NUM_MAX_LIMIT = 500;

    @Autowired
    private ProductDetailCache productDetailCache;

    @BProfiler(key = "BertMatchImpl.match",
            monitorType = {MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR})
    @Override
    public List<MatchItem2> match(MatchParam matchParam) {

        String uuid = matchParam.getUuid();
        Integer uid = matchParam.getUid();
        List<MatchItem2> result = new ArrayList<>();
        //通过uuid从redis中获取召回的商品信息
        String redisKey = AlgorithmRedisKeyConstants.MOSES_BERT_PREFIX;
        if(uid != null && uid > 0){
            redisKey = redisKey + uid;
        }else {
            redisKey = redisKey + uuid;
        }

        String productIds = algorithmRedisUtil.getString(redisKey);
        if(StringUtils.isBlank(productIds)){
            log.error("[一般异常][召回源]bert召回源商品数据为空, redisKey {}", redisKey);
            return result;
        }
        List<ProductScoreInfo> productScoreInfos = StringUtil.parseProductStr(productIds, PID_NUM_MAX_LIMIT, MatchStrategyConst.BERT);
        if(CollectionUtils.isEmpty(productScoreInfos)){
            log.error("[严重异常][召回源]解析召回源{}商品数据为空，uid {}, uuid {}", MatchStrategyConst.BERT, matchParam.getUid(), matchParam.getUuid());
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
            matchItem2.setSource(MatchStrategyConst.BERT);
            result.add(matchItem2);
        }

        return result;
    }
}
