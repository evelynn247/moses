package com.biyao.moses.context;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.params.ProductInfo;
import com.biyao.moses.params.SuProductInfo;
import com.biyao.search.facet.sdk.bean.Facet;
import com.biyao.search.facet.sdk.bean.FacetItem;
import com.biyao.search.facet.sdk.bean.FacetProduct;
import com.biyao.search.facet.sdk.bean.FacetSu;
import com.biyao.search.facet.sdk.constants.FacetConstants;
import com.biyao.search.facet.sdk.service.AbstractFacetManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * facetManager 用于聚合筛选项、筛选商品、选择su
 */
@Component
@Slf4j
@EnableScheduling
public class FacetManager extends AbstractFacetManager {

    @Value("${facet.facetUrlPath}")
    private String facetUrlPath;

    @Autowired
    private ProductDetailCache productDetailCache;

    @PostConstruct
    protected void init() {
        refresh();
    }

    @Scheduled(cron = "0 0/5 * * * ?")
    private void refresh() {
        try {
            super.refreshCache();
        } catch (Exception e) {
            log.error("facet sdk初始化错误", e);
        }
    }

    @Override
    protected List<FacetProduct>
    reloadFacetProduct() {
        List<FacetProduct> facetProductList = new ArrayList<>();
        Map<Long, ProductInfo> productInfoMap = productDetailCache.getProductInfoMap();
        Iterator<Map.Entry<Long, ProductInfo>> iterator = productInfoMap.entrySet().iterator();
        while (iterator.hasNext()) {
            try {
                Map.Entry<Long, ProductInfo> next = iterator.next();
                ProductInfo productInfo = productInfoMap.get(next.getKey());
                FacetProduct facetProduct = convert2Facet(productInfo);
                facetProductList.add(facetProduct);
            } catch (Exception e) {
                log.error("facet sdk初始化数据出错 {}", e);
                continue;
            }
        }

        return facetProductList;
    }

    @Override
    protected List<Facet> reloadFacet() {
        List<Facet> facetList = new ArrayList<>();
        try {
            facetList = loadFacet(facetUrlPath);
        } catch (Exception e) {
            log.error("读取Facet面板配置文件失败：" + e.getMessage());
        }

        return facetList;
    }

    /**
     * 转换spuFacet对象
     *
     * @param value
     * @return
     */
    private FacetProduct convert2Facet(ProductInfo value) {
        FacetProduct facetProduct = new FacetProduct();
        Integer isSetGoldenSize = value.getIsSetGoldenSize();
        if (isSetGoldenSize.equals(0)) {
            facetProduct.setSetGoldSize(false);
        } else {
            facetProduct.setSetGoldSize(true);
        }
        facetProduct.setProductId(value.getProductId());
        facetProduct.setCategory3Id(value.getThirdCategoryId());
        facetProduct.setPrice(value.getPrice());
        if (value.getGoldenSizeSet() != null) {
            facetProduct.setGoldSize(new ArrayList<>(value.getGoldenSizeSet()));
        }
        List<FacetSu> facetSuList = covert2SuFacet(value.getSuProductList());
        if (facetSuList != null) {
            facetProduct.setSus(facetSuList);
            for (FacetSu facetSu : facetSuList) {
                if (facetSu.getSuId().equals(value.getSuId())) {
                    facetProduct.setDefaultSu(facetSu);
                }
            }
        }
        return facetProduct;
    }

    /**
     * 转换suFacet对象(修改)
     * 改动点：facet字段解析并把自定义颜色加到suFacetMap
     * @param suProductList
     * @return
     */
    private List<FacetSu> covert2SuFacet(List<SuProductInfo> suProductList) {
        List<FacetSu> result = new ArrayList<>();
        Map<String, List<FacetItem>> suFacetMap;
        Map<String, List<FacetItem>> facetMap;
        if (suProductList != null) {
            for (SuProductInfo suProduct : suProductList) {
                FacetSu facetSu = new FacetSu();
                facetSu.setSuId(suProduct.getSuId());
                facetSu.setPrice(suProduct.getPrice());
                facetSu.setScore(suProduct.getSaleVolume7());
                suFacetMap = jsonConvert(suProduct.getSuFacet());
                facetMap = jsonFacetConvert(suProduct.getFacet());
                if (suFacetMap != null && facetMap != null && facetMap.get(FacetConstants.CUSTOM_COLOR) != null) {
                    suFacetMap.put(FacetConstants.CUSTOM_COLOR, facetMap.get(FacetConstants.CUSTOM_COLOR));
                }
                facetSu.setFacet(suFacetMap);
                result.add(facetSu);
            }
        }
        return result;
    }

    /**
     * 转换suFacet对象
     *
     * @param suFacet
     * @return
     */
    private Map<String, List<FacetItem>> jsonConvert(String suFacet) {

        Map<String, List<FacetItem>> result = new HashMap<>();
        if (!StringUtils.isBlank(suFacet)) {
            JSONObject jsonObject = JSONObject.parseObject(suFacet);
            for (Map.Entry item : jsonObject.entrySet()) {
                List<FacetItem> facetItems = new ArrayList<>();
                for (Object obj : (JSONArray) item.getValue()) {
                    facetItems.add(new FacetItem(obj.toString()));
                }

                result.put(item.getKey().toString(), facetItems);
            }
        }
        return result;
    }

    /**
     * 转换Facet对象
     * @param facet,数据示例：{"size":"36","price":"2300"}
     * @return
     */
    private Map<String, List<FacetItem>> jsonFacetConvert(String facet) {

        Map<String, List<FacetItem>> result = new HashMap<>();
        if (!StringUtils.isBlank(facet)) {
            JSONObject jsonObject = JSONObject.parseObject(facet);
            for (Map.Entry item : jsonObject.entrySet()) {
                List<FacetItem> facetItems = new ArrayList<>();
                facetItems.add(new FacetItem(item.getValue().toString()));
                result.put(item.getKey().toString(), facetItems);
            }
        }
        return result;
    }

    /**
     * 选择su
     *
     * @param productIdList
     * @param selectedFacetList
     * @param userCate3SizeMap
     */
    public Map<Long, Long> selectSu(List<Long> productIdList, List<Facet> selectedFacetList, Map<String, String> userCate3SizeMap) {
        Map<Long, List<String>> sdkUserMap = new HashMap<>();
        Iterator<Map.Entry<String, String>> iterator = userCate3SizeMap.entrySet().iterator();
        List<String> sizeList = new ArrayList<>();
        while (iterator.hasNext()) {
            Map.Entry<String, String> next = iterator.next();
            String userSizes = userCate3SizeMap.get(next.getKey());
            if (StringUtils.isBlank(userSizes)) {
                continue;
            }
            String[] split = userSizes.split(",");
            for (int i = 0; i < split.length; i++) {
                sizeList.add(split[i]);
            }
            sdkUserMap.put(Long.valueOf(next.getKey()), sizeList);
        }


        Map<Long, Long> rtMap = super.selectEntrySu(productIdList, selectedFacetList, sdkUserMap);
        return rtMap;
    }
}

