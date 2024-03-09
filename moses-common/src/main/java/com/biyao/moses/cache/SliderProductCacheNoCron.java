package com.biyao.moses.cache;

import com.alibaba.fastjson.JSONObject;
import com.biyao.moses.common.constant.MatchRedisKeyConstant;
import com.biyao.moses.model.match.ProductImage;
import com.biyao.moses.params.ProductInfo;
import com.biyao.moses.util.FilterUtil;
import com.biyao.moses.util.MatchRedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class SliderProductCacheNoCron {

    private Map<Long, ProductImage> productImageMap = new HashMap<>();

    private List<Long> productList = new ArrayList<>();

    // 高转化轮播图
    private List<String> highConvList = new ArrayList<>();

    // 热销轮播图
    private List<String> hotSaleList = new ArrayList<>();

    // 天气对应的商品列表
    private Map<String, List<String>> weatherList = new HashMap<>();

    // 上新基础流量
    private List<String> baseProductList = new ArrayList<>();

    //高转化地决策流量
    private List<String> highAndLowList = new ArrayList<>();

    @Resource
    private MatchRedisUtil matchRedisUtil;

    @Autowired
    private ProductDetailCacheNoCron productDetailCacheNoCron;

    protected void init() {
        refreshCache();
    }

    /**
     * 刷新内存缓存
     */
    protected void refreshCache() {
        log.info("[任务进度][轮播图商品缓存]开始获取轮播图商品缓存");
        long startTime = System.currentTimeMillis();
        try {
            highAndLowList = loadHighConvAndLowDecisionPids();
            highConvList = loadHighConvHomeSlidePids();
            hotSaleList = loadHotSaleHomeSlidePids();
            weatherList = loadWeatherProducts();
            baseProductList  = loadBaseNewArrival();

            Map<String, String> candidateProductMap = matchRedisUtil.hgetAll(MatchRedisKeyConstant.PRODUCT_CANDIDATE_SET);
            if (candidateProductMap != null && candidateProductMap.size() > 0) {
                Map<Long, ProductImage> tempProductImageMap = new HashMap<>();
                List<Long> tempProductList = new ArrayList<>();
                candidateProductMap.forEach((productIdStr, candidateProductStr) -> {
                    try {
                        Long productId = Long.valueOf(productIdStr);
                        tempProductList.add(productId);
                        ProductImage productImage = new ProductImage();
                        productImage.setProductId(productId);
                        if (!StringUtils.isEmpty(candidateProductStr)) {
                            JSONObject jsonObject = JSONObject.parseObject(candidateProductStr);
                            productImage.setImage(jsonObject.getString("image"));
                            productImage.setWebpImage(jsonObject.getString("webp"));
                            productImage.setRouteType(Integer.parseInt(jsonObject.getString("routerType")));
                            String routeParams = jsonObject.getString("routeParams");
                            if (!StringUtils.isBlank(routeParams)){
                                productImage.setRouteParams(JSONObject.parseObject(routeParams, Map.class));
                            }
                            tempProductImageMap.put(productId, productImage);
                        }
                    } catch (Exception e) {
                        log.error("[严重异常][轮播图商品缓存]轮播图候选集解析失败：pid={}, content={}", productIdStr, candidateProductStr, e);
                    }
                });

                if (tempProductImageMap.size() > 0) {
                    this.productImageMap = tempProductImageMap;
                    log.info("[任务进度][轮播图商品缓存]获取轮播图商品缓存耗时 {}ms, 个数 {}", System.currentTimeMillis()-startTime, tempProductImageMap.size());
                }

                if (tempProductList.size() > 0) {
                    this.productList = tempProductList;
                }
            }
        } catch (Exception e) {
            log.error("[严重异常][轮播图商品缓存]轮播图候选集缓存刷新失败：", e);
        }
    }

    /**
     * 获取全部候选商品ID
     *
     * @return
     */
    public List<Long> getProductList() {
        return productList;
    }

    /**
     * 根据商品ID获取商品图片及跳转信息
     *
     * @param productId
     * @return
     */
    public ProductImage getProductImageById(Long productId) {
        return productImageMap.get(productId);
    }

    public Map<Long, ProductImage> getProductImageMap() {
        return productImageMap;
    }

    public List<String> getHighConvList() {
        return highConvList;
    }

    public List<String> getHotSaleList() {
        return hotSaleList;
    }

    public Map<String, List<String>> getWeatherList() {
        return weatherList;
    }

    public List<String> getBaseProductList() {
        return baseProductList;
    }

    public List<String> getHighAndLowList (){return  highAndLowList;}

    /**
     * 获取高转化低决策的首页轮播图
     * @return
     */
    private List<String> loadHighConvAndLowDecisionPids(){
        //查询高转化低决策商品 String 类型 moses:10300162_SPM_2005 value=pid:{score},pid:{score}...
        String highAndLowStr = matchRedisUtil.getString(MatchRedisKeyConstant.CONVERSION_LOWER_PRODUCT);
        return splitIdAndScore(highAndLowStr);
    }

    /**
     * 获取高转化率的首页轮播图
     * @return
     */
    private List<String> loadHighConvHomeSlidePids(){
        // 查询高转化率商品 高转化：string类型 moses:10300162_SPM_2001 value=pid:{转化率},pid:{转化率}...
        String consStr = matchRedisUtil.getString(MatchRedisKeyConstant.CONVERSION_PRODUCT_EXPNUM);
        return splitIdAndScore(consStr);
    }

    /**
     * 获取热销的首页轮播图
     * @return
     */
    private List<String> loadHotSaleHomeSlidePids(){
        String hotSaleStr = matchRedisUtil.getString(MatchRedisKeyConstant.HOT_PRODUCT_EXPNUM);
        return splitIdAndScore(hotSaleStr);
    }

    /**
     * 获取推荐商品ID列表
     *
     * @param productScoreStr 数据格式 pid1:score1,pid2:score2...或者pid1,pid2...
     * @return
     */
    private List<String> splitIdAndScore(String productScoreStr) {
        if (org.apache.commons.lang3.StringUtils.isBlank(productScoreStr)) {
            return new ArrayList<>();
        }

        List<String> rcdProductList = Arrays.stream(productScoreStr.split(",")).map(productScore -> {
            return productScore.split(":")[0];
        }).collect(Collectors.toList());

        commonFilterProduct(rcdProductList);
        return rcdProductList;
    }

    // 获取基础上新流量池
    private List<String>  loadBaseNewArrival(){
        String baseNewArrival = matchRedisUtil.getString(MatchRedisKeyConstant.BASE_PRODUCT_EXPNUM);
        return splitIdAndScore(baseNewArrival);
    }

    private Map<String, List<String>> loadWeatherProducts(){
        Map<String, String> weatherStr = matchRedisUtil.hgetAll(MatchRedisKeyConstant.WEATHER_PRODUCT_EXPNUM);
        Map<String, List<String>>  result = new HashMap<>();
        if(weatherStr == null || weatherStr.size() == 0){
            return result;
        }

        Iterator<Map.Entry<String, String>> iterator = weatherStr.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> next = iterator.next();
            String weather = next.getKey();
            String pids = next.getValue();
            if(!StringUtils.isEmpty(pids)){
                result.put(weather, splitIdAndScore(pids));
            }
        }

        return result;
    }

    /**
     * 通用条件过滤商品
     * @param productList
     */
    public void commonFilterProduct(List<String> productList){
        if(CollectionUtils.isEmpty(productList)){
            return;
        }
        Set<String> pidSet = new HashSet<>();
        Iterator<String> iterator = productList.iterator();
        while(iterator.hasNext()){
            String productId = iterator.next();
            if(StringUtils.isBlank(productId)){
                iterator.remove();
                continue;
            }
            //去重
            if(pidSet.contains(productId)){
                iterator.remove();
                continue;
            }
            pidSet.add(productId);

            ProductInfo productInfo = productDetailCacheNoCron.getProductInfo(Long.valueOf(productId));
            if(FilterUtil.isCommonFilter(productInfo)){
                iterator.remove();
                continue;
            }
        }
    }

 }
