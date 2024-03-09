package com.biyao.moses.service.imp;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.biyao.dclog.service.DCLogger;
import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.cache.SliderProductCache;
import com.biyao.moses.common.constant.CommonConstants;
import com.biyao.moses.common.constant.UPCUserTypeConstants;
import com.biyao.moses.common.enums.MatchStrategyEnum;
import com.biyao.moses.model.exp.MatchDataSourceTypeConf;
import com.biyao.moses.model.match.ProductImage;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.params.ProductInfo;
import com.biyao.moses.service.RecommendMatch;
import com.biyao.moses.util.HttpClientUtil;
import com.biyao.moses.util.ProductExposureUtil;
import com.biyao.moses.util.RedisUtil;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 轮播图match
 *
 * @author zyj
 * @Description
 * @Date 2018年9月27日
 */
@Slf4j
@Component("SPM")
public class SwiperPicMatch implements RecommendMatch {

    @Autowired
    RedisUtil redisUtil; 

    // 共推出商品数
    private final static Integer ALL_PRODUCT_NUM = 6;
    // http请求超时时间
    private final static int TIMEOUT = 50;
    // 请求天气接口url
    private final static String WEATHER_URL = "http://dcapi.biyao.com/queryweather/v2";

    private final static String UTYPE = "utype"; //用户身份
    private final static String UCPREFER = "ucprefer"; //用户类目偏好
    private final static String USEX = "usex"; //用户性别
    private final static String UWEA = "wea"; //用户对应的天气

    @Autowired
    private ProductDetailCache productDetailCache;
    @Autowired
    private SliderProductCache sliderProductCache;

    @Autowired
    ProductExposureUtil productExposureUtil;

    private static DCLogger mosesuiDcLogger = DCLogger.getLogger("moses_slide_exposure");

    @BProfiler(key = "com.biyao.moses.service.imp.SwiperPicMatch.executeRecommendMatch", monitorType = {MonitorType.TP,
            MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR})
    @Override
    public Map<String, List<TotalTemplateInfo>> executeRecommendMatch(String dataKey, MatchDataSourceTypeConf mdst,
                                                                      String uuId) {
        // 返回结果集
        Map<String, List<TotalTemplateInfo>> resultMap = new HashMap<>();
        List<TotalTemplateInfo> totaltemplateInfoList = new ArrayList<>();
        Set<String> viewedSlidePids = new HashSet<String>();
        //存放dclog日志基础信息
        Map<String, String> baseLogMap = new HashMap<>();
        //存放dclog日志策略信息
        Map<String, String> tacticsLogMap = new HashMap<>();
        try {

            String sex = mdst.getSex();
            baseLogMap.put(USEX, sex);
            List<String> userPreferCate3Ids = getUuidPreferCate3(uuId, baseLogMap); // 用户偏好的三级类目
            viewedSlidePids = productExposureUtil.getViewedSlidePids(uuId);  // 用户最近3天展示的商品

            // {topicid}_{matchName}_{expNum}
            // 判断访客、老客 (访客 0、老客 1)，默认0
            Integer userType = mdst.getUpcUserType();
            baseLogMap.put(UTYPE, userType.toString());

            // resultProduct:最后的结果集 6个商品
            List<String> resultProduct = new ArrayList<>();
            // 老客 天气推荐1个商品，基础上新推荐2个，高转化推荐3-4个。如果最终不足6个，则用热销补齐
            if (userType == UPCUserTypeConstants.CUSTOMER) {

                // 天气商品推荐 1个
                List<String> weatherMatch = getProductByWeather(dataKey, mdst, uuId, baseLogMap);
                // 基础上新商品推荐 1个
                List<String> baseList = sliderProductCache.getBaseProductList();
                //List<String> baseProductMatch = productExposureUtil.sortByUserPreferCate3(userPreferCate3Ids, baseList, sex);
                List<String> baseProductMatch = productExposureUtil.sortByVector(baseList, uuId);
                // 高转化 个数： 2个
                List<String> highConvList = sliderProductCache.getHighConvList();
                //List<String> highConvProductMatch = productExposureUtil.sortByUserPreferCate3(userPreferCate3Ids, highConvList, sex);
                List<String> highConvProductMatch = productExposureUtil.sortByVector(highConvList, uuId);
                // 热销： 2个
                List<String> hotSaleList = sliderProductCache.getHotSaleList();
                //List<String> hotSaleProductMatch = productExposureUtil.sortByUserPreferCate3(userPreferCate3Ids, hotSaleList, sex);
                List<String> hotSaleProductMatch = productExposureUtil.sortByVector(hotSaleList, uuId);

                //构建结果集
                resultProduct = buildBuyerHomeSlide(weatherMatch, baseProductMatch, highConvProductMatch,
                        hotSaleProductMatch, viewedSlidePids, uuId, tacticsLogMap);

            } else if (userType == UPCUserTypeConstants.NEW_VISITOR) { // 新访客
                // 天气商品推荐 1个
                List<String> weatherMatch = getProductByWeather(dataKey, mdst, uuId, baseLogMap);
                // 高转化低决策个数： 3个
                List<String> highAndLowList = sliderProductCache.getHighAndLowList();
                List<String> highAndLowProductMatch = productExposureUtil.sortByUserPreferCate3(userPreferCate3Ids, highAndLowList, sex);
                // 热销： 2个
                List<String> hotSaleList = sliderProductCache.getHotSaleList();
                //List<String> hotSaleProductMatch = productExposureUtil.sortByUserPreferCate3(userPreferCate3Ids, hotSaleList, sex);
                List<String> hotSaleProductMatch = productExposureUtil.sortByVector(hotSaleList, uuId);

                //构建结果集
                resultProduct = buildNewVisitorHomeSlide(weatherMatch, highAndLowProductMatch, hotSaleProductMatch, viewedSlidePids, tacticsLogMap);
            } else {   // 老访客
                // 天气商品推荐 1个
                List<String> weatherMatch = getProductByWeather(dataKey, mdst, uuId, baseLogMap);
                // 高转化低决策个数： 2个
                List<String> highAndLowList = sliderProductCache.getHighAndLowList();
                List<String> highAndLowProductMatch = productExposureUtil.sortByUserPreferCate3(userPreferCate3Ids, highAndLowList, sex);
                // 高转化个数： 1个
                List<String> highConvList = sliderProductCache.getHighConvList();
                //List<String> highConvProductMatch = productExposureUtil.sortByUserPreferCate3(userPreferCate3Ids, highConvList, sex);
                List<String> highConvProductMatch = productExposureUtil.sortByVector(highConvList, uuId);
                // 热销： 2个
                List<String> hotSaleList = sliderProductCache.getHotSaleList();
                //List<String> hotSaleProductMatch = productExposureUtil.sortByUserPreferCate3(userPreferCate3Ids, hotSaleList, sex);
                List<String> hotSaleProductMatch = productExposureUtil.sortByVector(hotSaleList, uuId);

                //构建结果集
                resultProduct = buildOldVisitorHomeSlide(weatherMatch, highAndLowProductMatch, highConvProductMatch, hotSaleProductMatch, viewedSlidePids, tacticsLogMap);
            }

            // 如果有异常不足6个，用热销补齐
            if (resultProduct.size() < 6) {
                List<String> hotLogList = new ArrayList<>();
                List<String> hotSaleList = sliderProductCache.getHotSaleList();
                for (String pid : hotSaleList) {
                    hotLogList.add(pid);
                    resultProduct.add(pid);
                    if (resultProduct.size() >= 6) {
                        break;
                    }
                }
                dclogPut(tacticsLogMap, MatchStrategyEnum.HS.getName(), hotSaleList);
            }
            // 处理本次曝光的记录
            viewedSlidePids.addAll(resultProduct);
//            for (String pid:viewedSlidePids) {
//               log.info("曝光过的商品："+pid);
//            }
            log.info("首页轮播图-曝光过的商品：{}", JSON.toJSONString(viewedSlidePids));
            productExposureUtil.setViewedSlideProducts(uuId, resultProduct);
            printHomeSlideShowLog(mdst, uuId, resultProduct, baseLogMap, tacticsLogMap); // 上传曝光日志

            totaltemplateInfoList = generateResult(resultProduct);
            resultMap.put(dataKey, totaltemplateInfoList);
        } catch (Exception e) {
            log.error("[严重异常]首页轮播图SwiperPicMatch出现异常，uuid {} ", uuId, e);
        }

        //如果出现问题 使用热销随机6个兜底
        if (totaltemplateInfoList.size() < 6) {
            List<String> resultProduct = new ArrayList<String>();
            resultProduct.addAll(sliderProductCache.getHotSaleList());
            Collections.shuffle(resultProduct);
            if (resultProduct.size() >= 6) {
                resultProduct = resultProduct.subList(0, 6);
            }
            dclogPut(tacticsLogMap, MatchStrategyEnum.HS.getName(), resultProduct);
            totaltemplateInfoList = generateResult(resultProduct);
            resultMap.put(dataKey, totaltemplateInfoList);
        }
        return resultMap;
    }

    /**
     * 组装模版内容
     *
     * @param resultProduct
     * @return
     */
    private List<TotalTemplateInfo> generateResult(List<String> resultProduct) {

        List<TotalTemplateInfo> totaltemplateInfoList = new ArrayList<>();
        // 组装轮播图返回的数据格式
        // 对每个商品加入参数
        for (String pid : resultProduct) {
            ProductInfo productInfo = productDetailCache.getProductInfo(Long.parseLong(pid));
            TotalTemplateInfo tti = new TotalTemplateInfo();
            Map<String, String> routerParams = new HashMap<>();
            // 商品候选集缓存中拿商品参数
            ProductImage imageInfo = sliderProductCache.getProductImageById(Long.parseLong(pid));
            if (imageInfo == null) {
                log.error("imageInfo is NULL: {} ", pid);
                continue;
            }
            // 加入参数
            if (productInfo.getSupplierId() != null) {
                routerParams.put("supplierId", productInfo.getSupplierId().toString());
            }
            if (productInfo.getSuId() != null) {
                routerParams.put("suId", productInfo.getSuId().toString());
            }
            // 推荐中间页跳转类型
            if (imageInfo.getRouteType() == 6){
                routerParams.put("priorityProductIds", pid);
            }

            // 配置中的参数优先
            if (imageInfo.getRouteParams() != null && imageInfo.getRouteParams().size() > 0){
                routerParams.putAll(imageInfo.getRouteParams());
            }
            tti.setRouterParams(routerParams);

            tti.setImage(imageInfo.getImage());
            tti.setImageWebp(imageInfo.getWebpImage());
            List<String> list = new ArrayList<>();
            List<String> listWebp = new ArrayList<>();
            list.add(imageInfo.getImage());
            listWebp.add(imageInfo.getWebpImage());
            tti.setLongImages(list);
            tti.setLongImagesWebp(listWebp);
            tti.setImages(list);
            tti.setImagesWebp(listWebp);
            tti.setRouterType(imageInfo.getRouteType());
            totaltemplateInfoList.add(tti);
        }
        return totaltemplateInfoList;
    }

    /**
     * 新访客轮播图逻辑
     *
     * @param weatherMatch
     * @param hotSaleProductMatch
     * @param viewedSlidePids
     * @return
     */
    private List<String> buildNewVisitorHomeSlide(List<String> weatherMatch,
                                                  List<String> highAndLowProductMatch, List<String> hotSaleProductMatch,
                                                  Set<String> viewedSlidePids, Map<String, String> tacticsLogMap) {
        // 第一轮：完全岔开
        List<String> resultProduct = new ArrayList<>();

        Set<Long> existSuppliers = new HashSet<>();
        Set<Long> existCate3Ids = new HashSet<>();
        // 天气比较特殊，只出1个，出在第一个位置
        List<String> weaList = fetchProductFromCandidate(resultProduct, viewedSlidePids, existSuppliers, existCate3Ids, weatherMatch, 1);
        dclogPut(tacticsLogMap, MatchStrategyEnum.WEA.getName(), weaList);
        resultProduct.addAll(weaList);
        // 高转化低决策，最多出3个
        List<String> highAndLowList = fetchProductFromCandidate(resultProduct, viewedSlidePids, existSuppliers, existCate3Ids, highAndLowProductMatch, 3);
        dclogPut(tacticsLogMap, MatchStrategyEnum.GZH_DJC_NEW.getName(), highAndLowList);
        resultProduct.addAll(highAndLowList);
        // 热销
        List<String> hotList = fetchProductFromCandidate(resultProduct, viewedSlidePids, existSuppliers, existCate3Ids, hotSaleProductMatch, 6);
        dclogPut(tacticsLogMap, MatchStrategyEnum.HS.getName(), hotList);
        resultProduct.addAll(hotList);
        if (resultProduct.size() >= 6) {
            return resultProduct.subList(0, 6);
        }
        // 第二轮：如果三级类目至少有4个，对高转化和热销再次召回
        viewedSlidePids.addAll(resultProduct);  // 防止第二轮召回的时候再次召回
        if (existCate3Ids.size() >= 4) {

            Set<Long> existSuppliers_r2 = new HashSet<>();
            Set<Long> existCate3Ids_r2 = new HashSet<>();
            // 高转化，最多出6个
            List<String> fixHighList = fetchProductFromCandidate(resultProduct, viewedSlidePids, existSuppliers_r2, existCate3Ids_r2, highAndLowProductMatch, 6);
            dclogPut(tacticsLogMap, MatchStrategyEnum.GZH_NEW.getName(), fixHighList);
            resultProduct.addAll(fixHighList);
            // 热销，最多出6个
            List<String> fixHotList = fetchProductFromCandidate(resultProduct, viewedSlidePids, existSuppliers_r2, existCate3Ids_r2, hotSaleProductMatch, 6);
            dclogPut(tacticsLogMap, MatchStrategyEnum.HS.getName(), fixHotList);
            resultProduct.addAll(fixHotList);
            existSuppliers.addAll(existSuppliers_r2);
            existCate3Ids.addAll(existCate3Ids_r2);
        }

        if (resultProduct.size() >= 6) {
            return resultProduct.subList(0, 6);
        }

        // 第三轮  从已曝光的结果中召回
        viewedSlidePids.removeAll(resultProduct);  //防止从已曝光中推荐出重复的商品
        List<String> expList = fetchProductFromCandidate(resultProduct, viewedSlidePids, existSuppliers, existCate3Ids,
                new ArrayList<String>(viewedSlidePids), 6);
        dclogPut(tacticsLogMap, MatchStrategyEnum.EXP.getName(), expList);
        resultProduct.addAll(expList);
        // 将用户已曝光的数据重置
        viewedSlidePids.clear();
        // 返回召回结果
        if (resultProduct.size() >= 6) {
            return resultProduct.subList(0, 6);
        }
        return resultProduct;
    }


    /**
     * 老访客轮播图逻辑
     * @param weatherMatch
     * @param highAndLowProductMatch
     * @param highConvProductMatch
     * @param hotSaleProductMatch
     * @param viewedSlidePids
     * @param tacticsLogMap
     * @return
     */
    private List<String> buildOldVisitorHomeSlide(List<String> weatherMatch, List<String> highAndLowProductMatch,
                                                  List<String> highConvProductMatch, List<String> hotSaleProductMatch,
                                                  Set<String> viewedSlidePids, Map<String, String> tacticsLogMap) {
        // 第一轮：完全岔开
        List<String> resultProduct = new ArrayList<>();

        Set<Long> existSuppliers = new HashSet<>();
        Set<Long> existCate3Ids = new HashSet<>();
        // 天气比较特殊，只出1个，出在第一个位置
        List<String> weaList = fetchProductFromCandidate(resultProduct, viewedSlidePids, existSuppliers, existCate3Ids, weatherMatch, 1);
        dclogPut(tacticsLogMap, MatchStrategyEnum.WEA.getName(), weaList);
        resultProduct.addAll(weaList);

        // 高转化低决策，最多出2个
        List<String> highAndLowList = fetchProductFromCandidate(resultProduct, viewedSlidePids, existSuppliers, existCate3Ids, highAndLowProductMatch, 2);
        dclogPut(tacticsLogMap, MatchStrategyEnum.GZH_DJC_NEW.getName(), highAndLowList);
        resultProduct.addAll(highAndLowList);

        // 高转化，最多出1个
        List<String> highConvList = fetchProductFromCandidate(resultProduct, viewedSlidePids, existSuppliers, existCate3Ids, highConvProductMatch, 1);
        dclogPut(tacticsLogMap, MatchStrategyEnum.GZH_NEW.getName(), highConvList);
        resultProduct.addAll(highConvList);
        // 热销，最多出2个
        List<String> hotList = fetchProductFromCandidate(resultProduct, viewedSlidePids, existSuppliers, existCate3Ids, hotSaleProductMatch, 6);
        dclogPut(tacticsLogMap, MatchStrategyEnum.HS.getName(), hotList);
        resultProduct.addAll(hotList);
        if (resultProduct.size() >= 6) {
            return resultProduct.subList(0, 6);
        }
        // 第二轮：如果三级类目至少有4个，对高转化和热销再次召回
        viewedSlidePids.addAll(resultProduct);  // 防止第二轮召回的时候再次召回
        if (existCate3Ids.size() >= 4) {

            Set<Long> existSuppliers_r2 = new HashSet<>();
            Set<Long> existCate3Ids_r2 = new HashSet<>();
            // 高转化，最多出6个
            List<String> fixHighList = fetchProductFromCandidate(resultProduct, viewedSlidePids, existSuppliers_r2, existCate3Ids_r2, highConvProductMatch, 6);
            dclogPut(tacticsLogMap, MatchStrategyEnum.GZH_NEW.getName(), fixHighList);
            resultProduct.addAll(fixHighList);
            // 热销，最多出6个
            List<String> fixHotList = fetchProductFromCandidate(resultProduct, viewedSlidePids, existSuppliers_r2, existCate3Ids_r2, hotSaleProductMatch, 6);
            dclogPut(tacticsLogMap, MatchStrategyEnum.HS.getName(), fixHotList);
            resultProduct.addAll(fixHotList);
            existSuppliers.addAll(existSuppliers_r2);
            existCate3Ids.addAll(existCate3Ids_r2);
        }

        if (resultProduct.size() >= 6) {
            return resultProduct.subList(0, 6);
        }

        // 第三轮  从已曝光的结果中召回
        viewedSlidePids.removeAll(resultProduct);  //防止从已曝光中推荐出重复的商品
        List<String> expList = fetchProductFromCandidate(resultProduct, viewedSlidePids, existSuppliers, existCate3Ids,
                new ArrayList<String>(viewedSlidePids), 6);
        dclogPut(tacticsLogMap, MatchStrategyEnum.EXP.getName(), expList);
        resultProduct.addAll(expList);
        // 将用户已曝光的数据重置
        viewedSlidePids.clear();
        // 返回召回结果
        if (resultProduct.size() >= 6) {
            return resultProduct.subList(0, 6);
        }
        return resultProduct;
    }

    /**
     * 构造推荐策略日志map
     *
     * @param map
     * @param key
     * @param valueList
     */
    private void dclogPut(Map<String, String> map, String key, List<String> valueList) {
        if (!CollectionUtils.isEmpty(valueList)) {
            String joinStr = StringUtils.join(valueList, ",");
            String oldStr = map.get(key);
            if (StringUtils.isNotEmpty(oldStr)) {
                //当前策略已经推出过商品，则把后推出的商品拼在后面
                map.put(key, oldStr + "," + StringUtils.join(valueList, ","));
            } else {
                map.put(key, joinStr);
            }
        }
    }

    /**
     * 老客轮播图逻辑
     *
     * @param weatherMatch
     * @param baseProductMatch
     * @param highConvProductMatch
     * @param hotSaleProductMatch
     * @param viewedSlidePids
     * @return
     */
    private List<String> buildBuyerHomeSlide(List<String> weatherMatch, List<String> baseProductMatch,
                                             List<String> highConvProductMatch, List<String> hotSaleProductMatch,
                                             Set<String> viewedSlidePids, String uuid, Map<String, String> tacticsLogMap) {
        List<String> resultProduct = new ArrayList<>();

        // 第一轮：完全岔开
        Set<Long> existSuppliers = new HashSet<>();
        Set<Long> existCate3Ids = new HashSet<>();

        // 天气比较特殊，只出1个，出在第一个位置
        List<String> weatherList = fetchProductFromCandidate(resultProduct, viewedSlidePids, existSuppliers, existCate3Ids, weatherMatch, 1);
        dclogPut(tacticsLogMap, MatchStrategyEnum.WEA.getName(), weatherList);
        resultProduct.addAll(weatherList);
        // 基础流量，最多出1个
        List<String> baseList = fetchProductFromCandidate(resultProduct, viewedSlidePids, existSuppliers, existCate3Ids, baseProductMatch, 1);
        dclogPut(tacticsLogMap, MatchStrategyEnum.NEW_LOW.getName(), baseList);
        resultProduct.addAll(baseList);
        // 高转化，最多出6个
        List<String> highConvList = fetchProductFromCandidate(resultProduct, viewedSlidePids, existSuppliers, existCate3Ids, highConvProductMatch, 2);
        dclogPut(tacticsLogMap, MatchStrategyEnum.GZH_NEW.getName(), highConvList);
        resultProduct.addAll(highConvList);
        // 热销，最多出6个
        List<String> hotList = fetchProductFromCandidate(resultProduct, viewedSlidePids, existSuppliers, existCate3Ids, hotSaleProductMatch, 6);
        dclogPut(tacticsLogMap, MatchStrategyEnum.HS.getName(), highConvList);
        resultProduct.addAll(hotList);

        if (resultProduct.size() >= 6) {
            return resultProduct.subList(0, 6);
        }

        // 第二轮：如果三级类目至少有4个，对高转化和热销再次召回
        viewedSlidePids.addAll(resultProduct);  // 防止第二轮召回的时候再次召回
        if (existCate3Ids.size() >= 4) {

            Set<Long> existSuppliers_r2 = new HashSet<>();
            Set<Long> existCate3Ids_r2 = new HashSet<>();
            // 高转化，最多出6个
            List<String> fixHighConvList = fetchProductFromCandidate(resultProduct, viewedSlidePids, existSuppliers_r2, existCate3Ids_r2, highConvProductMatch, 6);
            dclogPut(tacticsLogMap, MatchStrategyEnum.GZH_NEW.getName(), fixHighConvList);
            resultProduct.addAll(fixHighConvList);
            // 热销，最多出6个
            List<String> fixHotList = fetchProductFromCandidate(resultProduct, viewedSlidePids, existSuppliers_r2, existCate3Ids_r2, hotSaleProductMatch, 6);
            dclogPut(tacticsLogMap, MatchStrategyEnum.HS.getName(), fixHotList);
            resultProduct.addAll(fixHotList);
            existSuppliers.addAll(existSuppliers_r2);
            existCate3Ids.addAll(existCate3Ids_r2);
        }

        if (resultProduct.size() >= 6) {
            return resultProduct.subList(0, 6);
        }

        // 第三轮  从已曝光的结果中召回
        viewedSlidePids.removeAll(resultProduct);
        List<String> expList = fetchProductFromCandidate(resultProduct, viewedSlidePids, existSuppliers, existCate3Ids,
                new ArrayList<String>(viewedSlidePids), 6);
        dclogPut(tacticsLogMap, MatchStrategyEnum.EXP.getName(), expList);
        resultProduct.addAll(expList);

        // 将用户已曝光的数据重置
        viewedSlidePids.clear();

        // 返回召回结果
        return resultProduct;
    }

    /**
     * 召回和exisitSupplier、existCate3不重复的结果集
     *
     * @param existSupplier
     * @param existCate3
     * @param candidates
     * @param target        期望值
     * @return 实际结果
     */
    private List<String> fetchProductFromCandidate(List<String> alreadyRecList, //本次召回商品结果集
                                                   Set<String> viewedProductIds,// 已被用户看过的商品
                                                   Set<Long> existSupplier, // 已经被筛选出来的商家
                                                   Set<Long> existCate3,    // 已经被筛选的三级类目
                                                   List<String> candidates,    // 候选集合
                                                   int target) {               // 期望的数量
        List<String> result = new ArrayList<>();
        Iterator<String> iterator = candidates.iterator();
        while (iterator.hasNext()) {
            // 满足基本条件
            Long productId = Long.parseLong(iterator.next());
            ProductInfo productInfo = productDetailCache.getProductInfo(productId);

            if (productInfo == null || productInfo.getShelfStatus() == null || !productInfo.getShelfStatus().toString().equals("1")) {
                iterator.remove();
                continue;
            }
            ProductImage imageInfo = sliderProductCache.getProductImageById(productId);
            if (imageInfo == null) {
                iterator.remove();
                continue;
            }

            // 已经被曝光过的不再曝光
//            if (viewedProductIds.contains(String.valueOf(productId))) {  // 已经被用户看过
//                iterator.remove();
//                continue;
//            }
            if (alreadyRecList.contains(String.valueOf(productId))) { //过滤本次推荐商品
                iterator.remove();
                continue;
            }
            // 类目、商家不重复，本次不筛选
            Long thirdCategoryId = productInfo.getThirdCategoryId();
            Long secondCategoryId = productInfo.getSecondCategoryId();
            if (CommonConstants.SPECIAL_CATEGORY2_IDS.contains(secondCategoryId)) { // 眼镜类目等特殊处理
                thirdCategoryId = secondCategoryId;
            }

            if (existCate3.contains(thirdCategoryId)) {
                continue;
            }
//            if (existSupplier.contains(productInfo.getSupplierId())) {
//                continue;
//            }

            existCate3.add(thirdCategoryId);
            existSupplier.add(productInfo.getSupplierId());

            result.add(String.valueOf(productId));
            iterator.remove();

            if (result.size() >= target) {
                break;
            }
        }
        return result;
    }

    /**
     * 获取天气推荐商品方法
     *
     * @param dataKey
     * @return
     */
    private List<String> getProductByWeather(String dataKey, MatchDataSourceTypeConf mdst, String uuId, Map<String, String> baseLogMap) {
        String weatherType = ""; // 天气枚举
        List<String> weatherList = new ArrayList<String>(); // 天气商品集合
        String lat = mdst.getLat();
        String lng = mdst.getLng();
        // 参数为空
        if (StringUtils.isEmpty(lat) || StringUtils.isEmpty(lng) || StringUtils.isEmpty(uuId)) {
            return weatherList;
        }
        // 调用http接口 获取用户对应天气枚举值
        try {
            String result = HttpClientUtil.sendGetRequest(WEATHER_URL + "?lng=" + lng + "&lat=" + lat + "&uuid=" + uuId,
                    TIMEOUT);
            if (!StringUtils.isEmpty(result)) {
                JSONObject json = JSONObject.parseObject(result);
                // 返回值为0，请求失败
                if ("1".equals(json.getString("code"))) {
                    weatherType = json.getString("data");
                } else {
                    log.error("[一般异常]查询天气枚举请求出错: uuid {}, errorMessage {}", uuId, json.getString("errorMessage"));
                }
            }
        } catch (Exception e) {
            log.error("[一般异常]查询天气枚举请求出错，uuid {}", uuId, e);
        }
        if (StringUtils.isEmpty(weatherType)) {
            baseLogMap.put(UWEA, weatherType);
            return weatherList;
        }
        baseLogMap.put(UWEA, weatherType);
        Map<String, List<String>> weatherProducts = sliderProductCache.getWeatherList();
        return weatherProducts.getOrDefault(weatherType, new ArrayList<String>());
    }

    /**
     * 打印dclog曝光日志
     * uuid=xxx\tpids=pid1,pid2\tst=1231\ttopicId=120131\tuid=\tstid=siteId\tavn=
     *
     * @param mdst
     */
    private void printHomeSlideShowLog(MatchDataSourceTypeConf mdst, String uuid, List<String> resultProduct
            , Map<String, String> baseLogMap, Map<String, String> tacticsLogMap) {
//        String siteId = !StringUtils.isEmpty((mdst.getSiteId())) ? mdst.getSiteId() : "";
//        logStr.append("siteId=" + siteId + "\t");
        String join = "";
        if (!CollectionUtils.isEmpty(resultProduct)) {
            join = StringUtils.join(resultProduct, ",");
        }
        StringBuffer logStr = new StringBuffer();
        logStr.append("lt=moses_slide_exposure").append("\t");
        String uu = !StringUtils.isEmpty(uuid) ? uuid : "";
        logStr.append("uu=").append(uu).append("\t");
        logStr.append("pids=").append(join).append("\t");
        String topicId = !StringUtils.isEmpty(mdst.getDataSourceType()) ? mdst.getDataSourceType() : "";
        logStr.append("topicId=").append(topicId).append("\t");
        String uid = !StringUtils.isEmpty(mdst.getUid()) ? mdst.getUid() : "";
        logStr.append("u=").append(uid).append("\t");
        String siteId = !StringUtils.isEmpty((mdst.getSiteId())) ? mdst.getSiteId() : "";
        logStr.append("stid=" + siteId + "\t");
        Date d = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        logStr.append("st=" + sdf.format(d) + "\t");
        logStr.append("wea=").append(StringUtils.isBlank(baseLogMap.get(UWEA)) ? "" : baseLogMap.get(UWEA)).append("\t");
        logStr.append("utype=").append(StringUtils.isBlank(baseLogMap.get(UTYPE)) ? "" : baseLogMap.get(UTYPE)).append("\t");
        logStr.append("ucprefer=").append(StringUtils.isBlank(baseLogMap.get(UCPREFER)) ? "" : baseLogMap.get(UCPREFER)).append("\t");
        logStr.append("usex=").append(StringUtils.isBlank(baseLogMap.get(USEX)) ? "" : baseLogMap.get(USEX)).append("\t");
        logStr.append("mixmatch=" + JSON.toJSONString(tacticsLogMap));
        String logString = logStr.toString();
        mosesuiDcLogger.printDCLog(logString);
    }

    public List<String> getUuidPreferCate3(String uuid, Map<String, String> logMap) {
        // 查询三级类目偏好 数据格式为 cateId1:count1,cateId2:count2
        String hobbyStr = redisUtil.getString(CommonConstants.LEVEL_HOBBY_PREFIX + uuid);
        logMap.put(UCPREFER, hobbyStr);
        return productExposureUtil.splitIdAndScore(hobbyStr);
    }
}
