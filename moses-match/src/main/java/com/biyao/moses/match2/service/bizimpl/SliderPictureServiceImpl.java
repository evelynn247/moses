package com.biyao.moses.match2.service.bizimpl;

import com.alibaba.fastjson.JSON;
import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.cache.SliderProductCache;
import com.biyao.moses.common.constant.CommonConstants;
import com.biyao.moses.common.constant.BizNameConst;
import com.biyao.moses.match2.constants.MatchStrategyConst;
import com.biyao.moses.match2.param.MatchParam;
import com.biyao.moses.match2.service.BizService;
import com.biyao.moses.match2.service.impl.HotSaleMatchImpl;
import com.biyao.moses.match2.util.MatchUtil;
import com.biyao.moses.model.match.ProductImage;
import com.biyao.moses.model.match2.MatchItem2;
import com.biyao.moses.params.ProductInfo;
import com.biyao.moses.params.match2.MatchRequest2;
import com.biyao.moses.rpc.UcRpcService;
import com.biyao.moses.service.imp.AsyncMatchService;
import com.biyao.moses.util.ApplicationContextProvider;
import com.biyao.moses.util.FilterUtil;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import com.uc.domain.bean.User;
import com.uc.domain.constant.UserFieldConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * 新轮播图match 20191108
 */
@Slf4j
@Component(value = BizNameConst.SLIDER_PICTURE2)
public class SliderPictureServiceImpl implements BizService {

    @Autowired
    SliderProductCache sliderProductCache;

    @Autowired
    ProductDetailCache productDetailCache;

    @Autowired
    AsyncMatchService asyncMatchService;

    @Autowired
    UcRpcService ucRpcService;

    @Autowired
    MatchUtil matchUtil;

    @Autowired
    FilterUtil filterUtil;
    //轮播图match 推出目标商品数
    private final int TARGET_MUM = 100;

    @BProfiler(key = "SliderPictureServiceImpl.match",
            monitorType = {MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR})
    @Override
    public List<MatchItem2> match(MatchRequest2 request) {

        List<MatchItem2> resultList = new ArrayList<>();
        Map<Long, MatchItem2> resultMap = new HashMap<>();
        MatchParam matchParam = MatchParam.builder().device(request.getDevice())
                .uid(request.getUid()).uuid(request.getUuid())
                .upcUserType(request.getUpcUserType())
                .userSex(request.getUserSex())
                .build();
        int targetNum = TARGET_MUM;
        String siteIdStr= request.getSiteId()==null ? null:String.valueOf(request.getSiteId());
        try {
            //调用UC 获取用户类目偏好map  key:后台三级类目ID val:分值 该map为linkedHashMap 按分值倒排
            Map<String, BigDecimal> level3HobbyMap = getlevel3HobbyMap(request);

            //获取全部小蜜蜂新品
            List<Long> newBeeProductList = productDetailCache.getNewBeeProductList();
            //对小蜜蜂新品进行类目偏好筛选
            List<MatchItem2> beeCategory3Products = matchUtil.getHobbyCategory3Products(newBeeProductList, level3HobbyMap,
                    BizNameConst.SLIDER_PICTURE2, TARGET_MUM);
            for (MatchItem2 item2 : beeCategory3Products) {
                if (targetNum <= 0) {
                    break;
                }
                // 过滤不支持用户所持端的商品
                if(filterUtil.isFilteredBySiteId(item2.getProductId(),siteIdStr)){
                    continue;
                }
                //检验是否重复
                if (resultMap.get(item2.getProductId()) != null) {
                    continue;
                }
                
                ProductImage imageInfo = sliderProductCache.getProductImageById(item2.getProductId());
                if (imageInfo == null) {
                    continue;
                }

                //获取商品对应商家&二级类目商品集合
                Set<Long> categoryAndSupplierSet = getCategoryAndSupplierSet(item2);

                if (categoryAndSupplierSet.size() < 30) {
                    continue;
                }
                
                item2.setScore(item2.getScore() * 10);
                resultMap.put(item2.getProductId(), item2);
                targetNum--;
            }

            if (targetNum > 0) {
                //获取全部非小蜜蜂新品
                List<Long> newProductWithoutBeeList = productDetailCache.getNewProductWithoutBeeList();
                //对非小蜜蜂新品进行类目偏好筛选
                List<MatchItem2> category3ProductsWithoutBee = matchUtil.getHobbyCategory3Products(newProductWithoutBeeList,
                        level3HobbyMap, BizNameConst.SLIDER_PICTURE2, targetNum);
                for (MatchItem2 item2 : category3ProductsWithoutBee) {
                    if (targetNum <= 0) {
                        break;
                    }
                    if(filterUtil.isFilteredBySiteId(item2.getProductId(),siteIdStr)){
                        continue;
                    }
                    //检验是否重复
                    if (resultMap.get(item2.getProductId()) != null) {
                        continue;
                    }
                    
                    ProductImage imageInfo = sliderProductCache.getProductImageById(item2.getProductId());
                    if (imageInfo == null) {
                        continue;
                    }

                    //获取商品对应商家&二级类目商品集合
                    Set<Long> categoryAndSupplierSet = getCategoryAndSupplierSet(item2);

                    if (categoryAndSupplierSet.size() < 30) {
                        continue;
                    }

                    item2.setScore(item2.getScore() * 1);
                    resultMap.put(item2.getProductId(), item2);
                    targetNum--;
                }
            }

            if (targetNum > 0) {
                //使用IBCF 进行补充
                targetNum = fillResultBySource(resultMap, matchParam, targetNum, MatchStrategyConst.IBCF, request.getSiteId());
            }
            if (targetNum > 0) {
                //使用TAG 进行补充
                targetNum = fillResultBySource(resultMap, matchParam, targetNum, MatchStrategyConst.TAG, request.getSiteId());
            }
            if (targetNum > 0) {
                //使用HOTS 进行补充
                targetNum = fillResultBySource(resultMap, matchParam, targetNum, MatchStrategyConst.HOTS, request.getSiteId());
            }
        } catch (Exception e) {
            log.error("[严重异常]入参={},新版老客轮播图match异常:", JSON.toJSONString(request), e);
            //出错时使用热销兜底
            fillResultBySource(resultMap, matchParam, targetNum, MatchStrategyConst.HOTS, request.getSiteId());
        }

        //组装结果
        resultList.addAll(resultMap.values());
        return resultList;
    }

    /**
     * 获取商品对应商家&二级类目商品集合
     * @param item2
     * @return
     */
    private Set<Long> getCategoryAndSupplierSet(MatchItem2 item2) {
        Set<Long> categoryAndSupplierSet = new HashSet<Long>();
        ProductInfo productInfo = productDetailCache.getProductInfo(item2.getProductId());
        if(productInfo==null){
            return categoryAndSupplierSet;
        }
        List<Long> productIdsBySupplierIdList = productDetailCache.getProductIdsBySupplierId(productInfo.getSupplierId());
        List<Long> category2ProductList = productDetailCache.getCategory2Product(productInfo.getSecondCategoryId());
        if (productIdsBySupplierIdList == null) {
            productIdsBySupplierIdList = new ArrayList<>();
        }
        if (category2ProductList == null) {
            category2ProductList = new ArrayList<>();
        }
        categoryAndSupplierSet.addAll(productIdsBySupplierIdList);
        categoryAndSupplierSet.addAll(category2ProductList);
        return categoryAndSupplierSet;
    }

    /**
     * 使用各个召回源补充直至100个商品 优先级：ibcf>tag>hot
     *
     * @param resultMap
     * @param matchParam
     * @param targetNum
     * @param source
     * @return
     */
    private int fillResultBySource(Map<Long, MatchItem2> resultMap, MatchParam matchParam, int targetNum, String source,Integer siteId) {
        List<Long> newBeeProductList = productDetailCache.getNewBeeProductList();
        List<MatchItem2> sourceList = new ArrayList<MatchItem2>();
        String siteIdStr= siteId == null ? null :String.valueOf(siteId);
        //热销不使用异步
        if (StringUtils.isNotBlank(source) && source.equals(MatchStrategyConst.HOTS)) {
            HotSaleMatchImpl hotSaleMatch = (HotSaleMatchImpl) ApplicationContextProvider.getBean(MatchStrategyConst.HOTS);
            sourceList = hotSaleMatch.match(matchParam);
        } else {
            Future<List<MatchItem2>> sourceFuture = asyncMatchService.executeMatch2(matchParam, source);
            try {
                sourceList = sourceFuture.get(CommonConstants.MATCH_MAX_WAIT_TIME, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                log.error("获取召回源商品数据异常，召回源" + source + "，超时时间{}ms", CommonConstants.MATCH_MAX_WAIT_TIME, e);
            }
        }
        //打散
        Collections.shuffle(sourceList);

        for (MatchItem2 item : sourceList) {
            if (targetNum <= 0) {
                break;
            }
            // 过滤不支持用户所持有端的商品
           if(filterUtil.isFilteredBySiteId(item.getProductId(),siteIdStr)){
               continue;
           }
            //检验是否重复
            if (resultMap.get(item.getProductId()) != null) {
                continue;
            }
            
            ProductImage imageInfo = sliderProductCache.getProductImageById(item.getProductId());
            if (imageInfo == null) {
                continue;
            }

            //获取商品对应商家&二级类目商品集合
            Set<Long> categoryAndSupplierSet = getCategoryAndSupplierSet(item);

            if (categoryAndSupplierSet.size() < 30) {
                continue;
            }

            if(newBeeProductList.contains(item.getProductId())){
                item.setScore(item.getScore() * 5);
            }else {
                if (source.equals(MatchStrategyConst.IBCF)) {
                    item.setScore(item.getScore() * 0.5);
                } else if (source.equals(MatchStrategyConst.TAG)) {
                    item.setScore(item.getScore() * 0.5);
                } else if (source.equals(MatchStrategyConst.HOTS)) {
                    item.setScore(item.getScore() * 0.1);
                }
            }

            resultMap.put(item.getProductId(), item);
            targetNum--;
        }
        return targetNum;
    }

    /**
     * 调用uc 获取类目偏好
     *
     * @param request
     * @return
     */
    private Map<String, BigDecimal> getlevel3HobbyMap(MatchRequest2 request) {
        String uid = null;
        if (request.getUid() != null && request.getUid() > 0) {
            uid = request.getUid().toString();
        }
        List<String> fields = new ArrayList<>();
        fields.add(UserFieldConstants.LEVEL3HOBBY);
        User ucUser = ucRpcService.getData(request.getUuid(), uid, fields, "mosesmatch");
        if (ucUser == null || ucUser.getLevel3Hobby() == null) {
            return new LinkedHashMap<>();
        }
        Map<String, BigDecimal> level3Hobby = ucUser.getLevel3Hobby();
        return level3Hobby;
    }
}
