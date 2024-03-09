package com.biyao.moses.match2.service.bizimpl;

import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.cache.SliderProductCache;
import com.biyao.moses.common.constant.BizNameConst;
import com.biyao.moses.common.constant.CommonConstants;
import com.biyao.moses.common.constant.UPCUserTypeConstants;
import com.biyao.moses.match2.constants.MatchStrategyConst;
import com.biyao.moses.match2.param.MatchParam;
import com.biyao.moses.match2.service.BizService;
import com.biyao.moses.match2.util.MatchUtil;
import com.biyao.moses.model.match2.MatchItem2;
import com.biyao.moses.params.ProductInfo;
import com.biyao.moses.params.match2.MatchRequest2;
import com.biyao.moses.rpc.UcRpcService;
import com.biyao.moses.service.imp.AsyncMatchService;
import com.biyao.moses.util.FilterUtil;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import com.uc.domain.bean.User;
import com.uc.domain.constant.UserFieldConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 首页轮播图-推荐V2.24.0-轮播图及分类页优化项目新增
 */
@Slf4j
@Component(value = BizNameConst.SLIDER_PICTURE3)
public class SliderPicture2ServiceImpl implements BizService {

    @Autowired
    private SliderProductCache sliderProductCache;

    @Autowired
    private ProductDetailCache productDetailCache;

    @Autowired
    private AsyncMatchService asyncMatchService;

    @Autowired
    private UcRpcService ucRpcService;

    @Autowired
    private MatchUtil matchUtil;

    @Autowired
    private FilterUtil filterUtil;

    /**
     *  轮播图match 期望推出的商品数
     */
    private final int EXPECT_PID_NUM = 100;

    /**
     * 后台二级类目下有效商品的数量下限
     */
    private final int CATE2_VALID_PID_NUM_LOWER_LIMIT = 30;

    /**
     * 小蜜蜂新品召回源期望召回商品数量上限
     */
    private final int CATEXMF_EXPECT_PID_NUM_UP_LIMIT = 5;

    /**
     * 普通新品召回源期望召回商品数量上限
     */
    private final int CATEXP_EXPECT_PID_NUM_UP_LIMIT = 5;

    @BProfiler(key = "SliderPicture2ServiceImpl.match",
            monitorType = {MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR})
    @Override
    public List<MatchItem2> match(MatchRequest2 request) {

        List<MatchItem2> resultList = new ArrayList<>();
        int targetNum = EXPECT_PID_NUM;
        //创建召回源权重配置信息
        Map<String, Double> allSourceAndWeight = createAllSourceAndWeightConfig();

        Map<String, String> sourceDataStrategyMap = new HashMap<>();
        Map<String, String> sourceRedisMap = new HashMap<>();
        List<String> orderedSourceList = new ArrayList<>();
        //创建召回源配置信息
        createSourceConfigInfo(orderedSourceList, sourceDataStrategyMap, sourceRedisMap);

        //后台二级类目下有效的商品集合缓存
        Map<Long, List<Long>> cate2ValidPidCacheMap = new HashMap<>();
        try {
            //获取召回源中使用的uc属性数据
            User ucUser = getUcUser(request, allSourceAndWeight.keySet());


            Integer upcUserType = request.getUpcUserType();
            if (UPCUserTypeConstants.CUSTOMER == upcUserType) {
                //获取小蜜蜂新品和非小蜜蜂新品召回商品信息
                resultList.addAll(getNewProductSourceData(ucUser, request, cate2ValidPidCacheMap));
            }

            targetNum = targetNum - resultList.size();
            List<MatchItem2> orderedSourceListData = getOrderedSourceListData(ucUser, request, cate2ValidPidCacheMap,
                    orderedSourceList, sourceDataStrategyMap, sourceRedisMap, targetNum);
            if (CollectionUtils.isNotEmpty(orderedSourceListData)) {
                targetNum = targetNum - orderedSourceListData.size();
                resultList.addAll(orderedSourceListData);
            }

        }catch (Exception e){
            log.error("[严重异常][首页轮播图]获取新首页轮播图召回数据时出现异常，sid {}，uuid {}，e", request.getSid(), request.getUuid(), e);
        }

        if (targetNum > 0) {
            resultList.addAll(getLbtHotSourceData(request, cate2ValidPidCacheMap, targetNum));
        }

        //按照排名填充商品分，并聚合商品分
        matchUtil.calculateAndFillScore(resultList, EXPECT_PID_NUM, allSourceAndWeight);

        //按商品分进行排序
        resultList = resultList.stream().sorted((m1, m2) -> -m1.getScore().compareTo(m2.getScore())).collect(Collectors.toList());

        return resultList;
    }


    /**
     * 根据uc中一次性获取所有召回源所需的信息
     * @param request
     * @param sourceSet
     * @return
     */
    private User getUcUser(MatchRequest2 request, Set<String> sourceSet){
        String uuid = request.getUuid();
        String uidStr = null;
        Integer uid = request.getUid();
        if(uid != null && uid > 0){
            uidStr = String.valueOf(uid);
        }
        Set<String> ucFieldSet = matchUtil.getUcFieldBySource(sourceSet);
        if(UPCUserTypeConstants.CUSTOMER == request.getUpcUserType()){
            ucFieldSet.add(UserFieldConstants.LEVEL3HOBBY);
        }
        if(CollectionUtils.isEmpty(ucFieldSet)){
            return null;
        }
        List<String> ucFieldList = new ArrayList<>(ucFieldSet);
        return ucRpcService.getData(uuid, uidStr, ucFieldList, "mosesmatch");
    }

    /**
     * 获取所有的召回源及其权重
     * @return
     */
    private Map<String, Double> createAllSourceAndWeightConfig(){
        Map<String, Double> sourceAndWeight = new HashMap<>();
        sourceAndWeight.put(MatchStrategyConst.CATEXMF, 2d);
        sourceAndWeight.put(MatchStrategyConst.CATEXP, 1d);
        sourceAndWeight.put(MatchStrategyConst.ACREC, 1d);
        sourceAndWeight.put(MatchStrategyConst.BERT, 0.5d);
        sourceAndWeight.put(MatchStrategyConst.IBCF, 0.5d);
        sourceAndWeight.put(MatchStrategyConst.TAG, 0.5d);
        sourceAndWeight.put(MatchStrategyConst.LBTHOT, 0.1d);
        return sourceAndWeight;
    }

    /**
     * 获取算法召回源的配置信息，包括召回源间排序结果、召回源的数据获取策略以及召回源数据存储的redis信息
     * @return
     */
    private void createSourceConfigInfo(List<String> orderedSourceList, Map<String, String> sourceDataStrategyMap,
                                        Map<String, String> sourceRedisMap){
        orderedSourceList.add(MatchStrategyConst.ACREC);
        orderedSourceList.add(MatchStrategyConst.BERT);
        orderedSourceList.add(MatchStrategyConst.IBCF);
        orderedSourceList.add(MatchStrategyConst.TAG);
        //上面配置的四个召回源走默认的数据获取策略，因此数据获取策略sourceDataStrategyMap不用配置
        //上面配置的四个召回源不需要配置数据存储的redis信息，因此sourceRedisMap不用配置
        //备注：如后续添加召回源，则需考虑此处是否需要添加对应的配置信息
    }

    /**
     * 是否满足轮播图推出的条件
     * @param productId
     * @param request
     * @return true 满足推出的条件；false 不满足推出的条件
     */
    private boolean isFitLbtCondition(Long productId, MatchRequest2 request, Map<Long, List<Long>> cate2ValidPidCacheMap){
        ProductInfo productInfo = productDetailCache.getProductInfo(productId);
        //下架、定制商品（定制咖啡除外）不满足推出条件
        if(FilterUtil.isCommonFilter(productInfo)){
            return false;
        }

        String siteIdStr = request.getSiteId() == null ? null : String.valueOf(request.getSiteId());
        // 不支持用户所持端的商品，则不满足推出条件
        if(filterUtil.isFilteredBySiteId(productId, siteIdStr)){
            return false;
        }

        //无轮播图商品，不满足推出条件
        if(sliderProductCache.getProductImageById(productId) == null){
            return false;
        }

        //该商品对应后台二级类目上架、未被隐藏、非定制商品的数量小于30，则不满足推出条件
        List<Long> cate2ValidProductIdList = getCate2ValidProductIdList(productInfo.getSecondCategoryId(), siteIdStr, cate2ValidPidCacheMap);
        if(CollectionUtils.isEmpty(cate2ValidProductIdList) || cate2ValidProductIdList.size() < CATE2_VALID_PID_NUM_LOWER_LIMIT){
            return false;
        }

        return true;
    }

    /**
     * 获取该后台二级类目下有效的商品集合
     * @param category2Id
     * @param siteId
     * @param cate2ValidPidCacheMap 必须不为空
     * @return
     */
    private List<Long> getCate2ValidProductIdList(Long category2Id, String siteId, Map<Long, List<Long>> cate2ValidPidCacheMap){
        //先从缓存中获取该后台二级类目下的有效商品集合
        if(cate2ValidPidCacheMap != null && cate2ValidPidCacheMap.containsKey(category2Id)){
            return cate2ValidPidCacheMap.get(category2Id);
        }

        List<Long> result = new ArrayList<>();
        List<Long> category2ProductIdList = productDetailCache.getCategory2Product(category2Id);
        if(CollectionUtils.isEmpty(category2ProductIdList)){
            return result;
        }

        for(Long pid : category2ProductIdList){
            ProductInfo productInfo = productDetailCache.getProductInfo(pid);
            if(FilterUtil.isCommonFilter(productInfo)){
                continue;
            }

            if(filterUtil.isFilteredBySiteId(pid, siteId)){
                continue;
            }
            result.add(pid);
        }

        //将结果更新到缓存中
        if(cate2ValidPidCacheMap != null){
            cate2ValidPidCacheMap.put(category2Id, result);
        }
        return result;
    }

    /**
     * 新品（小蜜蜂新品和非小蜜蜂新品）的召回规则
     * @param newProductList
     * @param ucUser
     * @param request
     * @param cate2ValidPidCacheMap
     * @return
     */
    private List<Long> getNewProductByRule(List<Long> newProductList, User ucUser, MatchRequest2 request, Map<Long, List<Long>> cate2ValidPidCacheMap){
        List<Long> result = new ArrayList<>();
        if(CollectionUtils.isEmpty(newProductList)){
            return result;
        }

        if(ucUser == null || ucUser.getLevel3Hobby() == null || ucUser.getLevel3Hobby().size() == 0){
            return result;
        }

        Map<String, BigDecimal> level3Hobby = ucUser.getLevel3Hobby();

        Integer userSex = request.getUserSex();

        for(Long pid : newProductList){
            ProductInfo productInfo = productDetailCache.getProductInfo(pid);
            if(productInfo == null || productInfo.getThirdCategoryId() == null){
                continue;
            }

            if(!level3Hobby.containsKey(productInfo.getThirdCategoryId().toString())){
                continue;
            }

            if(MatchUtil.isFilterBySex(productInfo, userSex)){
                continue;
            }

            if(!isFitLbtCondition(pid, request, cate2ValidPidCacheMap)){
                continue;
            }
            result.add(pid);
        }

        return result;
    }

    /**
     * 转化为MatchItem2
     * @return
     */
    private List<MatchItem2> convertToMatchItem2(List<Long> pidList, String sourceName){
        List<MatchItem2> result = new ArrayList<>();
        if(CollectionUtils.isEmpty(pidList)){
            return result;
        }

        for(Long pid : pidList){
            MatchItem2 matchItem2 = new MatchItem2();
            matchItem2.setProductId(pid);
            matchItem2.setSource(sourceName);
            result.add(matchItem2);
        }

        return result;
    }

    /**
     * 获取小蜜蜂新品和非小蜜蜂新品召回源数据
     * @param ucUser
     * @param request
     * @param cate2ValidPidCacheMap
     * @return
     */
    private List<MatchItem2> getNewProductSourceData(User ucUser, MatchRequest2 request, Map<Long, List<Long>> cate2ValidPidCacheMap){

        List<MatchItem2> result = new ArrayList<>();
        try {
            List<Long> newBeeProductList = productDetailCache.getNewBeeProductList();
            List<Long> newBeenCandidateProductList = getNewProductByRule(newBeeProductList, ucUser, request, cate2ValidPidCacheMap);
            if (CollectionUtils.isNotEmpty(newBeenCandidateProductList)) {
                Collections.shuffle(newBeenCandidateProductList);
                newBeenCandidateProductList = newBeenCandidateProductList.size() > CATEXMF_EXPECT_PID_NUM_UP_LIMIT
                        ? newBeenCandidateProductList.subList(0, CATEXMF_EXPECT_PID_NUM_UP_LIMIT)
                        : newBeenCandidateProductList;
                result.addAll(convertToMatchItem2(newBeenCandidateProductList, MatchStrategyConst.CATEXMF));
            }

            List<Long> newProductWithoutBeeList = productDetailCache.getNewProductWithoutBeeList();
            List<Long> newProductCandidateProductList = getNewProductByRule(newProductWithoutBeeList, ucUser, request, cate2ValidPidCacheMap);
            if (CollectionUtils.isNotEmpty(newProductCandidateProductList)) {
                Collections.shuffle(newProductCandidateProductList);
                newProductCandidateProductList = newProductCandidateProductList.size() > CATEXP_EXPECT_PID_NUM_UP_LIMIT
                        ? newProductCandidateProductList.subList(0, CATEXP_EXPECT_PID_NUM_UP_LIMIT)
                        : newProductCandidateProductList;
                result.addAll(convertToMatchItem2(newProductCandidateProductList, MatchStrategyConst.CATEXP));
            }
        }catch (Exception e){
            log.error("[严重异常][首页轮播图]-获取小蜜蜂新品召回源和非小蜜蜂新品召回源时出现异常 sid {}, uuid {}, e",
                    request.getSid(), request.getUuid(), e);
        }
        return result;
    }

    /**
     * 从有序的召回源中依次获取期望数量的召回商品信息
     * @param ucUser
     * @param request
     * @param cate2ValidPidCacheMap
     * @param orderedSourceList
     * @param expectNum
     * @return
     */
    private List<MatchItem2> getOrderedSourceListData(User ucUser, MatchRequest2 request, Map<Long, List<Long>> cate2ValidPidCacheMap,
                                                    List<String> orderedSourceList, Map<String, String> sourceDataStrategyMap,
                                                    Map<String,String> sourceRedisMap, int expectNum){
        List<MatchItem2> result = new ArrayList<>();
        if(CollectionUtils.isEmpty(orderedSourceList) || expectNum <= 0){
            return result;
        }

        Map<String, Future<List<MatchItem2>>> resultMap = new HashMap<>();
        for(String source : orderedSourceList){
            String sourceDataStrategy = sourceDataStrategyMap == null ? null : sourceDataStrategyMap.get(source);
            String sourceRedis = sourceRedisMap == null ? null : sourceRedisMap.get(source);
            //构造入参
            MatchParam matchParam = MatchParam.builder().device(request.getDevice())
                    .uid(request.getUid()).uuid(request.getUuid())
                    .upcUserType(request.getUpcUserType())
                    .userSex(request.getUserSex())
                    .pidList(request.getPidList())
                    .source(source)
                    .dataStrategy(sourceDataStrategy)
                    .redis(sourceRedis)
                    .ucUser(ucUser)
                    .build();

            Future<List<MatchItem2>> sourceFuture = asyncMatchService.executeMatch2(matchParam, source);
            resultMap.put(source, sourceFuture);
        }

        //获取异步获取召回源数据的结果
        for(String source : orderedSourceList){
            if(!resultMap.containsKey(source)){
                log.error("[严重异常][首页轮播图]获取召回源数据时，缺少召回源{}商品数据，sid {}， uuid {}", source, request.getSid(), request.getUuid());
                continue;
            }

            Future<List<MatchItem2>> asynMatchResult = resultMap.get(source);
            try {
                List<MatchItem2> matchResult = asynMatchResult.get(CommonConstants.MATCH_MAX_WAIT_TIME, TimeUnit.MILLISECONDS);
                if(CollectionUtils.isEmpty(matchResult)){
                    continue;
                }

                //过滤后，按商品分进行排序
                matchResult = matchResult.stream().filter((matchItem2)->{
                    if(matchItem2 == null || matchItem2.getProductId() == null){
                        return false;
                    }

                    if(!isFitLbtCondition(matchItem2.getProductId(), request, cate2ValidPidCacheMap)) {
                        return false;
                    }
                    return true;
                }).sorted((m1, m2) -> -m1.getScore().compareTo(m2.getScore())).collect(Collectors.toList());

                if(CollectionUtils.isEmpty(matchResult)){
                    continue;
                }

                //填充到结果中
                if(matchResult.size() >= expectNum){
                    result.addAll(matchResult.subList(0, expectNum));
                    break;
                }else{
                    result.addAll(matchResult);
                    expectNum = expectNum - matchResult.size();
                }
            } catch (Exception e) {
                log.error("[严重异常][首页轮播图]获取召回源商品数据异常，召回源{}，sid{}， uuid {}，e ", source, request.getSid(), request.getUuid(), e);
            }
        }

        return result;
    }

    /**
     * 获取轮播图热销兜底召回源数据
     * @param request
     * @param cate2ValidPidCacheMap
     * @param expectNum
     * @return
     */
    public List<MatchItem2> getLbtHotSourceData(MatchRequest2 request, Map<Long, List<Long>> cate2ValidPidCacheMap, int expectNum){
        List<MatchItem2> result = new ArrayList<>();
        List<String> hotSaleList = sliderProductCache.getHotSaleList();
        if(CollectionUtils.isEmpty(hotSaleList)){
            log.error("[严重异常][首页轮播图]轮播图热销lbthot召回源数据为空，sid {}，uuid {}", request.getSid(), request.getUuid());
            return result;
        }
        List<Long> notFitUserSexList = new ArrayList<>();
        for(String pid : hotSaleList){
            try {
                Long productId = Long.valueOf(pid);
                ProductInfo productInfo = productDetailCache.getProductInfo(productId);
                if (!isFitLbtCondition(productId, request, cate2ValidPidCacheMap)){
                    continue;
                }

                if(MatchUtil.isFilterBySex(productInfo, request.getUserSex())){
                    notFitUserSexList.add(productId);
                    continue;
                }
                MatchItem2 matchItem2 = new MatchItem2();
                matchItem2.setProductId(productId);
                matchItem2.setSource(MatchStrategyConst.LBTHOT);
                result.add(matchItem2);
                expectNum = expectNum - 1;
                if(expectNum <= 0){
                    return result;
                }
            }catch (Exception e){
                log.error("[严重异常][首页轮播图]处理轮播图热销lbthot召回源数据时出现异常，sid {}，uuid {} ", request.getSid(), request.getUuid(), e);
            }
        }

        notFitUserSexList = notFitUserSexList.size() <= expectNum ? notFitUserSexList : notFitUserSexList.subList(0, expectNum);
        result.addAll(convertToMatchItem2(notFitUserSexList, MatchStrategyConst.LBTHOT));
        return result;
    }
}
