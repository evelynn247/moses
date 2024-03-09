package com.biyao.moses.util;

import com.alibaba.fastjson.JSONObject;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.params.ProductInfo;
import com.biyao.moses.params.SuProductInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * @ClassName FacetUtil
 * @Description 筛选项公共处理方法
 * @Author xiaojiankai
 * @Date 2019/8/22 11:17
 * @Version 1.0
 **/
@Slf4j
public class FacetUtil {

    public static boolean filterAndFillInfoBySelectAtrrs(TotalTemplateInfo total, ProductInfo productInfo, Map<String, List<String>> selectAttrs) {
        if(CollectionUtils.isEmpty(selectAttrs) || productInfo == null) {
            return true;
        }
        //spu下的所有可售sku信息
        List<SuProductInfo> suProductList = productInfo.getSuProductList();
        //log.error("filterBySelectAtrrs spu下所有的销售属性：{}", JSON.toJSONString(skusAttrs));
        if(CollectionUtils.isEmpty(suProductList)) {
            return false;
        }
        String minPriceSkuId = null;
        String minPrice = null;
        String image = null;
        String imageWebp = null;
        //遍历spu下每一个sku的标准销售属性
        for(SuProductInfo suProductInfo : suProductList){
            Map<String, String> attrs;
            String skuId = suProductInfo.getSuId() != null ? suProductInfo.getSuId().toString() : "";
            //sku标准销售属性json格式
            String facet = suProductInfo.getFacet();
            try {
                if(StringUtils.isNotBlank(facet)){
                    attrs = JSONObject.parseObject(facet, Map.class);
                }else{
                    attrs = new HashMap<>();
                }

                boolean skuSelectedFlag = true;
                //遍历所有选中的筛选属性
                for (Map.Entry<String, List<String>> entry : selectAttrs.entrySet()) {
                    String key = entry.getKey();
                    //如果筛选的属性值为空，则认为没有该筛选属性
                    if (CollectionUtils.isEmpty(entry.getValue())) {
                        continue;
                    }
                    Set<String> attrValues = new HashSet<>(entry.getValue());
                    //如果sku没有该筛选属性 或 sku有该筛选属性但没有筛选的属性值，则认为该sku不满足筛选条件
                    //只要有1个筛选条件不满足，则认为该sku不满足筛选条件
                    if (!attrs.containsKey(key) || !attrValues.contains(attrs.get(key))) {
                        skuSelectedFlag = false;
                        break;
                    }
                }
                if (skuSelectedFlag) {
                    String price = suProductInfo.getPrice() != null ? suProductInfo.getPrice().toString() : "";
                    String imageCurrSku = suProductInfo.getSquarePortalImg();
                    String imageWebpCurrSku = suProductInfo.getSquarePortalImgWebp();
                    //如果sku无价格则认为数据不正常，该sku不返回
                    if (StringUtils.isBlank(price) || StringUtils.isBlank(skuId)) {
                        log.error("###sku无价格###, skuId {}", skuId);
                        continue;
                    }
                    if (StringUtils.isBlank(minPriceSkuId)) {
                        minPriceSkuId = skuId;
                        minPrice = price;
                        image = imageCurrSku;
                        imageWebp = imageWebpCurrSku;
                        continue;
                    }
                    if (Long.valueOf(price).compareTo(Long.valueOf(minPrice)) < 0) {
                        minPrice = price;
                        minPriceSkuId = skuId;
                        image = imageCurrSku;
                        imageWebp = imageWebpCurrSku;
                    } else if (Long.valueOf(price).compareTo(Long.valueOf(minPrice)) == 0) {
                        if (Long.valueOf(skuId).compareTo(Long.valueOf(minPriceSkuId)) < 0) {
                            minPrice = price;
                            minPriceSkuId = skuId;
                            image = imageCurrSku;
                            imageWebp = imageWebpCurrSku;
                        }
                    }

                }
            }catch(Exception e){
                log.error("选举最低价SKU时发生异常，productId {}, skuId {}, e ",productInfo.getProductId(), skuId, e );
            }
        }
        if(StringUtils.isNotBlank(minPriceSkuId)){
            total.setSkuId(minPriceSkuId);
            total.setSkuPrice(minPrice);
            total.setImage(image);
            total.setImageWebp(imageWebp);
            return true;
        }
        return false;
    }

}
