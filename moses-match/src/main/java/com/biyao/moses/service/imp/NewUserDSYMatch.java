package com.biyao.moses.service.imp;

import com.alibaba.fastjson.JSON;
import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.common.constant.CacheRedisKeyConstant;
import com.biyao.moses.common.constant.CommonConstants;
import com.biyao.moses.common.constant.MatchRedisKeyConstant;
import com.biyao.moses.common.constant.RedisKeyConstant;
import com.biyao.moses.model.exp.MatchDataSourceTypeConf;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.service.RecommendMatch;
import com.biyao.moses.util.CacheRedisUtil;
import com.biyao.moses.util.FilterUtil;
import com.biyao.moses.util.MatchRedisUtil;
import com.biyao.moses.util.RedisUtil;
import com.biyao.moses.params.ProductInfo;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.apache.commons.lang3.StringUtils;
import java.util.*;

/**
 * 新手专享数据源1热门match
 *
 * @Description
 * @author xjk
 * @Date 2019年06月20日
 */

@Slf4j
@Component("NewUserDSYMatch")
public class NewUserDSYMatch implements RecommendMatch {

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private MatchRedisUtil matchRedisUtil;

    @Autowired
    private CacheRedisUtil cacheRedisUtil;

    @Autowired
    private ProductDetailCache productDetailCache;

    // 用户浏览商品 redis key prefix
    private static final String USER_VIEW_RDK_PREFIX = "moses:user_viewed_products_";

    // 足迹时间限定在最近3天
    private static final int LAST_VIEW_TIME_LIMIT = 259200000;//3*24*3600*1000;
    //用户近3天3级类目偏好
    private static final String KEY_PREFIEX_LEVEL3_HOBBY = "moses:level3hobby_";

    @BProfiler(key = "com.biyao.moses.service.imp.NewUserDSYMatch.executeRecommendMatch",
            monitorType = {MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR})
    @Override
    public Map<String, List<TotalTemplateInfo>> executeRecommendMatch(String dataKey, MatchDataSourceTypeConf mdst, String uuId) {
        Map<String, List<TotalTemplateInfo>> resultMap = new HashMap<>();
        List<TotalTemplateInfo> totalList = new ArrayList<>();
        List<String> productsByFilterList=null;
        try {
            //获取所有的新手专享商品 格式为：productId,productId,...
            String newuserSpecialProduct = matchRedisUtil.getString(MatchRedisKeyConstant.MOSES_NEWUSER_SPECIAL_PRODUCTS);
            if (StringUtils.isEmpty(newuserSpecialProduct)) {
                log.error("[严重异常][运营配置的新手专享召回]查询新手专享商品无数据，key{}", MatchRedisKeyConstant.MOSES_NEWUSER_SPECIAL_PRODUCTS);
                resultMap.put(dataKey, totalList);
                return resultMap;
            }
            String[] specialProducts = newuserSpecialProduct.split(",");
            //log.error("所有的新手专享商品集合：{}", JSON.toJSONString(specialProducts));
            //过滤掉非一起拼商品、非上架商品、非传入的前台一级类目商品
            productsByFilterList = filterProduct(specialProducts, mdst);
            //log.error("过滤掉非一起拼商品、非上架商品、非传入的前台一级类目商品：{}", JSON.toJSONString(productsByFilterList));
            String sxzx1sort_switch = matchRedisUtil.getString(MatchRedisKeyConstant.XSZX1_SORT_SWITCH);
            if(StringUtils.isNotBlank(sxzx1sort_switch)&&"0".equals(sxzx1sort_switch)){
                totalList=dealResult(totalList,productsByFilterList);
                resultMap.put(dataKey, totalList);
                return resultMap;
            }
        }catch (Exception e){
            log.error("[严重异常][运营配置的新手专享召回]获取新手专享数据源1异常 ",e);
            resultMap.put(dataKey, totalList);
            return resultMap;
        }

        try{
            //获取所有商品的7日商品付款订单总数
            //格式为： productId:porderNum,productId:porderNum,...  其中：productId为商品Id，porderNum 为7日商品付款订单总数
            String productPorderNum = redisUtil.getString(RedisKeyConstant.MOSES_PRODUCT_PORDER_NUM_KEY);
            Map<String,String> productPorderNumMap = parseStringToMap(productPorderNum);
            //log.error("所有商品的7日商品付款订单总数集合：{}", JSON.toJSONString(productPorderNumMap));

            //获取7日内商品首单转化率
            //格式为： productId:forderRate,productId:forderRate,...  其中：productId为商品Id，forderRate 为7日商品首单平均转化率
            String productForderRate = redisUtil.getString(RedisKeyConstant.MOSES_PRODUCT_FORDER_RATE_KEY);
            Map<String,String> productForderRateMap = parseStringToMap(productForderRate);
            //log.error("7日内商品首单转化率：{}", JSON.toJSONString(productForderRateMap));

            //获取用户性别
            String sex = mdst.getSex();

            //获取用户在全站近三天的浏览商品信息
            List<String> view3DayProductList = getView3DayProductList(uuId);
            //log.error("用户性别：{}，用户在全站近三天的浏览商品信息：{}",sex, JSON.toJSONString(view3DayProductList));


            //获取用户在全站近三天的浏览商品的三级类目信息
            //格式为： cate3Id:count,cate3Id:count,...  其中：cate3Id为3级类目Id，count为用户对该三级类目下商品的浏览次数
            String userhobbyCate3Str = redisUtil.getString(KEY_PREFIEX_LEVEL3_HOBBY + uuId);
            //key为三级类目Id,value为浏览次数
            Map<String,String> userhobbyCate3IdMap = parseStringToMap(userhobbyCate3Str);
            //log.error("用户在全站近三天的浏览商品的三级类目信息：{}", JSON.toJSONString(userhobbyCate3IdMap));


            //获取用户在新手专享数据源1曝光商品
            //格式为： productId:count,productId:count,...  其中：productId为商品ID，count为该商品对用户的曝光次数
            String exposureProducts = cacheRedisUtil.getString(CacheRedisKeyConstant.MOSES_NEWUSER_EXPOSURE_PRODUCT_PREFIX+uuId);
            //key为商品Id,value为曝光次数
            Map<String, String> exposureProductMap = parseStringToMap(exposureProducts);
            //log.error("用户在新手专享数据源1曝光商品：{}", JSON.toJSONString(exposureProductMap));

            //获取曝光3次但3天内无点击的商品
            //V1.1 曝光3次修改为曝光大于1次
            List<String> exposureLarge3AndNoScanList = getExposuerLarge3AndNoScanProduct(productsByFilterList,exposureProductMap,view3DayProductList);
            //对曝光大于1次但3天内无点击的商品进行排序
            List<String> sortedProductExposureLarge3AndNoScan = sortFilterProducts(exposureLarge3AndNoScanList,null,sex,productPorderNumMap,productForderRateMap,userhobbyCate3IdMap);
            //log.error("曝光3次但3天内无点击的商品：{},排序后商品集合：{}", JSON.toJSONString(exposureLarge3AndNoScanList), JSON.toJSONString(sortedProductExposureLarge3AndNoScan));

            List<String> productFilterExposureLarge3AndNoScan = new ArrayList<>(productsByFilterList);
            //去除掉曝光大于1次但3天内无点击商品
            productFilterExposureLarge3AndNoScan.removeAll(exposureLarge3AndNoScanList);
            //log.error("去除掉曝光后的商品的结果：{}, {}",JSON.toJSONString(productFilterExposureLarge3AndNoScan), JSON.toJSONString(sortedProductExposureLarge3AndNoScan));
            //去除曝光大于1次但3天内无点击的商品后剩下的商品进行排序
            List<String> sortedProduct = sortFilterProducts(productFilterExposureLarge3AndNoScan,view3DayProductList,sex,productPorderNumMap,productForderRateMap,userhobbyCate3IdMap);
            //log.error("过滤掉曝光1次但3天内无点击商品：{},排序后商品集合：{}", JSON.toJSONString(productFilterExposureLarge3AndNoScan), JSON.toJSONString(sortedProduct));

            //最后加入曝光大于1次但3天内无点击的商品
            //log.error("allAll前的的结果：{}", JSON.toJSONString(sortedProductExposureLarge3AndNoScan));
            sortedProduct.addAll(sortedProductExposureLarge3AndNoScan);
            //log.error("排序后的结果：{}", JSON.toJSONString(sortedProduct));

            //对排序后的商品信息进行每页随机，每页20个
            //V1.1 删除按页随机
//            int page_size = 20;
//            int page_num = sortedProduct.size() % page_size == 0 ? sortedProduct.size() / page_size : sortedProduct.size() / page_size + 1;
//            for(int i = 0; i < page_num; i++){
//                if((i+1) * page_size < sortedProduct.size()){
//                    Collections.shuffle(sortedProduct.subList(i*page_size,(i+1)*page_size));
//                }else{
//                    Collections.shuffle(sortedProduct.subList(i*page_size,sortedProduct.size()));
//                }
//            }
            //log.error("按页随机后的结果：{}", JSON.toJSONString(sortedProduct));

            //打散规则
            List<String> finallyProductList = reorderByCate3IdDiscontinuous(sortedProduct);
            //log.error("最终排序结果：{},打散后结果：{}", JSON.toJSONString(sortedProduct), JSON.toJSONString(finallyProductList));
            //log.error("排序完成");
            totalList=dealResult(totalList,finallyProductList);
            resultMap.put(dataKey, totalList);
            return resultMap;
        }catch(Exception e){
            log.error("[严重异常][运营配置的新手专享召回]新手专享数据源1热门match异常：",e);
        }
        totalList=dealResult(totalList,productsByFilterList);
        resultMap.put(dataKey, totalList);
        return resultMap;
    }

   private   List<TotalTemplateInfo> dealResult(List<TotalTemplateInfo> totalList,List<String> list){
       for (String productId : list) {
           TotalTemplateInfo tti = new TotalTemplateInfo();
           tti.setId(productId);
           //填充新手价格，只有价格有效，才添加到结果中
           ProductInfo productInfo = productDetailCache.getProductInfo(Long.valueOf(tti.getId()));
           if(productInfo != null && productInfo.getNovicePrice() != null && productInfo.getNovicePrice().signum() == 1){
               tti.setNovicePrice(String.valueOf(productInfo.getNovicePrice()));
               totalList.add(tti);
           }
       }
       return totalList;
   }

    /**
     * 将如下格式的String： category3Id:count,category3Id:count,category3Id:count
     * 解析为map, key 为category3Id, value为count
     * @param param
     * @return
     */
    private Map<String, String> parseStringToMap(String param){
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
     * @param uuId
     * @return
     */
    private List<String> getView3DayProductList(String uuId){
        // 用户浏览历史是lpush，此处用lrange取出时，也可以保证最新浏览的在最前面。
        List<String> viewProductValues = redisUtil.lrange(USER_VIEW_RDK_PREFIX + uuId, 0, 100);
        List<String> view3DayProductList = new ArrayList<>();
        Long currentTime = System.currentTimeMillis();
        if(CollectionUtils.isNotEmpty(viewProductValues)){
            for(String viewProductValue : viewProductValues){
                if (StringUtils.isNotBlank(viewProductValue)) {
                    String[] productValue = viewProductValue.split(":");
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
     * 过滤掉一起拼商品、非上架商品、不属于传入的前台一级类目的商品
     * @param products
     * @param mdst
     * @return
     */
    private List<String> filterProduct(String[] products, MatchDataSourceTypeConf mdst){
        List<String> result = new ArrayList<>();
        if(products == null || products.length == 0){
            return result;
        }

        String noviceFrontcategory1Id =  mdst.getNovicefrontcategoryOneId();
        Set<String> categoryIdSet = null;
        if(CollectionUtils.isNotEmpty(mdst.getCategoryIds())){
            categoryIdSet = new HashSet<>();
            categoryIdSet.addAll(mdst.getCategoryIds());
        }
        Set<String> scmTagIdSet = null;
        if(CollectionUtils.isNotEmpty(mdst.getScmIds())){
            scmTagIdSet = new HashSet<>();
            scmTagIdSet.addAll(mdst.getScmIds());
        }
        Set<Long> discountNPidSet = new HashSet<>(productDetailCache.getProductByScmTagId(CommonConstants.DISCOUNT_N_SCM_TAGID));
        for(String product : products){
            try{
                //过滤productId为空
                if(StringUtils.isEmpty(product)){
                    continue;
                }
                Long pid = Long.valueOf(product);
                ProductInfo productInfo = productDetailCache.getProductInfo(pid);
                if(FilterUtil.isCommonFilter(productInfo)){
                    continue;
                }

                //过滤掉非一起拼商品
                if(productInfo.getIsToggroupProduct() == null || !productInfo.getIsToggroupProduct().toString().equals("1")) {
                	continue;
                }

                //过滤掉N折商品池中的商品
                if(CollectionUtils.isNotEmpty(discountNPidSet) && discountNPidSet.contains(pid)){
                    continue;
                }

                //过滤未满足前台一级类目商品
                if(!isFitFcate1(productInfo, noviceFrontcategory1Id, categoryIdSet, scmTagIdSet)){
                    continue;
                }
                result.add(product);

            }catch(Exception e){
                log.error("[严重异常][运营配置的新手专享召回]过滤商品时，出现异常：",e);
            }
        }
        return result;
    }

    /**
     * 商品是否满足前台一级类目
     * @param
     * @return
     */
    private boolean isFitFcate1(ProductInfo productInfo, String noviceFrontCategory1Id, Set<String> categoryIdSet, Set<String> scmTagIdSet){
        if(StringUtils.isBlank(noviceFrontCategory1Id)){
            return true;
        }else {
            //如果属于传入的前台一级类目的商品，则返回true
            if (productInfo.getFCategory1Ids().contains(noviceFrontCategory1Id)) {
                return true;
            }

            //如果属于前台一级类目对应的后台三级类目商品，则返回true
            if (CollectionUtils.isNotEmpty(categoryIdSet)) {
                Long thirdCategoryId = productInfo.getThirdCategoryId();
                if (thirdCategoryId != null && categoryIdSet.contains(thirdCategoryId.toString())) {
                    return true;
                }
            }

            //如果属于前台一级类目对应的scmTagId商品，则返回true
            if(CollectionUtils.isNotEmpty(scmTagIdSet)){
                String scmTagIds = productInfo.getTagsId();
                if(StringUtils.isNotBlank(scmTagIds)){
                    String[] scmTagIdArray = scmTagIds.split(",");
                    for(String scmTagId : scmTagIdArray){
                        if(StringUtils.isNotBlank(scmTagId) && scmTagIdSet.contains(scmTagId)){
                           return true;
                        }
                    }
                }
            }
        }
        return false;
    }
    /**
     * 获取对用户曝光3次但无点击的商品
     * @param allProducts
     * @param exposureProductMap
     * @param view3DayProductList
     * @return
     */
    private List<String> getExposuerLarge3AndNoScanProduct(List<String> allProducts, Map<String, String> exposureProductMap, List<String> view3DayProductList){
        List<String> result = null;
        //校验参数，如果参数为空，或空集合，则直接返回传入的allProducts
        if(CollectionUtils.isEmpty(allProducts) || exposureProductMap.size() == 0){
            result = new ArrayList<>(allProducts);
            return result;
        }

        result = new ArrayList<>();
        Set<String> view3DayProductSet = new HashSet<>(view3DayProductList);
        for(String productId : allProducts){
            try{
                //有浏览记录则直接跳过
                if(view3DayProductSet.contains(productId)){
                    continue;
                }
                //没有曝光则直接跳过
                if(!exposureProductMap.containsKey(productId)){
                    continue;
                }
                String count = exposureProductMap.get(productId);
                //获取曝光次数大于1的商品信息
                if(!StringUtils.isBlank(count) && Long.valueOf(count)>=1){
                    result.add(productId);
                }
            }catch(Exception ex){
                log.error("[严重异常][运营配置的新手专享召回]处理对用户曝光3次但无点击的商品Id {}时，发出异常", productId, ex);
            }
        }
        return result;
    }

    /**
     * 对过滤后的商品进行排序
     * @param productsByFilterList 过滤后的商品集合
     * @param view3DayProductList 商品浏览记录
     * @param sex  用户性别
     * @param productPorderNumMap 每个商品付款订单数
     * @param productForderRateMap 每个商品用户首单转化率
     * @param userhobbyCate3IdMap 用户3级类目偏好
     * @return 排序后的商品集合
     */
    private List<String> sortFilterProducts(List<String> productsByFilterList, List<String> view3DayProductList, String sex, Map<String, String> productPorderNumMap, Map<String, String> productForderRateMap, Map<String, String> userhobbyCate3IdMap) {
        if(CollectionUtils.isEmpty(productsByFilterList) || productsByFilterList.size() < 2 ) return productsByFilterList;

        List<String> result = new ArrayList<>();
        Boolean isView = CollectionUtils.isEmpty(view3DayProductList) ? Boolean.FALSE : Boolean.TRUE;
        try {
            if (isView) {
                //按三级类目偏好进行分组，访问次数多的在前
                List<List<String>> productLayerByhobbyCate3Id = productLayerByUserHoppyCate3Id(productsByFilterList, userhobbyCate3IdMap);

                //三级类目偏好的每个分组中获取有浏览记录的商品
                for (List<String> layerByhobbyList : productLayerByhobbyCate3Id) {
                    List<String> noscanProduct = new ArrayList<>(layerByhobbyList);
                    List<String> scanProduct = new ArrayList<>(layerByhobbyList);
                    scanProduct.retainAll(view3DayProductList);
                    noscanProduct.removeAll(scanProduct);
                    List<String> list = sortByForderRate(noscanProduct, productForderRateMap);
                    result.addAll(list);
                    //再随机插入已浏览的商品
                    Collections.shuffle(scanProduct);
                    result.addAll(scanProduct);
                }
                //非用户三级类目偏好的商品
                List<String> productNoHobbyCate3Id = new ArrayList<>(productsByFilterList);
                //获取用户三级类目偏好的所有商品
                List<String> productHobbyCate3Id = new ArrayList<>();
                for (List<String> productList : productLayerByhobbyCate3Id) {
                    productHobbyCate3Id.addAll(productList);
                }
                productNoHobbyCate3Id.removeAll(productHobbyCate3Id);
                //针对非用户三级类目偏好的商品按无浏览行为的情况排序
                List<String> sortNoHobbyProduct = sortFilterProducts(productNoHobbyCate3Id, null, sex, productPorderNumMap, productForderRateMap, userhobbyCate3IdMap);
                result.addAll(sortNoHobbyProduct);
            } else {
                //先按照用户性别将商品分组
                List<List<String>> productLayerBySex = productLayerBySex(productsByFilterList, sex);
                //log.error("productLayerBySex {}", JSON.toJSONString(productLayerBySex));
                //在用户性别分组的基础上在进行商品付款量分组
                List<List<String>> productLayerBySexAndPorderNum = new ArrayList<>();
                for (List<String> productList : productLayerBySex) {
                    List<List<String>> productLayerByPorderNum = productLayerByPorderNum(productList, productPorderNumMap);
                    productLayerBySexAndPorderNum.addAll(productLayerByPorderNum);
                    //log.error("productLayerByPorderNum{} ", JSON.toJSONString(productLayerByPorderNum));
                }

                //对每个分组按商品转化率降序排序
                for (List<String> productList : productLayerBySexAndPorderNum) {
                    List<String> list = sortByForderRate(productList, productForderRateMap);
                    //log.error("list {}", JSON.toJSONString(list));
                    result.addAll(list);
                }
            }
        }catch(Exception e){
            log.error("[严重异常][运营配置的新手专享召回]对过滤后的商品进行排序失败：", e);
            result = productsByFilterList;
        }
        return result;
    }

    /**
     * 根据用户喜爱的三级类目分层，按三级类目下的浏览商品次数降序排序
     * @param productList
     * @param userhobbyCate3IdMap
     * @return
     */
    private List<List<String>> productLayerByUserHoppyCate3Id(List<String> productList,Map<String, String> userhobbyCate3IdMap){
        List<List<String>> result = new ArrayList<>();
        if(CollectionUtils.isEmpty(productList) || userhobbyCate3IdMap == null || userhobbyCate3IdMap.size() == 0){
            result.add(productList);
            return result;
        }
        try{
            //根据用户喜爱的三级类目的浏览商品次数降序排序
            List<Map.Entry<String,String>> list = new ArrayList<>(userhobbyCate3IdMap.entrySet());
            Collections.sort(list, new Comparator<Map.Entry<String, String>>() {
                @Override
                public int compare(Map.Entry<String, String> o1, Map.Entry<String, String> o2) {
                    int result = Long.valueOf(o1.getValue()).compareTo(Long.valueOf(o2.getValue()));
                    return -result;
                }
            });

            for(Map.Entry<String,String> map : list){
                List<String> layerList = new ArrayList<>();
                for(String product: productList){
                    //to do  增加getProductInfo返回值的判空
                    ProductInfo productInfo = productDetailCache.getProductInfo(Long.valueOf(product));
                    Long cate3Id = getCate3Id(productInfo);
                    if(productInfo != null && cate3Id != null && map.getKey().equals(cate3Id.toString())){
                        layerList.add(product);
                    }
                }
                if(layerList.size()>0){
                    result.add(layerList);
                }
            }
        }catch(Exception e) {
            log.error("[严重异常][运营配置的新手专享召回]处理根据用户喜爱的三级类目分组失败：",e);
            result.add(productList);
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
                    log.error("[严重异常][运营配置的新手专享召回]根据用户性别进行分组，处理发生错误：",e);
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
     * 根据商品销量进行分组
     * @param productList
     * @param productPorderNumMap
     * @return
     */
    private List<List<String>> productLayerByPorderNum(List<String> productList, Map<String, String> productPorderNumMap){
        List<List<String>> result = new ArrayList<>();
        if(CollectionUtils.isEmpty(productList) || productPorderNumMap == null || productPorderNumMap.size()==0){
            result.add(productList);
            return result;
        }
        List<String> orderNumLarge400 = new ArrayList<>();
        List<String> orderNumBetween100And400 = new ArrayList<>();
        List<String> orderNumBetween50And100 = new ArrayList<>();
        List<String> orderNumBetweenSmall50 = new ArrayList<>();
        for(String product : productList){
            try{
                if(productPorderNumMap.containsKey(product)){
                    String pOrderNumStr = productPorderNumMap.get(product);
                    //如果首单转化率中有该商品Id,但是无对应的首单转化率值，则放入到小于50的集合中
                    if(StringUtils.isEmpty(pOrderNumStr)){
                        orderNumBetweenSmall50.add(product);
                        continue;
                    }
                    Long pOrderNum = Long.valueOf(pOrderNumStr);
                    if(pOrderNum >= 400){
                        orderNumLarge400.add(product);
                    }else if(pOrderNum>= 100 && pOrderNum <400){
                        orderNumBetween100And400.add(product);
                    }else if(pOrderNum>=50 && pOrderNum<100){
                        orderNumBetween50And100.add(product);
                    }else{
                        orderNumBetweenSmall50.add(product);
                    }
                }else{
                    //如果首单转化率无该商品Id,则放入到小于50的集合中
                    orderNumBetweenSmall50.add(product);
                }
            }catch(Exception e){
                log.error("[严重异常][运营配置的新手专享召回]根据商品订单量分组处理失败：",e);
                //如果解析异常，则将该商品加入到小于50的集合中
                orderNumBetweenSmall50.add(product);
            }
        }
        //加入顺序为：销量大于等于400、销量小于400大于等于100、销量小于100大于等于50、销量小于50
        if(orderNumLarge400.size() > 0) result.add(orderNumLarge400);
        if(orderNumBetween100And400.size() > 0) result.add(orderNumBetween100And400);
        if(orderNumBetween50And100.size() > 0) result.add(orderNumBetween50And100);
        if(orderNumBetweenSmall50.size() > 0) result.add(orderNumBetweenSmall50);
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

        List<String> result = null;
        List<String> noForderRateProducts = new ArrayList<>(productList);
        try{
            Map<String,String> filterProductForderRateMap = new HashMap<>();
            for(String product : productList){
                if(productForderRateMap.containsKey(product)){
                    String forderRateStr = productForderRateMap.get(product);
                    if(StringUtils.isNotBlank(forderRateStr)) {
                        filterProductForderRateMap.put(product, forderRateStr);
                    }
                }
            }
            //log.error("filterProductForderRateMap {}", filterProductForderRateMap);
            List<Map.Entry<String,String>> list = new ArrayList<>(filterProductForderRateMap.entrySet());
            Collections.sort(list, new Comparator<Map.Entry<String, String>>() {
                @Override
                public int compare(Map.Entry<String, String> o1, Map.Entry<String, String> o2) {
                    int result = Double.valueOf(o1.getValue()).compareTo(Double.valueOf(o2.getValue()));
                    return -result;
                }
            });
            //log.error("filterProductForderRateMap list {}", list);

            result = new ArrayList<>();
            for (Map.Entry<String, String> map : list){
                result.add(map.getKey());
            }
            //找出无首单转化率的商品
            noForderRateProducts.removeAll(result);
            //log.error("noForderRateProducts {}", noForderRateProducts);
            //将无首单转化率的商品放在有首单转化率的后面
            result.addAll(noForderRateProducts);
        }catch (Exception e){
            log.error("[严重异常][运营配置的新手专享召回]按照商品转化率排序失败：",e);
            result = productList;
        }
        return result;
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
            log.error("[严重异常][运营配置的新手专享召回]查找不同的三级类目商品失败：",e);
            result = products.get(0);
        }
        return result;
    }

    /**
     * 对排序后的商品进行打散：同三级类目商品之间间隔2个
     * @param products
     * @return
     */
    private List<String> reorderByCate3IdDiscontinuous(List<String> products){
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
            log.error("[严重异常][运营配置的新手专享召回]打散时发生错误：",e);
            result = products;
        }
        return result;
    }
}
