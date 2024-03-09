package com.biyao.moses.rank.impl;

import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.common.constant.RankNameConstants;
import com.biyao.moses.common.constant.RedisKeyConstant;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.params.rank.CommonRankRequest;
import com.biyao.moses.rank.RankRequest;
import com.biyao.moses.rank.RecommendRank;
import com.biyao.moses.params.ProductInfo;
import com.biyao.moses.util.CommonRankUtil;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 通用排序Rank类
 * @author xiaojiankai
 * @date 2019年8月5日
 */
@Slf4j
@Component(RankNameConstants.COMMON_RANK)
public class CommonRank implements RecommendRank {

    @Autowired
    private CommonRankUtil commonRankUtil;

    @Autowired
    private ProductDetailCache productDetailCache;

    @Override
    public List<TotalTemplateInfo> executeRecommend(RankRequest rankRequest) {
        return null;
    }

    @BProfiler(key = "CommonRank.recommendPids", monitorType = {
            MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR })
    public List<Long> recommendPids(CommonRankRequest commonRankRequest){
        List<Long> result = new ArrayList<>();
        String uuid = commonRankRequest.getUuid();
        String uid = commonRankRequest.getUid();
        String sceneId = commonRankRequest.getSceneId();
        String tagId = commonRankRequest.getTagId();
        List<Long> pids = commonRankRequest.getPids();

        //过滤掉不存在的pid
        List<String> filteredProducts = new ArrayList<>();
        List<Long> noExistProducts = new ArrayList<>();
        pids.stream().forEach(p->{
            ProductInfo productInfo = productDetailCache.getProductInfo(Long.valueOf(p));
            if(productInfo == null){
                noExistProducts.add(p);
            }else{
                filteredProducts.add(p.toString());
            }
        });
//        log.error("commonRankRequest {}, uuid={}", JSON.toJSONString(commonRankRequest), uuid);
//        log.error("filteredProducts {}, uuid={}", JSON.toJSONString(filteredProducts), uuid);
//        log.error("noExistProducts {}, uuid={}", JSON.toJSONString(noExistProducts), uuid);
        String exposureRedisKey = RedisKeyConstant.KEY_PREFIEX_COMMON_EXPOSURE+sceneId+RedisKeyConstant.SPLIT_LINE+tagId+RedisKeyConstant.SPLIT_LINE+uuid;
        List<String> sortedPids = commonRankUtil.commonSort(filteredProducts, uuid, uid, exposureRedisKey);
        List<Long> sortedLongPids = sortedPids.stream().map(p -> Long.valueOf(p)).collect(Collectors.toList());
        result.addAll(sortedLongPids);
        result.addAll(noExistProducts);
        return result;
    }
}
