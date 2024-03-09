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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author xiaojiankai@idstaff.com
 * @date 2020/03/27
 * bert2召回源，复购提升专项V2.0-加价购及相关优化项目新增
 **/
@Slf4j
@Component(value = MatchStrategyConst.BERT2)
public class Bert2MatchImpl implements Match2 {
    @Autowired
    private AlgorithmRedisUtil algorithmRedisUtil;

    private static  final int PID_NUM_MAX_LIMIT = 500;

    @Autowired
    private ProductDetailCache productDetailCache;

    @BProfiler(key = "Bert2MatchImpl.match",
            monitorType = {MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR})
    @Override
    public List<MatchItem2> match(MatchParam matchParam) {

        String uuid = matchParam.getUuid();
        Integer uid = matchParam.getUid();
        List<String> pidList = matchParam.getPidList();
        List<MatchItem2> result = new ArrayList<>();
        //通过uuid从redis中获取召回的商品信息
        String redisKey = AlgorithmRedisKeyConstants.MOSES_BERT2_PREFIX;
        if(uid != null && uid > 0){
            redisKey = redisKey + uid;
        }else {
            redisKey = redisKey + uuid;
        }
        List<ProductScoreInfo> productScoreInfo1 = new ArrayList<>();
        String productIds = algorithmRedisUtil.getString(redisKey);
        if(StringUtils.isBlank(productIds)){
            log.error("[一般异常][召回源]bert2召回源用户感兴趣部分商品数据为空, redisKey {}", redisKey);
        }else {
            productScoreInfo1 = StringUtil.parseProductStr(productIds, PID_NUM_MAX_LIMIT, MatchStrategyConst.BERT2);
            if (CollectionUtils.isEmpty(productScoreInfo1)) {
                log.error("[严重异常][召回源]解析召回源{}用户感兴趣部分商品数据为空，uid {}, uuid {}", MatchStrategyConst.BERT2, matchParam.getUid(), matchParam.getUuid());
            }
        }

        List<List<ProductScoreInfo>> productScoreInfoList = new ArrayList<>();
        if(CollectionUtils.isNotEmpty(pidList)) {
            List<String> similarPidScoreStrList = algorithmRedisUtil.hmget(AlgorithmRedisKeyConstants.MOSES_BERT2_SIMILAR, pidList.toArray(new String[pidList.size()]));
            if(CollectionUtils.isNotEmpty(similarPidScoreStrList)){
                for(String pidScoreStr : similarPidScoreStrList) {
                    List<ProductScoreInfo> productScoreInfoTmp = StringUtil.parseProductStr(pidScoreStr, PID_NUM_MAX_LIMIT, "bert2_similar");
                    productScoreInfoList.add(productScoreInfoTmp);
                }
            }
        }
        List<ProductScoreInfo> productScoreInfos = aggProductScoreInfo(productScoreInfo1, productScoreInfoList);
        for (ProductScoreInfo p : productScoreInfos) {
            ProductInfo productInfo = productDetailCache.getProductInfo(p.getProductId());
            if(FilterUtil.isCommonFilter(productInfo)){
                continue;
            }
            MatchItem2 matchItem2 = new MatchItem2();
            matchItem2.setProductId(p.getProductId());
            matchItem2.setScore(p.getScore());
            matchItem2.setSource(MatchStrategyConst.BERT2);
            result.add(matchItem2);
        }

        return result;
    }

    /**
     * 将用户感兴趣商品 和 通过传入的pid查找到的相似商品聚合
     * @param productScoreInfo
     * @param productScoreList
     * @return
     */
    private List<ProductScoreInfo> aggProductScoreInfo(List<ProductScoreInfo> productScoreInfo, List<List<ProductScoreInfo>> productScoreList){
        Map<Long, ProductScoreInfo> productScoreInfoMap = new HashMap<>();
        if(CollectionUtils.isNotEmpty(productScoreInfo)){
            for(ProductScoreInfo pidInfo : productScoreInfo){
                if(pidInfo == null || pidInfo.getProductId() == null){
                    continue;
                }
                productScoreInfoMap.put(pidInfo.getProductId(), pidInfo);
            }
        }

        if(CollectionUtils.isNotEmpty(productScoreList)){
            for(List<ProductScoreInfo> list : productScoreList){
                if(CollectionUtils.isEmpty(list)){
                    continue;
                }
                for(ProductScoreInfo pidInfo : list){
                    if(pidInfo == null || pidInfo.getProductId() == null){
                        continue;
                    }
                    Long productId = pidInfo.getProductId();
                    if(productScoreInfoMap.containsKey(productId)){
                        ProductScoreInfo productScoreInfo1 = productScoreInfoMap.get(productId);
                        productScoreInfo1.setScore(productScoreInfo1.getScore()+pidInfo.getScore());
                    }else{
                        productScoreInfoMap.put(pidInfo.getProductId(), pidInfo);
                    }
                }
            }
        }

        List<ProductScoreInfo> result = new ArrayList<>(productScoreInfoMap.values());
        return result;
    }
}
