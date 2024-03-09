package com.biyao.moses.service.imp;

import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.constants.CommonConstants;
import com.biyao.moses.context.ByUser;
import com.biyao.moses.model.template.FrontendCategory;
import com.biyao.moses.util.FilterUtil;
import com.biyao.moses.util.RedisUtil;
import com.biyao.moses.params.ProductInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 类目页入口图刷新
 */
@Slf4j
@Service
public class CategoryPicRefreshSevice {

    @Autowired
    ProductDetailCache productDetailCache;

    @Autowired
    RedisUtil redisUtil;

    private static final int LAST_VIEW_TIME_LIMIT = 604800000; //60*60*24*7*1000 7天

    /**
     * 刷新类目页信息中的入口图及其对应的priorityProductId
     * @param frontendCategoryList
     * @param user
     * @return
     */
    public List<FrontendCategory> refreshCategoryPic(List<FrontendCategory> frontendCategoryList, ByUser user){
        if(CollectionUtils.isEmpty(frontendCategoryList)){
            return frontendCategoryList;
        }

        //如果个性化推荐设置开关关闭，则不更新类目入口图，即使用cms配置的类目入口图
        if(!user.isPersonalizedRecommendSwitch()){
            return frontendCategoryList;
        }

        List<FrontendCategory> result = new ArrayList<>();
        try {
            String uuid = user.getUuid();
            //当日零点到7日前浏览的商品集合
            List<String> view7DayProductList = new ArrayList<>();
            //当日浏览的商品集合
            List<String> view1DayProductList = new ArrayList<>();

            getView1Dayand7DayProductList(view1DayProductList, view7DayProductList, uuid);

            Map<String, List<String>> view1Dayfcate3Products = new HashMap<>();
            Map<String, List<String>> view7Dayfcate3Products = new HashMap<>();
            if (CollectionUtils.isNotEmpty(view1DayProductList)) {
                view1Dayfcate3Products = productGroupByFcate3(view1DayProductList);
            }
            //log.error("view1Dayfcate3Products {}, uuid {}", view1Dayfcate3Products, uuid);
            if (CollectionUtils.isNotEmpty(view7DayProductList)) {
                view7Dayfcate3Products = productGroupByFcate3(view7DayProductList);
            }
            //log.error("view7Dayfcate3Products {}, uuid {}", view7Dayfcate3Products, uuid);

            Map<Integer, Long> allFcate3ProductId = getAllFcate3ProductId(view1Dayfcate3Products, view7Dayfcate3Products);
            //log.error("allFcate3ProductId {}", allFcate3ProductId);
            for (FrontendCategory frontendCategory : frontendCategoryList) {
                FrontendCategory frontendCategoryTmp = deepCopyFrontendCategory(frontendCategory);
                updateFrontendCategory(frontendCategoryTmp, allFcate3ProductId);
                result.add(frontendCategoryTmp);
            }
        }catch(Exception e){
            log.error("[严重异常][类目入口图]处理类目页入口图出现异常", e);
            result =  frontendCategoryList;
        }
        return result;
    }

    /**
     * 将商品按照前台三级类目分组
     * @param productList
     * @return
     */
    private Map<String, List<String>> productGroupByFcate3(List<String> productList){
        Map<String, List<String>> viewFcate3Products = new HashMap<>();
        for(String productId : productList){
            try {
                if(StringUtils.isBlank(productId)){
                    continue;
                }
                ProductInfo productInfo = productDetailCache.getProductInfo(Long.valueOf(productId));
                if (productInfo == null || !"1".equals(productInfo.getShelfStatus().toString())) {
                    continue;
                }
                List<String> fcate3List = productInfo.getFCategory3Ids();
                if(CollectionUtils.isEmpty(fcate3List)){
                    continue;
                }
                for (String fcate3 : fcate3List) {
                    if (!viewFcate3Products.containsKey(fcate3)) {
                        List<String> products = new ArrayList<>();
                        viewFcate3Products.put(fcate3, products);
                    }
                    viewFcate3Products.get(fcate3).add(productId);
                }
            }catch(Exception e){
                log.error("[一般异常]商品按前台3级类目分组异常，productId {}，", productId, e);
            }
        }
        return viewFcate3Products;
    }

    /**
     * 获取用户当日和7日浏览的商品信息
     * @param view1DayProductList
     * @param view7DayProductList
     * @param uuid
     */
    private void getView1Dayand7DayProductList(List<String> view1DayProductList, List<String> view7DayProductList, String uuid) {

        try {
            //当日0点之前7X24小时
            List<String> view7ProductList = new ArrayList<>();
            //当日0点至当前时间
            List<String> view1ProductList = new ArrayList<>();

            // 用户浏览历史是lpush，此处用lrange取出时，也可以保证最新浏览的在最前面。
            List<String> viewProductValues = redisUtil.lrange(CommonConstants.USER_VIEW_RDK_PREFIX + uuid, 0, 100);
//			log.error("用户浏览足迹redis大小：{},viewProductValues内容：{}, uuid={}, uid={}", viewProductValues.size(),
//					JSONObject.toJSONString(viewProductValues), uuid, uid);
            //获取当日0点毫秒数
            long currentTime = System.currentTimeMillis();
            long zeroTime = currentTime - (currentTime + TimeZone.getDefault().getRawOffset()) % (1000 * 3600 * 24);

            if (CollectionUtils.isNotEmpty(viewProductValues)) {
                for (String value : viewProductValues) {
                    if (StringUtils.isNotBlank(value)) {
                        String[] productValue = value.split(":");
                        if (productValue.length != 2) {
                            continue;
                        }
                        String time = productValue[1];
                        String productId = productValue[0];
                        //通过pid的长度过滤掉衍生商品，衍生商品pid长度为29，普通商品pid长度为10
                        if (StringUtils.isBlank(time) || StringUtils.isBlank(productId)
                            || productId.length() > 19) {
                            continue;
                        }
                        if (zeroTime < Long.valueOf(time)) {
                            view1ProductList.add(productId);
                            continue;
                        }
                        if (zeroTime - Long.valueOf(time) <= LAST_VIEW_TIME_LIMIT) {
                            view7ProductList.add(productId);
                            continue;
                        }
                    }
                }
            }

            if (CollectionUtils.isNotEmpty(view1ProductList)) {
                view1DayProductList.addAll(view1ProductList);
            }

            if (CollectionUtils.isNotEmpty(view7ProductList)) {
                view7DayProductList.addAll(view7ProductList);
            }
        }catch(Exception e){
            log.error("[一般异常]获取用户当日浏览和7日浏览信息异常 uuid {}", uuid, e);
        }
    }

    /**
     * 获取每个前台三级目录对应的priorityProductId
     * @param view1Dayfcate3Products
     * @param view7Dayfcate3Products
     * @return
     */
    private Map<Integer, Long> getAllFcate3ProductId(Map<String, List<String>> view1Dayfcate3Products, Map<String, List<String>> view7Dayfcate3Products){
        Map<Integer,Long> result = new HashMap<>();
        Map<String, List<Long>> allFrontendCategory3ProductId = productDetailCache.getAllFrontendCategory3ProductId();
        //log.error("allFrontendCategory3ProductId {}", allFrontendCategory3ProductId);
        for(Map.Entry<String, List<Long>> entry : allFrontendCategory3ProductId.entrySet()){
            String key = entry.getKey();
            List<Long> productList = entry.getValue();
            try {
                if (view1Dayfcate3Products.containsKey(key)) {
                    List<String> list = view1Dayfcate3Products.get(key);
                    if (CollectionUtils.isNotEmpty(list)) {
                        //当日浏览的商品存在，则取最近查看的商品
                        result.put(Integer.valueOf(key), Long.valueOf(list.get(0)));
                        continue;
                    }
                }
                //过滤下架的商品
                List<Long> filterProductList = productList.stream().filter(p -> {
                    ProductInfo productInfo = productDetailCache.getProductInfo(p);
                    if (productInfo == null || !"1".equals(productInfo.getShelfStatus().toString())) {
                        return false;
                    }
                    return true;
                }).collect(Collectors.toList());

                if (CollectionUtils.isEmpty(filterProductList)) {
                    continue;
                }
                //从7日浏览和热销中取前20个商品，在从这20个商品中随机取1个
                int productMaxNum = 20;
                Random random = new Random();
                //若前台三级类目下商品的总数小于productMaxNum时，则直接从已有的商品中随机选择一个
                if (filterProductList.size() <= productMaxNum) {
                    //log.error("cate3 {} , productList0 {}", key, productList);
                    int index = random.nextInt(filterProductList.size());
                    result.put(Integer.valueOf(key), filterProductList.get(index));
                    continue;
                }

                List<String> productRandomList = new ArrayList<>();
                if (view7Dayfcate3Products.containsKey(key)) {
                    productRandomList = view7Dayfcate3Products.get(key);
                }
                if (CollectionUtils.isNotEmpty(productRandomList)) {
                    //同一个前台三级类目下7日浏览的商品个数大于productMaxNum时，则直接从这些商品中随机选择一个
                    if (productRandomList.size() >= productMaxNum) {
                        int index = random.nextInt(productRandomList.size());
                        result.put(Integer.valueOf(key), Long.valueOf(productRandomList.get(index)));
                        continue;
                    }
                }
                //log.error("cate3 {} , productList1 {}", key, productList);
                List<Long> sortedProductList = filterProductList.stream().sorted((p1, p2) -> {
                    ProductInfo productInfo1 = productDetailCache.getProductInfo(p1);
                    ProductInfo productInfo2 = productDetailCache.getProductInfo(p2);
                    Long p1Sales = productInfo1.getSalesVolume7() == null ? 0 : productInfo1.getSalesVolume7();
                    Long p2Sales = productInfo2.getSalesVolume7() == null ? 0 : productInfo2.getSalesVolume7();
                    return -p1Sales.compareTo(p2Sales);
                }).collect(Collectors.toList());
                //log.error("cate3 {} , sortedProductList {}", key, sortedProductList);
                for (Long product : sortedProductList) {
                    if (productRandomList.size() >= productMaxNum) {
                        break;
                    }
                    if(productRandomList.contains(product.toString())){
                        continue;
                    }
                    productRandomList.add(product.toString());
                }
                if (CollectionUtils.isEmpty(productRandomList)) {
                    //log.error("cate3 {} , productRandomList为空", key);
                    continue;
                }
                //log.error("cate3 {} , productRandomList {}", key, productRandomList);
                int index = random.nextInt(productRandomList.size());
                result.put(Integer.valueOf(key), Long.valueOf(productRandomList.get(index)));
            }catch(Exception e){
                log.error("[一般异常]获取前台三级类目下的置顶商品ID异常，类目Id {}",key ,e);
            }
        }
        return result;
    }

    /**
     * 深度复制FrontendCategory
     * @param frontendCategory
     * @return
     */
    private FrontendCategory deepCopyFrontendCategory(FrontendCategory frontendCategory){
        if(frontendCategory == null){
            return null;
        }
        FrontendCategory result = new FrontendCategory();

        BeanUtils.copyProperties(frontendCategory, result);
        List<FrontendCategory> thirdCategoryDtoList = frontendCategory.getThirdCategoryDtoList();
        if(CollectionUtils.isNotEmpty(thirdCategoryDtoList)){
            List<FrontendCategory> thirdCategoryListTmp = new ArrayList<>();
            for(FrontendCategory cate3: thirdCategoryDtoList){
                FrontendCategory cate3FrontendCategory = new FrontendCategory();
                BeanUtils.copyProperties(cate3, cate3FrontendCategory);
                thirdCategoryListTmp.add(cate3FrontendCategory);
            }
            result.setThirdCategoryDtoList(thirdCategoryListTmp);
        }

        List<FrontendCategory> fCategory2List = frontendCategory.getSubCategoryList();
        if(CollectionUtils.isNotEmpty(fCategory2List)){
            List<FrontendCategory> fCategory2ListTmp = new ArrayList<>();
            for(FrontendCategory fCategory2 : fCategory2List){
                if(fCategory2 == null){
                    continue;
                }
                FrontendCategory fCategory2Tmp = new FrontendCategory();
                BeanUtils.copyProperties(fCategory2, fCategory2Tmp);
                List<FrontendCategory> fCategory3List = fCategory2.getSubCategoryList();
                if(CollectionUtils.isNotEmpty(fCategory3List)){
                    List<FrontendCategory> fCategory3ListTmp = new ArrayList<>();
                    for(FrontendCategory fCategory3 : fCategory3List){
                        FrontendCategory fCategory3Tmp = new FrontendCategory();
                        BeanUtils.copyProperties(fCategory3, fCategory3Tmp);
                        fCategory3ListTmp.add(fCategory3Tmp);
                    }
                    fCategory2Tmp.setSubCategoryList(fCategory3ListTmp);
                }
                fCategory2ListTmp.add(fCategory2Tmp);
            }
            result.setSubCategoryList(fCategory2ListTmp);
        }

        return result;
    }

    /**
     * 更新类目信息中的thirdCategoryDtoList和subCategoryList
     * @param frontendCategory
     * @param cate3Product
     */
    private void updateFrontendCategory(FrontendCategory frontendCategory, Map<Integer, Long> cate3Product){
        if(frontendCategory == null){
            return;
        }
        List<FrontendCategory> thirdCategoryDtoList = frontendCategory.getThirdCategoryDtoList();
        if(CollectionUtils.isNotEmpty(thirdCategoryDtoList)){
            for(FrontendCategory cate3: thirdCategoryDtoList){
                updateFCategory3Pic(cate3, cate3Product);
            }
        }
        List<FrontendCategory> fCategory2List = frontendCategory.getSubCategoryList();
        if(CollectionUtils.isNotEmpty(fCategory2List)){
            for(FrontendCategory fCategory2 : fCategory2List){
                if(fCategory2 == null){
                    continue;
                }
                List<FrontendCategory> fCategory3List = fCategory2.getSubCategoryList();
                if(CollectionUtils.isNotEmpty(fCategory3List)){
                    for(FrontendCategory fCategory3 : fCategory3List){
                        updateFCategory3Pic(fCategory3, cate3Product);
                    }
                }
            }
        }

    }

    /**
     * 更新类目信息中的入口图和priorityProduct
     * @param frontendCategory
     * @param cate3Product
     */
    private void updateFCategory3Pic(FrontendCategory frontendCategory, Map<Integer, Long> cate3Product){
        if(frontendCategory == null){
            return;
        }
        Integer categoryId = frontendCategory.getCategoryId();
        if(cate3Product.containsKey(categoryId)){
            Long productId = cate3Product.get(categoryId);
            ProductInfo productInfo = productDetailCache.getProductInfo(productId);
            if(FilterUtil.isCommonFilter(productInfo)){
                //下架、定制商品不被推出
                return;
            }
            frontendCategory.setImageUrl(productInfo.getSquarePortalImg());
            frontendCategory.setWebpImageUrl(productInfo.getSquarePortalImgWebp());
            frontendCategory.setPriorityProductId(productId.toString());
        }
    }
}
