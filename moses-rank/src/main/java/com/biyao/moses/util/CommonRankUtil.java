package com.biyao.moses.util;

import com.alibaba.fastjson.JSON;
import com.biyao.moses.cache.ProductDetailCacheNoCron;
import com.biyao.moses.common.constant.CommonConstants;
import com.biyao.moses.common.constant.RedisKeyConstant;
import com.biyao.moses.params.ProductInfo;
import com.biyao.moses.rpc.UcRpcService;
import com.uc.domain.bean.User;
import com.uc.domain.constant.UserFieldConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 公共的通用排序方法类
 * @author xiaojiankai
 * @date 2019年8月5日
 */
@Slf4j
@Component
public class CommonRankUtil {

    // 足迹时间限定在最近3天
    private static final int LAST_VIEW_TIME_LIMIT = 259200000;//3*24*3600*1000;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private ProductDetailCacheNoCron productDetailCache;

    @Autowired
    private UcRpcService ucRpcService;

    /**
     * 获取用户性别
     * UNKNOWN_SEX = "-1"：未知性别， COMMON_SEX = "2"：中性，
     * MALE_SEX = "0"：男性，FEMALE_SEX = "1"：女性
     * @param uuid
     * @return
     */
    public String getUserSex(String uuid, String uid){
        String result = CommonConstants.UNKNOWN_SEX;
        List<String> fields = new ArrayList<>();
        fields.add(UserFieldConstants.SEX);
        User ucUser = ucRpcService.getData(uuid, uid, fields, "mosesrank");
        if (ucUser != null && ucUser.getSex() != null){
            result = ucUser.getSex().toString();
        }
        return result;
    }

    /**
     * 首单转化率String格式如下：pid:forderRate,pid:forderRate,...
     * @param pids 表示商品ID的合集，若为空(NULL或空集合)，则返回所有解析的结果；若非空，则只返回pids对应的解析结果
     * @return
     */
    public Map<String, String> getForderRate(List<String> pids){
        Map<String,String> result = new HashMap<>();
        String productForderRate = redisUtil.getString(RedisKeyConstant.MOSES_PRODUCT_FORDER_RATE_KEY);
        Map<String, String> map = parseStringToMap(productForderRate);
        if(CollectionUtils.isNotEmpty(pids)){
            for (String pid : pids){
                if(map.containsKey(pid)){
                    result.put(pid,map.get(pid));
                }else{
                    result.put(pid,"0");
                }
            }
        }else{
            result = map;
        }
        return result;
    }

    /**
     * 将如下格式的String： key:value,key:value,key:value...
     * @param param
     * @return
     */
    public Map<String, String> parseStringToMap(String param){
        Map<String, String> result = new HashMap<>();
        if(StringUtils.isNotBlank(param)){
            String[] values = param.split(",");
            for(String value: values){
                String[] tmp = value.split(":");
                if(tmp.length == 2 && StringUtils.isNotBlank(tmp[0]) && StringUtils.isNotBlank(tmp[1])) {
                    result.put(tmp[0], tmp[1]);
                }
            }
        }
        return result;
    }

    /**
     * 获取用户近3天的浏览商品记录
     * @param uuid
     * @return
     */
    public List<String> getView3DayProductList(String uuid){
        // 用户浏览历史是lpush，此处用lrange取出时，也可以保证最新浏览的在最前面。
        // 格式为：pid,time:pid,time...
        List<String> viewProductValues = redisUtil.lrange(RedisKeyConstant.USER_VIEW_RDK_PREFIX + uuid, 0, 100);
        List<String> view3DayProductList = new ArrayList<>();
        Long currentTime = System.currentTimeMillis();
        if(CollectionUtils.isNotEmpty(viewProductValues)){
            for(String viewProductValue : viewProductValues){
                if (StringUtils.isNotBlank(viewProductValue)) {
                    String[] productValue = viewProductValue.split(":");
                    if(productValue.length != 2){
                        continue;
                    }
                    String productId = productValue[0];
                    String time = productValue[1];
                    if(StringUtils.isEmpty(time)){
                        continue;
                    }
                    if (currentTime - Long.valueOf(time) <= LAST_VIEW_TIME_LIMIT) {
                        view3DayProductList.add(productId);
                    }
                }
            }
        }
        return view3DayProductList;
    }

    /**
     * 获取用户三级类目偏好信息
     * @param uuid
     * @return
     */
    public Map<String,String> getLevel3Hobby(String uuid){
        //格式为： cate3Id:count,cate3Id:count,...  其中：cate3Id为3级类目Id，count为用户对该三级类目下商品的浏览次数
        String userhobbyCate3Str = redisUtil.getString(RedisKeyConstant.KEY_PREFIEX_LEVEL3_HOBBY + uuid);
        //key为三级类目Id,value为浏览次数
        return parseStringToMap(userhobbyCate3Str);
    }

    /**
     * 获取曝光商品信息
     * @param exposureRedisKey
     * @return
     */
    public List<String> getExposureProducts(String exposureRedisKey){
        List<String> result = new ArrayList<>();
        if(StringUtils.isNotBlank(exposureRedisKey)) {
            //格式为pid,pid,pid...
            String exposureProducts = redisUtil.getString(exposureRedisKey);
            if (StringUtils.isNotBlank(exposureProducts)) {
                String[] pids = exposureProducts.split(",");
                result = Arrays.stream(pids).filter(p -> StringUtils.isNotBlank(p)).collect(Collectors.toList());
            }
        }
        return result;
    }

    /**
     * 获取对用户曝光但无点击的商品
     * @param allProducts
     * @param exposureProducts
     * @param view3DayProducts
     * @return
     */
    private List<String> getExposureAndNoClickProduct(List<String> allProducts, Set<String> exposureProducts, Set<String> view3DayProducts){
        List<String> result = new ArrayList<>();
        //校验参数，如果商品信息为空，或无曝光商品，则返回空集合
        if(CollectionUtils.isEmpty(allProducts) || exposureProducts.size() == 0){
            return result;
        }

        for(String productId : allProducts){
            try{
                //有浏览记录则直接跳过
                if(view3DayProducts.contains(productId)){
                    continue;
                }
                //没有曝光则直接跳过
                if(!exposureProducts.contains(productId)){
                    continue;
                }
                result.add(productId);
            }catch(Exception ex){
                log.error("处理对用户曝光但无点击的商品Id{}时，发出异常{}",productId,JSON.toJSONString(ex));
            }
        }
        return result;
    }

    /**
     * 通用排序规则
     * @param products
     * @param uuid
     * @param exposureRedisKey 曝光商品Redis key
     * @return
     */
    public List<String> commonSort(List<String> products, String uuid, String uid,  String exposureRedisKey ){
        List<String> result = new ArrayList<>();
        List<String> exposureProducts = getExposureProducts(exposureRedisKey);
        List<String> view3DayProductList = getView3DayProductList(uuid);
        Map<String, String> forderRateMap = getForderRate(products);
        Map<String, String> level3HobbyMap = getLevel3Hobby(uuid);
        String userSex = getUserSex(uuid, uid);
        
        Set<String> exposureProductSet = new HashSet<>(exposureProducts);
        Set<String> view3DayProductSet = new HashSet<>(view3DayProductList);
        List<String> exposureAndNoClickProducts = getExposureAndNoClickProduct(products, exposureProductSet, view3DayProductSet);
        List<String> filterExposureAndNoClickProducts = new ArrayList<>(products);
        if(CollectionUtils.isNotEmpty(exposureAndNoClickProducts)) {
            filterExposureAndNoClickProducts.removeAll(exposureAndNoClickProducts);
        }

        List<String> sortedProducts = null;
        if(CollectionUtils.isNotEmpty(view3DayProductList)) {
            sortedProducts = sortHasView(filterExposureAndNoClickProducts, userSex, forderRateMap, level3HobbyMap, view3DayProductList);
        }else{
            sortedProducts = sortNoView(filterExposureAndNoClickProducts, userSex, forderRateMap);
        }
        List<String> sortedExposureAndNoClickProducts = sortNoView(exposureAndNoClickProducts, userSex, forderRateMap);

        if(CollectionUtils.isNotEmpty(sortedProducts)) {
            result.addAll(sortedProducts);
        }
        if(CollectionUtils.isNotEmpty(sortedExposureAndNoClickProducts)) {
            result.addAll(sortedExposureAndNoClickProducts);
        }
        //打散
        result = reorderByCate3IdDiscontinuous(result);
        return result;
    }

    /**
     * 用户无浏览行为时的排序规则
     * @param products 待排序商品pid集合
     * @param sex 用户性别
     * @param forderRateMap 商品7日首单转化率
     * @return
     */
    public List<String> sortHasView(List<String> products, String sex, Map<String, String> forderRateMap, Map<String,String> level3HobbyMap, List<String> view3DayProductList){
        List<String> result = new ArrayList<>();

        List<List<String>> productLayerByHobbyCate3Id = productLayerByUserHobbyCate3Id(products, level3HobbyMap);

//        log.error("productLayerByhobbyCate3Id {}", JSON.toJSONString(productLayerByHobbyCate3Id));

        //非用户三级类目偏好的商品
        List<String> productNoHobbyCate3Id = new ArrayList<>(products);

        //三级类目偏好的每个分组中获取有浏览记录的商品
        for (List<String> layerByhobbyList : productLayerByHobbyCate3Id) {
            productNoHobbyCate3Id.removeAll(layerByhobbyList);
            List<String> noscanProduct = new ArrayList<>(layerByhobbyList);
            List<String> scanProduct = new ArrayList<>(layerByhobbyList);
            scanProduct.retainAll(view3DayProductList);
            noscanProduct.removeAll(scanProduct);
            List<List<String>> sexLayerProducts = productLayerBySex(noscanProduct, sex);
            for(List<String> sexProducts : sexLayerProducts){
                if(CollectionUtils.isEmpty(sexProducts)){
                    continue;
                }
                List<String> list = sortByForderRate(sexProducts, forderRateMap);
                result.addAll(list);
            }

            //再随机插入已浏览的商品
            Collections.shuffle(scanProduct);
            result.addAll(scanProduct);
        }

//        log.error("productNoHobbyCate3Id {}", JSON.toJSONString(productNoHobbyCate3Id));
        //针对非用户三级类目偏好的商品按无浏览行为的情况排序
        List<String> sortNoHobbyProduct = sortNoView(productNoHobbyCate3Id, sex, forderRateMap);
        result.addAll(sortNoHobbyProduct);
        return result;
    }

    /**
     * 用户无浏览行为时的排序规则
     * @param products 待排序商品pid集合
     * @param sex 用户性别
     * @param forderRateMap 商品7日首单转化率
     * @return
     */
    public List<String> sortNoView(List<String> products, String sex, Map<String, String> forderRateMap){
        List<String> result = new ArrayList<>();
        if(CollectionUtils.isEmpty(products)){
            return result;
        }
        List<List<String>> sexLayerProducts = productLayerBySex(products, sex);
        List<List<String>> sexAndPOrderLayerProducts = new ArrayList<>();
        for(List<String> sexProducts : sexLayerProducts){
            List<List<String>> lists = productLayerByPorder(sexProducts);
            sexAndPOrderLayerProducts.addAll(lists);
        }

//        log.error("sexLayerProducts {}", JSON.toJSONString(sexLayerProducts));
//        log.error("sexAndPOrderLayerProducts {}", JSON.toJSONString(sexAndPOrderLayerProducts));

        for(List<String> sexAndPorderProducts : sexAndPOrderLayerProducts){
            List<String> list = sortByForderRate(sexAndPorderProducts, forderRateMap);
            result.addAll(list);
        }

        return result;
    }

    /**
     * 根据用户性别对商品进行分组
     * @param products
     * @param sex
     * @return
     */
    private List<List<String>> productLayerBySex(List<String> products, String sex){
        List<List<String>> result = new ArrayList<>();
        //若可以获取到用户性别，则将新手专享商品分为用户同性别商品、通用性别商品和非用户性别商品
        if (StringUtils.isBlank(sex) || CommonConstants.UNKNOWN_SEX.equals(sex)){
            result.add(products);
        }else{
            List<String> sexSameProductList = new ArrayList<>();
            List<String> sexNosameProductList = new ArrayList<>();
            List<String> othersProductList = new ArrayList<>();
            for (String product : products){
                try{
                    ProductInfo productInfo = productDetailCache.getProductInfo(Long.valueOf(product));
                    if(productInfo == null) {continue;}
                    String productGender = productInfo.getProductGender() == null ? CommonConstants.UNKNOWN_SEX : productInfo.getProductGender().toString();
                    if(sex.equals(productGender)){
                        sexSameProductList.add(product);
                    }else if((sex.equals(CommonConstants.MALE_SEX) && productGender.equals(CommonConstants.FEMALE_SEX)) ||
                            (sex.equals(CommonConstants.FEMALE_SEX) && productGender.equals(CommonConstants.MALE_SEX)) ){
                        sexNosameProductList.add(product);
                    }else{
                        othersProductList.add(product);
                    }
                }catch (Exception e){
                    log.error("根据用户性别进行分组，处理发生错误：{}",e);
                }
            }
            //加入顺序为：用户性别商品、通用性别商品、非用户性别商品
            result.add(sexSameProductList);
            result.add(othersProductList);
            result.add(sexNosameProductList);
        }
        return result;
    }

    /**
     * 根据商品销量进行分组，销量从高到低排序，前25%为一组，25%~75%为一组，后75%为一组
     * @param products
     * @return
     */
    private List<List<String>> productLayerByPorder(List<String> products){
        List<List<String>> result = new ArrayList<>();
        List<String> orderHighPri = new ArrayList<>();
        List<String> orderMiddlePri= new ArrayList<>();
        List<String> orderLowPri = new ArrayList<>();
        if(products == null){
            return result;
        }
        //如果商品个数小于等于4，则不需要将商品按销量进行分组
        if(products.size() <= 4){
            orderHighPri.addAll(products);
            result.add(orderHighPri);
            return result;
        }
        //将商品按销量降序排序
        products.sort((p1,p2)->{
            ProductInfo proInfo;
            Long p1Order = 0L;
            Long p2Order = 0L;
            if((proInfo = productDetailCache.getProductInfo(Long.valueOf(p1))) != null && proInfo.getSalesVolume7() != null){
                p1Order = proInfo.getSalesVolume7();
            }
            if((proInfo = productDetailCache.getProductInfo(Long.valueOf(p2))) != null && proInfo.getSalesVolume7() != null){
                p2Order = proInfo.getSalesVolume7();
            }
            return -p1Order.compareTo(p2Order);
        });


        int size = products.size();
        int highPriMaxIndex =  size / 4;
        int middlePriMaxIndex = size - highPriMaxIndex;
        orderHighPri.addAll(products.subList(0,highPriMaxIndex));
        orderMiddlePri.addAll(products.subList(highPriMaxIndex, middlePriMaxIndex));
        orderLowPri.addAll(products.subList(middlePriMaxIndex,size));

        if(orderHighPri.size() > 0) {
            result.add(orderHighPri);
        }
        if(orderMiddlePri.size() > 0) {
            result.add(orderMiddlePri);
        }
        if(orderLowPri.size() > 0) {
            result.add(orderLowPri);
        }
        return result;
    }

    /**
     * 根据首单转化率降序排序
     * @param productList
     * @param productForderRateMap
     * @return
     */
    private List<String> sortByForderRate(List<String> productList,  Map<String, String> productForderRateMap){

        if(CollectionUtils.isEmpty(productList) || productForderRateMap == null || productForderRateMap.size()==0){
            return productList;
        }

        try{
            productList.sort((p1, p2) -> -Double.valueOf(productForderRateMap.getOrDefault(p1,"0")).compareTo(Double.valueOf(productForderRateMap.getOrDefault(p2,"0"))));
        }catch (Exception e){
            log.error("按照商品转化率排序失败：{}",e);
        }
        return productList;
    }

    /**
     * 获取商品对应的3级类目Id，二级类目是眼镜的需要特殊处理
     * @param productInfo
     * @return
     */
    private Long getCate3Id(ProductInfo productInfo){
        if(productInfo == null) return null;
        // 二级类目是眼镜的需要特殊处理
        if(productInfo.getSecondCategoryId() != null && CommonConstants.SPECIAL_CATEGORY2_IDS.contains(productInfo.getSecondCategoryId())){
            return productInfo.getSecondCategoryId();
        }else{
            return productInfo.getThirdCategoryId();
        }
    }

    /**
     * 根据用户喜爱的三级类目分层，按三级类目下的浏览商品次数降序排序
     * @param productList
     * @param userHobbyCate3IdMap
     * @return
     */
    private List<List<String>> productLayerByUserHobbyCate3Id(List<String> productList,Map<String, String> userHobbyCate3IdMap){
        List<List<String>> result = new ArrayList<>();
        if(CollectionUtils.isEmpty(productList) || userHobbyCate3IdMap == null || userHobbyCate3IdMap.size() == 0){
            result.add(productList);
            return result;
        }
        try{
            //根据用户喜爱的三级类目的浏览商品次数降序排序
            List<Map.Entry<String,String>> list = new ArrayList<>(userHobbyCate3IdMap.entrySet());
            list.sort((o1, o2) -> -Long.valueOf(o1.getValue()).compareTo(Long.valueOf(o2.getValue())));

            list.forEach(map -> {
                List<String> layerList = new ArrayList<>();
                productList.forEach(product -> {
                    ProductInfo productInfo = productDetailCache.getProductInfo(Long.valueOf(product));
                    Long cate3Id = getCate3Id(productInfo);
                    if (productInfo != null && cate3Id != null && map.getKey().equals(cate3Id.toString())) {
                        layerList.add(product);
                    }
                });
                if (layerList.size() > 0) {
                    result.add(layerList);
                }
            });
        }catch(Exception e) {
            log.error("处理根据用户喜爱的三级类目分组失败：{}",e);
            result.add(productList);
        }
        return result;
    }

    /**
     * 返回与入参product1 和product2 第一个不同的3级类目的商品信息
     *   若没有满足条件的商品，则找到与product2 第一个不同的3级类目的商品信息
     *     若还是没有满足条件的商品，则返回第一个商品
     * @param products
     * @param product1
     * @param product2
     * @return
     */
    private String getProductOfNotSameCate3Id(List<String> products, String product1, String product2){
        if(CollectionUtils.isEmpty(products)) return null;
        String result = null;
        try {
            ProductInfo product1Info = product1 == null ? null : productDetailCache.getProductInfo(Long.valueOf(product1));
            ProductInfo product2Info = product2 == null ? null : productDetailCache.getProductInfo(Long.valueOf(product2));
            Long product1Cate3Id = getCate3Id(product1Info);
            Long product2Cate3Id = getCate3Id(product2Info);
            Boolean firstNoSameProduct2 = false;
            String productNoSame = null;
            Boolean firstNoSameAll = false;
            String productNoSameAll = null;
            for (String product : products) {
                ProductInfo productInfo = productDetailCache.getProductInfo(Long.valueOf(product));
                Long productCate3Id = getCate3Id(productInfo);
                if (productCate3Id == null) {
                    return product;
                } else if (product1Cate3Id != null && product2Cate3Id != null) {
                    if (!firstNoSameProduct2 && productCate3Id.intValue() != product2Cate3Id.intValue()) {
                        firstNoSameProduct2 = true;
                        productNoSame = product;
                    }
                    if (productCate3Id.intValue() != product1Cate3Id.intValue() && productCate3Id.intValue() != product2Cate3Id.intValue()) {
                        firstNoSameAll = true;
                        productNoSameAll = product;
                        break;
                    }
                }else if (product1Cate3Id != null && product2Cate3Id == null) {
                    if(productCate3Id.intValue() != product1Cate3Id.intValue()){
                        firstNoSameAll = true;
                        productNoSameAll = product;
                        break;
                    }
                }else if (product1Cate3Id == null && product2Cate3Id != null) {
                    if(productCate3Id.intValue() != product2Cate3Id.intValue()){
                        firstNoSameAll = true;
                        productNoSameAll = product;
                        break;
                    }
                }
            }

            result =  firstNoSameAll ? productNoSameAll : (firstNoSameProduct2 ? productNoSame : products.get(0));
        }catch(Exception e){
            log.error("查找不同的三级类目商品失败：{}",e);
            result = products.get(0);
        }
        return result;
    }

    /**
     * 对排序后的商品进行打散：同三级类目商品之间间隔2个
     * @param products
     * @return
     */
    public List<String> reorderByCate3IdDiscontinuous(List<String> products){
        if (CollectionUtils.isEmpty(products)) return products;
        int size = products.size();
        List<String> baseProducts = new ArrayList<>(products);
        List<String> result = new ArrayList<>();
        try {
            for(int i = 0; i < size; i++) {
                if (result.size() == 0) {
                    result.add(baseProducts.get(0));
                    baseProducts.remove(0);
                } else if (result.size() == 1) {
                    String productOfNotSameCate3Id = getProductOfNotSameCate3Id(baseProducts, result.get(0), null);
                    if(productOfNotSameCate3Id != null) {
                        result.add(productOfNotSameCate3Id);
                        baseProducts.remove(productOfNotSameCate3Id);
                    }
                } else {
                    int length = result.size();
                    String productOfNotSameCate3Id = getProductOfNotSameCate3Id(baseProducts, result.get(length - 2), result.get(length - 1));
                    if(productOfNotSameCate3Id != null) {
                        result.add(productOfNotSameCate3Id);
                        baseProducts.remove(productOfNotSameCate3Id);
                    }
                }
            }
        }catch (Exception e){
            log.error("打散时发生错误：{}",e);
            result = products;
        }
        return result;
    }

}
