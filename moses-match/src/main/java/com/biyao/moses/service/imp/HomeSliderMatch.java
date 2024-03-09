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
import com.biyao.moses.model.feature.UserFeature;
import com.biyao.moses.model.match.ProductImage;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.service.RecommendMatch;
import com.biyao.moses.util.FilterUtil;
import com.biyao.moses.util.HttpClientUtil;
import com.biyao.moses.util.ProductExposureUtil;
import com.biyao.moses.util.RedisUtil;
import com.biyao.moses.params.ProductInfo;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 轮播图match
 *
 * @author zyj
 * @Description
 * @Date 2018年9月27日
 */
@Slf4j
@Component("homeSliderMatch")
public class HomeSliderMatch implements RecommendMatch {

    @Autowired
    private RedisUtil redisUtil;
 
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
    @Autowired
    FilterUtil filterUtil;

    private static DCLogger mosesuiDcLogger = DCLogger.getLogger("moses_slide_exposure");

    @BProfiler(key = "com.biyao.moses.service.imp.HomeSliderMatch.executeRecommendMatch", monitorType = {MonitorType.TP,
            MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR})
    @Override
    public Map<String, List<TotalTemplateInfo>> executeRecommendMatch(
            String dataKey, MatchDataSourceTypeConf mdst, String uuId) {
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

            // 判断访客、老客 (访客 0、老客 1)，默认0
            Integer userType = mdst.getUpcUserType();
            baseLogMap.put(UTYPE, userType.toString());

            // 用户特征
            UserFeature userFeature = UserFeature.builder()
                    .uuid(uuId)
                    .sex(sex)
                    .preferCategoryList(userPreferCate3Ids)
                    .build();
            // resultProduct:最后的结果集 6个商品
            List<String> resultProduct = new ArrayList<>();
            // 老客 天气推荐1个商品，基础上新推荐2个，高转化推荐3-4个。如果最终不足6个，则用热销补齐
            // 天气商品
            List<String> weatherMatch = new ArrayList<>(getProductByWeather(dataKey, mdst, uuId, baseLogMap));
            // 基础上新商品
            List<String> baseList = sliderProductCache.getBaseProductList();
            List<String> baseProductMatch = productExposureUtil.sortByUserFeature(baseList, userFeature);

            // 高转化商品
            List<String> highConvList = sliderProductCache.getHighConvList();
            List<String> highConvProductMatch = productExposureUtil.sortByUserFeature(highConvList, userFeature);

            // 热销商品
            List<String> hotSaleList = sliderProductCache.getHotSaleList();
            List<String> hotSaleProductMatch = productExposureUtil.sortByUserFeature(hotSaleList, userFeature);

            if (userType == UPCUserTypeConstants.CUSTOMER) {
                //构建结果集 天气1个，热销4个 基础上新1个 高转化1个
                resultProduct = buildBuyerHomeSlide(weatherMatch, baseProductMatch, highConvProductMatch,
                        hotSaleProductMatch, tacticsLogMap,mdst.getSiteId());

            } else {   // 新老访客
                //构建结果集 天气1个 热销5(6)个
                resultProduct = buildVisitorHomeSlide(weatherMatch, hotSaleProductMatch, tacticsLogMap,mdst.getSiteId());
            }

            // 如果有异常不足6个，用热销补齐
            List<String> hotSaleFillPidList = new ArrayList<>();
            if (resultProduct.size() < 6) {
                for (String pid : hotSaleList) {
                    if (resultProduct.contains(pid)){
                        continue;
                    }
                    if(StringUtils.isBlank(pid)){
                        continue;
                    }
                    Long productId = Long.parseLong(pid);
                    if(filterUtil.isFilteredBySiteId(productId,mdst.getSiteId())){
                        continue;
                    }
                    resultProduct.add(pid);
                    hotSaleFillPidList.add(pid);
                    if (resultProduct.size() >= 6) {
                        break;
                    }
                }
                dclogPut(tacticsLogMap, MatchStrategyEnum.HS.getName(), hotSaleFillPidList);
            }
            // 处理本次曝光的记录
            viewedSlidePids.addAll(resultProduct);
            productExposureUtil.setViewedSlideProducts(uuId, resultProduct);
            printHomeSlideShowLog(mdst, uuId, resultProduct, baseLogMap, tacticsLogMap); // 上传曝光日志

            totaltemplateInfoList = generateResult(resultProduct);
            resultMap.put(dataKey, totaltemplateInfoList);
        } catch (Exception e) {
            log.error("[严重异常]新客轮播图 HomeSliderMatch 异常！datakey：{},uuid:{}",dataKey,uuId, e);
        }

        //如果出现问题 使用热销随机6个兜底
        if (totaltemplateInfoList.size() < 6) {
            List<String> resultProduct = new ArrayList<String>();
            List<String> hotSaleList = sliderProductCache.getHotSaleList();
            // 过滤掉不支持用户所持有端的商品
            hotSaleList = hotSaleList.stream().filter(pid -> {
                if(StringUtils.isBlank(pid)){
                    return false;
                }
                long productId = Long.parseLong(pid);
                if (filterUtil.isFilteredBySiteId(productId,mdst.getSiteId())) {
                    return false;
                }
                return true;
            }).collect(Collectors.toList());
            resultProduct.addAll(hotSaleList);
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
     * 新老访客轮播图逻辑
     * @param weatherMatch
     * @param hotSaleProductMatch
     * @param tacticsLogMap
     * @return
     */
    private List<String> buildVisitorHomeSlide(List<String> weatherMatch, List<String> hotSaleProductMatch,
                                               Map<String, String> tacticsLogMap,String siteId) {
        // 第一轮：完全岔开
        List<String> resultProduct = new ArrayList<>();

        Set<Long> existSuppliers = new HashSet<>();
        Set<Long> existCate3Ids = new HashSet<>();
        // 天气比较特殊，只出1个，出在第一个位置
        List<String> weaList = fetchProductFromCandidate(resultProduct, existSuppliers, existCate3Ids, weatherMatch, 1,siteId);
        dclogPut(tacticsLogMap, MatchStrategyEnum.WEA.getName(), weaList);
        resultProduct.addAll(weaList);

        // 热销，最多出2个
        List<String> hotList = fetchProductFromCandidate(resultProduct,
                existSuppliers, existCate3Ids, hotSaleProductMatch, 6,siteId);
        dclogPut(tacticsLogMap, MatchStrategyEnum.HS.getName(), hotList);
        resultProduct.addAll(hotList);

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
     * @return
     */
    private List<String> buildBuyerHomeSlide(List<String> weatherMatch, List<String> baseProductMatch,
                                             List<String> highConvProductMatch, List<String> hotSaleProductMatch,
                                             Map<String, String> tacticsLogMap,String siteId) {
        List<String> resultProduct = new ArrayList<>();

        // 第一轮：完全岔开
        Set<Long> existSuppliers = new HashSet<>();
        Set<Long> existCate3Ids = new HashSet<>();

        // 天气比较特殊，只出1个，出在第一个位置
        List<String> weatherList = fetchProductFromCandidate(resultProduct,
                existSuppliers, existCate3Ids, weatherMatch, 1,siteId);
        dclogPut(tacticsLogMap, MatchStrategyEnum.WEA.getName(), weatherList);
        resultProduct.addAll(weatherList);

        // 热销，最多出4个
        List<String> hotList = fetchProductFromCandidate(resultProduct,
                existSuppliers, existCate3Ids, hotSaleProductMatch, 4,siteId);
        dclogPut(tacticsLogMap, MatchStrategyEnum.HS.getName(), hotList);
        resultProduct.addAll(hotList);

        // 基础流量，最多出1个
        List<String> baseList = fetchProductFromCandidate(resultProduct,
                existSuppliers, existCate3Ids, baseProductMatch, 1,siteId);
        dclogPut(tacticsLogMap, MatchStrategyEnum.NEW_LOW.getName(), baseList);
        resultProduct.addAll(baseList);

        // 高转化，最多出1个
        List<String> highConvList = fetchProductFromCandidate(resultProduct,
                existSuppliers, existCate3Ids, highConvProductMatch, 1,siteId);
        dclogPut(tacticsLogMap, MatchStrategyEnum.GZH_NEW.getName(), highConvList);
        resultProduct.addAll(highConvList);

        if (resultProduct.size() >= 6) {
            return resultProduct.subList(0, 6);
        }

        // 使用热销补齐
        List<String> fixHotList = fetchProductFromCandidate(resultProduct,
                    existSuppliers, existCate3Ids, hotSaleProductMatch, 6,siteId);
        dclogPut(tacticsLogMap, MatchStrategyEnum.HS.getName(), fixHotList);
        resultProduct.addAll(fixHotList);

        if (resultProduct.size() >= 6) {
            return resultProduct.subList(0, 6);
        }

        // 返回召回结果
        return resultProduct;
    }

    /**
     * 召回和exisitSupplier、existCate3不重复的结果集
     * @param existSupplier
     * @param existCate3
     * @param candidates
     * @param target        期望值
     * @return 实际结果
     */
    private List<String> fetchProductFromCandidate(List<String> alreadyRecList, //本次召回商品结果集
                                                   Set<Long> existSupplier, // 已经被筛选出来的商家
                                                   Set<Long> existCate3,    // 已经被筛选的三级类目
                                                   List<String> candidates,    // 候选集合
                                                   int target,
                                                   String siteId) {               // 期望的数量
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
            // 过滤掉不支持用户持有端的商品
           if(filterUtil.isFilteredBySiteId(productId,siteId)){
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

    private List<String> getUuidPreferCate3(String uuid, Map<String, String> logMap) {
        // 查询三级类目偏好 数据格式为 cateId1:count1,cateId2:count2
        String hobbyStr = redisUtil.getString(CommonConstants.LEVEL_HOBBY_PREFIX + uuid);
        logMap.put(UCPREFER, hobbyStr);
        return productExposureUtil.splitIdAndScore(hobbyStr);
    }
}
