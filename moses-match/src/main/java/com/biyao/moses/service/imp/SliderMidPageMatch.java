package com.biyao.moses.service.imp;

import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.common.constant.CommonConstants;
import com.biyao.moses.common.constant.RedisKeyConstant;
import com.biyao.moses.model.exp.MatchDataSourceTypeConf;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.params.ProductInfo;
import com.biyao.moses.service.RecommendMatch;
import com.biyao.moses.util.FilterUtil;
import com.biyao.moses.util.RedisUtil;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component("sliderMidPageMatch")
public class SliderMidPageMatch implements RecommendMatch{

    @Autowired
    RedisUtil redisUtil;
    @Autowired
    ProductDetailCache productDetailCache;

    private final static int MATCH_NUM = 200;

    @BProfiler(key = "com.biyao.moses.service.imp.SliderMidPageMatch.executeRecommendMatch",
            monitorType = {MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR})
    @Override
    public Map<String, List<TotalTemplateInfo>> executeRecommendMatch(String dataKey, MatchDataSourceTypeConf mdst, String uuId) {
        Map<String, List<TotalTemplateInfo>> result = new HashMap<>();
        List<TotalTemplateInfo> totalTemplateInfoList = new ArrayList<>();

        String priorityProductId = mdst.getPriorityProductId();
        // 如果优先展示商品为空，则直接返回
        if (StringUtils.isBlank(priorityProductId)){
            result.put(dataKey, baseRcdProduct());
            log.error("[一般异常]轮播图落地页match-走托底推荐：{}", uuId);
            return result;
        }
        try {
            ProductInfo priorityProductInfo = productDetailCache.getProductInfo(Long.valueOf(priorityProductId));
            Long supplierId = priorityProductInfo.getSupplierId();
            List<String> fCategory2Ids = priorityProductInfo.getFCategory2Ids();
            String icfRcdProductScoreStr = redisUtil.hgetStr(RedisKeyConstant.MOSES_ICF_P2VEC, priorityProductId);
            List<Long> supplierProductIdList = productDetailCache.getProductIdsBySupplierId(supplierId);
            List<Long> categoryProductIdList = new ArrayList<>();
            if (fCategory2Ids != null && fCategory2Ids.size() > 0) {
                categoryProductIdList = productDetailCache.getProductIdsByFrontendCategory2Id(fCategory2Ids.get(0));
            }

            List<TotalTemplateInfo> icfTotalTemplateInfoList = convert2totalTemplateInfo(icfRcdProductScoreStr);
            icfTotalTemplateInfoList.sort(Comparator.comparing(TotalTemplateInfo::getScore).reversed());

            List<TotalTemplateInfo> supplierTotalTemplateInfoList = convert2totalTemplateInfo(supplierProductIdList);
            supplierTotalTemplateInfoList.sort(Comparator.comparing(TotalTemplateInfo::getScore).reversed());

            List<TotalTemplateInfo> categoryTotalTemplateInfoList = convert2totalTemplateInfo(categoryProductIdList);
            categoryTotalTemplateInfoList.sort(Comparator.comparing(TotalTemplateInfo::getScore).reversed());

            // 组合+去重 先40个icf 10个 same supplier 10 same frontend category2
            Set<String> existsProductIdSet = new HashSet<>(); // 全局去重集合
            List<TotalTemplateInfo> majorTotalTemplateInfoList = new ArrayList<>();
            List<TotalTemplateInfo> minorTotalTemplateInfoList = new ArrayList<>();
            int icfIndex = 0, supplierIndex = 0, categoryIndex = 0;
            /********************************第一轮召回 start**************************************/
            icfIndex = rcdProductList(majorTotalTemplateInfoList, icfTotalTemplateInfoList, existsProductIdSet, 30, 0);
            categoryIndex = rcdProductList(majorTotalTemplateInfoList, categoryTotalTemplateInfoList, existsProductIdSet, 20, 0);
            supplierIndex = rcdProductList(majorTotalTemplateInfoList, supplierTotalTemplateInfoList, existsProductIdSet, 10, 0);
            /********************************第一轮召回 end**************************************/
            /********************************第二轮召回 start**************************************/
            int rcdNum = majorTotalTemplateInfoList.size();
            int targetNum = MATCH_NUM - rcdNum > 60 ? 60 : MATCH_NUM - rcdNum;
            rcdProductList(minorTotalTemplateInfoList, categoryTotalTemplateInfoList, existsProductIdSet, targetNum, categoryIndex);
            rcdNum = majorTotalTemplateInfoList.size() + minorTotalTemplateInfoList.size();
            targetNum = MATCH_NUM - rcdNum > 60 ? 60 : MATCH_NUM - rcdNum;
            rcdProductList(minorTotalTemplateInfoList, supplierTotalTemplateInfoList, existsProductIdSet, targetNum, supplierIndex);
            targetNum = MATCH_NUM - rcdNum;
            rcdProductList(minorTotalTemplateInfoList, icfTotalTemplateInfoList, existsProductIdSet, targetNum, icfIndex);
            /********************************第二轮召回 end**************************************/

            majorTotalTemplateInfoList.sort(Comparator.comparing(TotalTemplateInfo::getScore).reversed());
            totalTemplateInfoList.addAll(majorTotalTemplateInfoList);
            minorTotalTemplateInfoList.sort(Comparator.comparing(TotalTemplateInfo::getScore).reversed());
            totalTemplateInfoList.addAll(minorTotalTemplateInfoList);
            result.put(dataKey, totalTemplateInfoList);
        }catch (Exception e){
            log.error("[严重异常]轮播图落地页match失败：priorityProductId={}, uuid={}", priorityProductId, uuId);
        }
        return result;
    }

    /**
     * 将pid:score,pid:score...转为模板List
     * @param rcdProductScoreStr
     * @return
     */
    private List<TotalTemplateInfo> convert2totalTemplateInfo(String rcdProductScoreStr){
        if (StringUtils.isBlank(rcdProductScoreStr)){
            return new ArrayList<>();
        }
        String[] rcdProductScoreArr = rcdProductScoreStr.split(",");
        List<TotalTemplateInfo> result = new ArrayList<>();
        for (String productScore : rcdProductScoreArr){
            try {
                TotalTemplateInfo totalTemplateInfo = new TotalTemplateInfo();
                String[] productScoreArr = productScore.split(":");
                if (productScoreArr.length != 2){
                    continue;
                }
                totalTemplateInfo.setId(productScoreArr[0]);
                totalTemplateInfo.setScore(Double.valueOf(productScoreArr[1]));
                result.add(totalTemplateInfo);
            }catch (Exception e){
                log.error("[一般异常]轮播图落地页match-icf相似商品转模板失败：{}", productScore);
            }
        }
        return result;

    }

    /**
     * 将商品ID列表转为模板列表
     * @param rcdProductList
     * @return
     */
    private List<TotalTemplateInfo> convert2totalTemplateInfo(List<Long> rcdProductList){
        if (rcdProductList == null || rcdProductList.size() == 0){
            return new ArrayList<>();
        }

        List<TotalTemplateInfo> totalTemplateInfoList = new ArrayList<>();
        for (Long productId : rcdProductList){
            ProductInfo productInfo = productDetailCache.getProductInfo(productId);
            if (productInfo == null || productInfo.getShelfStatus() != 1){
                continue;
            }
            TotalTemplateInfo totalTemplateInfo = new TotalTemplateInfo();
            totalTemplateInfo.setId(productId.toString());
            totalTemplateInfo.setScore(Double.valueOf(productInfo.getSalesVolume7()));
            totalTemplateInfoList.add(totalTemplateInfo);
        }

        return totalTemplateInfoList;
    }

    /**
     * 推荐商品
     * @param targetList
     * @param sourceList
     * @param existsIdSet
     * @param targetNum
     * @param startIndex
     * @return
     */
    private int rcdProductList(List<TotalTemplateInfo> targetList, List<TotalTemplateInfo> sourceList, Set<String> existsIdSet, int targetNum, int startIndex){
        int count = 0, lastIndex = startIndex;
        for (int i=startIndex; i<sourceList.size(); i++){
            lastIndex = i;
            TotalTemplateInfo tempTotalTemplateInfo = sourceList.get(i);
            try {
                if (existsIdSet.contains(tempTotalTemplateInfo.getId())){
                    continue;
                }
                ProductInfo productInfo = productDetailCache.getProductInfo(Long.valueOf(tempTotalTemplateInfo.getId()));
                // 商品不存在 or 商品下架 or 定制商品不推荐
                if(FilterUtil.isCommonFilter(productInfo)){
                    continue;
                }
                tempTotalTemplateInfo.setScore(Double.valueOf(productInfo.getSalesVolume7()));
                targetList.add(tempTotalTemplateInfo);
                existsIdSet.add(tempTotalTemplateInfo.getId());
                count++;
                if (count >= targetNum){
                    break;
                }
            }catch (Exception e){
                log.error("[一般异常]轮播图落地页match-商品推荐失败：{}", tempTotalTemplateInfo.getId());
            }
        }

        return lastIndex;
    }

    private List<TotalTemplateInfo> baseRcdProduct(){
        List<TotalTemplateInfo> result = new ArrayList<>();
        String baseRcdProductStr = redisUtil.getString(CommonConstants.MOSES_TOTAL_HOT_SALE);
        try{
            result = Arrays.stream(baseRcdProductStr.split(",")).map(productId->{
                TotalTemplateInfo totalTemplateInfo = new TotalTemplateInfo();
                totalTemplateInfo.setId(productId.toString());
                totalTemplateInfo.setScore(1.0);
                return totalTemplateInfo;
            }).collect(Collectors.toList());
        }catch (Exception e){
            log.error("[严重异常]轮播图落地页match-托底推荐失败:", e);
        }
        return result;
    }
}
