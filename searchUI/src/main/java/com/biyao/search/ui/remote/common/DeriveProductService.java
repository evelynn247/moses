package com.biyao.search.ui.remote.common;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.biyao.api.deriveproduct.util.IdConvertUtil;
import com.biyao.search.bs.service.DeriveProductMatch;
import com.biyao.search.bs.service.model.request.MatchRequest;
import com.biyao.search.bs.service.model.response.DeriveProductMatchResult;
import com.biyao.search.common.enums.PlatformEnum;
import com.biyao.search.common.enums.SearchOrderByEnum;
import com.biyao.search.common.model.CommonRequestParam;
import com.biyao.search.common.model.RPCResult;
import com.biyao.search.ui.cache.DeriveProductDetailCache;
import com.biyao.search.ui.cache.RedisDataCache;
import com.biyao.search.ui.constant.ColorCodeConsts;
import com.biyao.search.ui.constant.CommonConstant;
import com.biyao.search.ui.constant.PageSourceEnum;
import com.biyao.search.ui.constant.RedisKeyConsts;
import com.biyao.search.ui.model.DeriveProductInfo;
import com.biyao.search.ui.remote.request.UISearchRequest;
import com.biyao.search.ui.remote.response.ProductLabel;
import com.biyao.search.ui.remote.response.SearchProduct;
import com.biyao.search.ui.util.TrackParamUtil;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zhaiweixi@idstaff.com
 * @date 2020/2/4
 **/
@Service
public class DeriveProductService {

    private Logger logger = LoggerFactory.getLogger(DeriveProductService.class);
    @Autowired
    DeriveProductDetailCache deriveProductDetailCache;
    @Autowired
    RedisDataCache redisDataCache;
    /**
     * searchbs服务
     */
    @Autowired
    DeriveProductMatch deriveProductMatch;


    private static final DecimalFormat DF = new DecimalFormat("#.##");

    private static final String deriveMatchBlockId = "qmatch_mgc";

    /**
     * 衍生商品转前端模板
     * @param deriveProductId
     * @param blockId
     * @param request
     * @return
     */
    private SearchProduct detailDeriveProduct(String deriveProductId, String blockId,
                                              UISearchRequest request, CommonRequestParam commonParam,
                                              String aid, int position) {

        SearchProduct searchProduct = new SearchProduct();
        try {
            DeriveProductInfo deriveProductInfo = deriveProductDetailCache.getDeriveProductInfo(deriveProductId);
            if (deriveProductInfo == null) {
                return null;
            }
            // 过滤下架商品
            if (CommonConstant.BYTE_FALSE.equals(deriveProductInfo.getShelfStatus())) {
                return null;
            }

            // 过滤不支持当前平台售卖的商品
            if (StringUtils.isEmpty(deriveProductInfo.getSupportPlatform())
                    || !deriveProductInfo.getSupportPlatform().contains(request.getPlatform().getNum() + "")) {
                return null;
            }

            /* 基本信息 */
            searchProduct.setTitle(deriveProductInfo.getShortTitle());
            searchProduct.setFullTitle(deriveProductInfo.getTitle());
            searchProduct.setImage(deriveProductInfo.getSquarePortalImg());
            String squarePortalImgWebp = deriveProductInfo.getSquarePortalImgWebp();
            searchProduct.setImageWebp(StringUtils.isNoneBlank(squarePortalImgWebp) ? squarePortalImgWebp : deriveProductInfo.getSquarePortalImg());
            searchProduct.setProductDes(deriveProductInfo.getSupplierBackground());
            searchProduct.setSuId(deriveProductInfo.getSuId());
            searchProduct.setSpuId(deriveProductInfo.getProductId());
            searchProduct.setCustomFlag(1);
            searchProduct.setSalesVolume(deriveProductInfo.getSalesVolume());
            searchProduct.setSalesVolume7(deriveProductInfo.getSalesVolume7());
            searchProduct.setFirstOnShelfTime(deriveProductInfo.getFirstOnShelfTime());
            searchProduct.setSaleAgent(deriveProductInfo.getSaleAgent());
            searchProduct.setBackground(Strings.isNullOrEmpty(deriveProductInfo.getSupplierBackground()) ? ""
                    : deriveProductInfo.getSupplierBackground());

            //设置价格及拼团价
            setPriceAndGroupBuyPrice(searchProduct,deriveProductInfo.getPrice(),deriveProductInfo.getGroupPrice(),deriveProductInfo.getDeriveAddvalTotalPrice());

            //设置路由跳转信息
            //searchProduct.setRedirectUrl(buildDeriveProductUrl(request, commonParam, deriveProductInfo, aid, deriveMatchBlockId, position));
            searchProduct.setTrackParam(TrackParamUtil.generateProductTrackParam(searchProduct, blockId, request));

            // 设置商品标签
            if (CommonConstant.BYTE_TRUE.equals(deriveProductInfo.getIsToggroupProduct())) {
                setDeriveProductLabels(searchProduct, redisDataCache.getLabelConfig());
            }
        }catch (Exception e){
            logger.error("[严重异常]填充衍生商品信息异常，[uuid={}, pid={}, sid={}]", request.getUuid(), deriveProductId, request.getSid(), e);
            return null;
        }

        return searchProduct;
    }

    /**
     * 设置价格及拼团价
     * 衍生商品均为定制商品，需要在价格上加增值服务费
     * @param searchProduct
     * @param price
     * @param groupPrice
     * @param deriveAddvalTotalPrice
     */
    private void setPriceAndGroupBuyPrice(SearchProduct searchProduct, Long price, Long groupPrice, Long deriveAddvalTotalPrice) {

        Long realDeriveAddvalTotalPrice = deriveAddvalTotalPrice == null ? 0L : deriveAddvalTotalPrice / 100;
        Long realPrice = price == null ? 0L : price / 100;
        Long realGroupPrice = groupPrice == null ? 0L : groupPrice / 100;

        searchProduct.setPriceStr(DF.format(realPrice + realDeriveAddvalTotalPrice));
        searchProduct.setGroupBuyPriceStr(DF.format(realGroupPrice + realDeriveAddvalTotalPrice));
    }

    /**
     * 设置衍生商品标签，目前只有一起拼
     */
    private void setDeriveProductLabels(SearchProduct item, JSONObject labelConfig) {

        ProductLabel label = new ProductLabel();
        label.setContent(CommonConstant.LABEL_CONTENT_YQP);
        if (!labelConfig.containsKey(label.getContent())) {
            label.setColor(RedisKeyConsts.DEFAULT_LABEL_COLOR);
            label.setTextColor(ColorCodeConsts.TITLE_COLOR_WHITE);
            label.setRoundColor(ColorCodeConsts.TITLE_COLOR_WHITE);
        } else {
            JSONObject thisConfig = labelConfig.getJSONObject(label.getContent());
            label.setColor(thisConfig.getString("color")); // 背景颜色

            if (!thisConfig.containsKey("textColor")) { // 文本颜色
                label.setTextColor(ColorCodeConsts.TITLE_COLOR_WHITE);
            } else {
                label.setTextColor(thisConfig.getString("textColor"));
            }

            if (!thisConfig.containsKey("roundColor")) { // 边框颜色
                label.setRoundColor(ColorCodeConsts.TITLE_COLOR_WHITE);
            } else {
                label.setRoundColor(thisConfig.getString("roundColor"));
            }
        }

        item.setLabels(Lists.newArrayList(label));
    }

    /**
     * 从searchbs match衍生商品
     * @param query
     * @param commonParam
     * @param onlyTogetherGroupProduct 0:召回全部衍生商品 1:只召回一起拼衍生商品
     * @return
     */
    public List<DeriveProductMatchResult> match(String query, CommonRequestParam commonParam, int onlyTogetherGroupProduct){
        List<DeriveProductMatchResult> deriveProductMatchList = new ArrayList<>();
        long start = System.currentTimeMillis();
        try{
            MatchRequest matchRequest = new MatchRequest();
            matchRequest.setQuery(query);
            matchRequest.setCommonParam(commonParam);
            matchRequest.setOnlyTogetherGroupProduct(onlyTogetherGroupProduct);
            matchRequest.setExpectNum(redisDataCache.getDeriveProductSize());
            RPCResult<List<DeriveProductMatchResult>> deriveResult =deriveProductMatch.match(matchRequest);
            if(deriveResult !=null){
                if(deriveResult.getData() !=null){
                    deriveProductMatchList = deriveResult.getData();
                }
            }
        }catch(Exception e){
            logger.error("[严重异常][rpc异常]调用searchbs获取衍生商品异常,query = {}, commonParam = {}, onlyTogetherGroupProduct = {} ",query , JSON.toJSONString(commonParam), onlyTogetherGroupProduct, e);
        }

        long end = System.currentTimeMillis();
        logger.info("[日志监控]uuid:{};sid:{};query:{};衍生商品召回耗时:{};召回衍生商品数量:{}",commonParam.getUuid(),commonParam.getSid(),query,end-start,deriveProductMatchList.size());
        return deriveProductMatchList;
    }


    /**
     * 通用排序时，将衍生商品按照规则进行横插
     * @param products
     * @param deriveList
     * @param blockId
     * @param request
     * @param commonRequestParam
     * @param aid
     * @return
     */
    public List<SearchProduct> insertDeriveProduct(List<SearchProduct> products, DeriveProductMatchResult deriveList, String blockId,
                                                    UISearchRequest request, CommonRequestParam commonRequestParam,
                                                   String aid) {

        int commonProductInterval = redisDataCache.getCommonProductInterval();
        List<SearchProduct> sortedItems =  sortDeriveProduct(deriveList, blockId, request, commonRequestParam, aid);

        for (int i=commonProductInterval; i<products.size(); i = i+commonProductInterval+1 ){
            if(sortedItems.size()>0){
                products.add(i,sortedItems.get(0));
                sortedItems.remove(0);
            }
        }
        if(sortedItems.size()>0){
            products.addAll(sortedItems);
        }

        logger.info("[日志监控]uuid:{};sid:{};填充衍生商品信息后，衍生商品数量:{}",commonRequestParam.getUuid(),commonRequestParam.getSid(),products.size());
        return products;
    }

    /**
     * 按规则将衍生商品排序和岔开
     * @param deriveList
     * @param blockId
     * @param request
     * @return
     */
    private List<SearchProduct> sortDeriveProduct(DeriveProductMatchResult deriveList, String blockId,
                                                  UISearchRequest request, CommonRequestParam commonRequestParam,
                                                  String aid) {
        List<SearchProduct> result = new ArrayList<>();
        try {
            List<SearchProduct> tempList = new ArrayList<>();
            AtomicInteger position = new AtomicInteger(0);
            deriveList.getItems().forEach(derive -> {
                SearchProduct searchProduct = detailDeriveProduct(derive.getProductId(), blockId, request, commonRequestParam, aid, position.get());
                if (searchProduct != null) {
                    tempList.add(searchProduct);
                    position.getAndIncrement();
                }
            });
            tempList.sort((o1, o2) -> o2.getSalesVolume7().compareTo(o1.getSalesVolume7()));
            Set<Long> originalpids = new HashSet<>();
            // todo 效率低，会循环好多次
            do {
                for (SearchProduct item : tempList) {
                    Long originalpid = IdConvertUtil.covertSpuIdTo10(new BigInteger(item.getSpuId()));
                    if (!originalpids.contains(originalpid) && !result.contains(item)) {
                        result.add(item);
                        originalpids.add(originalpid);
                    }
                }
                originalpids = new HashSet<>();

            } while (result.size() < tempList.size());

        }catch (Exception e){
            logger.error("[严重异常]一次定制商品排序异常:", e);
            return new ArrayList<>();
        }
        //设置衍生商品路由
        setDeriveRedirectUrl(result,request,commonRequestParam,aid);
        return result;
    }

    /**
     * 不是通用排序时，将衍生商品插入，然后再按照规则排序
     * @param products
     * @param deriveList
     * @param blockId
     * @param request
     * @param isGroupBuy
     * @param commonRequestParam
     * @param aid
     * @return
     */
    public List<SearchProduct> insertAndSortDeriveProduct(List<SearchProduct> products, DeriveProductMatchResult deriveList, String blockId,
                                                          UISearchRequest request, Boolean isGroupBuy, CommonRequestParam commonRequestParam,
                                                          String aid) {
        AtomicInteger position = new AtomicInteger(0);

        //填充衍生商品信息并转换对象
        deriveList.getItems().forEach(derive->{
            SearchProduct searchProduct =  detailDeriveProduct(derive.getProductId(), blockId, request, commonRequestParam, aid, position.get());
            if(searchProduct !=null){
                products.add(searchProduct);
                position.getAndIncrement();
            }
        });

        List<SearchProduct> copyOfList = new ArrayList<>(products.size());
        copyOfList.addAll(products);
        //按照排序规则重新排序
        if(request.getOrderBy() == SearchOrderByEnum.SALE_QUANTITY){
            copyOfList.sort((o1, o2) -> o2.getSalesVolume().compareTo(o1.getSalesVolume()));
        }
        if(request.getOrderBy() == SearchOrderByEnum.NEW){
            copyOfList.sort((o1, o2) -> o2.getFirstOnShelfTime().compareTo(o1.getFirstOnShelfTime()));
        }
        if(request.getOrderBy() == SearchOrderByEnum.PRICE_DESC){
            if(isGroupBuy){
                //copyOfList.sort((o1, o2) -> o2.getGroupBuyPriceStr().compareTo(o1.getGroupBuyPriceStr()));
                copyOfList.sort((o1, o2) -> {
                    Long l1 = StringUtils.isBlank(o1.getGroupBuyPriceStr())? 0L:Long.valueOf(o1.getGroupBuyPriceStr());
                    Long l2 = StringUtils.isBlank(o2.getGroupBuyPriceStr())? 0L:Long.valueOf(o2.getGroupBuyPriceStr());
                    return l2.compareTo(l1);
                });
            }else{
                //copyOfList.sort((o1, o2) -> o2.getPriceStr().compareTo(o1.getPriceStr()));
                copyOfList.sort((o1, o2) -> {
                    Long l1 = StringUtils.isBlank(o1.getPriceStr())? 0L:Long.valueOf(o1.getPriceStr());
                    Long l2 = StringUtils.isBlank(o2.getPriceStr())? 0L:Long.valueOf(o2.getPriceStr());
                    return l2.compareTo(l1);
                });
            }
        }
        if(request.getOrderBy() == SearchOrderByEnum.PRICE_ASC){
            if(isGroupBuy){
                copyOfList.sort((o1, o2) -> {
                    Long l1 = StringUtils.isBlank(o1.getGroupBuyPriceStr())? 0L:Long.valueOf(o1.getGroupBuyPriceStr());
                    Long l2 = StringUtils.isBlank(o2.getGroupBuyPriceStr())? 0L:Long.valueOf(o2.getGroupBuyPriceStr());
                    return l1.compareTo(l2);
                });
            }else{
                copyOfList.sort((o1, o2) -> {
                    Long l1 = StringUtils.isBlank(o1.getPriceStr())? 0L:Long.valueOf(o1.getPriceStr());
                    Long l2 = StringUtils.isBlank(o2.getPriceStr())? 0L:Long.valueOf(o2.getPriceStr());
                    return l1.compareTo(l2);
                });
            }
        }
        //设置衍生商品路由
        setDeriveRedirectUrl(copyOfList,request,commonRequestParam,aid);
        logger.info("[日志监控]uuid:{};sid:{};填充衍生商品信息后，衍生商品数量:{}",commonRequestParam.getUuid(),commonRequestParam.getSid(),copyOfList.size());
        return copyOfList;
    }

    /**
     * 构造一次定制商品的路由
     * @param request
     * @param commonParam
     * @param deriveProductInfo
     * @return
     */
    private String buildDeriveProductUrl(UISearchRequest request, CommonRequestParam commonParam,
                                         DeriveProductInfo deriveProductInfo, String aid, String trackBlock, int position){

        String stpParamStr = TrackParamUtil.generateSTP(request, trackBlock, String.valueOf(position), aid,"");

        if (PlatformEnum.MINI.equals(commonParam.getPlatform())){
            // 小程序路由
            return CommonConstant.MINI_PRODUCT_URL_PREFIX + deriveProductInfo.getSuId() + "&stp=" + stpParamStr;
        }else if (PlatformEnum.ANDROID.equals(commonParam.getPlatform()) || PlatformEnum.IOS.equals(commonParam.getPlatform())){
            // APP路由
            /**
             * 一次定制商品路由，
             *  biyao://product/browse/mWeb?mUrl=xx
             *  mUrl：参数为appsup站点的url编码后的参数
             *  appsup站点的路由地址为：https://appsup.biyao.com/product/detail/suId?editorType=xx&joinGroupType=xx&groupType=xx&fromType=xx
             *  路由链接suId为商品Id，必填
             *  路由参数：
             *  editorType， // 编辑器类型，必填， 0 为普通编辑器， 1为一起拼编辑器
             *  joinGroupType, // 1参团
             *  groupType, // 1阶梯团
             *  fromType, // 1: 新手专享时使用
             *  备注说明：editorType为appsup新增参数，joinGroupType、groupType、fromType为原来app端路由参数
             *
             *  示例如下：
             *  一起拼参团的商品ID为1300476627010100001，appsup路由地址如下：
             *  https://appsup.biyao.com/product/detail/1300476627010100001?joinGroupType=1&editorType=1
             *
             *  appsup路由编码的地址为：
             *  https%3A%2F%2Fappsup.biyao.com%2Fproduct%2Fdetail%2F1300476627010100001%3FjoinGroupType%3D1%26editorType%3D1
             *
             *  app端拼接的路由地址为：
             *  biyao://product/browse/mWeb?mUrl=https%3A%2F%2Fappsup.biyao.com%2Fproduct%2Fdetail%2F1300476627010100001%3FjoinGroupType%3D1%26editorType%3D1
             */
            String appSupUrl = CommonConstant.APPRoute.APP_SUP_URL_PREFIX + deriveProductInfo.getSuId() + "?editorType=";
            String sourceId = request.getSourceId();
            // 用户身份
            boolean isNewer = request.getIsNewUser() == null ? false : request.getIsNewUser();
            // 一起拼标识
            boolean isJumpToGroup = request.getIsJumpTogroup() == null ? false : request.getIsJumpTogroup();
            // 是否是参团落地页请求
            boolean isJoinGroup = PageSourceEnum.JOINGROUP.getSourceId().equals(sourceId);
            if (CommonConstant.BYTE_TRUE.equals(deriveProductInfo.getIsToggroupProduct())){
                // 商品支持一起拼
                if (isJoinGroup) {
                    // 参团落地页请求
                    appSupUrl = appSupUrl + "1&joinGroupType=1";
                }else{
                    // 非参团落地页请求
                    if (!isNewer || (isNewer && isJumpToGroup)){
                        // 用户身份和一起拼标识支持一起拼
                        appSupUrl = appSupUrl + "1";
                    }else{
                        appSupUrl = appSupUrl + "0";
                    }
                }

            }else{
                // 商品不支持一起拼
                appSupUrl = appSupUrl + "0";
            }
            return CommonConstant.APPRoute.DERIVE_PRODUCT_URL_PREFIX + URLEncoder.encode(appSupUrl) + "&stp=" + stpParamStr;
        }else{
            // 其他平台默认使用M站路由
            return CommonConstant.M_PRODUCT_URL_PREFIX + deriveProductInfo.getSuId() + ".html?jumpTog=1&stp=" + stpParamStr;
        }
    }

    /**
     * 设置衍生商品spm埋点position
     * @param searchProductList
     */
    private void setDeriveRedirectUrl(List<SearchProduct> searchProductList,UISearchRequest request, CommonRequestParam commonParam,
                                   String aid){
        //集合为空会直接返回
        if(searchProductList == null || searchProductList.size() == 0){
            return;
        }
        for(int i = 0;i<searchProductList.size(); i++){
            DeriveProductInfo deriveProductInfo = deriveProductDetailCache.getDeriveProductInfo(searchProductList.get(i).getSpuId());
            if(deriveProductInfo != null ){
                searchProductList.get(i).setRedirectUrl(buildDeriveProductUrl(request, commonParam, deriveProductInfo, aid, deriveMatchBlockId, i));
            }
        }

    }

}
