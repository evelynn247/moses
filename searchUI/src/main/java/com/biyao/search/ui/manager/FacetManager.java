package com.biyao.search.ui.manager;

import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.biyao.client.model.SuProduct;
import com.biyao.search.facet.sdk.bean.Facet;
import com.biyao.search.facet.sdk.bean.FacetItem;
import com.biyao.search.facet.sdk.bean.FacetProduct;
import com.biyao.search.facet.sdk.bean.FacetSu;
import com.biyao.search.facet.sdk.constants.FacetConstants;
import com.biyao.search.facet.sdk.service.AbstractFacetManager;
import com.biyao.search.ui.cache.ProductCache;
import com.biyao.search.ui.model.SearchProductInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zj
 * @version 1.0
 * @date 2019/10/23 13:35
 * @description
 */
@Component
@Slf4j
public class FacetManager extends AbstractFacetManager {

    @Autowired
    ProductCache productCache;

    @Value("${facet.facetUrlPath}")
    private String facetUrlPath;

    @Override
    protected List<FacetProduct> reloadFacetProduct() {
        List<FacetProduct> result = new ArrayList<>();
        Map<Long, SearchProductInfo> searchProductInfoMap = productCache.getAllSearchProductInfo();
        if(searchProductInfoMap !=null){
            for (Map.Entry<Long, SearchProductInfo> item:searchProductInfoMap.entrySet()) {
                try{
                    result.add(convert2Facet(item.getValue()));
                } catch(Exception e){
                    log.error("[严重异常]转换FacetProduct对象时，source={}，发生异常", JSON.toJSONString(item.getValue()), e);
                }
            }
        }
        log.info("[操作日志]成功转换FacetProduct对象数量为："+result.size());
        return result;
    }
    /**
     * 转换spuFacet对象
     * @param value
     * @return
     */
    private FacetProduct convert2Facet(SearchProductInfo value) {
        FacetProduct facetProduct = new FacetProduct();
        facetProduct.setProductId(value.getProductId());
        facetProduct.setCategory3Id(value.getThirdCategoryId());
        facetProduct.setPrice(value.getPrice());
        if(value.getIsSetGoldenSize() == 1){
            facetProduct.setSetGoldSize(true);
        }
        facetProduct.setFacet(jsonConvert(value.getProductFacet()));
        if(value.getGoldenSizeSet()!=null){
            facetProduct.setGoldSize(new ArrayList<>(value.getGoldenSizeSet()));
        }
        List<FacetSu> facetSuList = covert2SuFacet(value.getSuProductList());
        if(facetSuList != null){
            facetProduct.setSus(facetSuList);
            for (FacetSu facetSu:facetSuList) {
                if(facetSu.getSuId().equals(value.getSuId())){
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
    private List<FacetSu> covert2SuFacet(List<SuProduct> suProductList) {
        List<FacetSu> result = new ArrayList<>();
        Map<String, List<FacetItem>> suFacetMap;
        Map<String, List<FacetItem>> facetMap;
        if (suProductList != null) {
            for (SuProduct suProduct : suProductList) {
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
     * @param suFacet，数据示例：{"尺码": ["36"]}
     * @return
     */
    private Map<String, List<FacetItem>> jsonConvert(String suFacet) {

        Map<String, List<FacetItem>> result = new HashMap<>();
        if (!StringUtils.isBlank(suFacet)) {
            JSONObject jsonObject = JSONObject.parseObject(suFacet);
            for (Map.Entry item : jsonObject.entrySet()) {
                List<FacetItem> facetItems = new ArrayList<>();
                for (Object obj: (JSONArray)item.getValue()) {
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
            if (jsonObject == null) {
                return result;
            }
            for (Map.Entry item : jsonObject.entrySet()) {
                List<FacetItem> facetItems = new ArrayList<>();
                facetItems.add(new FacetItem(item.getValue().toString()));
                result.put(item.getKey().toString(), facetItems);
            }
        }
        return result;
    }

    @Override
    protected List<Facet> reloadFacet() {
        List<Facet> facetList = new ArrayList<>();
        try{
            facetList = loadFacet(facetUrlPath);
        }catch (Exception e){
            log.error("[严重异常]读取Facet面板配置文件失败："+e.getMessage());
        }

        return facetList;
    }
}
