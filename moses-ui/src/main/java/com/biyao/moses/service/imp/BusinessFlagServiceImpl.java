package com.biyao.moses.service.imp;

import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.cache.Similar3AndNotFilterCache;
import com.biyao.moses.common.constant.CacheRedisKeyConstant;
import com.biyao.moses.params.ProductInfo;
import com.biyao.moses.service.BusinessFlagService;
import com.biyao.moses.util.CacheRedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 判断业务标识接口实现
 */
@Service
@Slf4j
public class BusinessFlagServiceImpl implements BusinessFlagService {

    @Autowired
    ProductDetailCache productDetailCache;

    @Autowired
    CacheRedisUtil redisUtil;

    @Autowired
    Similar3AndNotFilterCache similar3AndNotFilterCache;

    //72小时毫秒数
    private final long days3Time = 259200000;

    @Override
    public Boolean hasNewsByCategroys(String category, String scmIds, String uuid, String frontendCategoryId) {
        boolean result = false;

        ArrayList<String> cateList = str2List(category, ",");
        ArrayList<String> scmIdList = str2List(scmIds, ",");
        ArrayList<ProductInfo> productList = new ArrayList<>();
        //组装类目页数据
        for (String categoryId : cateList) {
            if(StringUtils.isBlank(categoryId)){
                continue;
            }
            List<Long> productIdsByCategoryId = productDetailCache.getProductIdsByCategoryId(Long.valueOf(categoryId));
            if (productIdsByCategoryId == null || productIdsByCategoryId.isEmpty()) {
                continue;
            }
            for (Long spuId : productIdsByCategoryId) {
                ProductInfo productInfo = productDetailCache.getProductInfo(spuId);
                if (productInfo != null) {
                    productList.add(productInfo);
                }
            }
        }
        //组装SCM标签数据
        if (scmIds != null && scmIdList.size() > 0) {
            for (String scmId : scmIdList) {
                List<Long> productIdsByScmId = productDetailCache.getProductByScmTagId(scmId);
                if (productIdsByScmId == null || productIdsByScmId.isEmpty()) {
                    continue;
                }
                for (Long spuId : productIdsByScmId) {
                    ProductInfo productInfo = productDetailCache.getProductInfo(spuId);
                    if (productInfo != null) {
                        productList.add(productInfo);
                    }
                }
            }
        }
        long spaceOfTime = 0L;
        long nowTime = System.currentTimeMillis();
        String timeStr = redisUtil.getString(CacheRedisKeyConstant.NEW_PRODUCT_TAG_TIME + "_" + uuid + "_" + frontendCategoryId);
        if (StringUtils.isBlank(timeStr) || nowTime - Long.parseLong(timeStr) > days3Time) {
            spaceOfTime = days3Time;
        } else {
            spaceOfTime = nowTime - Long.parseLong(timeStr);
        }
        //判断该前台类目是否为过滤商品的前台类目  true:定制类目、咖啡类目
        boolean isFilterCategory = similar3AndNotFilterCache.isNotFilterCustomPidFcateId(frontendCategoryId);

        for (ProductInfo item : productList) {

            ProductInfo productInfo = productDetailCache.getProductInfo(item.getProductId());
            if (productInfo == null) {
                continue;
            }
            //过滤下架商品
            if(productInfo.getShelfStatus() == null || !productInfo.getShelfStatus().toString().equals("1")){
                continue;
            }

            //不是定制、咖啡类目
            if(!isFilterCategory){
                if(productInfo.getSupportTexture() != null && productInfo.getSupportTexture().toString().equals("1")){
                    continue;
                }
            }

            if (item.getFirstOnshelfTime() != null) {
                long onShelfTime = item.getFirstOnshelfTime().getTime();
                //距离用户上次访问上新标签时存在上新商品
                if (nowTime - onShelfTime < spaceOfTime) {
                    result = true;
                    break;
                }
            }

        }

        return result;
    }

    private ArrayList<String> str2List(String str, String split) {
        ArrayList<String> resultList = new ArrayList<>();

        if (StringUtils.isNotBlank(str)) {
            String[] data = str.split(split);
            if (data != null) {
                for (int i = 0; i < data.length; i++) {
                    resultList.add(data[i]);
                }
            }
        }

        return resultList;
    }
}
