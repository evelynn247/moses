package com.biyao.search.ui.remote.common;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;

import com.biyao.cms.client.material.dto.MaterialElementBaseDTO;
import com.biyao.search.common.enums.SearchOrderByEnum;
import com.biyao.search.common.model.SearchItem;
import com.biyao.search.facet.sdk.bean.Facet;
import com.biyao.search.ui.cache.ProductCache;
import com.biyao.search.ui.cache.RedisDataCache;
import com.biyao.search.ui.enums.ActivityEnum;
import com.biyao.search.ui.model.SearchProductInfo;
import com.biyao.search.ui.remote.response.SearchProduct;
import com.biyao.search.ui.util.CmsUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.biyao.search.common.enums.PlatformEnum;
import com.biyao.search.common.model.CommonRequestParam;
import com.biyao.search.common.model.FacetItem;
import com.biyao.search.ui.constant.CommonConstant;
import com.biyao.search.ui.remote.request.UISearchRequest;
import com.biyao.search.ui.remote.response.UIFacet;
import com.biyao.search.ui.remote.response.UIFacetValue;
import com.biyao.search.ui.rest.response.SearchOrderBy;
import com.biyao.search.ui.rest.response.SearchOrderByConsts;
import com.google.common.collect.Lists;

/**
 * @author biyao
 * @date long long ago
 */
@Service
@Slf4j
public class BeanConvertService {


    @Autowired
    private RedisDataCache redisDataCache;

    @Autowired
    private ProductDetailService productDetailService;
    @Autowired
    ProductCache productCache;

    private DecimalFormat df = new DecimalFormat("#.##");

    private static final long ALLOWANCE_ORDER_TEXT_MATERIAL_ID = 10830272L;

    /**
     * 将后端的facet转换成前端所需要的facet格式
     *
     * @param facetsFromAs
     * @param facetsSelected
     * @param request
     * @return
     * @author: luozhuo
     * @date: 2018年7月26日 上午11:25:40
     */
    public List<UIFacet> convertFacetItem2UIFacet(List<FacetItem> facetsFromAs, List<FacetItem> facetsSelected, UISearchRequest request) {
        if (facetsFromAs == null) {
            facetsFromAs = new ArrayList<>();
        }

        /*
         * 一起拼1.2.1需求，加入一起拼筛选条件（除了m站）
         */
        if (!PlatformEnum.M.getName().equals(request.getPlatform().getName())) {
            FacetItem activity = new FacetItem();
            activity.setKey("活动");
            activity.setValues(Lists.newArrayList("一起拼", "特权金"));
            if (facetsFromAs.size() <= 1) {
                // 第一个是价格
                facetsFromAs.add(activity);
            } else {
                facetsFromAs.set(1, activity);
            }
        }

        /*
         * 已选择的筛选条件信息
         * key -> facetKey
         * value -> ( key -> facetValueKey, value -> facetValue)
         */
        Map<String, Map<String, Boolean>> selectedMap = new HashMap<>();
        for (FacetItem item : facetsSelected) {
            if (!selectedMap.containsKey(item.getKey())) {
                selectedMap.put(item.getKey(), new HashMap<>());
            }

            for (String value : item.getValues()) {
                selectedMap.get(item.getKey()).put(value, true);
            }
        }

        /*
         *	拼接前端结果
         */
        List<UIFacet> result = new ArrayList<>();
        for (FacetItem item : facetsFromAs) {
            UIFacet uiFacet = new UIFacet();

            List<String> allValues = item.getValues();
            for (String value : allValues) {
                UIFacetValue uiValue = new UIFacetValue();
                uiValue.setCode(value);
                uiValue.setDesc(value);
                uiValue.setSelected(selectedMap.containsKey(item.getKey()) && selectedMap.get(item.getKey()).containsKey(value)
                        ? 1 : 0);

                uiFacet.getValues().add(uiValue);
            }

            uiFacet.setTitle(item.getKey());
            uiFacet.setKey(item.getKey());

            if (item.getValues().contains("特权金") && item.getValues().contains("一起拼")) {
                uiFacet.setType(CommonConstant.FacetType.SINGLE_SELECT);
            } else {
                uiFacet.setType(CommonConstant.FacetType.MUILT_SELECT);
            }

            result.add(uiFacet);
        }

        return result;
    }

    /**
     * 将sdk获取到的facet转换成前端所需要的facet格式
     *
     * @param facetList
     * @param facetsSelected
     * @return
     * @author: zj
     * @date: 2019年10月23日 下午14:26:33
     */
    public List<UIFacet> convertFacetItem2UIFacet(List<Facet> facetList, List<FacetItem> facetsSelected) {

        List<UIFacet> result = new ArrayList<>();

        Map<String, FacetItem> facetsSelectedMap = new HashMap<>();
        if (facetsSelected != null) {
            facetsSelected.forEach(item -> {
                facetsSelectedMap.put(item.getKey(), item);
            });
        }

        for (Facet facet : facetList) {
            UIFacet uiFacet = new UIFacet();
            uiFacet.setTitle(facet.getTitle());
            uiFacet.setType(facet.getType());
            uiFacet.setKey(facet.getTitle());
            for (com.biyao.search.facet.sdk.bean.FacetItem facetItem : facet.getValues()) {
                com.biyao.search.common.model.FacetItem selectFacetItem = facetsSelectedMap.get(facet.getTitle());
                UIFacetValue uiFacetValue = new UIFacetValue();
                uiFacetValue.setCode(facetItem.getTitle());
                uiFacetValue.setDesc(facetItem.getTitle());
                if (selectFacetItem != null) {
                    for (String item : selectFacetItem.getValues()) {
                        if (item.equals(facetItem.getTitle())) {
                            uiFacetValue.setSelected(1);
                        }
                    }
                }
                uiFacet.getValues().add(uiFacetValue);
            }
            result.add(uiFacet);
        }
        return result;
    }

    /**
     * 从redis获取排序方式的配置，组装成前端需要的格式
     *
     * @param request
     * @return
     * @author: luozhuo
     * @date: 2018年7月26日 上午11:26:42
     */
    public List<SearchOrderBy> generateOrderByList(UISearchRequest request) {
        if (ActivityEnum.ALLOWANCE_DEDUCTION.equals(ActivityEnum.judgeActivityType(request.getToActivity()))) {
            MaterialElementBaseDTO queryMaterial = CmsUtil.queryMaterial(ALLOWANCE_ORDER_TEXT_MATERIAL_ID);
            if (null != queryMaterial) {
                Object value = queryMaterial.getValue();
                if (!org.springframework.util.StringUtils.isEmpty(value)) {
                    SearchOrderBy allowanceOrderBy = new SearchOrderBy(SearchOrderByConsts.TWO_WAY, value.toString(),
                            SearchOrderByEnum.PRICE_ASC.getCode(), SearchOrderByEnum.PRICE_DESC.getCode());
                    return Lists.newArrayList(SearchOrderByConsts.NORMAL, SearchOrderByConsts.SALE, allowanceOrderBy);
                }
                return Lists.newArrayList(SearchOrderByConsts.NORMAL, SearchOrderByConsts.SALE, SearchOrderByConsts.PRICE);
            }
        }
        if (ActivityEnum.BUY2_RETURN_ALLOWANCE.equals(ActivityEnum.judgeActivityType(request.getToActivity()))) {
            return Lists.newArrayList(SearchOrderByConsts.NORMAL, SearchOrderByConsts.SALE, SearchOrderByConsts.PRICE);
        }
        if (redisDataCache.getPanelConfig() != null) {
            return redisDataCache.getPanelConfig().getSearchOrderByList();
        }
        return Lists.newArrayList(SearchOrderByConsts.NORMAL, SearchOrderByConsts.SALE, SearchOrderByConsts.NEW, SearchOrderByConsts.PRICE);

    }


    /**
     * 一起拼搜索 综合变成热门
     *
     * @param commonParam
     * @return
     */
    public List<SearchOrderBy> groupBuyOrderByList(CommonRequestParam commonParam) {
        if (redisDataCache.getPanelConfig() != null) {
            List<SearchOrderBy> searchOrderByList = new ArrayList<>();
            List<SearchOrderBy> tempSearchOrderByList = redisDataCache.getPanelConfig().getSearchOrderByList();
            // 将综合排序换成热门排序
            for (SearchOrderBy searchOrderBy : tempSearchOrderByList) {
                if (SearchOrderByConsts.NORMAL.getDesc().equals(searchOrderBy.getDesc())) {
                    searchOrderByList.add(SearchOrderByConsts.HOT);
                } else {
                    searchOrderByList.add(searchOrderBy);
                }
            }
            return searchOrderByList;
        } else {
            return Lists.newArrayList(SearchOrderByConsts.HOT, SearchOrderByConsts.SALE, SearchOrderByConsts.NEW, SearchOrderByConsts.PRICE);
        }

    }

    public List<SearchProduct> convert2SearchProduct(List<SearchItem> searchItemList, UISearchRequest
            request, String templateType, Boolean isGroupBuy) {
        List<SearchProduct> searchProductList = new ArrayList<>();
        if (searchItemList != null) {
            searchItemList.forEach(searchItem -> {
                SearchProduct searchProduct = buildBaseInfo(searchItem, request, templateType, isGroupBuy);
                if (searchProduct != null) {
                    searchProductList.add(searchProduct);
                }
            });
        }
        return searchProductList;
    }

    /**
     * 填充基本信息
     */
    private SearchProduct buildBaseInfo(SearchItem searchItem, UISearchRequest request, String
            templateType, Boolean isGroupBuy) {
        SearchProductInfo searchProductInfo = productCache.getSearchProductInfo(searchItem.getProductId().longValue());
        if (searchProductInfo == null) {
            return null;
        }
        SearchProduct searchProduct = new SearchProduct();
        searchProduct.setProductId(searchItem.getProductId());
        searchProduct.setScore(searchItem.getMatchScore());
        searchProduct.setTitle(searchProductInfo.getShortTitle());
        searchProduct.setFullTitle(searchProductInfo.getTitle());
        searchProduct.setIsAllowance(searchProductInfo.getIsAllowance());
        searchProduct.setAllowancePrice(searchProductInfo.getAllowancePrice());
        searchProduct.setImage(searchProductInfo.getSquarePortalImg());
        String squarePortalImgWebp = searchProductInfo.getSquarePortalImgWebp();
        searchProduct.setImageWebp(StringUtils.isNoneBlank(squarePortalImgWebp) ? squarePortalImgWebp : searchProductInfo.getSquarePortalImg());
        searchProduct.setPriceStr(df.format(searchProductInfo.getPrice() / 100.00));
        searchProduct.setSpuId(searchProductInfo.getProductId().toString());
        searchProduct.setSalesVolume(searchProductInfo.getSalesVolume());
        searchProduct.setSalesVolume7(searchProductInfo.getSalesVolume7());
        searchProduct.setFirstOnShelfTime(searchProductInfo.getFirstOnShelfTime());
        searchProduct.setSuId(searchProductInfo.getSuId().toString());
        searchProduct.setSemStr(searchItem.getSemStr());
        searchProduct.setCreationPriceStr(searchProductInfo.getCreationPriceStr());
        if (isGroupBuy) {
            searchProduct.setGroupBuyPriceStr(searchItem.getGroupPriceStr());
        }
        if (templateType.equals(CommonConstant.TemplateType.GROUP_BUY_SINGLE_PRODCUT)) {
            if ((searchProductInfo.getIsLaddergroupProduct() != null && searchProductInfo.getIsLaddergroupProduct() == 1)
                    && (searchProductInfo.getIsToggroupProduct() != null && searchProductInfo.getIsToggroupProduct() != 1)
                    && (searchProduct.getGroupBuyPriceStr() != null)) {
                try {
                    searchProduct.setGroupBuyPriceStr(df.format(new BigDecimal(searchProduct.getGroupBuyPriceStr())));
                } catch (Exception e) {
                    // 2019-09-06 zhaiweixi 日志注释
                    searchProduct.setGroupBuyPriceStr("0");
                }

            } else if (searchProductInfo.getGroupPrice() != null) {
                searchProduct.setGroupBuyPriceStr(df.format(searchProductInfo.getGroupPrice() / 100.00));
            }
        }
        if (!isGroupBuy) {
            try {
                //2019-10-24 搜索V1.7 重置sku信息
                productDetailService.setSkuInfo(searchProduct, request.getFacets(), request.getUserSizeMap());
            } catch (Exception e) {
                log.error("[一般异常]商品重置sku信息失败:[uuid={}, pid={}, sid={}]", request.getUuid(), searchItem.getProductId(), request.getSid(), e);
            }
        }
        return searchProduct;
    }
}
