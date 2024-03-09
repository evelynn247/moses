package com.biyao.moses.match2.service.bizimpl;

import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.common.constant.AlgorithmRedisKeyConstants;
import com.biyao.moses.common.constant.BizNameConst;
import com.biyao.moses.match2.constants.MatchStrategyConst;
import com.biyao.moses.match2.service.BizService;
import com.biyao.moses.match2.util.MatchUtil;
import com.biyao.moses.model.match2.MatchItem2;
import com.biyao.moses.params.ProductInfo;
import com.biyao.moses.params.ProductScoreInfo;
import com.biyao.moses.params.match2.MatchRequest2;
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

import java.util.*;
import java.util.stream.Collectors;


/**
 * 小蜜蜂轮播图落地页match2
 */
@Slf4j
@Component(value = BizNameConst.SLIDER_MIDDLE_PAGE3)
public class SliderMiddlePageService2Impl implements BizService {

    @Autowired
    private ProductDetailCache productDetailCache;

    @Autowired
    private FilterUtil filterUtil;

    @Autowired
    private AlgorithmRedisUtil algorithmRedisUtil;

    @Autowired
    private MatchUtil matchUtil;

    /**
     * 新品召回源期望的商品数量
     */
    private static final int NEW_PRODUCT_SOURCE_EXPECT_PID_NUM = 2;

    /**
     * 轮播图落地页期望的商品数量
     */
    private static final int EXPECT_PID_NUM = 200;


    @BProfiler(key = "SliderMiddlePageService2Impl.match",
            monitorType = {MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR})
    @Override
    public List<MatchItem2> match(MatchRequest2 request) {
        List<MatchItem2> resultList = new ArrayList<>();
        //点击的轮播图商品
        String priorityProductId = request.getPriorityProductId();
        String siteIdStr= request.getSiteId() == null ? null : String.valueOf(request.getSiteId());
        if (StringUtils.isBlank(priorityProductId)) {
            log.error("[严重异常][轮播图落地页]入参中轮播图商品id为空，sid {}，uuid {}", request.getSid(), request.getUuid());
            return resultList;
        }

        ProductInfo productInfo = productDetailCache.getProductInfo(Long.valueOf(priorityProductId));
        if (productInfo == null) {
            log.error("[严重异常][轮播图落地页]根据入参中轮播图商品id获取不到商品详细信息，sid {}，uuid {}", request.getSid(), request.getUuid());
            return resultList;
        }

        Set<Long> hadSourcePidSet = new HashSet<>();
        //获取新品召回源
        List<MatchItem2> newProductSourceDataList = getNewProductSource(productInfo, siteIdStr, hadSourcePidSet);
        resultList.addAll(newProductSourceDataList);

        int targetNum = EXPECT_PID_NUM - resultList.size();
        if(targetNum > 0) {
            //获取相似商品召回源
            List<MatchItem2> likeSourceDataList = getLikeSourceData(productInfo, siteIdStr, hadSourcePidSet);
            fillSpecifiedNum(resultList, likeSourceDataList, targetNum);
        }

        targetNum = EXPECT_PID_NUM - resultList.size();
        if(targetNum > 0){
            //获取相同后台三级类目召回源
            List<MatchItem2> thirdCategorySourceDataList = getThirdCategorySourceData(productInfo, siteIdStr, hadSourcePidSet);
            fillSpecifiedNum(resultList, thirdCategorySourceDataList, targetNum);
        }

        targetNum = EXPECT_PID_NUM - resultList.size();
        if(targetNum > 0){
            //获取相同后台二级类目召回源
            List<MatchItem2> thirdCategorySourceDataList = getSecondCategorySourceData(productInfo, siteIdStr, hadSourcePidSet);
            fillSpecifiedNum(resultList, thirdCategorySourceDataList, targetNum);
        }

        //按照排名填充商品分，并聚合商品分
        matchUtil.calculateAndFillScore(resultList, EXPECT_PID_NUM, new HashMap<>());

        //按商品分进行排序
        resultList = resultList.stream().sorted((m1, m2) -> -m1.getScore().compareTo(m2.getScore())).collect(Collectors.toList());
        return resultList;
    }

    /**
     * 获取后台三级类目召回源数据
     * @param priorityProductInfo
     * @param siteId
     * @param hadSourcePidSet
     * @return
     */
    private List<MatchItem2> getThirdCategorySourceData(ProductInfo priorityProductInfo, String siteId, Set<Long> hadSourcePidSet){

        if(priorityProductInfo == null || priorityProductInfo.getThirdCategoryId() == null){
            return new ArrayList<>();
        }
        List<Long> thirdCategoryPidList = null;
        try {
            Long thirdCategoryId = priorityProductInfo.getThirdCategoryId();
            thirdCategoryPidList = productDetailCache.getProductIdsByCategoryId(thirdCategoryId);
            thirdCategoryPidList = filterAndSortCategorySourceData(thirdCategoryPidList, siteId, hadSourcePidSet);
        }catch (Exception e){
            log.error("[严重异常][轮播图落地页]处理后台三级类目召回源时发生异常， priorityPid {}， e", priorityProductInfo.getProductId());
        }
        return convertToMatchItem2(thirdCategoryPidList, MatchStrategyConst.LBTLDY_CATE3_HOT);
    }

    /**
     * 获取后台二级类目召回源数据
     * @param priorityProductInfo
     * @param siteId
     * @param hadSourcePidSet
     * @return
     */
    private List<MatchItem2> getSecondCategorySourceData(ProductInfo priorityProductInfo, String siteId, Set<Long> hadSourcePidSet){
        if(priorityProductInfo == null || priorityProductInfo.getSecondCategoryId() == null){
            return new ArrayList<>();
        }

        List<Long> secondCategoryPidList = null;
        try {
            Long secondCategoryId = priorityProductInfo.getSecondCategoryId();
            secondCategoryPidList = productDetailCache.getCategory2Product(secondCategoryId);
            secondCategoryPidList = filterAndSortCategorySourceData(secondCategoryPidList, siteId, hadSourcePidSet);
        }catch (Exception e){
            log.error("[严重异常][轮播图落地页]处理后台二级类目召回源时发生异常， priorityPid {}， e", priorityProductInfo.getProductId());
        }
        return convertToMatchItem2(secondCategoryPidList, MatchStrategyConst.LBTLDY_CATE2_HOT);
    }

    /**
     * 对类目召回源数据过滤和排序（包括后台三级类目、后台二级类目）
     * @param categoryPidList
     * @param siteId
     * @param hadSourcePidSet
     * @return
     */
    private List<Long> filterAndSortCategorySourceData(List<Long> categoryPidList, String siteId, Set<Long> hadSourcePidSet){
        if(CollectionUtils.isEmpty(categoryPidList)){
            return new ArrayList<>();
        }

        List<Long> fitCategoryList = categoryPidList.stream().filter(pid -> {
            if (!isFitLbtldy(pid, siteId)) {
                return false;
            }

            if (hadSourcePidSet.contains(pid)) {
                return false;
            }
            return true;
        }).sorted((pid1, pid2) -> {
            ProductInfo productInfo1 = productDetailCache.getProductInfo(pid1);
            ProductInfo productInfo2 = productDetailCache.getProductInfo(pid2);
            return productInfo2.getSalesVolume7().compareTo(productInfo1.getSalesVolume7());
        }).collect(Collectors.toList());

        hadSourcePidSet.addAll(fitCategoryList);
        return fitCategoryList;
    }

    /**
     * 获取相似商品召回源数据
     * @param priorityProductInfo
     * @param siteId
     * @param hadSourcePidSet
     * @return
     */
    private List<MatchItem2> getLikeSourceData(ProductInfo priorityProductInfo, String siteId, Set<Long> hadSourcePidSet){
        List<Long> fitLikePidList = null;
        try {
            String priorityProductId = priorityProductInfo.getProductId().toString();
            String likeProductsStr = algorithmRedisUtil.getString(AlgorithmRedisKeyConstants.MOSES_LBTLDY_SIMILAR_PREFIX + priorityProductId);
            List<ProductScoreInfo> productScoreInfoList = StringUtil.parseProductStr(likeProductsStr, -1, MatchStrategyConst.LBTLDY_LIKE);
            if (CollectionUtils.isEmpty(productScoreInfoList)) {
                return new ArrayList<>();
            }

            fitLikePidList = productScoreInfoList.stream().filter(productScoreInfo -> {
                Long pid = productScoreInfo.getProductId();
                if (!isFitLbtldy(pid, siteId)) {
                    return false;
                }

                if (hadSourcePidSet.contains(pid)) {
                    return false;
                }
                return true;
            }).sorted(Comparator.comparing(ProductScoreInfo::getScore).reversed())
                    .map(ProductScoreInfo::getProductId)
                    .collect(Collectors.toList());

            hadSourcePidSet.addAll(fitLikePidList);
        }catch (Exception e){
            log.error("[严重异常][轮播图落地页]处理相似商品召回源时发生异常， priorityPid {}， e", priorityProductInfo.getProductId());
        }
        return convertToMatchItem2(fitLikePidList, MatchStrategyConst.LBTLDY_LIKE);
    }

    /**
     * 获取新品召回源数据
     * @param priorityProductInfo
     * @param siteId
     * @param hadSourcePidSet 已召回的商品集合
     * @return
     */
    private List<MatchItem2> getNewProductSource(ProductInfo priorityProductInfo, String siteId, Set<Long> hadSourcePidSet){

        List<MatchItem2> result = new ArrayList<>();
        int target_num = NEW_PRODUCT_SOURCE_EXPECT_PID_NUM;
        try {
            List<MatchItem2> beeNewProductList = getFitNewProduct(productDetailCache.getNewBeeProductList(), priorityProductInfo, siteId, hadSourcePidSet, target_num);
            if (CollectionUtils.isNotEmpty(beeNewProductList)) {
                result.addAll(beeNewProductList);
                target_num = target_num - beeNewProductList.size();
            }

            if (target_num <= 0) {
                return result;
            }
            List<MatchItem2> notBeeNewProductList = getFitNewProduct(productDetailCache.getNewProductWithoutBeeList(), priorityProductInfo, siteId, hadSourcePidSet, target_num);
            if (CollectionUtils.isNotEmpty(notBeeNewProductList)) {
                result.addAll(notBeeNewProductList);
            }
        }catch (Exception e){
            log.error("[严重异常][轮播图落地页]处理新品召回源时发生异常， priorityPid {}， e", priorityProductInfo.getProductId());
        }
        return result;
    }

    /**
     * 获取满足条件的新品
     * @param newProductList
     * @param priorityProductInfo
     * @param siteId
     * @param hadSourcePidSet
     * @return
     */
    private List<MatchItem2> getFitNewProduct(List<Long> newProductList, ProductInfo priorityProductInfo, String siteId, Set<Long> hadSourcePidSet, int targetNum){
        List<MatchItem2> result = new ArrayList<>();
        if(CollectionUtils.isEmpty(newProductList) || targetNum <= 0){
            return result;
        }

        List<Long> fitNewProductIdList = new ArrayList<>();
        Long secondCategoryId = priorityProductInfo.getSecondCategoryId();
        for (Long pid : newProductList) {
            ProductInfo productInfo = productDetailCache.getProductInfo(pid);
            if(!secondCategoryId.equals(productInfo.getSecondCategoryId())){
                continue;
            }

            if (!isFitLbtldy(pid, siteId)) {
                continue;
            }

            if(hadSourcePidSet.contains(pid)){
                continue;
            }

            fitNewProductIdList.add(pid);
        }

        if(CollectionUtils.isEmpty(fitNewProductIdList)){
            return result;
        }

        Collections.shuffle(fitNewProductIdList);
        if(fitNewProductIdList.size() >= targetNum){
            fitNewProductIdList = fitNewProductIdList.subList(0, targetNum);
        }

        hadSourcePidSet.addAll(fitNewProductIdList);

        return convertToMatchItem2(fitNewProductIdList, MatchStrategyConst.LBTLDY_NEW_PRODUCT);
    }

    /**
     * 将pid集合转化为MatchItem2集合
     * @param pidList
     * @param source
     * @return
     */
    private List<MatchItem2> convertToMatchItem2(List<Long> pidList, String source){
        List<MatchItem2> result = new ArrayList<>();
        if(CollectionUtils.isEmpty(pidList)){
            return result;
        }

        for(Long pid : pidList){
            MatchItem2 matchItem2 = new MatchItem2();
            matchItem2.setSource(source);
            matchItem2.setProductId(pid);
            result.add(matchItem2);
        }
        return result;
    }

    /**
     * 判断该商品是否满足轮播图落地页的展示规则
     * @param productId
     * @param siteId
     * @return true: 满足，false: 不满足
     */
    private boolean isFitLbtldy(Long productId, String siteId){
        ProductInfo productInfo = productDetailCache.getProductInfo(productId);
        if(FilterUtil.isCommonFilter(productInfo)){
            return false;
        }

        if(filterUtil.isFilteredBySiteId(productId, siteId)){
            return false;
        }
        return true;
    }

    /**
     * 将原链表中指定数量的节点放入到目标链表中
     * @param descList
     * @param sourceList
     * @param specifiedNum
     */
    private void fillSpecifiedNum(List<MatchItem2> descList, List<MatchItem2> sourceList, int specifiedNum){
        if(CollectionUtils.isEmpty(sourceList) || specifiedNum <= 0){
            return;
        }

        if(sourceList.size() > specifiedNum){
            descList.addAll(sourceList.subList(0, specifiedNum));
        }else{
            descList.addAll(sourceList);
        }

    }
}
