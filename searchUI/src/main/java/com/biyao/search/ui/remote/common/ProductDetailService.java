package com.biyao.search.ui.remote.common;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.biyao.client.model.SuProduct;
import com.biyao.cms.client.common.bean.ImageDto;
import com.biyao.cms.client.material.dto.MaterialElementBaseDTO;
import com.biyao.mac.client.redbag.shop.privilegebag.dto.ShowPrivilegeLogoResultDto;
import com.biyao.mac.client.redbag.shop.privilegebag.service.IShopRedBagPrivilegeBagService;
import com.biyao.orderquery.client.tob.IBOrderBaseQueryService;
import com.biyao.search.common.enums.PlatformEnum;
import com.biyao.search.common.enums.SearchOrderByEnum;
import com.biyao.search.common.model.CommonRequestParam;
import com.biyao.search.common.model.FacetItem;
import com.biyao.search.common.model.SearchItem;
import com.biyao.search.facet.sdk.bean.Facet;
import com.biyao.search.ui.cache.AlgorithmRedisDataCache;
import com.biyao.search.ui.cache.CoffeePrivateCache;
import com.biyao.search.ui.cache.ProductCache;
import com.biyao.search.ui.cache.RedisDataCache;
import com.biyao.search.ui.constant.*;
import com.biyao.search.ui.enums.ActivityEnum;
import com.biyao.search.ui.manager.FacetManager;
import com.biyao.search.ui.model.SearchProductInfo;
import com.biyao.search.ui.remote.request.UISearchRequest;
import com.biyao.search.ui.remote.response.ProductLabel;
import com.biyao.search.ui.remote.response.SearchProduct;
import com.biyao.search.ui.remote.response.Template;
import com.biyao.search.ui.remote.response.TemplateData;
import com.biyao.search.ui.util.CmsUtil;
import com.biyao.search.ui.util.TrackParamUtil;
import com.biyao.upc.dubbo.client.business.toc.IBusinessTocDubboService;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

import com.biyao.search.ui.constant.CommonConstant;

@Service
public class ProductDetailService {

    @Autowired
    IBOrderBaseQueryService orderBaseService;

    @Autowired
    IShopRedBagPrivilegeBagService shopRedBagPrivilegeBagService;

    @Autowired
    IBusinessTocDubboService iBusinessTocDubboService;

    @Autowired
    ProductCache productCache;

    @Autowired
    RedisDataCache redisDataCache;

    @Autowired
    AlgorithmRedisDataCache algorithmRedisDataCache;

    @Autowired
    FacetManager facetManager;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private DecimalFormat df = new DecimalFormat("#.##");

    private final List<Long> materialIdIn = Arrays.asList(CommonConstant.CREATION_SHOW_ICON_ID,CommonConstant.CREATION_PAPERWORK_ICON_ID,CommonConstant.CREATION_LABEL_ID);

    /**
     * 排序后填充商品信息，因为原来的实现方式在排序置后填充价格信息，重置sku会打乱价格排序，为了解决价格排序乱序问题，将价格、销量的等基础信息填充移到排序之前
     *
     * @param products    含基础信息（价格、销量）的商品
     * @param commonParam
     * @param request
     * @param blockId
     * @param request
     * @param trackBlock  spm中的模块ID
     * @return
     */
    public void detailProductAfterSort(List<SearchProduct> products, CommonRequestParam commonParam, String blockId,
                                       UISearchRequest request, String trackBlock, String templateType, String aid) {

        //没有商品直接返回
        if (products == null || products.size() == 0) {
            return;
        }

        /**
         * 基本信息
         * */
        PlatformEnum platform = request.getPlatform();
        Integer uid = commonParam.getUid();
        String sourceId = request.getSourceId();


        /**
         * 业务标识
         * */
        //是否展示一起拼标签 使用新路由
        boolean isShowTogetherAndNewRoute = true;
        // 特权金标识
        ShowPrivilegeLogoResultDto isUserHasPrivilege = request.getPrivilegeLogo();
        // 一起拼标识，用户身份标识
        boolean isJumpTogroup = request.getIsJumpTogroup();
        boolean isNewUser = request.getIsNewUser();
        //标签模板
        JSONObject labelConfig = redisDataCache.getLabelConfig();

        String videoSwitch = CmsUtil.getMaterialValue(CmsSwitch.VIDEO_SWITCH_ID);//视频标识展示开关
        String videoImageUrl = CmsUtil.getMaterialImage(CmsSwitch.VIDEO_ICON_ID);//视频播放标识

        Map<Long, MaterialElementBaseDTO> MaterialElements = null;
        Boolean isSupportCreation = request.getIsSupportCreation() == CommonConstant.IS_SUPPORT_CREATOR_YES;
        if(isSupportCreation) {
            //只有当前搜索支持才查询造物商品素材
            MaterialElements = CmsUtil.queryMaterial(materialIdIn);
        }

        /**
         * 商品处理
         * */
        //循环处理商品信息
        int position = 0;
        Iterator<SearchProduct> iterator = products.iterator();
        while (iterator.hasNext()) {
            SearchProduct item = iterator.next();
            try {
                SearchProductInfo searchProductInfo = productCache.getSearchProductInfo(item.getProductId().longValue());
                if (searchProductInfo == null) {
                    iterator.remove();
                    continue;
                }
                // 过滤下架商品
                if (searchProductInfo.getShelfStatus() == 0 && !redisDataCache.getMaskList().contains(searchProductInfo.getProductId().toString())) {
                    iterator.remove();
                    continue;
                }
                //搜本店进行店铺兜底过滤
                if (ActivityEnum.SUPPLIER_SEARCH.equals(ActivityEnum.judgeActivityType(request.getToActivity()))
                        && (StringUtils.isEmpty(request.getSupplierId())
                        || !searchProductInfo.getSupplierId().equals(Long.parseLong(request.getSupplierId())))) {
                    iterator.remove();
                    continue;
                }
                // 2020-01-15 过滤su列表为空的商品，解决NullPointerException bug
                if (searchProductInfo.getSuProductList() == null || searchProductInfo.getSuProductList().size() == 0) {
                    iterator.remove();
                    continue;
                }

                //20190715 zhangjian过滤不支持全民拼商品
                if ((PageSourceEnum.JOINGROUP.getSourceId().equals(sourceId) && !isNewUser) || PageSourceEnum.ALLTOGETHER_GROUP_CHANNEL.getSourceId().equals(sourceId)) {
                    if (searchProductInfo.getAllTogether() == (byte) 0) {
                        iterator.remove();
                        continue;
                    }
                }

                // 20181127 houkun 通过用户的平台 判断当前商品是否展示
                if (StringUtils.isEmpty(searchProductInfo.getSupportPlatform())
                        || !searchProductInfo.getSupportPlatform().contains(platform.getNum() + "")) {
                    // 如果当前商品支持平台为空，或者支持平台中不包含当前用户使用的平台
                    iterator.remove();
                    continue;
                }

                //过滤 一起拼频道页、一起拼参团落地页的旧版签名、低模眼镜商品
                //并为非一起拼频道页、一起拼参团落地页的签名、低模眼镜商品设置isShowTogetherAndNewRoute
                if (searchProductInfo.getIsToggroupProduct() != null &&
                        searchProductInfo.getSupportCarve() != null &&
                        searchProductInfo.getRasterType() != null
                        && searchProductInfo.getIsToggroupProduct() == 1 //支持一起拼
                        && ("1".equals(searchProductInfo.getSupportCarve().toString()) //签名商品
                        || "1".equals(searchProductInfo.getRasterType().toString())))//低模眼镜
                {
                    if (PlatformEnum.IOS.getName().equals(commonParam.getPlatform().getName())
                            && request.getAppVersionNum() < CommonConstant.IOS_GLASSES_VERSION) {
                        if (StringUtils.isNotBlank(sourceId) &&
                                ((sourceId.equals(PageSourceEnum.TOGETHER_GROUP_CHANNEL.getSourceId()))  //一起拼频道页
                                        || (sourceId.equals(PageSourceEnum.JOINGROUP.getSourceId()))) //一起拼参团落地页面
                        ) {
                            iterator.remove();
                            continue;
                        } else {
                            isShowTogetherAndNewRoute = false;
                        }
                    }
                    if (PlatformEnum.ANDROID.getName().equals(commonParam.getPlatform().getName())
                            && request.getAppVersionNum() < CommonConstant.ANDROID_GLASSES_VERSION) {
                        if (StringUtils.isNotBlank(sourceId) &&
                                ((sourceId.equals(PageSourceEnum.TOGETHER_GROUP_CHANNEL.getSourceId()))  //一起拼频道页
                                        || (sourceId.equals(PageSourceEnum.JOINGROUP.getSourceId()))) //一起拼参团落地页面
                        ) {
                            iterator.remove();
                            continue;
                        } else {
                            isShowTogetherAndNewRoute = false;
                        }
                    }
                }


                if (PlatformEnum.IOS.getName().equals(commonParam.getPlatform().getName())
                        && CoffeePrivateCache.checkCoffeePid(searchProductInfo.getProductId())
                        && request.getAppVersionNum() < CommonConstant.IOS_COFFEE_VERSION) {
                    iterator.remove();
                    continue;
                }
                if (PlatformEnum.ANDROID.getName().equals(commonParam.getPlatform().getName())
                        && CoffeePrivateCache.checkCoffeePid(searchProductInfo.getProductId())
                        && request.getAppVersionNum() < CommonConstant.ANDROID_COFFEE_VERSION) {
                    iterator.remove();
                    continue;
                }

                // 双排不展示卖点
                if (templateType.equals(CommonConstant.TemplateType.SINGLE_PRODCUT)
                        || templateType.equals(CommonConstant.TemplateType.GROUP_BUY_SINGLE_PRODCUT)
                        || templateType.equals(CommonConstant.TemplateType.LADDER_GROUP_PRODUCT)) {
                    item.setSalePoint(searchProductInfo.getSalePoint());
                }
                item.setBackground(Strings.isNullOrEmpty(searchProductInfo.getSupplierBackground()) ? ""
                        : searchProductInfo.getSupplierBackground());

                item.setPosition(position++);

                // 图标展示信息 左上角提示类型。0没有，1新品 2 团购 3 一起拼
                // add by yangle
                if (isNewProduct(searchProductInfo)) {
                    item.setIsShowIcon(1);
                } else {
                    item.setIsShowIcon(0);
                }
                // 路由跳转信息
                item.setRedirectUrl(generateRedirectRoute(request, item, commonParam, trackBlock, aid,
                        searchProductInfo, isJumpTogroup, isNewUser, isShowTogetherAndNewRoute, sourceId));
                item.setTrackParam(generateProductTrackparam(item, blockId, request));
                // add by yangle 20181212 添加好友买过字段，仅对小程序M站使用
                item.setFriendBuy("");
                // 评价信息 app端复用这个字段，好友买+两个空格+评价信息
                String commentInfo = "";
                //ios android旧版本
                if ((PlatformEnum.IOS.getName().equals(commonParam.getPlatform().getName())
                        && request.getAppVersionNum() < CommonConstant.IOS_PLANTFORM_VERSION) ||
                        PlatformEnum.ANDROID.getName().equals(commonParam.getPlatform().getName())
                                && request.getAppVersionNum() < CommonConstant.ANDROID_PLANTFORM_VERSION) {
                    //处理旧版本好评
                    if (searchProductInfo.getPositiveComment() >= 1) {
                        commentInfo = (searchProductInfo.getPositiveComment() < 10000 ? searchProductInfo.getPositiveComment()
                                : String.format("%.1fw", (float) searchProductInfo.getPositiveComment() / 10000)) + "条好评";
                    }

                } else {
                    //获取所有人可见好评数（包含默认好评）
                    Integer goodCommentToAll = searchProductInfo.getGoodCommentToAll();

                    //好评数=0时不显示此标签
                    if (goodCommentToAll != null && goodCommentToAll.intValue() > 0) {
                        commentInfo = fillNewGoodComment(goodCommentToAll) + "条好评";
                    }

                }
                // add by yangle 20181212 app端好友关系复用此字段，所以得做特殊处理
                if (PlatformEnum.IOS.getName().equals(platform.getName())
                        || PlatformEnum.ANDROID.getName().equals(platform.getName())) {
                    commentInfo = StringUtils.isBlank(item.getFriendBuy()) ? commentInfo
                            : item.getFriendBuy() + "  " + commentInfo;
                }
                item.setCommentInfo(commentInfo.trim());

                boolean isShowPrivilege = false;
                // 默认传0
                item.setPrivilegeInfo("0");
                // TODO 特权金2.2.1 ，在一起拼频道页中 商品是否展示特权金与商品展示抵扣金额一致
                if (ActivityEnum.GROUP_BUY.equals(ActivityEnum.judgeActivityType(request.getToActivity()))) {

                    try {
                        // redBagCashInfoQueryService
                        // 如果前端传的面额为空 或 为0,或者当前商品不支持老客特权金,不显示抵扣金额
                        // productFromRpc.getOldUserPrivilege()!=1||
                        if (StringUtils.isEmpty(request.getPrivilegeAmount()) || "0".equals(request.getPrivilegeAmount())) {
                            isShowPrivilege = false;
                        } else {
                            // 当前未使用的特权金面额不为0， 计算当前商品的优惠金额
                            BigDecimal newPrivilateLimit = searchProductInfo.getNewPrivilateLimit();
                            BigDecimal oldPrivilateLimit = searchProductInfo.getOldPrivilateLimit();
                            BigDecimal free = new BigDecimal(0);
                            if (isUserHasPrivilege != null
                                    && !org.springframework.util.StringUtils.isEmpty(isUserHasPrivilege.getUserType())) {
                                // 1新用户 2老用户
                                if (isUserHasPrivilege.getUserType() == 1) {
                                    free = newPrivilateLimit;
                                } else if (isUserHasPrivilege.getUserType() == 2) {
                                    free = oldPrivilateLimit;
                                }
                            }
                            BigDecimal genAmount = null;
                            try {
                                if (Strings.isNullOrEmpty(request.getPrivilegeAmount())) {
                                    genAmount = new BigDecimal(0);
                                } else {
                                    genAmount = new BigDecimal(request.getPrivilegeAmount());
                                }
                            } catch (Exception e) {
                                logger.error("[一般异常]计算一起拼中特权金商品可抵扣金额PrivilegeAmount参数错误，PrivilegeAmount={}", request.getPrivilegeAmount());
                                genAmount = new BigDecimal(0);
                            }
                            BigDecimal discountAmount = genAmount.compareTo(free) > 0 ? free : genAmount;
                            if (discountAmount.toBigInteger().intValue() > 0) {
                                item.setPrivilegeInfo(discountAmount.toBigInteger().toString());
                                isShowPrivilege = true;
                            }
                        }
                    } catch (Exception e) {
                        logger.error("[一般异常]计算一起拼中特权金商品可抵扣金额错误");
                    }

                } else {
                    // 非一起拼频道页 商品是否展示特权金
                    isShowPrivilege = isShowPrivilege(isUserHasPrivilege, searchProductInfo);
                }

                //------支持必要造物-start------
                //判断本次调用当前版本+活动页都支持必要造物商品 && 该商品是必要造物商品，满足上述条件时，封装造物价文案和造物角标
                if(isSupportCreation && CommonConstant.IS_CREATOR_YES == searchProductInfo.getIsCreation()){
                    setCreationInfos(item,MaterialElements,request);
                }else{
                    //如果重置SKU时计算出了造物价，但是活动或版本不支持，那就干掉
                    item.setCreationPriceStr(null);
                }
                //--------必要造物end------


                // 设置商品标签
                setLabels(item, searchProductInfo, isShowTogetherAndNewRoute, isNewUser, isJumpTogroup, isShowPrivilege, platform, request, labelConfig);
                // 设置商品描述
                setProductDes(item, searchProductInfo);

                //设置商品是否展示视频标识以及视频标识Icon图片地址Produ
                setProductVideoIconInfo(item,videoSwitch,videoImageUrl);

            } catch (Exception e) {
                logger.error("[严重异常]商品信息补全失败并移除，[uuid={}, pid={}, sid={}]", request.getUuid(), item.getProductId(), request.getSid(), e);
                iterator.remove();
            }
        }

    }

    /**
     * 填充前端需要的商品信息
     *
     * @param products    在方法内部会填充SearchProduct的属性
     * @param commonParam
     * @param request
     * @param blockId
     * @param request
     * @param trackBlock  spm中的模块ID
     * @return
     */
    public void detailProduct(List<SearchProduct> products, CommonRequestParam commonParam, String blockId,
                              UISearchRequest request, String trackBlock, String templateType, String aid) {

        //没有商品直接返回
        if (products == null || products.size() == 0) {
            return;
        }

        /**
         * 基本信息
         * */
        PlatformEnum platform = request.getPlatform();
        String sourceId = request.getSourceId();


        /**
         * 业务标识
         * */
        //是否展示一起拼标签 使用新路由
        boolean isShowTogetherAndNewRoute = true;
        // 特权金标识
        ShowPrivilegeLogoResultDto isUserHasPrivilege = request.getPrivilegeLogo();
        // 一起拼标识，用户身份标识
        boolean isJumpTogroup = request.getIsJumpTogroup();
        boolean isNewUser = request.getIsNewUser();
        //标签模板
        JSONObject labelConfig = redisDataCache.getLabelConfig();

        String videoSwitch = CmsUtil.getMaterialValue(CmsSwitch.VIDEO_SWITCH_ID);//视频标识展示开关
        String videoImageUrl = CmsUtil.getMaterialImage(CmsSwitch.VIDEO_ICON_ID);//视频播放标识


        Map<Long, MaterialElementBaseDTO> MaterialElements = null;
        Boolean isSupportCreation = request.getIsSupportCreation() == CommonConstant.IS_SUPPORT_CREATOR_YES;
        if(isSupportCreation && "searchResult_feeds".equals(trackBlock)) {
            //只有当前版本号大于等于必要造物商品版本号才查询造物商品素材
            MaterialElements = CmsUtil.queryMaterial(materialIdIn);
        }


        /**
         * 商品处理
         * */
        //循环处理商品信息
        int position = 0;
        Iterator<SearchProduct> iterator = products.iterator();
        while (iterator.hasNext()) {
            SearchProduct item = iterator.next();
            try {
                SearchProductInfo searchProductInfo = productCache.getSearchProductInfo(item.getProductId().longValue());
                if (searchProductInfo == null) {
                    iterator.remove();
                    continue;
                }
                // 过滤下架商品
                if (searchProductInfo.getShelfStatus() == 0 && !redisDataCache.getMaskList().contains(searchProductInfo.getProductId().toString())) {
                    iterator.remove();
                    continue;
                }

                // 2020-01-15 过滤su列表为空的商品，解决NullPointerException bug
                if (searchProductInfo.getSuProductList() == null || searchProductInfo.getSuProductList().size() == 0) {
                    iterator.remove();
                    continue;
                }

                //20190715 zhangjian过滤不支持全民拼商品
                if ((PageSourceEnum.JOINGROUP.getSourceId().equals(sourceId) && !isNewUser) || PageSourceEnum.ALLTOGETHER_GROUP_CHANNEL.getSourceId().equals(sourceId)) {
                    if (searchProductInfo.getAllTogether() == (byte) 0) {
                        iterator.remove();
                        continue;
                    }
                }

                // 20181127 houkun 通过用户的平台 判断当前商品是否展示
                if (StringUtils.isEmpty(searchProductInfo.getSupportPlatform())
                        || !searchProductInfo.getSupportPlatform().contains(platform.getNum() + "")) {
                    // 如果当前商品支持平台为空，或者支持平台中不包含当前用户使用的平台
                    iterator.remove();
                    continue;
                }

                //过滤 一起拼频道页、一起拼参团落地页的旧版签名、低模眼镜商品
                //并为非一起拼频道页、一起拼参团落地页的签名、低模眼镜商品设置isShowTogetherAndNewRoute
                if (searchProductInfo.getIsToggroupProduct() != null &&
                        searchProductInfo.getSupportCarve() != null &&
                        searchProductInfo.getRasterType() != null
                        && searchProductInfo.getIsToggroupProduct() == 1 //支持一起拼
                        && ("1".equals(searchProductInfo.getSupportCarve().toString()) //签名商品
                        || "1".equals(searchProductInfo.getRasterType().toString())))//低模眼镜
                {
                    if (PlatformEnum.IOS.getName().equals(commonParam.getPlatform().getName())
                            && request.getAppVersionNum() < CommonConstant.IOS_GLASSES_VERSION) {
                        if (StringUtils.isNotBlank(sourceId) &&
                                ((sourceId.equals(PageSourceEnum.TOGETHER_GROUP_CHANNEL.getSourceId()))  //一起拼频道页
                                        || (sourceId.equals(PageSourceEnum.JOINGROUP.getSourceId()))) //一起拼参团落地页面
                        ) {
                            iterator.remove();
                            continue;
                        } else {
                            isShowTogetherAndNewRoute = false;
                        }
                    }
                    if (PlatformEnum.ANDROID.getName().equals(commonParam.getPlatform().getName())
                            && request.getAppVersionNum() < CommonConstant.ANDROID_GLASSES_VERSION) {
                        if (StringUtils.isNotBlank(sourceId) &&
                                ((sourceId.equals(PageSourceEnum.TOGETHER_GROUP_CHANNEL.getSourceId()))  //一起拼频道页
                                        || (sourceId.equals(PageSourceEnum.JOINGROUP.getSourceId()))) //一起拼参团落地页面
                        ) {
                            iterator.remove();
                            continue;
                        } else {
                            isShowTogetherAndNewRoute = false;
                        }
                    }
                }


                if (PlatformEnum.IOS.getName().equals(commonParam.getPlatform().getName())
                        && CoffeePrivateCache.checkCoffeePid(searchProductInfo.getProductId())
                        && request.getAppVersionNum() < CommonConstant.IOS_COFFEE_VERSION) {
                    iterator.remove();
                    continue;
                }
                if (PlatformEnum.ANDROID.getName().equals(commonParam.getPlatform().getName())
                        && CoffeePrivateCache.checkCoffeePid(searchProductInfo.getProductId())
                        && request.getAppVersionNum() < CommonConstant.ANDROID_COFFEE_VERSION) {
                    iterator.remove();
                    continue;
                }

                /* 基本信息 */
                item.setTitle(searchProductInfo.getShortTitle());
                item.setFullTitle(searchProductInfo.getTitle());
                item.setImage(searchProductInfo.getSquarePortalImg());
                String squarePortalImgWebp = searchProductInfo.getSquarePortalImgWebp();
                item.setImageWebp(StringUtils.isNoneBlank(squarePortalImgWebp) ? squarePortalImgWebp : searchProductInfo.getSquarePortalImg());
                item.setPriceStr(df.format(searchProductInfo.getPrice() / 100.00));
                item.setSpuId(searchProductInfo.getProductId().toString());
                item.setSalesVolume(searchProductInfo.getSalesVolume());
                item.setSalesVolume7(searchProductInfo.getSalesVolume7());
                item.setFirstOnShelfTime(searchProductInfo.getFirstOnShelfTime());

                if (templateType.equals(CommonConstant.TemplateType.GROUP_BUY_SINGLE_PRODCUT)) {
                    if ((searchProductInfo.getIsLaddergroupProduct() != null && searchProductInfo.getIsLaddergroupProduct() == 1)
                            && (searchProductInfo.getIsToggroupProduct() != null && searchProductInfo.getIsToggroupProduct() != 1)
                            && (item.getGroupBuyPriceStr() != null)) {
                        try {
                            item.setGroupBuyPriceStr(df.format(new BigDecimal(item.getGroupBuyPriceStr())));
                        } catch (Exception e) {
                            // 2019-09-06 zhaiweixi 日志注释
                            item.setGroupBuyPriceStr("0");
                        }

                    } else if (searchProductInfo.getGroupPrice() != null) {
                        item.setGroupBuyPriceStr(df.format(searchProductInfo.getGroupPrice() / 100.00));
                    }
                }

                // 双排不展示卖点
                if (templateType.equals(CommonConstant.TemplateType.SINGLE_PRODCUT)
                        || templateType.equals(CommonConstant.TemplateType.GROUP_BUY_SINGLE_PRODCUT)
                        || templateType.equals(CommonConstant.TemplateType.LADDER_GROUP_PRODUCT)) {
                    item.setSalePoint(searchProductInfo.getSalePoint());
                }
                item.setSuId(searchProductInfo.getSuId().toString());
                item.setBackground(Strings.isNullOrEmpty(searchProductInfo.getSupplierBackground()) ? ""
                        : searchProductInfo.getSupplierBackground());

                item.setPosition(position++);

                // 图标展示信息 左上角提示类型。0没有，1新品 2 团购 3 一起拼
                // add by yangle
                if (isNewProduct(searchProductInfo)) {
                    item.setIsShowIcon(1);
                } else {
                    item.setIsShowIcon(0);
                }
                try {
                    //2019-10-24 搜索V1.7 重置sku信息
                    setSkuInfo(item, request.getFacets(), request.getUserSizeMap());
                } catch (Exception e) {
                    logger.error("[一般异常]商品重置sku信息失败:[uuid={}, pid={}, sid={}]", request.getUuid(), item.getProductId(), request.getSid(), e);
                }
                // 路由跳转信息
                item.setRedirectUrl(generateRedirectRoute(request, item, commonParam, trackBlock, aid,
                        searchProductInfo, isJumpTogroup, isNewUser, isShowTogetherAndNewRoute, sourceId));
                item.setTrackParam(generateProductTrackparam(item, blockId, request));
                item.setFriendBuy("");
                // 评价信息 app端复用这个字段，好友买+两个空格+评价信息
                String commentInfo = "";
                //ios android旧版本
                if ((PlatformEnum.IOS.getName().equals(commonParam.getPlatform().getName())
                        && request.getAppVersionNum() < CommonConstant.IOS_PLANTFORM_VERSION) ||
                        PlatformEnum.ANDROID.getName().equals(commonParam.getPlatform().getName())
                                && request.getAppVersionNum() < CommonConstant.ANDROID_PLANTFORM_VERSION) {
                    //处理旧版本好评
                    if (searchProductInfo.getPositiveComment() >= 1) {
                        commentInfo = (searchProductInfo.getPositiveComment() < 10000 ? searchProductInfo.getPositiveComment()
                                : String.format("%.1fw", (float) searchProductInfo.getPositiveComment() / 10000)) + "条好评";
                    }

                } else {
                    //获取所有人可见好评数（包含默认好评）
                    Integer goodCommentToAll = searchProductInfo.getGoodCommentToAll();

                    //好评数=0时不显示此标签
                    if (goodCommentToAll != null && goodCommentToAll.intValue() > 0) {
                        commentInfo = fillNewGoodComment(goodCommentToAll) + "条好评";
                    }

                }
                // add by yangle 20181212 app端好友关系复用此字段，所以得做特殊处理
                if (PlatformEnum.IOS.getName().equals(platform.getName())
                        || PlatformEnum.ANDROID.getName().equals(platform.getName())) {
                    commentInfo = StringUtils.isBlank(item.getFriendBuy()) ? commentInfo
                            : item.getFriendBuy() + "  " + commentInfo;
                }
                item.setCommentInfo(commentInfo.trim());

                boolean isShowPrivilege = false;
                // 默认传0
                item.setPrivilegeInfo("0");
                // TODO 特权金2.2.1 ，在一起拼频道页中 商品是否展示特权金与商品展示抵扣金额一致
                if (ActivityEnum.GROUP_BUY.equals(ActivityEnum.judgeActivityType(request.getToActivity()))) {

                    try {
                        // 如果前端传的面额为空 或 为0,或者当前商品不支持老客特权金,不显示抵扣金额
                        if (StringUtils.isEmpty(request.getPrivilegeAmount()) || "0".equals(request.getPrivilegeAmount())
                                || StringUtils.equals("(null)", request.getPrivilegeAmount())) {
                            isShowPrivilege = false;
                        } else {
                            // 当前未使用的特权金面额不为0， 计算当前商品的优惠金额
                            BigDecimal newPrivilateLimit = searchProductInfo.getNewPrivilateLimit();
                            BigDecimal oldPrivilateLimit = searchProductInfo.getOldPrivilateLimit();
                            BigDecimal free = new BigDecimal(0);
                            if (isUserHasPrivilege != null
                                    && !org.springframework.util.StringUtils.isEmpty(isUserHasPrivilege.getUserType())) {
                                // 1新用户 2老用户
                                if (isUserHasPrivilege.getUserType() == 1) {
                                    free = newPrivilateLimit;
                                } else if (isUserHasPrivilege.getUserType() == 2) {
                                    free = oldPrivilateLimit;
                                }
                            }
                            BigDecimal genAmount = null;
                            try {
                                if (Strings.isNullOrEmpty(request.getPrivilegeAmount())) {
                                    genAmount = new BigDecimal(0);
                                } else {
                                    genAmount = new BigDecimal(request.getPrivilegeAmount());
                                }
                            } catch (Exception e) {
                                logger.error("[一般异常]计算一起拼中特权金商品可抵扣金额PrivilegeAmount参数错误，PrivilegeAmount={}", request.getPrivilegeAmount(), e);
                                genAmount = new BigDecimal(0);
                            }
                            BigDecimal discountAmount = genAmount.compareTo(free) > 0 ? free : genAmount;
                            if (discountAmount.toBigInteger().intValue() > 0) {
                                item.setPrivilegeInfo(discountAmount.toBigInteger().toString());
                                isShowPrivilege = true;
                            }
                        }
                    } catch (Exception e) {
                        logger.error("[一般异常]计算一起拼中特权金商品可抵扣金额错误, request = {}", JSON.toJSONString(request), e);
                    }

                } else {
                    // 非一起拼频道页 商品是否展示特权金
                    isShowPrivilege = isShowPrivilege(isUserHasPrivilege, searchProductInfo);
                }

                //判断本次调用是猜你喜欢 && 当前版本+活动页都支持必要造物商品 && 该商品是必要造物商品，满足上述条件时，封装造物价文案和造物角标
                if("searchResult_feeds".equals(trackBlock) && isSupportCreation && searchProductInfo.getIsCreation() == CommonConstant.IS_CREATOR_YES){
                    //猜你喜欢填充商品信息都在这个方法，缺少造物价的填充逻辑
                    item.setCreationPriceStr(searchProductInfo.getCreationPriceStr());
                    //其他造物信息的填充，这里跟主搜一致
                    setCreationInfos(item,MaterialElements, request);
                }else{
                    //如果重置SKU时计算出了造物价，但是活动或版本不支持，那就干掉
                    item.setCreationPriceStr(null);
                }

                // 设置商品标签
                setLabels(item, searchProductInfo, isShowTogetherAndNewRoute, isNewUser, isJumpTogroup, isShowPrivilege, platform, request, labelConfig);
                // 设置商品描述
                setProductDes(item, searchProductInfo);


                //设置商品是否展示视频标识以及视频标识Icon图片地址Produ
                setProductVideoIconInfo(item,videoSwitch,videoImageUrl);


            } catch (Exception e) {
                logger.error("[严重异常]商品信息补全失败并移除，[uuid={}, pid={}, sid={}]", request.getUuid(), item.getProductId(), request.getSid(), e);
                iterator.remove();
            }
        }

    }

    /**
     * 重置sku信息（suid、入口图、价格）
     *
     * @return
     */
    public void setSkuInfo(SearchProduct product, List<FacetItem> facetItems, Map<Long, List<String>> userSizeMap) {

        List<Long> productIds = Lists.newArrayList(product.getProductId().longValue());

        //转换facet对象
        List<Facet> facetList = new ArrayList<>();
        if (facetItems != null) {
            for (FacetItem facetItem : facetItems) {
                Facet facet = facetManager.getFacet(facetItem.getKey());
                if (facet != null) {
                    List<com.biyao.search.facet.sdk.bean.FacetItem> selectValues = new ArrayList<>();
                    facetItem.getValues().forEach(item -> {
                        selectValues.add(new com.biyao.search.facet.sdk.bean.FacetItem(item));
                    });
                    facet.setValues(selectValues);
                    facetList.add(facet);
                }
            }
        }

        //获取选定sku并重置信息
        Map<Long, Long> resultMap = facetManager.selectEntrySu(productIds, facetList, userSizeMap);
        if (resultMap.size() > 0) {
            Set<Long> resultProductIds = resultMap.keySet();
            if (resultProductIds.contains(product.getProductId().longValue())) {
                SearchProductInfo searchProductInfo = productCache.getSearchProductInfo(product.getProductId().longValue());
                if (searchProductInfo != null) {
                    if (searchProductInfo.getSuProductList() != null) {
                        for (SuProduct suProduct : searchProductInfo.getSuProductList()) {
                            if (suProduct.getSuId().equals(resultMap.get(product.getProductId().longValue()))) {
                                product.setSuId(suProduct.getSuId().toString());
                                product.setImage(suProduct.getSquarePortalImg());
                                product.setImageWebp(suProduct.getSquarePortalImgWebp());
                                product.setPriceStr(String.valueOf(suProduct.getPrice() / 100));
                                product.setAllowancePrice(suProduct.getAllowancePrice());
                                setCreationPriceStrOfSP(product,suProduct);//此处计算SKU造物价并赋值给SPU造物价

                                //判断当前商品是造物商品时，计算SKU造物价
                                if(searchProductInfo.getIsCreation()!= null && CommonConstant.IS_CREATOR_YES == searchProductInfo.getIsCreation()){
                                    setCreationPriceStrOfSP(product,suProduct);//此处计算SKU造物价并赋值给SPU造物价
                                }
                            }
                        }
                    } else {
                        String json = JSONObject.toJSONString(searchProductInfo);
                        logger.error("[严重异常]获取商品sku信息异常，pid：" + searchProductInfo.getProductId() + ",异常商品信息：" + json);
                    }
                } else {
                    logger.error("[一般异常]缓存中读取商品信息异常，pid：" + product.getProductId());
                }
            }
        }

    }

    /**
     * 设置商品标签
     */
    private void setLabels(SearchProduct item, SearchProductInfo productFromRpc, boolean isShowTogetherAndNewRoute, boolean isNewUser, boolean isJumpTogroup, boolean isShowPrivilege, PlatformEnum platform, UISearchRequest request, JSONObject labelConfig) {
        boolean allowanceDeductionFlag = ActivityEnum.ALLOWANCE_DEDUCTION.equals(ActivityEnum.judgeActivityType(request.getToActivity()));
        if (redisDataCache.getMaskList().contains(productFromRpc.getProductId().toString()) && productFromRpc.getShelfStatus() == 0) {
            //口罩商品下架时标签特殊处理
            ProductLabel textureLabel = new ProductLabel();
            textureLabel.setContent("今日产能已饱和，每日10点开放产能");
            textureLabel.setColor(ColorCodeConsts.TITLE_COLOR_WHITE);
            textureLabel.setTextColor(ColorCodeConsts.TITLE_COLOR_RED);
            textureLabel.setRoundColor(ColorCodeConsts.TITLE_COLOR_RED);
            item.getLabels().add(textureLabel);
        } else {
            if (productFromRpc.getSearchLabels() == null) {
                item.setLabels(new ArrayList<>());
            } else {
                List<String> labelContent = productFromRpc.getSearchLabels().size() <= 2
                        ? productFromRpc.getSearchLabels()
                        : productFromRpc.getSearchLabels().subList(0, 2);
                item.setLabels(labelContent.stream().map(i -> {
                    ProductLabel label = new ProductLabel();
                    label.setContent(i);
                    return label;
                }).collect(Collectors.toList())); // 最多展示两个标签
            }

            //-----------------------------------业务标签----start-----------------------
            //当前版本和活动支持必要造物商品 并且当前商品是必要造物商品
            if(request.getIsSupportCreation() != null && request.getIsSupportCreation() == CommonConstant.IS_SUPPORT_CREATOR_YES && CommonConstant.IS_CREATOR_YES==productFromRpc.getIsCreation()) {
                MaterialElementBaseDTO creationLabelDto = CmsUtil.queryMaterial(CommonConstant.CREATION_LABEL_ID);
                if(creationLabelDto != null){
                    Object creationLabel = creationLabelDto.getValue();
                    if(creationLabel != null && !"".equals(creationLabel)){
                        //判断造物标签如果取到了，则添加到业务标签中，否则不展示业务标签
                        ProductLabel textureLabel = new ProductLabel();
                        textureLabel.setContent((String)creationLabel);
                        textureLabel.setType(CommonConstant.LABEL_TYPE_CREATOR);//标识该标签为造物标签
                        item.getLabels().add(textureLabel);
                    }
                }
            }else{
                //不支持必要造物商品的，依然走以前的逻辑
                // zhaiweixi 20190505 定制标签
                if (productFromRpc.getSupportTexture() != null && productFromRpc.getSupportTexture() == 2) {
                    ProductLabel textureLabel = new ProductLabel();
                    textureLabel.setContent("定制");
                    item.getLabels().add(textureLabel);
                }
                if (allowanceDeductionFlag) {
                    addAllowanceLabel(item,request.getUserTotalAllowanceAmt());
                } else {
                    // 一起拼标签
                    addTogetherLabel(item, productFromRpc, isShowTogetherAndNewRoute, isNewUser, isJumpTogroup, platform, request);
                    // 特权金标签
                    addprivilegeTag(item, isShowPrivilege);
                }
            }

            //-----------------------------------业务标签----end-----------------------
            //设置标签样式
            setLabelProperties(item.getLabels(), labelConfig);
        }
    }

    /**
     * 设置标签属性
     *
     * @param labels
     * @param labelConfig
     */
    private void setLabelProperties(List<ProductLabel> labels, JSONObject labelConfig) {
        for (ProductLabel label : labels) {
            if (StringUtils.isNotEmpty(label.getContent())) {
                //造物标签和津贴标签是特殊处理的
                //判断该标签是否是造物标签，即label的type==1
                if (CommonConstant.LABEL_TYPE_CREATOR == label.getType()) {
                    label.setColor(ColorCodeConsts.TITLE_CREATOR_LABEL_FF2783);
                    label.setTextColor(ColorCodeConsts.TITLE_COLOR_WHITE);
                    label.setRoundColor(ColorCodeConsts.TITLE_CREATOR_LABEL_FF2783);
                    continue;
                }

                //判断该标签是否是津贴可抵  标签内容前缀为：津贴可抵
                if (CommonConstant.LABEL_TYPE_ALLOWANCE == label.getType()) {
                    label.setColor(ColorCodeConsts.EMPTY);
                    label.setTextColor(ColorCodeConsts.TITLE_COLOR_RED_FF7773);
                    label.setRoundColor(ColorCodeConsts.TITLE_COLOR_RED_FF7773);
                    continue;
                }
            }
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
        }
    }

    /**
     * 构建特权金标签并加入商品标签列表
     *
     * @param item
     * @param isShowPrivilege
     */
    private void addprivilegeTag(SearchProduct item, boolean isShowPrivilege) {
        if (isShowPrivilege) {
            ProductLabel privilegeTag = new ProductLabel();
            privilegeTag.setContent("特权金");
            item.getLabels().add(privilegeTag);
        }
    }

    /**
     * 构建一起拼标签并加入商品标签列表
     *
     * @param item
     * @param productFromRpc
     * @param isShowTogetherAndNewRoute
     * @param isNewUser
     * @param isJumpTogroup
     * @param platform
     * @param request
     */
    private void addTogetherLabel(SearchProduct item, SearchProductInfo productFromRpc, boolean isShowTogetherAndNewRoute, boolean isNewUser, boolean isJumpTogroup, PlatformEnum platform, UISearchRequest request) {
        ProductLabel together = new ProductLabel();
        if (productFromRpc.getIsToggroupProduct() == 1 && isShowTogetherAndNewRoute) {
            if (isNewUser) {
                if (isJumpTogroup) {//新客符如果符合条件，要添加标签
                    together.setContent(CommonConstant.LABEL_CONTENT_YQP);
                    item.getLabels().add(together);
                }
            } else {//老客逻辑不变
                together.setContent(CommonConstant.LABEL_CONTENT_YQP);
                item.getLabels().add(together);
            }
        } else if (PlatformEnum.IOS.getName().equals(platform.getName())
                && request.getAppVersionNum() > CommonConstant.IOS_YIQIPIN17_VERSION
                && (!org.springframework.util.StringUtils.isEmpty(productFromRpc.getIsLaddergroupProduct())
                && productFromRpc.getIsLaddergroupProduct().intValue() == 1)
        ) {
            together.setContent(CommonConstant.LABEL_CONTENT_YQP);
            item.getLabels().add(together);
        } else if (PlatformEnum.ANDROID.getName().equals(platform.getName())
                && request.getAppVersionNum() > CommonConstant.ANDROID_YIQIPIN17_VERSION
                && (!org.springframework.util.StringUtils.isEmpty(productFromRpc.getIsLaddergroupProduct())
                && productFromRpc.getIsLaddergroupProduct().intValue() == 1)
        ) {
            together.setContent(CommonConstant.LABEL_CONTENT_YQP);
            item.getLabels().add(together);
        } else if ((PlatformEnum.MINI.getName().equals(platform.getName())
                || PlatformEnum.M.getName().equals(platform.getName()))
                && (!org.springframework.util.StringUtils.isEmpty(productFromRpc.getIsLaddergroupProduct())
                && productFromRpc.getIsLaddergroupProduct().intValue() == 1)
        ) {
            together.setContent(CommonConstant.LABEL_CONTENT_YQP);
            item.getLabels().add(together);
        }
    }

    /**
     * 构建津贴标签并加入商品标签列表
     *  @param item 商品对象
     * @param userTotalAllowance
     */
    private void addAllowanceLabel(SearchProduct item, BigDecimal userTotalAllowance) {

        BigDecimal allowancePrice =  item.getAllowancePrice();
        //从CMS获取津贴文案，第一次查CMS，之后查本地缓存
        String allowancePre = CmsUtil.getMaterialInput(CommonConstant.ALLOWANCE_LABEL_ID);
        //情况1：商品津贴可抵小于等于0
        //情况2：津贴标签文案设置变量yonghujintiekedi，但是用户总津贴数小于等于0
        //不封装津贴标签
        if (allowancePrice.intValue() <= 0 || (allowancePre!=null && allowancePre.contains(CommonConstant.ALLOWANCE_LABEL_YONGHUKEDI_CONS) && userTotalAllowance.intValue() <=0)) {
            return;
        }
        //--------------------------------------------原逻辑写死文案，弃用-----------------------------------------------------------------------
        //String allowanceLabelContent = CommonConstant.LABEL_CONTENT_ALLOWANCE_PREFIX + item.getAllowancePrice().toString() + "元";//
        //--------------------------------------------原逻辑写死文案，弃用-----------------------------------------------------------------------
        /**新逻辑：
         * 1、从CMS获取津贴文案素材（素材ID：10830249）缓存到本地：allowancePre = CmsUtil.getMaterialInput(CommonConstant.ALLOWANCE_LABEL_ID)
         * 2、判断如果素材信息为空，则不封装津贴标签，如果不为空，则继续第3步
         * 3、判断如果素材支持变量 yonghujintiekedi，则 获取当前用户的总津贴数： userTotalAllowance = getUserAllowanceTotal(uid)
         * 4、判断allowancePrice 和 userTotalAllowance 值的大小，如果allowancePrice大于userTotalAllowance，则将userTotalAllowance赋值给allowancePrice
         * 5、拼接标签内容：allowancePre + allowancePrice.toString() + "元"
         * 6、创建标签对象，赋值标签内容，并且赋值标签类型：allowanceLabel.setType(2)(2标识该标签为津贴标签)
         * 7、将该标签对象添加到商品的标签集合中
         */
        String allowanceLabelContent = null;
        //若从CMS获取的津贴标签内容为空，或者没有配置变量jintiezuigaokedi或yonghujintiekedi时，走托底
        if( allowancePre==null || (!allowancePre.contains(CommonConstant.ALLOWANCE_LABEL_YONGHUKEDI_CONS) && !allowancePre.contains(CommonConstant.ALLOWANCE_LABEL_ZUIGAOKEDI_CONS))){
            logger.error("[严重异常]从CMS获取津贴文案格式异常，文案为：{}",allowancePre);
            allowanceLabelContent = CommonConstant.ALLOWANCE_LABEL_TUODI_CONS + allowancePrice.toString()+"元";
        }
        //津贴标签文案不为空，且配置变量为{yonghujintiekedi}
        if(allowanceLabelContent == null && allowancePre.contains(CommonConstant.ALLOWANCE_LABEL_YONGHUKEDI_CONS)){
            //用户总津贴数不为空并小于商品津贴可抵时,配置变量替换为用户总津贴数，否则用商品津贴可抵金额替换变量
            allowanceLabelContent = allowancePre.replace(CommonConstant.ALLOWANCE_LABEL_YONGHUKEDI_CONS,allowancePrice.intValue()>userTotalAllowance.intValue() ? userTotalAllowance.toString() : allowancePrice.toString());
        }
        if(allowanceLabelContent == null){
            //判断 此时cms配置变量为jintiezuigaokedi
            allowanceLabelContent = allowancePre.replace(CommonConstant.ALLOWANCE_LABEL_ZUIGAOKEDI_CONS,allowancePrice.toString());
        }
        ProductLabel allowanceLabel = new ProductLabel();
        allowanceLabel.setContent(allowanceLabelContent);
        allowanceLabel.setType(CommonConstant.LABEL_TYPE_ALLOWANCE);
        item.getLabels().add(allowanceLabel);
    }


    /**
     * 设置商品描述（在有制造商背景的商品中，1/4设置为制造商背景，其余全部设置为卖点）
     */
    private void setProductDes(SearchProduct item, SearchProductInfo searchProductInfo) {
        if (StringUtils.isBlank(searchProductInfo.getSupplierBackground())) {
            item.setProductDes(searchProductInfo.getSalePoint());
        } else {
            Double limit = 0.25d;
            if (Math.random() < limit) {
                item.setProductDes(searchProductInfo.getSupplierBackground());
            } else {
                item.setProductDes(searchProductInfo.getSalePoint());
            }

        }

    }

    /**
     * 处理新好评数
     *
     * @param goodCommentToAll
     * @return
     */
    private String fillNewGoodComment(Integer goodCommentToAll) {
        int num = goodCommentToAll.intValue();
        //好评数<10000时显示具体数值，如9999条好评
        if (num < 10000) {
            return String.valueOf(num);
        }
        //好评数=整数万且<100万时，显示“x.0w”，如10000条显示1.0w
        if (num < 1000000) {
            int rt = num % 10000;
            if (rt == 0) {
                int rtNum = num / 10000;
                return String.valueOf(rtNum) + ".0w";
            }
        }
        //1万条<好评数<100万条，且不为整数万时，显示“x.xw+”，截取至小数点后一位；例如：10001条，显示成1.0w+条；11200条，显示成1.1w+条。118801条，显示11.8w+条
        if (10000 < num && num < 1000000) {
            int rt = num % 10000;
            //不为整数
            if (rt != 0) {
                int rtNum = num / 1000;
                String val = String.valueOf(rtNum);
                return val.substring(0, val.length() - 1) + "." + val.substring(val.length() - 1, val.length()) + "w+";
            }
        }

        //好评数=整数万且好评数>=100万时，显示“xw”，如1010000条，显示101w条；
        if (num >= 1000000) {
            int rt = num % 10000;
            //整数万时
            if (rt == 0) {
                int rtNum = num / 10000;
                return String.valueOf(rtNum) + "w";
            }
            //好评数>100万且不为整数万时，显示“xw+”，如1000009条，显示100w+条；
            else {
                int rtNum = num / 10000;
                return String.valueOf(rtNum) + "w+";
            }
        }

        return "";
    }


    /**
     * 获取来源
     *
     * @param request
     * @return
     */
    private String getSourceId(UISearchRequest request) {
        String sch = request.getSch();
        String sourceId = "";
        if (StringUtils.isNotBlank(sch)) {
            sch = URLDecoder.decode(sch);
            JSONObject parseObject = null;
            try {
                parseObject = JSONObject.parseObject(sch);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (parseObject != null) {
                sourceId = parseObject.getString("sourceId");
            }
        }
        return sourceId;
    }

    private String generateProductTrackparam(SearchProduct item, String blockId, UISearchRequest request) {
        return URLEncoder.encode(String.format("sid=%s&bkId=%s&pos=%s&suid=%s", request.getSid(), blockId,
                item.getPosition(), item.getSuId()));
    }


    private String generateRedirectRoute(UISearchRequest request, SearchProduct product, CommonRequestParam commonParam,
                                         String trackBlock, String aid, SearchProductInfo productFromRpc, boolean isJumpTogroup,
                                         boolean isNewUser, boolean isShowTogetherAndNewRoute, String sourceId) {

        // stp:siteId.pageId.trackBlock.postion
        String trackParam = String.format("sid=%s&pos=%d&stp=%s", commonParam.getSid(), product.getPosition(),
                TrackParamUtil.generateSTP(request, trackBlock, String.valueOf(product.getPosition()), aid,product.getSemStr()));
        //津贴抵扣
        if (ActivityEnum.ALLOWANCE_DEDUCTION.equals(ActivityEnum.judgeActivityType(request.getToActivity()))) {
            if (PlatformEnum.IOS.getName().equals(commonParam.getPlatform().getName())
                    || PlatformEnum.ANDROID.getName().equals(commonParam.getPlatform().getName())) {
                return CommonConstant.APPRoute.L_PRODUCT_URL_PREFIX + product.getSuId() + "&" + trackParam;
            }
            if (PlatformEnum.MINI.getName().equals(commonParam.getPlatform().getName())) {
                return "/pages/products/products?suId=" + product.getSuId() + "&" + trackParam + "&ignoreBusiness=1&querySource=shopAllow";
            }
        }
        //买二返一
        if (ActivityEnum.BUY2_RETURN_ALLOWANCE.equals(ActivityEnum.judgeActivityType(request.getToActivity()))) {
            if (PlatformEnum.IOS.getName().equals(commonParam.getPlatform().getName())
                    || PlatformEnum.ANDROID.getName().equals(commonParam.getPlatform().getName())) {
                if (CoffeePrivateCache.checkCoffeePid(productFromRpc.getProductId())) {
                    return CommonConstant.APPRoute.COFFEE_PRODUCT_URL_PREFIX + product.getSuId() + "&customCoffeeId=2&productShowType=6" + "&"
                            + trackParam;
                }
                return CommonConstant.APPRoute.L_PRODUCT_URL_PREFIX + product.getSuId() + "&productShowType=6&" + trackParam;
            }
            if (PlatformEnum.MINI.getName().equals(commonParam.getPlatform().getName())) {
                if (CoffeePrivateCache.checkCoffeePid(productFromRpc.getProductId())) {
                    return "/pages/products/products?suId=" + product.getSuId() + "&designId=2&" + trackParam;
                }
                return "/pages/products/products?suId=" + product.getSuId() + "&" + trackParam;

            }
        }

        /** APP返回路由表 **/
        // TODO 咖啡版本判断
        if (PlatformEnum.IOS.getName().equals(commonParam.getPlatform().getName())
                || PlatformEnum.ANDROID.getName().equals(commonParam.getPlatform().getName())) {
            // if (product.getIsShowIcon() == 2) { // 团购方式售卖
            // return CommonConstant.APPRoute.GROUP_PRODUCT_URL_PREFIX + product.getSuId() +
            // "&" + trackParam;
            // } else
            if (CoffeePrivateCache.checkCoffeePid(productFromRpc.getProductId())) {
                return CommonConstant.APPRoute.COFFEE_PRODUCT_URL_PREFIX + product.getSuId() + "&customCoffeeId=2" + "&"
                        + trackParam;
            }

            //满足此次支持必要造物的页面搜索走的都是造物商品编辑器//// 无模型或者低模商品
            String urlSuffix = "";
            if(request.getIsSupportCreation() != null && request.getIsSupportCreation() == CommonConstant.IS_SUPPORT_CREATOR_YES && productFromRpc.getIsCreation()==CommonConstant.IS_CREATOR_YES){
                urlSuffix = urlSuffix + "&productShowType=17&";//purchaseType=17标识该路由跳转造物编辑器
                //isShowSwell是造物需求新增标识是否有膨胀效果 1：是  默认是0 不膨胀
                if(ActivityEnum.FRESH_CUST_PRIVILEGE_SEARCH.getCode().equals(request.getToActivity())){
                    //新手特权金下发成功页
                    urlSuffix = urlSuffix + "isShowSwell=1";
                }else{
                    urlSuffix = urlSuffix + "isShowSwell=0";
                }
                return CommonConstant.APPRoute.L_PRODUCT_URL_PREFIX + product.getSuId() + "&" + trackParam + urlSuffix;
            }



            if (productFromRpc.getIsToggroupProduct() == 1) {
                // 同事一起拼
                // 一起拼搜索 跳转参团落地页 JOINGROUP_PRODUCT_URL_PREFIX
                if (StringUtils.isNotBlank(sourceId) && sourceId.equals(PageSourceEnum.JOINGROUP.getSourceId())) {
                    return CommonConstant.APPRoute.JOINGROUP_PRODUCT_URL_PREFIX + product.getSuId() + "&" + trackParam;
                } else {
                    if (isNewUser && isJumpTogroup && isShowTogetherAndNewRoute) {
                        return CommonConstant.APPRoute.TOGETHER_PRODUCT_URL_PREFIX + product.getSuId() + "&" + trackParam;
                    } else if (!isNewUser && isShowTogetherAndNewRoute) {
                        // 此处判断，和下面小程序的出发点相同
                        return CommonConstant.APPRoute.TOGETHER_PRODUCT_URL_PREFIX + product.getSuId() + "&" + trackParam;
                    } else {//普通商品页
                        return CommonConstant.APPRoute.L_PRODUCT_URL_PREFIX + product.getSuId() + "&" + trackParam;
                    }
                }
            } else if (PlatformEnum.IOS.equals(commonParam.getPlatform())
                    && request.getAppVersionNum() != null
                    && request.getAppVersionNum() > CommonConstant.IOS_YIQIPIN17_VERSION
                    && !org.springframework.util.StringUtils.isEmpty(productFromRpc.getIsLaddergroupProduct())
                    && productFromRpc.getIsLaddergroupProduct() == 1) {
                // 阶梯团
                return CommonConstant.APPRoute.LAGGER_GROUP_URL_PREFIX + product.getSuId() + "&groupType=1" + "&"
                        + trackParam;
            } else if (PlatformEnum.ANDROID.equals(commonParam.getPlatform())
                    && request.getAppVersionNum() != null
                    && request.getAppVersionNum() > CommonConstant.ANDROID_YIQIPIN17_VERSION
                    && (!org.springframework.util.StringUtils.isEmpty(productFromRpc.getIsLaddergroupProduct())
                    && productFromRpc.getIsLaddergroupProduct() == 1)) {
                // 阶梯团
                return CommonConstant.APPRoute.LAGGER_GROUP_URL_PREFIX + product.getSuId() + "&groupType=1" + "&"
                        + trackParam;

            } else {
                return CommonConstant.APPRoute.L_PRODUCT_URL_PREFIX + product.getSuId() + "&" + trackParam;
            }
            // PC、M返回链接
        } else if (PlatformEnum.M.getName().equals(commonParam.getPlatform().getName())) {
            return "https://m.biyao.com/products/" + product.getSuId() + ".html" + "?" + trackParam + "&" + "jumpTog=1";
        } else if (PlatformEnum.PC.getName().equals(commonParam.getPlatform().getName())) {
            return "https://www.biyao.com/products/" + product.getSuId() + "-0.html" + "?" + trackParam;
        } else if (PlatformEnum.MINI.getName().equals(commonParam.getPlatform().getName())) { // 小程序和m站一致

            //满足此次支持必要造物的页面搜索走的都是造物商品编辑器
            String urlPre = "/pages/products/products?suId=";
            String urlSuffix = "";
            if(request.getIsSupportCreation() != null && request.getIsSupportCreation() == CommonConstant.IS_SUPPORT_CREATOR_YES && productFromRpc.getIsCreation()==CommonConstant.IS_CREATOR_YES){
                //判断当前请求是支持必要造物的，包括版本和活动页  并且当前商品是必要造物商品而且造物价封装正常
                urlSuffix = "&purchaseType=17";
            }


            if (!org.springframework.util.StringUtils.isEmpty(productFromRpc.getIsLaddergroupProduct())
                    && productFromRpc.getIsLaddergroupProduct() == 1
                    && productFromRpc.getIsToggroupProduct() != 1) {
                if (org.springframework.util.StringUtils.isEmpty(request.getGroupActId())) {
                    return urlPre + product.getSuId() + "&ladderId=-1" + "&" + trackParam + urlSuffix;
                } else {
                    return urlPre + product.getSuId() + "&groupId=" + request.getGroupId()
                            + "&ladderId=" + request.getGroupActId() + "&" + trackParam + urlSuffix;
                }
            } else {
                if (isNewUser && isJumpTogroup) {
                    return urlPre + product.getSuId() + "&" + trackParam + urlSuffix;
                } else if (!isNewUser) {//这个条件可以和上面的if合并，直接判断isJumpTogroup = true；但实际的逻辑是：如果是新客，并符合一起拼规则，就跳到一起拼页面；并且，老客逻辑不变；所以多写个条件判断有助于人理解
                    return urlPre + product.getSuId() + "&" + trackParam + urlSuffix;
                } else {
                    return urlPre + product.getSuId() + "&" + trackParam + "&ignoreBusiness=1" + urlSuffix;
                }
            }

        }

        return "";
    }

    /**
     * 按照商品基础信息进行排序，需求：重置sku在价格排序后导致价格排序乱序问题的bug修复，重写排序方法
     *
     * @param products
     * @param orderBy
     * @param isGroupBuy 是否为一起拼，一起拼时价格排序使用拼团价
     * @return
     * @author: luozhuo
     * @date: 2018年7月26日 上午11:43:28
     */
    public List<SearchProduct> sortByBaseInfoAfterConvert(List<SearchProduct> products, SearchOrderByEnum orderBy, Boolean isGroupBuy) {
        if (products == null || products.size() == 0) {
            return products;
        }
        switch (orderBy) {
            case PRICE_ASC:
                products.sort((o1, o2) -> {
                    if (isGroupBuy) {
                        return Double.valueOf(o1.getGroupBuyPriceStr()).compareTo(Double.valueOf(o2.getGroupBuyPriceStr()));
                    } else {
                        return Double.valueOf(o1.getPriceStr()).compareTo(Double.valueOf(o2.getPriceStr()));
                    }
                });
                break;
            case PRICE_DESC:
                products.sort((o1, o2) -> {
                    if (isGroupBuy) {
                        return Double.valueOf(o2.getGroupBuyPriceStr()).compareTo(Double.valueOf(o1.getGroupBuyPriceStr()));
                    } else {
                        return Double.valueOf(o2.getPriceStr()).compareTo(Double.valueOf(o1.getPriceStr()));
                    }
                });
                break;
            case SALE_QUANTITY:
                products.sort(Comparator.comparing(SearchProduct::getSalesVolume).reversed());
                break;
            case NEW:
                products.sort((o1, o2) -> Long.valueOf(o2.getFirstOnShelfTime().getTime()).compareTo(Long.valueOf(o1.getFirstOnShelfTime().getTime())));
                break;
            default:
        }
        return products;
    }

    /**
     * 按照商品基础信息进行排序
     *
     * @param items
     * @param orderBy
     * @param isGroupBuy 是否为一起拼，一起拼时价格排序使用拼团价
     * @return
     * @author: luozhuo
     * @date: 2018年7月26日 上午11:43:28
     */
    public List<SearchItem> sortByBaseInfo(List<SearchItem> items, SearchOrderByEnum orderBy, Boolean isGroupBuy) {
        if (items == null || items.size() == 0) {
            return items;
        }

        List<SearchItem> copyOfList = new ArrayList<SearchItem>(items.size());
        copyOfList.addAll(items);

        switch (orderBy) {
            case PRICE_ASC:
                copyOfList.sort((i1, i2) -> {
                    Long price1 = 0L, price2 = 0L;
                    SearchProductInfo productInfo1 = productCache.getSearchProductInfo(i1.getProductId().longValue());
                    if (productInfo1 != null) {
                        if (isGroupBuy) {
                            price1 = productInfo1.getGroupPrice();
                        } else {
                            price1 = productInfo1.getPrice();
                        }
                    }
                    SearchProductInfo productInfo2 = productCache.getSearchProductInfo(i2.getProductId().longValue());
                    if (productInfo2 != null) {
                        if (isGroupBuy) {
                            price2 = productInfo2.getGroupPrice();
                        } else {
                            price2 = productInfo2.getPrice();
                        }
                    }

                    return price1.compareTo(price2);
                });
                break;
            case PRICE_DESC:
                copyOfList.sort((i1, i2) -> {
                    Long price1 = 0L, price2 = 0L;

                    SearchProductInfo productInfo1 = productCache.getSearchProductInfo(i1.getProductId().longValue());
                    if (productInfo1 != null) {
                        if (isGroupBuy) {
                            price1 = productInfo1.getGroupPrice();
                        } else {
                            price1 = productInfo1.getPrice();
                        }
                    }
                    SearchProductInfo productInfo2 = productCache.getSearchProductInfo(i2.getProductId().longValue());
                    if (productInfo2 != null) {
                        if (isGroupBuy) {
                            price2 = productInfo2.getGroupPrice();
                        } else {
                            price2 = productInfo2.getPrice();
                        }
                    }

                    return price2.compareTo(price1);
                });
                break;
            case SALE_QUANTITY:
                copyOfList.sort((i1, i2) -> {
                    Long salesVolume1 = 0L, saleVolume2 = 0L;

                    SearchProductInfo productInfo1 = productCache.getSearchProductInfo(i1.getProductId().longValue());
                    if (productInfo1 != null) {
                        salesVolume1 = productInfo1.getSalesVolume();
                    }
                    SearchProductInfo productInfo2 = productCache.getSearchProductInfo(i2.getProductId().longValue());
                    if (productInfo2 != null) {
                        saleVolume2 = productInfo2.getSalesVolume();
                    }

                    return saleVolume2.compareTo(salesVolume1);
                });
                break;
            case NEW:
                copyOfList.sort((i1, i2) -> {
                    Long onSelfTime1 = 0L, onSelfTime2 = 0L;

                    SearchProductInfo productInfo1 = productCache.getSearchProductInfo(i1.getProductId().longValue());
                    if (productInfo1 != null) {
                        onSelfTime1 = productInfo1.getFirstOnShelfTime().getTime();
                    }
                    SearchProductInfo productInfo2 = productCache.getSearchProductInfo(i2.getProductId().longValue());
                    if (productInfo2 != null) {
                        onSelfTime2 = productInfo2.getFirstOnShelfTime().getTime();
                    }

                    return onSelfTime2.compareTo(onSelfTime1);
                });
                break;
            default:
        }

        return copyOfList;
    }

    @Deprecated
    public void detailBlockDataProdcut(List<Template> templates, CommonRequestParam commonParam) {
        List<SearchProduct> products = new ArrayList<>();
        for (Template template : templates) {
            List<TemplateData> datas = template.getData();
            for (TemplateData data : datas) {
                if (!(data instanceof SearchProduct)) {
                    continue;
                }
                products.add((SearchProduct) data);
            }
        }

        // detailProduct(products, commonParam);
        Map<Integer, SearchProduct> productMap = products.stream()
                .collect(Collectors.toMap(SearchProduct::getProductId, i -> i));

        Iterator<Template> iterator = templates.iterator();
        while (iterator.hasNext()) {
            Template template = iterator.next();
            List<TemplateData> datas = template.getData();

            boolean detailFailed = false;
            for (TemplateData data : datas) {
                if (!(data instanceof SearchProduct)) {
                    continue;
                }

                SearchProduct product = productMap.get(((SearchProduct) data).getProductId());
                if (Strings.isNullOrEmpty(product.getTitle())) {
                    // 商品信息填充失败
                    detailFailed = true;
                    break;
                }
            }

            // 删除掉信息填充失败的模板
            if (detailFailed) {
                iterator.remove();
            }
        }

    }

    /**
     * 是否要显示特权金
     */
    private boolean isShowPrivilege(ShowPrivilegeLogoResultDto isUserHasPrivilege, SearchProductInfo product) {

        boolean isShowPrivilege = false;
        try {
            if (isUserHasPrivilege == null || product == null
                    || org.springframework.util.StringUtils.isEmpty(isUserHasPrivilege.getUserType())
                    || org.springframework.util.StringUtils.isEmpty(isUserHasPrivilege.getPrivilegeLogoShowType())
                    || org.springframework.util.StringUtils.isEmpty(product.getOldUserPrivilege())
                    || org.springframework.util.StringUtils.isEmpty(product.getNewUserPrivilege())) {
                return isShowPrivilege;
            }
            // 类型中含有新客类型并且用户身份是新客，privilegeLogoShowType赋值为1返回,同理，通用特权金1，老客2，不展示
            // 0。无需关注userType
            if (isUserHasPrivilege.getPrivilegeLogoShowType() == 1 && product.getNewUserPrivilege() == 1) {
                // 新客特权金，通用特权金逻辑相同
                isShowPrivilege = true;
            }
            if (isUserHasPrivilege.getPrivilegeLogoShowType() == 2 && product.getOldUserPrivilege() == 1) {
                // 老客特权金
                isShowPrivilege = true;
            }
        } catch (Exception e) {
            logger.error("[严重异常]是否显示特权金标识异常，ShowPrivilegeLogoResultDto={}，[SearchProductInfo={}]",
                    JSON.toJSONString(isUserHasPrivilege), JSONObject.toJSONString(product));
        }
        return isShowPrivilege;
    }

    /**
     * 判断是否为新品
     *
     * @param productInfo
     * @return
     */
    private boolean isNewProduct(SearchProductInfo productInfo) {
        /* 判断是否是新品，7天以内 */
        Date firstOnshelfDate = productInfo.getFirstOnShelfTime();
        int days = 0;
        if (firstOnshelfDate != null) {
            days = (int) ((System.currentTimeMillis() - firstOnshelfDate.getTime()) / (1000 * 3600));
        }
        if (days <= 72) {
            return true;
        } else {
            return false;
        }
    }


    /**
     * 设置商品是否展示视频标识以及视频标识Icon图片地址
     * @param item
     * @param videoSwitch
     * @param imageUrl
     */
    private void setProductVideoIconInfo(SearchProduct item, String videoSwitch, String imageUrl) {

        //通过商品id获取含有轮播图视频标识
        Boolean hasVideoFlag = algorithmRedisDataCache.getProductVideoFlagByPid(Long.parseLong(item.getProductId().toString()));
        if(CommonConstant.IS_SHOW_VIDEO_FLAG_YES.equals(videoSwitch) && hasVideoFlag){
            item.setVideoStatus(CommonConstant.IS_SHOW_VIDEO_FLAG_YES);
            if(imageUrl != null){
                item.setVideoIcon(imageUrl);
            }
        }
    }

    /**
     * @description 设置商品造物信息,包括造物价文案、造物角标
     * @project 【鸿源商品底盘V1.0.0-新增支持必要造物商品】
     * @author 张志敏
     * @date 2022-02-16
     */
    private void setCreationInfos(SearchProduct item, Map<Long, MaterialElementBaseDTO> MaterialElements, UISearchRequest request) {
        //封装造物价文案
        setCreationPaperwork(item, MaterialElements.get(CommonConstant.CREATION_PAPERWORK_ICON_ID));
        //封装造物角标
        setCreationShowIcon(item,MaterialElements.get(CommonConstant.CREATION_SHOW_ICON_ID),request);
        //封装造物标识
        item.setIsCreation(CommonConstant.IS_CREATOR_YES);
    }




    /**
     * @description 设置商品造物文案，造物价文案，取CMS配置（单行文本素材id：11560207），托底“优惠价”
     * @project 【鸿源商品底盘V1.0.0-新增支持必要造物商品】
     * @author 张志敏
     * @date 2022-02-16
     */
    private void setCreationPaperwork(SearchProduct item, MaterialElementBaseDTO materialElementBaseDTO) {
        if(null == materialElementBaseDTO  || null == materialElementBaseDTO.getValue() || "".equals(materialElementBaseDTO.getValue())){
            //如果获取不到素材，则赋值托底“优惠价”
            item.setCreationPaperwork(CommonConstant.CREATION_PAPERWORK_EXCHANGE);
            return;
        }
        item.setCreationPaperwork((String)(materialElementBaseDTO.getValue()));
    }


    /**
     * @description 设置商品造物角标,取CMS配置（图片素材id：11560606），造物角标优先级高于新品角标（目前为最高）,取不到不展示造物角标（顺次展示此位置其他角标）
     * @project 【鸿源商品底盘V1.0.0-新增支持必要造物商品】
     * @author 张志敏
     * @date 2022-02-16
     */
    private void setCreationShowIcon(SearchProduct item, MaterialElementBaseDTO materialElementBaseDTO, UISearchRequest request) {
        //获取造物角标素材
        ImageDto showIcon =(ImageDto) (materialElementBaseDTO.getValue());
        //判断造物角标素材为空
        if(showIcon == null || (StringUtils.isEmpty(showIcon.getWebpImageUrl()) && StringUtils.isEmpty(showIcon.getOriginUrl())) ){
            return;
        }
        String webpImageUrl = showIcon.getWebpImageUrl();//webp图片
        //如果造物角标，则将isShowIcon字段设置成8，标识展示造物角标
        item.setIsShowIcon(CommonConstant.SHOW_CREATOR_ICON);
        //判断request中是否支持webp的字段存在且值为1 && 素材中的webp图片Url不为空,同时满足，则赋值该webp图片给造物角标 并设置展示商品角标为8：造物角标
        if(request.getIsSupportWebP() != null && request.getIsSupportWebP()==CommonConstant.IS_SUPPORT_WEBP_YES && StringUtils.isEmpty(webpImageUrl)){
            item.setCreationShowIcon(webpImageUrl);
            return;
        }
        item.setCreationShowIcon(showIcon.getOriginUrl());
    }


    /**
     * @description 重置SKU信息时，计算SKU造物价，如果介于0和price原件之间，则赋值给商品的SKU造物价和SPU造物价属性，并且设置商品造物信息是否完整值为1：完整
     * @project 【鸿源商品底盘V1.0.0-新增支持必要造物商品】
     * @author 张志敏
     * @date 2022-02-16
     */
    public void setCreationPriceStrOfSP(SearchProduct searchProduct,SuProduct suProduct){

        Long price = suProduct.getPrice();//原价
        Long platformDiscountPrice = suProduct.getPlatformDiscountPrice();//获取平台折扣价
        Long supplierDiscountPrice = suProduct.getSupplierDiscountPrice();//获取商家折扣价

        if(price!=null && platformDiscountPrice!=null && supplierDiscountPrice!= null){
            //造物价 = 原价-平台折扣价-商家折扣价
            Long creationPrice = (suProduct.getPrice() - suProduct.getPlatformDiscountPrice() - suProduct.getSupplierDiscountPrice());
            if(creationPrice > 0 && creationPrice <= price){

                BigDecimal creationPriceDec = new BigDecimal(creationPrice.toString());
                BigDecimal hundred = new BigDecimal("100");
                String  creationPriceStr = creationPriceDec.divide(hundred).stripTrailingZeros().toPlainString();
                //creationPrice = creationPrice/100;//上述三个价格的单位都是分，需要转成元，所以除以100
                suProduct.setCreatorPrice(creationPriceStr);//设置SKU造物价
                searchProduct.setCreationPriceStr(creationPriceStr);//设置SPU造物价
            }
        }

    }



}
