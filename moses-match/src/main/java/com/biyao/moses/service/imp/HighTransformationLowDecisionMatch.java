package com.biyao.moses.service.imp;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.model.exp.MatchDataSourceTypeConf;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.params.ProductInfo;
import com.biyao.moses.service.RecommendMatch;
import com.biyao.moses.util.FileUtil;
import com.biyao.moses.util.FilterUtil;
import com.biyao.moses.util.MatchFilterUtil;
import com.biyao.moses.util.RedisUtil;
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
 * 高转化地决策数据 媒体共盈1.0
 */
@Slf4j
@Component("HighTransLowDecMatch")
public class HighTransformationLowDecisionMatch implements RecommendMatch {

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private ProductDetailCache productDetailCache;

    @Autowired
    MatchFilterUtil matchFilterUtil;

    //热销 抽出
    public static final String HOT_PRODUCT_EXPNUM = "moses:10300162_SPM_2000";

    @BProfiler(key = "com.biyao.moses.service.imp.HighTransformationLowDecisionMatch.executeRecommendMatch",
            monitorType = {MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR})
    @Override
    public Map<String, List<TotalTemplateInfo>> executeRecommendMatch(String dataKey, MatchDataSourceTypeConf mdst, String uuId) {

        //获取高转化地决策商品 算法提供 媒体共赢1.0项目使用 key:moses:10300173_ HighTransformationLowDecision _0000
        // value:pid1,pid2,pid3
        List<TotalTemplateInfo> totalList = new ArrayList<>();
        Map<String, List<TotalTemplateInfo>> resultMap = new HashMap<>();
        try {
            String HighTransLowDescStr = redisUtil.getString(dataKey);
            List<Long> HighTransLowDescList = matchFilterUtil.getRcdProductList(HighTransLowDescStr);

            if (CollectionUtils.isNotEmpty(HighTransLowDescList)) {
                for (Long pid : HighTransLowDescList) {
                    TotalTemplateInfo tti = new TotalTemplateInfo();
                    ProductInfo productInfo = productDetailCache.getProductInfo(pid);

                    //通用过滤
                    if (FilterUtil.isCommonFilter(productInfo)) {
                        continue;
                    }

                    tti.setId(pid.toString());
                    totalList.add(tti);
                }
            }

            resultMap.put(dataKey, totalList);
        } catch (Exception e) {
            log.error("媒体共赢1.0高转化地决策match异常", e);
        }

        try {
            //当高转化地决策推不出商品，用热销兜底
            if (totalList.size() < 200) {
                fillWithHotSaleProduct(totalList);
            }
            resultMap.put(dataKey, totalList);
        } catch (Exception e) {
            log.error("高转化地决策中热销兜底方法异常", e);
        }

        return resultMap;
    }

    /**
     * 热销补齐
     *
     * @param totalList
     */
    private void fillWithHotSaleProduct(List<TotalTemplateInfo> totalList) {
        //获取热销数据
        String hotSaleStr = redisUtil.getString(HOT_PRODUCT_EXPNUM);
        List<Long> rcdProductList = matchFilterUtil.getRcdProductList(hotSaleStr);
        //如果不足200个，补足商品
        for (int i = totalList.size(); i < 200; i++) {
            if(i >= rcdProductList.size()){
                break;
            }
            TotalTemplateInfo tti = new TotalTemplateInfo();
            ProductInfo productInfo = productDetailCache.getProductInfo(rcdProductList.get(i));
            //通用过滤
            if(FilterUtil.isCommonFilter(productInfo)){
                continue;
            }
            tti.setId(rcdProductList.get(i).toString());
            totalList.add(tti);
        }

    }
}
