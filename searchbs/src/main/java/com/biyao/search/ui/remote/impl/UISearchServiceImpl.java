package com.biyao.search.ui.remote.impl;

import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.rpc.protocol.rest.support.ContentType;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.biyao.gba.dubbo.client.ladder.compute.service.ILadderComputeService;
import com.biyao.nova.novaservice.service.RecommendPageDubboService;
import com.biyao.search.as.service.ASMainSearchService;
import com.biyao.search.as.service.model.request.SearchRequest;
import com.biyao.search.as.service.model.request.TopicProductSearchRequest;
import com.biyao.search.as.service.model.request.TopicSearchRequest;
import com.biyao.search.as.service.model.response.ASProdcutSearchResult;
import com.biyao.search.as.service.model.response.ProductSearchBlock;
import com.biyao.search.bs.service.model.response.DeriveProductMatchResult;
import com.biyao.search.common.constant.SearchLimit;
import com.biyao.search.common.constant.SearchStatus;
import com.biyao.search.common.enums.PlatformEnum;
import com.biyao.search.common.enums.SearchOrderByEnum;
import com.biyao.search.common.model.*;
import com.biyao.search.facet.sdk.bean.Facet;
import com.biyao.search.facet.sdk.constants.FacetConstants;
import com.biyao.search.ui.cache.ProductCache;
import com.biyao.search.ui.cache.RedisDataCache;
import com.biyao.search.ui.constant.ColorCodeConsts;
import com.biyao.search.ui.constant.CommonConstant;
import com.biyao.search.ui.constant.PageSourceEnum;
import com.biyao.search.ui.constant.RedisKeyConsts;
import com.biyao.search.ui.enums.ActivityEnum;
import com.biyao.search.ui.exp.UIExperimentSpace;
import com.biyao.search.ui.manager.FacetManager;
import com.biyao.search.ui.model.SearchProductInfo;
import com.biyao.search.ui.model.VModel;
import com.biyao.search.ui.remote.UISearchService;
import com.biyao.search.ui.remote.common.*;
import com.biyao.search.ui.remote.request.UISearchPageRequest;
import com.biyao.search.ui.remote.request.UISearchRequest;
import com.biyao.search.ui.remote.response.*;
import com.biyao.search.ui.rest.impl.cache.CommonCache;
import com.biyao.search.ui.rest.response.SearchOrderByConsts;
import com.biyao.search.ui.rpc.Couponquery2cRpcService;
import com.biyao.search.ui.service.FillRpcInfoService;
import com.biyao.search.ui.service.HomeFeedDataService;
import com.biyao.search.ui.service.NewGuyBenefitService;
import com.biyao.search.ui.service.VModelHandler;
import com.biyao.search.ui.util.*;
import com.biyao.upc.dubbo.client.business.toc.IBusinessTocDubboService;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

import static com.biyao.search.ui.constant.CommonConstant.ROUTE_TYPE_NEW_PAGE;

/**
 * @author
 */
@Service("uiNewSearchService")
@Path("/")
@Produces({ContentType.APPLICATION_JSON_UTF_8})
public class UISearchServiceImpl implements UISearchService {

    @Autowired
    ASMainSearchService asMainService;

    @Autowired
    ProductDetailService productDetailService;

    @Autowired
    PageAndCacheService pageAndCacheService;

    @Autowired
    BeanConvertService beanConvertService;

    @Autowired
    private UIExperimentSpace experimentSpace;

    @Autowired
    private CommonCache commonCache;

    @Autowired
    private RecommendPageDubboService recommendPageDubboService;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    ILadderComputeService ladderComputeService;

    @Autowired
    IBusinessTocDubboService iBusinessTocDubboService;

    @Autowired
    NewGuyBenefitService newGuyBenefitService;

    @Autowired
    HomeFeedDataService homeFeedDataService;

    @Autowired
    ProductCache productCache;

    @Autowired
    UISearchRequestHandler uiSearchRequestHandler;

    @Autowired
    RedisDataCache redisDataCache;

    @Autowired
    FacetManager facetManager;

    @Autowired
    VModelHandler vModelHandler;

    @Autowired
    DeriveProductService deriveProductService;

    @Autowired
    CommonService commonService;
    @Autowired
    private FillRpcInfoService fillRpcInfoService;

    @Autowired
    Couponquery2cRpcService couponquery2cRpcService;

    private DecimalFormat df = new DecimalFormat("#.000000");


    private Logger logger = LoggerFactory.getLogger(getClass());
    private Logger requestLogger = LoggerFactory.getLogger(CommonConstant.REQUEST_LOG_NAME);

    /**
     * 一天的毫秒数
     */
    private Long oneDayMillisecond = 86400000L;

    /**
     * 新品限制天数（上架时间小于此天数为新品）
     */
    private int newProductDays = 3;


    private static final int PRODUCT_ID_TOP_NUM = 50;


    /**
     * 搜索主接口
     *
     * @param request
     * @return
     * @author: luozhuo
     * @date: 2018年7月26日 上午11:06:47
     * @see com.biyao.search.ui.remote.UISearchService#search(com.biyao.search.ui.remote.request.UISearchRequest)
     */
    @Override
    @GET
    @Path("newsearch")
    @BProfiler(key = "com.biyao.search.ui.remote.impl.search", monitorType = {MonitorType.TP, MonitorType.HEARTBEAT,
            MonitorType.FUNCTION_ERROR})
    public HttpResult2<UISearchResponse> search(@BeanParam UISearchRequest request) {
        try {
            UISearchResponse response = new UISearchResponse();
            // 参数校验
            if (request == null || !request.checkParameter()) {
                logger.error("[严重异常]参数校验不通过，request={}", JSONObject.toJSONString(request));
                response = buildFullbackResponse(request);
                return new HttpResult2<>(response);
            }
            if (ActivityEnum.SUPPLIER_SEARCH.equals(ActivityEnum.judgeActivityType(request.getToActivity()))
                    && StringUtils.isEmpty(request.getSupplierId())) {
                logger.error("[严重异常][参数校验不通过]搜本店时supplierId必传，request={}", JSONObject.toJSONString(request));
                Status error = new Status(400001, "参数校验不通过,搜本店时supplierId必传");
                return new HttpResult2<>(error);
            }
            requestLogger.info("原始请求参数：" + JSONObject.toJSONString(request));
            // 参数转换和处理
            request.preHandleParam();
            if (redisDataCache.isBlockQuery(request.getQuery())) {
                response = buildFullbackResponse(request);
                return new HttpResult2<>(response);
            }
            request = experimentSpace.divert(request);
            uiSearchRequestHandler.handleRequest(request);

            requestLogger.info("处理后请求参数：" + JSONObject.toJSONString(request));

            //打印search-detail日志（uuid白名单用户）- searchui请求参数日志
            if (redisDataCache.isSearchDetailLogUuid(request.getUuid())) {
                DcLogUtil.printSearchDetailLog("search_request", request.getUuid(), request.getSid(), JSON.toJSONString(request));
            }

            StringBuilder sb = new StringBuilder(10240);

            sb.append("sid=");
            sb.append(request.getSid());
            sb.append("\tip=");
            sb.append(request.getIp());
            sb.append("\tq=");
            sb.append(request.getQuery());
            // 增加搜索建议词参数
            sb.append("\tsuginput=").append(Strings.isNullOrEmpty(request.getSuggestionInput()) ? "" : request.getSuggestionInput());
            sb.append("\tsugindex=").append(request.getSuggestionIndex() == null ? "" : request.getSuggestionIndex());
            sb.append("\tsuguniqid=").append(request.getSuggestionUniqueId() == null ? "" : request.getSuggestionUniqueId());
            // 增厚搜索词来源 来自热搜还是来自历史搜索词
            sb.append("\tqf=").append(StringUtils.isEmpty(request.getQueryFrom()) ? "" : request.getQueryFrom());
            sb.append("\tfct=");
            sb.append(Strings.isNullOrEmpty(request.getFacetStr()) ? "" : request.getFacetStr());
            sb.append("\todb=");
            sb.append(Strings.isNullOrEmpty(request.getOrderByStr()) ? "" : request.getOrderByStr());
            sb.append("\tuu=");
            sb.append(request.getUuid());
            sb.append("\tpf=");
            sb.append(request.getPlatform().getName());
            sb.append("\tu=");
            sb.append(request.getUid() == null ? "" : request.getUid() + "");
            sb.append("\tav=");
            sb.append(Strings.isNullOrEmpty(request.getAppVersion()) ? "" : request.getAppVersion());
            sb.append("\td=");
            sb.append(Strings.isNullOrEmpty(request.getDevice()) ? "" : request.getDevice());
            sb.append("\treqt=");
            sb.append(System.currentTimeMillis());
            sb.append("\texpId=");
            sb.append(request.getExpIds().toString());
            sb.append("\taidBase=");
            sb.append(request.getAid());
            sb.append("\tact=");
            sb.append(request.getToActivity());
            sb.append("\tstp=");
            sb.append(request.getStp());
            sb.append("\tctp=");
            sb.append(Strings.isNullOrEmpty(request.getCtp()) ? "" : request.getCtp());
            sb.append("\tifilter=");
            sb.append(Strings.isNullOrEmpty(request.getImageFilterParam()) ? "" : request.getImageFilterParam());

            String topicIdLog = "\ttopicId=";

            //判断如果是津贴页，则从CMS获取津贴文案 以及 从卡券系统获取用户总津贴数
            if(ActivityEnum.ALLOWANCE_DEDUCTION.getCode().equals(request.getToActivity()) && request.getUid() != null){

                String allowancePre = CmsUtil.getMaterialInput(CommonConstant.ALLOWANCE_LABEL_ID);
                if(null != allowancePre && allowancePre.contains(CommonConstant.ALLOWANCE_LABEL_YONGHUKEDI_CONS)) {
                    //查询接口，算出总津贴数
                    BigDecimal userTotalAllowance = couponquery2cRpcService.queryUserTotalAllowance(Long.parseLong(String.valueOf(request.getUid())));
                    //均取值小数点后两位，前端展示时一律抹去小数点后面的0（例如19.90展示为19.9，20.00展示为20）
                    request.setUserTotalAllowanceAmt(dealWithPrice(userTotalAllowance));
                }
            }

            CommonRequestParam commonParam = new CommonRequestParam();
            commonParam.setPlatform(request.getPlatform());
            commonParam.setSid(request.getSid());
            commonParam.setUid(request.getUid());
            commonParam.setUuid(request.getUuid());

            List<BlockData> blockDatas = new ArrayList<BlockData>();
            TopPanel topPanel = new TopPanel();

            if (request.getToTopicPage() == 1) {
                try {
                    // 特殊请求，跳转到新页面
                    topicIdLog = topicIdLog + request.getTopicId();
                    generateTopicResponse(request, commonParam, sb, blockDatas, topPanel, response);
                } catch (Exception e) {
                    logger.error("[严重异常]TOPIC搜索报错: [sid={}, uuid={}]", request.getSid(), request.getUuid(), e);
                    throw e;
                }
            } else if (request.getToSupplierListPage() == 1) {
                try {
                    // 跳转到商家列表页
                    generateSupplierListResponse(request, commonParam, sb, blockDatas, topPanel, response);
                } catch (Exception e) {
                    logger.error("[严重异常]商家列表搜索报错: [sid={}, uuid={}]", request.getSid(), request.getUuid(), e);
                    throw e;
                }
            } else if (ActivityEnum.GROUP_BUY.equals(ActivityEnum.judgeActivityType(request.getToActivity()))) {
                try {
                    // 跳转到一起拼
                    generateGroupBuyResponse(request, commonParam, sb, blockDatas, topPanel, response);
                } catch (Exception e) {
                    logger.error("[严重异常]一起拼搜索报错: [sid={}, uuid={}]", request.getSid(), request.getUuid(), e);
                    throw e;
                }
            } else if (isTagQuery(request.getQuery())
                    && ActivityEnum.NO_ACTIVITY.equals(ActivityEnum.judgeActivityType(request.getToActivity()))) {
                try {
                    // 白名单搜索
                    generateTagResponse(request, commonParam, sb, blockDatas, topPanel, response);
                } catch (Exception e) {
                    logger.error("[严重异常]白名单搜索报错: [sid={}, uuid={}]", request.getSid(), request.getUuid(), e);
                    throw e;
                }
            } else {
                try {
                    // 主搜处理逻辑
                    generateNewResponse(request, commonParam, sb, blockDatas, topPanel, response);
                    Integer miniAppVersionNum = getMiniAppVersionNum(request.getMiniappVersion());
                    boolean isAfterDayunheVersion12 = AppNumVersionUtil.isAfterDayunheVersion1_2(request.getPlatform().getName(), request.getAppVersionNum(), miniAppVersionNum);
                    if (isAfterDayunheVersion12 && ActivityEnum.NO_ACTIVITY.equals(ActivityEnum.judgeActivityType(request.getToActivity()))) {
                        List<VModel> vmodelList = vModelHandler.getVmodelList(request);
                        response.setVmodelList(vmodelList);
                    }
                } catch (Exception e) {
                    logger.error("[严重异常]主搜逻辑报错: [sid={}, uuid={}]", request.getSid(), request.getUuid(), e);
                    throw e;
                }
            }
            try {
                sb.append(topicIdLog);

                response.setQuery(request.getOriginalQuery());
                response.setSid(request.getSid());
                response.setOrderBy(request.getOrderBy().getCode());
                response.setBlockData(blockDatas);
                response.setTopPanel(topPanel);
                // 配置操作栏开关
                if (response.getTopPanel().getOnOff() == 1) {
                    response.getTopPanel().setOnOff(getTopPanelOnOff());
                }
                response.setBottomHanging(getBottomHanging());
                if (blockDatas.size() == 0) {
                    response.setIsEmpty("1");
                } else if (blockDatas.get(0).getTemplates().size() == 0) {
                    response.setIsEmpty("1");
                }
                DcLogUtil.printUIResponse(sb.toString());
            } catch (Exception e) {
                logger.error("[严重异常]封装response报错: [sid={}, uuid={}]", request.getSid(), request.getUuid(), e);
                throw e;
            }
            //fillRpcInfoService.fillProductInfo(blockDatas);//封装展示视频标识旧逻辑弃用
            response.setBlockData(blockDatas);
            //打印search-detail日志（uuid白名单用户）- searchui返回对象日志
            if (redisDataCache.isSearchDetailLogUuid(request.getUuid())) {
                DcLogUtil.printSearchDetailLog("search-response", request.getUuid(), response.getSid(), JSON.toJSONString(response));
            }
            return new HttpResult2<UISearchResponse>(response);
        } catch (Exception e) {
            logger.error("[严重异常]搜索发生未知错误:[sid={}, uuid={}]", request.getSid(), request.getUuid(), e);
            UISearchResponse response = buildFullbackResponse(request);
            return new HttpResult2<>(response);
        }
    }

    /**
     * 处理价格：均取值小数点后两位，前端展示时一律抹去小数点后面的0（例如19.90展示为19.9，20.00展示为20）
     * @param userTotalAllowance
     */
    private BigDecimal dealWithPrice(BigDecimal userTotalAllowance) {
        BigDecimal decimal = new BigDecimal(BigDecimal.valueOf(Double.parseDouble(userTotalAllowance.toString())).stripTrailingZeros().toPlainString());
        return decimal;


    }

    /**
     * @param miniappVersion
     * @return
     */
    private Integer getMiniAppVersionNum(String miniappVersion) {
        Integer result = 0;
        if (StringUtils.isBlank(miniappVersion)) {
            return result;
        }
        try {
            StringBuffer sb = new StringBuffer();
            List<String> list = Arrays.asList(miniappVersion.replace('.', ',').split(","));
            list.forEach(sb::append);

            result = Integer.parseInt(sb.toString());
        } catch (Exception e) {
            logger.error("[严重异常]小程序版本号转换异常", e);
        }
        return result;
    }

    /**
     * 生成商家列表页结果数据
     *
     * @param request
     * @param commonParam
     * @param sb
     * @param blockDatas
     * @param topPanel
     * @param response
     */
    private void generateSupplierListResponse(UISearchRequest request, CommonRequestParam commonParam, StringBuilder sb,
                                              List<BlockData> blockDatas, TopPanel topPanel, UISearchResponse response) {
        BlockData supplierListBlock = new BlockData();
        supplierListBlock.setCurQuery(request.getQuery());
        supplierListBlock.setBlockId(IdCalculateUtil.createBlockId());
        supplierListBlock.setHasMore(0);
        supplierListBlock.setPageIndex(1);
        supplierListBlock.setTitle(new ArrayList<>());

        TopicSearchRequest topicSearchParam = new TopicSearchRequest();
        topicSearchParam.setCommonParam(commonParam);
        topicSearchParam.setExpectNum(15);
        // 商家类型
        topicSearchParam.setTopicType(5);
        RPCResult<List<TopicItem>> topicRpc = null;
        try {
            topicRpc = asMainService.topicSearch(topicSearchParam);
        } catch (Exception e) {
            logger.error("[严重异常]远程调用searchas dubbo服务失败:[method=topicSearch, param={}]", JSONObject.toJSONString(topicSearchParam), e);
        }
        if (topicRpc != null && SearchStatus.OK.equals(topicRpc.getStatus())) {
            Template template = new Template();
            template.setType(CommonConstant.TemplateType.STORE);
            int position = 0;
            String aid = IdCalculateUtil.createAid(request.getAid() + IdCalculateUtil.createBlockId());
            for (TopicItem item : topicRpc.getData()) {
                // 基础信息组装
                StoreTemplateData templateData = new StoreTemplateData();
                templateData
                        .setImageUrl(item.getProductImgUrlList().size() < 1 ? "" : item.getProductImgUrlList().get(0));
                templateData.setStoreName(item.getShowQuery());
                templateData.setStoreDesc(item.getSubTitle() == null ? "" : item.getSubTitle());

                TemplateRoute route = new TemplateRoute();
                route.setQuery(item.getShowQuery());
                route.setTrackParam(URLEncoder.encode(String.format("sid=%s&bkId=%s&pos=%s", request.getSid(),
                        supplierListBlock.getBlockId(), position++)));
                route.setType(ROUTE_TYPE_NEW_PAGE);
                route.setRouteUrl(RouterUtil.getSearchNewPageRoute(request.getPlatform(),
                        "q=" + item.getShowQuery() + "&tpid=" + item.getTopicId() + "&toTP=1",
                        TrackParamUtil.generateSTP(request, "topiccard", "" + position, aid,"")));

                templateData.setRoute(route);

                // 获取商家主题的前4个商品
                List<SearchProduct> products = new ArrayList<SearchProduct>();
                TopicProductSearchRequest topicProductParam = new TopicProductSearchRequest();
                topicProductParam.setCommonParam(commonParam);
                topicProductParam.setExpectNum(20);
                topicProductParam.setQuery(request.getQuery());
                topicProductParam.setTopicId(item.getTopicId());
                RPCResult<ASProdcutSearchResult> topicPrdocutRpc = null;
                try {
                    topicPrdocutRpc = asMainService.topicProductSearch(topicProductParam);
                } catch (Exception e) {
                    logger.error("[严重异常]远程调用searchas dubbo服务失败:[method=topicProductSearch, param ={}]", JSONObject.toJSONString(topicProductParam), e);
                }
                if (SearchStatus.OK.equals(topicPrdocutRpc.getStatus())) {
                    /*
                     * 分页缓存，并取出第一页数据
                     */
                    for (ProductSearchBlock rpcItem : topicPrdocutRpc.getData().getProductBlocks()) {
                        // 获取商品基础信息
                        products = rpcItem.getItems().stream().map(i -> {
                            SearchProduct p = new SearchProduct();
                            p.setProductId(i.getProductId());
                            return p;
                        }).collect(Collectors.toList());
                        productDetailService.detailProduct(products, commonParam, supplierListBlock.getBlockId(),
                                request, "splist", CommonConstant.TemplateType.SINGLE_PRODCUT, aid);
                    }
                }

                if (products.size() < 4) {
                    continue;
                }

                templateData.setProducts(products.subList(0, 4));
                template.getData().add(templateData);
            }

            supplierListBlock.getTemplates().add(template);
            blockDatas.add(supplierListBlock);

            sb.append("\tsplist=");
            sb.append(generateTopicCardLog(supplierListBlock.getBlockId(), topicRpc.getData(), aid));
        }
    }

    /**
     * 标签搜索
     *
     * @param request
     * @param commonParam
     * @param sb
     * @param blockDatas
     * @param topPanel
     * @param response
     */
    private void generateTagResponse(UISearchRequest request, CommonRequestParam commonParam, StringBuilder sb,
                                     List<BlockData> blockDatas, TopPanel topPanel, UISearchResponse response) {
        /*
         * 搜索词匹配相关结果
         */
        RPCResult<ASProdcutSearchResult> productSearchRpcResult = getProductSearchRpcResult(request, commonParam, 500);
        List<ProductSearchBlock> rpcBlocks = new ArrayList<>();
        if (productSearchRpcResult != null && productSearchRpcResult.getStatus().equals(SearchStatus.OK)) {
            rpcBlocks = productSearchRpcResult.getData().getProductBlocks();

            // tag召回，此时只需要拿第一个完全匹配的块结果即可
            rpcBlocks = rpcBlocks.size() > 0 ? rpcBlocks.subList(0, 1) : rpcBlocks;

            /*
             * 分页缓存，并取出第一页数据
             */
            int qsMatchPos = 0; // 部分匹配结果序列，用于记录日志
            String aid = IdCalculateUtil.createAid(request.getAid() + IdCalculateUtil.createBlockId());
            for (ProductSearchBlock item : rpcBlocks) {
                String blockId = IdCalculateUtil.createBlockId();
                String matchBlock = generateMatchBlockId(request.getQuery(), item.getQuery(), qsMatchPos++);

                // 获取商品基础信息
                List<SearchProduct> products = beanConvertService.convert2SearchProduct(item.getItems(), request, CommonConstant.TemplateType.DOUBLE_PRODUCT, false);
                if (request.getOrderBy() != SearchOrderByEnum.NORMAL) {
                    productDetailService.sortByBaseInfoAfterConvert(products, request.getOrderBy(), false);
                }
                productDetailService.detailProductAfterSort(products, commonParam, blockId, request, matchBlock,
                        CommonConstant.TemplateType.DOUBLE_PRODUCT, aid);

                BlockData firstPage = pageAndCacheService.cacheAndGetFirstPageProducts(blockId, request, products,
                        commonParam, CommonConstant.TemplateType.DOUBLE_PRODUCT, item.getQuery());
                if (firstPage.getTemplates().size() <= 0) {
                    continue;
                }

                if (!request.getQuery().equals(item.getQuery())) {
                    // 非完全匹配区块展示标题
                    firstPage.setTitle(generateBlockTitleTextList(item.getQuery()));
                }
                firstPage.setCurQuery(item.getQuery());
                blockDatas.add(firstPage);
                // 记录结果日志
                sb.append("\t" + matchBlock + "=");
                sb.append(generateProductResultLog(item.getQuery(), blockId, null, products, aid));
            }

        }

        topPanel.setOnOff(0);
        if (blockDatas.size() == 0) {
            // 搜索无结果
            response.setTips(generateTips(request.getOriginalQuery()));

            /*
             * 返回托底商品
             */
            List<SearchItem> searchItems = new ArrayList<>();
            SearchFallback searchFallback = commonCache.getSearchFallback();
            if (searchFallback == null) {
                searchFallback = new SearchFallback();
            }

            List<ASProduct> asProducts = searchFallback.getProducts();
            for (int i = 0; i < asProducts.size(); i++) {
                if (searchItems.size() >= 25) {
                    // 取出25，避免发现有商品下架导致返回不足20个
                    break;
                }

                // 小程序过滤高模商品
                if ("miniapp".equals(request.getPlatform()) && asProducts.get(i).getSuId().endsWith("0")) {
                    continue;
                }

                SearchItem item = new SearchItem();
                item.setProductId(Integer.valueOf(asProducts.get(i).getSuId().substring(0, 10)));
                searchItems.add(item);
            }

            String blockId = IdCalculateUtil.createBlockId();
            List<SearchProduct> prodcuts = searchItems.stream().map(i -> {
                SearchProduct p = new SearchProduct();
                p.setProductId(i.getProductId());
                return p;
            }).collect(Collectors.toList());
            String aid = IdCalculateUtil.createAid(request.getAid() + IdCalculateUtil.createBlockId());
            productDetailService.detailProduct(prodcuts, commonParam, blockId, request, "fallback",
                    CommonConstant.TemplateType.DOUBLE_PRODUCT, aid);

            BlockData firstPage = pageAndCacheService.cacheAndGetFirstPageProducts(blockId, request, prodcuts,
                    commonParam, CommonConstant.TemplateType.DOUBLE_PRODUCT, "");
            if (firstPage.getTemplates().size() <= 0) {
                return;
            }

            firstPage.setHasMore(0); // 特殊处理

            firstPage.setCurQuery(request.getQuery());
            blockDatas.add(firstPage);

            // 记录托底数据结果日志
            sb.append("\tfallback=");
            sb.append(generateProductResultLog(request.getQuery(), blockId, null, prodcuts, aid));
        }
    }

    /**
     * 判断是否是白名单搜索
     *
     * @param query
     * @return 修改为从redis中获取
     */
    private boolean isTagQuery(String query) {
        // 判断是否是白名单词
        RPCResult<Boolean> rpcResult = null;
        try {
            rpcResult = asMainService.isTagQuery(query);
        } catch (Exception e) {
            logger.error("[严重异常]远程调用searchas dubbo服务失败:[method=isTagQuery, param ={}]", JSONObject.toJSONString(query), e);
            return false;
        }

        if (rpcResult == null || !SearchStatus.OK.equals(rpcResult.getStatus()) || rpcResult.getData() == null) {
            return false;
        }

        return rpcResult.getData();
    }

    /**
     * topic商品承载搜索结果页
     *
     * @param request
     * @param commonParam
     * @param sb
     * @param blockDatas
     * @param topPanel
     * @param response
     */
    private void generateTopicResponse(UISearchRequest request, CommonRequestParam commonParam, StringBuilder sb,
                                       List<BlockData> blockDatas, TopPanel topPanel, UISearchResponse response) {

        TopicProductSearchRequest topicProductParam = new TopicProductSearchRequest();
        topicProductParam.setCommonParam(commonParam);
        topicProductParam.setExpectNum(200);
        topicProductParam.setQuery(request.getQuery());
        topicProductParam.setTopicId(request.getTopicId());
        RPCResult<ASProdcutSearchResult> topicProducttRpc = null;
        try {
            topicProducttRpc = asMainService.topicProductSearch(topicProductParam);
        } catch (Exception e) {
            logger.error("[严重异常]远程调用searchas dubbo服务失败:[method=topicProductSearch, param ={}]", JSONObject.toJSONString(topicProductParam), e);
        }
        if (topicProducttRpc != null && SearchStatus.OK.equals(topicProducttRpc.getStatus())) {
            /*
             * 分页缓存，并取出第一页数据
             */
            String aid = IdCalculateUtil.createAid(request.getAid() + IdCalculateUtil.createBlockId());
            for (ProductSearchBlock rpcItem : topicProducttRpc.getData().getProductBlocks()) {
                String blockId = IdCalculateUtil.createBlockId();
                // 获取商品基础信息
                List<SearchProduct> prodcuts = rpcItem.getItems().stream().map(i -> {
                    SearchProduct p = new SearchProduct();
                    p.setProductId(i.getProductId());
                    return p;
                }).collect(Collectors.toList());
                productDetailService.detailProduct(prodcuts, commonParam, blockId, request, "topic",
                        CommonConstant.TemplateType.DOUBLE_PRODUCT, aid);

                BlockData firstPage = pageAndCacheService.cacheAndGetFirstPageProducts(blockId, request, prodcuts,
                        commonParam, CommonConstant.TemplateType.DOUBLE_PRODUCT, "");

                firstPage.setTitle(new ArrayList<>()); // 不展示标题

                blockDatas.add(firstPage);

                sb.append("\ttopic0=");
                sb.append(generateProductResultLog("", blockId, request.getTopicId(), prodcuts, aid));
            }
        }

    }

    /**
     * 搜索结果
     * 完全匹配(嵌套搜索建议词)  部分匹配  猜你喜欢
     *
     * @param request
     * @param commonParam
     * @param sb          dclog的日志内容
     * @param blockDatas  需要返回的数据
     * @param topPanel
     * @param response
     */
    private void generateNewResponse(UISearchRequest request, CommonRequestParam commonParam, StringBuilder sb,
                                     List<BlockData> blockDatas, TopPanel topPanel, UISearchResponse response) {



        /**
         * @description
         *           1、先调用AppNumVersionUtil#isAfterByCreationVersion判断当前版本是否支持必要造物
         *           2、判断request.getToActivity()的值为0：无活动（首页、分类页、单类目中间页（三级类目页））
         *              5：轮播图落地页搜索（推荐中间页），6：商家店铺页搜本店（店铺页），7：商家店铺页搜全站（店铺页）8：新手特权金下发成功页搜索页搜索
         *              中的其中一个值，满足上述两个条件则认为此次搜索支持必要造物商品。
         *          3、在UISearchRequest对象中设置属性isSupportCreation值为1（默认是0表示不支持必要造物商品，1表示版本和活动上都支持）；
         * @project 【鸿源商品底盘V1.0.0-新增支持必要造物商品】
         * @author 张志敏
         * @date 2022-02-16
         */

        if(AppNumVersionUtil.isAfterByCreationVersion(request)){
            request.setIsSupportCreation(CommonConstant.IS_SUPPORT_CREATOR_FLAG_2);
            if((ActivityEnum.NO_ACTIVITY.getCode().equals(request.getToActivity()) ||
                    ActivityEnum.RECOMMEND_SEARCH.getCode().equals(request.getToActivity()) ||
                    ActivityEnum.SUPPLIER_SEARCH.getCode().equals(request.getToActivity()) ||
                    ActivityEnum.SUPPLIER_ALL_SEARCH.getCode().equals(request.getToActivity()) ||
                    ActivityEnum.FRESH_CUST_PRIVILEGE_SEARCH.getCode().equals(request.getToActivity()))){
                request.setIsSupportCreation(CommonConstant.IS_SUPPORT_CREATOR_YES);
            }
        }



        // 根据搜索词获取到完全匹配、部分匹配(即相关搜索) 去除facet
        RPCResult<ASProdcutSearchResult> productSearchRpcResult = getProductSearchRpcResultNew(request, commonParam,
                SearchLimit.MAX_HIT_COUNT, request.getToActivity());
        List<ProductSearchBlock> rpcBlocks = new ArrayList<>();
        List<Long> productIds4BuildFacets = new ArrayList<>();
        List<Long> allProductIds = new ArrayList<>();
        boolean isOnlyDeriveProduct = false;
        boolean buy2returnFlag = ActivityEnum.BUY2_RETURN_ALLOWANCE.equals(ActivityEnum.judgeActivityType(request.getToActivity()));
        boolean allowanceDeductionFlag = ActivityEnum.ALLOWANCE_DEDUCTION.equals(ActivityEnum.judgeActivityType(request.getToActivity()));
        // 根据搜索词检索衍生商品
        // 抽到外面
        List<DeriveProductMatchResult> deriveProductMatchList = new ArrayList<>();
        if (AppNumVersionUtil.isAfterDaYunHeVersion1_4(request)
                && ActivityEnum.NO_ACTIVITY.equals(ActivityEnum.judgeActivityType(request.getToActivity()))) {
            if (commonService.checkFlowLimit(request)) {
                deriveProductMatchList = deriveProductService.match(request.getQuery(), commonParam, 0);
            }
        }
        try {
            if (productSearchRpcResult != null && productSearchRpcResult.getStatus().equals(SearchStatus.OK)) {
                rpcBlocks = productSearchRpcResult.getData().getProductBlocks();
                // 分页缓存，并取出第一页数据
                // 部分匹配结果序列，用于记录日志
                int qsMatchPos = 0;
                boolean isShowPartMatchTitle = false;
                for (ProductSearchBlock item : rpcBlocks) {
                    // 生成追踪标识aid
                    String aid = IdCalculateUtil.createAid(request.getAid() + IdCalculateUtil.createBlockId());
                    // 前端传了排序选项，则此处需要重新排序
                    // 生成区块唯一标识
                    String blockId = IdCalculateUtil.createBlockId();
                    String matchBlock = generateMatchBlockId(request.getQuery(), item.getQuery(), qsMatchPos++);
                    String templateType = "";
                    //3表示买二返一活动，使用双排模版
                    if (buy2returnFlag || allowanceDeductionFlag) {
                        templateType = CommonConstant.TemplateType.DOUBLE_PRODUCT;
                    } else {
                        templateType = CommonConstant.TemplateType.SINGLE_PRODCUT;
                    }
                    List<SearchProduct> products = beanConvertService.convert2SearchProduct(item.getItems(), request, templateType, false);
                    if (request.getOrderBy() != SearchOrderByEnum.NORMAL) {
                        productDetailService.sortByBaseInfoAfterConvert(products, request.getOrderBy(), false);
                    }
                    try {
                        // 获取商品基础信息
                        productDetailService.detailProductAfterSort(products, commonParam, blockId, request, matchBlock,
                                templateType, aid);
                    } catch (Exception e) {
                        logger.error("[严重异常]填充商品模板信息异常:[sid={}, uuid={}]", request.getSid(), request.getUuid(), e);
                        throw e;
                    }
                    // 2019-09-06 搜索V1.5 增加图片筛选项。(完全匹配、部分匹配同时存在时,只有完全匹配项增加图片筛选项)
                    // 1. 同时有完全匹配搜索结果和部分匹配搜索结果时，只聚合完全匹配搜索结果的筛选项 即：rpcBlocks.size() > 1 && item.getQuery().equals(request.getQuery())
                    // 2. 只有完全匹配或者只有部分匹配时，需要聚合搜索结果的筛选项 即rpcBlocks.size()==1
                    boolean shouldDealImageFilter = (rpcBlocks.size() > 1 && item.getQuery().equals(request.getQuery())) || rpcBlocks.size() == 1;
                    if (shouldDealImageFilter) {
                        dealImageFilter(request, response, products);
                        productIds4BuildFacets = getFacetProductIdList(products);
                    }
                    //2019-10-23 搜索V1.7 根据传入facet过滤商品
                    filterProducts(products, request.getFacets());
                    products.forEach(product->{allProductIds.add(product.getProductId().longValue());});

                    if (request.getOrderBy().getDesc().equals(SearchOrderByEnum.NEW.getDesc())) {
                        //用户点击上新排序
                        updateUuidNewProductMap(products, request.getUuid());
                    } else {
                        //用户未点击上新排序
                        updateIsShowRedDot(products, topPanel, request.getUuid());
                    }
                    //打印search-detail日志（uuid白名单用户）- searchui返回对象日志
                    if (redisDataCache.isSearchDetailLogUuid(request.getUuid())) {
                        List<Integer> pidList = products.stream().map(SearchProduct::getProductId).collect(Collectors.toList());
                        JSONObject rpcBlock = new JSONObject();
                        rpcBlock.put("block", matchBlock);
                        rpcBlock.put("pids", pidList);
                        DcLogUtil.printSearchDetailLog("search-resFilledProductInfo", request.getUuid(), request.getSid(), JSON.toJSONString(rpcBlock));
                    }
                    if (request.getFacets().size() == 0 && StringUtils.isBlank(request.getImageFilterParam()) && deriveProductMatchList != null && deriveProductMatchList.size() > 0) {
                        if (request.getOrderBy() == SearchOrderByEnum.NORMAL) {
                            // 通用排序
                            if (item.getQuery().equals(request.getQuery())) {
                                for (DeriveProductMatchResult deriveProductMatchResult : deriveProductMatchList) {
                                    if (deriveProductMatchResult.getQuery().equals(request.getQuery())) {
                                        products = deriveProductService.insertDeriveProduct(products, deriveProductMatchResult, blockId, request, commonParam, aid);
                                    }
                                }
                            } else {
                                for (DeriveProductMatchResult deriveProductMatchResult : deriveProductMatchList) {
                                    if (!deriveProductMatchResult.getQuery().equals(request.getQuery())) {
                                        products = deriveProductService.insertDeriveProduct(products, deriveProductMatchResult, blockId, request, commonParam, aid);
                                    }
                                }
                            }
                            // end of 通用排序
                        } else {
                            // 非通用排序
                            if (item.getQuery().equals(request.getQuery())) {
                                for (DeriveProductMatchResult deriveProductMatchResult : deriveProductMatchList) {
                                    if (deriveProductMatchResult.getQuery().equals(request.getQuery())) {
                                        products = deriveProductService.insertAndSortDeriveProduct(products, deriveProductMatchResult, blockId, request, false, commonParam, aid);
                                    }
                                }
                            } else {
                                for (DeriveProductMatchResult deriveProductMatchResult : deriveProductMatchList) {
                                    if (!deriveProductMatchResult.getQuery().equals(request.getQuery())) {
                                        products = deriveProductService.insertAndSortDeriveProduct(products, deriveProductMatchResult, blockId, request, false, commonParam, aid);
                                    }
                                }
                            }
                            // end of 非用用排序
                        }
                    }
                    try {
                        // 对结果进行分页缓存并取出第一页结果
                        BlockData firstPage = pageAndCacheService.cacheAndGetFirstPageProducts(blockId, request, products,
                                commonParam, templateType, item.getQuery());
                        if (firstPage.getTemplates().size() <= 0) {
                            continue;
                        }
                        if (request.getQuery().equals(item.getQuery())) {
                            isShowPartMatchTitle = true;
                        }
                        // 非完全匹配区块展示标题(有完全匹配区块才展示)
                        if (!request.getQuery().equals(item.getQuery()) && isShowPartMatchTitle) {
                            List<SearchTitle> title = generateBlockTitleTextList(request.getQuery());
                            firstPage.setTitle(title);
                            //展示一次过后不再展示
                            isShowPartMatchTitle = false;
                        }

                        firstPage.setCurQuery(item.getQuery());
                        blockDatas.add(firstPage);
                    } catch (Exception e) {
                        logger.error("[严重异常]缓存并返回第一页数据异常:[sid={}, uuid={}]", request.getSid(), request.getUuid(), e);
                        throw e;
                    }
                    // 记录结果日志
                    sb.append("\t" + matchBlock + "=");
                    sb.append(generateProductResultLog(item.getQuery(), blockId, null, products, aid));
                    //买二返一页+津贴页只返回完全匹配商品
                    if ((buy2returnFlag || allowanceDeductionFlag) && "qmatch".equals(matchBlock)) {
                        break;
                    }
                }
                //普通商品无匹配结果且衍生商品有匹配结果的处理
                if (rpcBlocks.size() == 0 && deriveProductMatchList.size() > 0 && request.getFacets().size() == 0 && StringUtils.isBlank(request.getImageFilterParam())) {
                    // 生成追踪标识aid
                    String aid = IdCalculateUtil.createAid(request.getAid() + IdCalculateUtil.createBlockId());
                    // 生成区块唯一标识
                    String blockId = IdCalculateUtil.createBlockId();

                    List<SearchProduct> products = new ArrayList<>();
                    if (request.getFacets().size() == 0 && StringUtils.isBlank(request.getImageFilterParam()) && deriveProductMatchList != null && deriveProductMatchList.size() > 0) {
                        if (request.getOrderBy() == SearchOrderByEnum.NORMAL) {
                            // 通用排序
                            for (DeriveProductMatchResult deriveProductMatchResult : deriveProductMatchList) {
                                products = deriveProductService.insertDeriveProduct(products, deriveProductMatchResult, blockId, request, commonParam, aid);
                            }
                            // end of 通用排序
                        } else {
                            // 非通用排序
                            List<DeriveSearchItem> items = new ArrayList<>();
                            for (DeriveProductMatchResult deriveProductMatchResult : deriveProductMatchList) {
                                items.addAll(deriveProductMatchResult.getItems());
                            }
                            if (items.size() > 0) {
                                DeriveProductMatchResult total = new DeriveProductMatchResult();
                                total.setItems(items);
                                products = deriveProductService.insertAndSortDeriveProduct(products, total, blockId, request, false, commonParam, aid);

                            }
                            // end of 非通用排序
                        }
                        if (products.size() > 0) {
                            isOnlyDeriveProduct = true;
                        }
                    }
                    try {
                        // 对结果进行分页缓存并取出第一页结果
                        BlockData firstPage = pageAndCacheService.cacheAndGetFirstPageProducts(blockId, request, products,
                                commonParam, CommonConstant.TemplateType.SINGLE_PRODCUT, request.getQuery());
                        if (firstPage.getTemplates().size() > 0) {
                            firstPage.setCurQuery(request.getQuery());
                            blockDatas.add(firstPage);
                        }
                    } catch (Exception e) {
                        logger.error("[严重异常]缓存并返回第一页数据异常:[sid={}, uuid={}]", request.getSid(), request.getUuid(), e);
                        throw e;
                    }

                }

            }
        } catch (Exception e) {
            logger.error("[严重异常]封装主搜搜索结果异常:[sid={}, uuid={}]", request.getSid(), request.getUuid(), e);
            throw e;
        }

        // 增加facet内容 完全匹配无结果或者搜索无结果时，tips也在这里处理
        executeFacetPanel(request, blockDatas, topPanel, response, productSearchRpcResult, rpcBlocks, productIds4BuildFacets, isOnlyDeriveProduct);
        try {
            // 主搜挂接首页feed流内容
            if (ActivityEnum.NO_ACTIVITY.equals(ActivityEnum.judgeActivityType(request.getToActivity()))
                    || ActivityEnum.RECOMMEND_SEARCH.equals(ActivityEnum.judgeActivityType(request.getToActivity()))
                    || ActivityEnum.SUPPLIER_ALL_SEARCH.equals(ActivityEnum.judgeActivityType(request.getToActivity()))) {
                BlockData homeFeedData = homeFeedDataService.getHomeFeedData(request, commonParam, sb, allProductIds);
                if (homeFeedData != null && homeFeedData.getTemplates() != null && homeFeedData.getTemplates().size() > 0) {
                    blockDatas.add(homeFeedData);
                }
            }
        } catch (Exception e) {
            logger.error("[严重异常]挂接mosesmatch feed流异常:[sid={}, uuid={}]", request.getSid(), request.getUuid(), e);
            throw e;
        }
    }

    /**
     * 生成待聚合Facet的productId list
     * 召回产品中选定占比多的后台二级类目secondCategoryIdBuildFacet，
     * 保留products中二级类目与选定二级类目一致的product;否则保留与第一个召回产品二级类目一致的product
     *
     * @param productList
     * @return 返回用于聚合筛选项面板展示内容的productId
     */
    private List<Long> getFacetProductIdList(List<SearchProduct> productList) {
        List<Long> finalProductIdList = new ArrayList<>();
        Integer maxNum = 0;
        Map<Long, Integer> category2CountMap = new LinkedHashMap<>();
        for (int i = 0; i < PRODUCT_ID_TOP_NUM && i < productList.size(); i++) {
            Long productId = productList.get(i).getProductId().longValue();
            SearchProductInfo productInfo = productCache.getSearchProductInfo(productId);
            if (productInfo == null) {
                continue;
            }
            Long cate2Id = productInfo.getSecondCategoryId();
            Integer cate2Count = 1;
            if (category2CountMap.containsKey(cate2Id)) {
                cate2Count = category2CountMap.get(cate2Id) + 1;
            }
            category2CountMap.put(cate2Id, cate2Count);
            if (cate2Count > maxNum) {
                maxNum = cate2Count;
            }
        }
        Long finalCate2Id = 0L;
        for (Map.Entry<Long, Integer> entry : category2CountMap.entrySet()) {
            if (maxNum.equals(entry.getValue())) {
                finalCate2Id = entry.getKey();
                break;
            }
        }
        for (int i = 0; i < productList.size(); i++) {
            Long productId = productList.get(i).getProductId().longValue();
            SearchProductInfo productInfo = productCache.getSearchProductInfo(productId);
            if (productInfo == null) {
                continue;
            }
            Long cate2Id = productInfo.getSecondCategoryId();
            if (finalCate2Id.equals(cate2Id)) {
                finalProductIdList.add(productId);
            }
        }

        return finalProductIdList;
    }

    /**
     * 判断搜索结果商品中是否存在新品
     * 如果存在，判断此新品是否存在于uuid浏览过的新品集合
     * 如果不存在，将TopPanel中isShowRedDot设置为1（显示红点）
     */
    private void updateIsShowRedDot(List<SearchProduct> products, TopPanel topPanel, String uuid) {
        //获取uuid对应浏览过新品集合
        String redisKey = String.format(RedisKeyConsts.UUID_NEWPRODUCT_CACHE, uuid);
        List<String> newProductIds = getNewProductIds(redisKey);

        for (SearchProduct product : products) {
            SearchProductInfo searchProductInfo = productCache.getSearchProductInfo(product.getProductId().longValue());
            if (isNewProduct(searchProductInfo.getFirstOnShelfTime())) {

                if (newProductIds.size() > 0) {
                    if (!newProductIds.contains(searchProductInfo.getProductId().toString())) {
                        topPanel.setIsShowRedDot((byte) 1);
                        return;
                    }
                } else {
                    topPanel.setIsShowRedDot((byte) 1);
                    return;
                }

            }
        }
    }

    /**
     * 判断商品是否为新品
     * 是，判断商品是否已存在于uuid浏览过新品集合，不在则将此商品加入集合
     *
     * @return
     */
    private void updateUuidNewProductMap(List<SearchProduct> products, String uuid) {

        try {
            String redisKey = String.format(RedisKeyConsts.UUID_NEWPRODUCT_CACHE, uuid);
            List<String> newProductIds = getNewProductIds(redisKey);
            //将新品插入集合
            for (SearchProduct product : products) {
                SearchProductInfo searchProductInfo = productCache.getSearchProductInfo(product.getProductId().longValue());
                if (isNewProduct(searchProductInfo.getFirstOnShelfTime())) {
                    if (!newProductIds.contains(searchProductInfo.getProductId().toString())) {
                        newProductIds.add(searchProductInfo.getProductId().toString());
                    }
                }
            }

            //重置缓存
            if (newProductIds.size() > 0) {
                redisUtil.setString(redisKey, String.join(",", newProductIds), 60 * 60 * 24);
            }
        } catch (Exception e) {
            logger.error("[严重异常]重置uuid-新品缓存失败：", e);
        }
    }

    /**
     * 判断商品是否为新品（上架时间三天之内）
     *
     * @return
     */
    private boolean isNewProduct(Date firstOnShelfTime) {
        return ((System.currentTimeMillis() - firstOnShelfTime.getTime()) / oneDayMillisecond) < newProductDays;
    }

    /**
     * 获取uuid对应浏览过新品集合
     *
     * @return
     */
    private List<String> getNewProductIds(String redisKey) {
        List<String> result = new ArrayList<>();

        String redisResult = redisUtil.getString(redisKey);
        if (!StringUtils.isBlank(redisResult)) {
            String[] arr = redisResult.split(",");
            for (int i = 0; i < arr.length; i++) {
                result.add(arr[i]);
            }
        }
        return result;
    }

    /**
     * 根据facet过滤商品
     *
     * @return
     */
    private void filterProducts(List<SearchProduct> products, List<FacetItem> facets) {

        //没有筛选项直接返回
        if (facets == null) {
            return;
        }
        if (facets.size() == 0) {
            return;
        }

        List<Long> productIds = new ArrayList<>();
        products.forEach(item -> {
            productIds.add(item.getProductId().longValue());
        });

        //转换facet对象
        List<Facet> facetList = new ArrayList<>();
        for (FacetItem facetItem : facets) {
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

        //获取返回过滤结果
        List<Long> resultList = facetManager.facetProducts(productIds, facetList);

        //根据结果过滤商品
        products.removeIf(item -> !resultList.contains(item.getProductId().longValue()));

    }


    /**
     * 获取Facet面板
     * 判断是否命中缓存，命中则直接返回，未命中则添加到redis
     *
     * @return
     */
    private List<Facet> getFacetList(List<Long> productIds) {
        List<Facet> result = new ArrayList<>();
        //没有商品直接返回
        if (productIds.size() == 0) {
            return result;
        }
        try {
            result = facetManager.buildFacets(productIds);
        } catch (Exception e) {
            logger.error("[严重异常]根据商品聚合Facet面板失败,productIds={}", JSON.toJSONString(productIds), e);
        }
        return result;

    }

    /**
     * 一起拼搜索器
     *
     * @param request
     * @param commonParam
     * @param sb
     * @param blockDatas
     * @param topPanel
     * @param response
     */
    private void generateGroupBuyResponse(UISearchRequest request, CommonRequestParam commonParam, StringBuilder sb,
                                          List<BlockData> blockDatas, TopPanel topPanel, UISearchResponse response) {

        /*
         * 搜索词匹配相关结果
         */
        topPanel.setOrderByList(
                Lists.newArrayList(SearchOrderByConsts.HOT, SearchOrderByConsts.SALE, SearchOrderByConsts.NEW, SearchOrderByConsts.PRICE));
        RPCResult<ASProdcutSearchResult> productSearchRpcResult = getProductSearchRpcResultNew(request, commonParam,
                SearchLimit.MAX_HIT_COUNT, request.getToActivity());
        String sourceId = request.getSourceId() == null ? "" : request.getSourceId();

        List<ProductSearchBlock> rpcBlocks = new ArrayList<>();
        List<ProductSearchBlock> productBlocks = new ArrayList<>();
        //用于判断过滤后是否存在商品
        List<SearchProduct> productCount = new ArrayList<>();
        if (productSearchRpcResult != null) {

            if (productSearchRpcResult != null && productSearchRpcResult.getStatus() != null && productSearchRpcResult.getStatus().equals(SearchStatus.OK)) {
                rpcBlocks = productSearchRpcResult.getData().getProductBlocks();
            }

            if (!PageSourceEnum.JOINGROUP.getSourceId().equals(sourceId)) {

                if (productBlocks.size() > 0 && productBlocks.get(0).getItems() != null
                        && productBlocks.get(0).getItems().size() > 0) {
                    List<SearchItem> items = productBlocks.get(0).getItems();
                    if (rpcBlocks.size() > 0 && rpcBlocks.get(0).getItems() != null
                            && rpcBlocks.get(0).getItems().size() > 0) {
                        List<SearchItem> items2 = rpcBlocks.get(0).getItems();
                        items2.addAll(items);
                    } else {
                        rpcBlocks.addAll(productBlocks);
                    }

                }

                Map<Integer, Integer> productMap = new HashMap<>();
                for (ProductSearchBlock item : rpcBlocks) {
                    List<SearchItem> newItems = new ArrayList<>();
                    List<SearchItem> items = item.getItems();
                    for (int i = 0; i < items.size(); i++) {
                        Integer productId = items.get(i).getProductId();
                        if (productMap.containsKey(productId)) {
                            continue;
                        } else {
                            productMap.put(productId, productId);
                            newItems.add(items.get(i));
                        }
                    }
                    item.setItems(newItems);
                }
            }

            /*
             * 分页缓存，并取出第一页数据
             */
            // 部分匹配结果序列，用于记录日志
            int qsMatchPos = 0;

            for (ProductSearchBlock item : rpcBlocks) {

                String aid = IdCalculateUtil.createAid(request.getAid() + IdCalculateUtil.createBlockId());

                String blockId = IdCalculateUtil.createBlockId();
                String matchBlock = generateMatchBlockId(request.getQuery(), item.getQuery(), qsMatchPos++);

                // 获取商品基础信息
                List<SearchProduct> products = beanConvertService.convert2SearchProduct(item.getItems(), request, CommonConstant.TemplateType.GROUP_BUY_SINGLE_PRODCUT, true);
                if (request.getOrderBy() != SearchOrderByEnum.NORMAL) {
                    productDetailService.sortByBaseInfoAfterConvert(products, request.getOrderBy(), true);
                }
                productDetailService.detailProductAfterSort(products, commonParam, blockId, request, matchBlock,
                        CommonConstant.TemplateType.GROUP_BUY_SINGLE_PRODCUT, aid);
                productCount = products;

                if (request.getOrderBy().getDesc().equals(SearchOrderByEnum.NEW.getDesc())) {
                    //用户点击上新排序
                    updateUuidNewProductMap(products, request.getUuid());
                } else {
                    //用户未点击上新排序
                    updateIsShowRedDot(products, topPanel, request.getUuid());
                }

                // 对结果进行分页缓存并取出第一页结果
                BlockData firstPage = pageAndCacheService.cacheAndGetFirstPageProducts(blockId, request, products,
                        commonParam, CommonConstant.TemplateType.GROUP_BUY_SINGLE_PRODCUT, item.getQuery());
                if (firstPage.getTemplates().size() <= 0) {
                    continue;
                }

                if (!request.getQuery().equals(item.getQuery())) {
                    // 非完全匹配区块展示标题
                    firstPage.setTitle(generateBlockTitleTextList(request.getQuery()));
                }

                firstPage.setCurQuery(item.getQuery());
                blockDatas.add(firstPage);

                // 记录结果日志
                sb.append("\t" + matchBlock + "=");
                sb.append(generateProductResultLog(item.getQuery(), blockId, null, products, aid));
            }
        }

        // 处理一起拼面板相关信息
        if (rpcBlocks.size() > 0) {
            // 经过筛选之后没有结果时，照样显示facet面板
            List<FacetItem> facetFromAS = productSearchRpcResult.getData().getFacets();
            if (facetFromAS.size() > 10) {
                facetFromAS = facetFromAS.subList(0, 10);
            }

            topPanel.setFacet(beanConvertService.convertFacetItem2UIFacet(facetFromAS, request.getFacets(), request));
            topPanel.setOrderByList(beanConvertService.groupBuyOrderByList(commonParam));
            //实际无商品时，不显示
            if (productCount.size() != 0) {
                topPanel.setOnOff(1);
            } else {
                topPanel.setOnOff(0);
            }

        } else { // 确实搜索无结果
            topPanel.setOnOff(0);
        }

    }


    /**
     * 处理面板相关信息 展示逻辑改来改去，最后不知道该怎么写了，且用着 后续可以重新梳理此逻辑，对此块代码进行优化
     * tips 逻辑 -- 搜索优化V1.4
     * 无完全匹配，有部分匹配时：顶部文案为 "没有找到XXX的搜索结果\n为您推荐相关商品"
     * 无完全匹配，无部分匹配时：顶部文案为 "没有找到XXX的搜索结果"
     *
     * @param request
     * @param blockDatas
     * @param topPanel
     * @param response
     * @param productSearchRpcResult
     * @param rpcBlocks
     */
    private void executeFacetPanel(UISearchRequest request, List<BlockData> blockDatas,
                                   TopPanel topPanel, UISearchResponse response, RPCResult<ASProdcutSearchResult> productSearchRpcResult,
                                   List<ProductSearchBlock> rpcBlocks, List<Long> productIds, Boolean isOnlyDeriveProduct) {
        // 经过筛选之后没有结果时，照样显示facet面板，即rpcBlocks.size() == 0 && request.getFacets().size() > 0
        if (rpcBlocks.size() > 0 || (request.getFacets().size() > 0)) {
            try {
                List<FacetItem> facetFromAS = productSearchRpcResult.getData().getFacets();
                if (facetFromAS.size() > 10) {
                    facetFromAS = facetFromAS.subList(0, 10);
                }

                //根据商品聚合出facet面板
                List<Facet> facetList = getFacetList(productIds);
                // M站需要过滤活动筛选项
                if (PlatformEnum.M.equals(request.getPlatform())) {
                    facetList = facetList.stream().filter(
                            x -> !FacetConstants.ACTIVITY.equals(x.getTitle())
                    ).collect(Collectors.toList());
                }
                topPanel.setFacet(beanConvertService.convertFacetItem2UIFacet(facetList, request.getFacets()));
                topPanel.setOrderByList(beanConvertService.generateOrderByList(request));
                topPanel.setOnOff(1);
            } catch (Exception e) {
                logger.error("[严重异常]构造facet面板异常:[sid={}, uuid={}]", request.getSid(), request.getUuid(), e);
                throw e;
            }

            try {
                //特殊口罩商品tips需求 20200302
                boolean showMaskTips = true;
                List<Long> maskList = new ArrayList<>();
                for (Long productId : productIds) {
                    if (redisDataCache.getMaskList().contains(productId.toString())) {
                        SearchProductInfo searchProductInfo = productCache.getSearchProductInfo(productId);
                        if (searchProductInfo.getShelfStatus() == 1) {
                            showMaskTips = false;
                        }
                        maskList.add(productId);
                    }
                }
                if (maskList.size() == 0) {
                    showMaskTips = false;
                }
                if (showMaskTips) {
                    response.getTips().addAll(setMaskTips());
                }


                if (blockDatas.size() > 0 && !request.getQuery().equals(blockDatas.get(0).getCurQuery())) {
                    // 没有完全匹配，有部分匹配时
                    if (request.getFacets().size() > 0) {
                        // 有facet，有部分匹配结果
                        response.getTips().addAll(generateTipsWhenFilter(request.getOriginalQuery()));
                    } else {
                        // 没有facet，有部分匹配
                        //response.setTips(genNoMatchAllDataTips(request.getOriginalQuery()));
                    }
                } else if (blockDatas.size() == 0) {
                    // 没有完全匹配，也没有部分匹配
                    if (request.getFacets().size() > 0) {
                        // 有facet 没有部分匹配
                        response.getTips().addAll(genNoMatchTipsWithFilter(request.getOriginalQuery()));
                    } else {
                        // 没有facet 没有部分匹配
                        response.getTips().addAll(genNoMatchDataTips(request.getOriginalQuery()));
                    }
                }
            } catch (Exception e) {
                logger.error("[严重异常]构造搜索结果提示信息异常:[sid={}, uuid={}]", request.getSid(), request.getUuid(), e);
                throw e;
            }
        } else {
            if (isOnlyDeriveProduct) {
                //只有衍生商品结果
                topPanel.setOrderByList(beanConvertService.generateOrderByList(request));
                topPanel.setOnOff(1);
            } else {
                // 无结果
                response.getTips().addAll(genNoMatchDataTips(request.getOriginalQuery()));
                topPanel.setOrderByList(beanConvertService.generateOrderByList(request));
                topPanel.setOnOff(0);
            }
        }
    }

    private List<SearchTitle> setMaskTips() {
        List<SearchTitle> result = new ArrayList<SearchTitle>();

        SearchTitle title1 = new SearchTitle();
        List<TitleText> oneLine = new ArrayList<>();
        oneLine.add(new TitleText("今日口罩产能已饱和，每日10点开放产能", ColorCodeConsts.TITLE_COLOR_RED, 0));

        title1.setPicType(0);
        title1.setContents(oneLine);

        result.add(title1);

        return result;
    }

    /**
     * 根据搜索词从AS获取搜索结果 包含完全匹配与部分匹配结果
     *
     * @param request
     * @param commonParam
     * @param expectNum
     * @return
     * @author: luozhuo
     * @date: 2018年7月26日 上午11:18:17
     */
    private RPCResult<ASProdcutSearchResult> getProductSearchRpcResult(UISearchRequest request,
                                                                       CommonRequestParam commonParam, Integer expectNum) {
        RPCResult<ASProdcutSearchResult> productSearchRpcResult = null;

        SearchRequest productSearchParam = new SearchRequest();
        productSearchParam.setQuery(request.getQuery());
        productSearchParam.setCommonParam(commonParam);
        productSearchParam.setQuery(request.getQuery());
        productSearchParam.setExpectNum(expectNum);
        productSearchParam.setFacets(request.getFacets());
        try {
            productSearchRpcResult = asMainService.productSearch(productSearchParam);
        } catch (Exception e) {
            logger.error("[严重异常]远程调用searchas dubbo服务失败:[method=productSearch, param={}]", JSONObject.toJSONString(productSearchParam), e);
        }
        return productSearchRpcResult;
    }

    /**
     * 根据搜索词从AS获取搜索结果 包含完全匹配与部分匹配结果（新版主搜使用）
     *
     * @param request
     * @param commonParam
     * @param expectNum
     * @return
     * @author: luozhuo
     * @date: 2018年7月26日 上午11:18:17
     */
    private RPCResult<ASProdcutSearchResult> getProductSearchRpcResultNew(UISearchRequest request,
                                                                          CommonRequestParam commonParam, Integer expectNum, Integer aliasType) {
        RPCResult<ASProdcutSearchResult> productSearchRpcResult = null;
        try {
            SearchRequest productSearchParam = new SearchRequest();
            productSearchParam.setQuery(request.getQuery());
            productSearchParam.setCommonParam(commonParam);
            productSearchParam.setExpectNum(expectNum);
            productSearchParam.setAliasType(aliasType);
            productSearchParam.setIsSupportCreation(request.getIsSupportCreation());
            if (StringUtils.isNotEmpty(request.getSupplierId())) {
                productSearchParam.setSupplierId(Long.parseLong(request.getSupplierId()));
            }
            try {
                productSearchRpcResult = asMainService.productSearch(productSearchParam);
            } catch (Exception e) {
                logger.error("[严重异常]远程调用searchas dubbo服务失败:[method=productSearch, param={}]", JSONObject.toJSONString(productSearchParam), e);
            }
        } catch (Exception e) {
            logger.error("[严重异常]调用searchas dubbo服务获取商品异常:[sid={}, uuid={}]", request.getSid(), request.getUuid(), e);
            throw e;
        }
        return productSearchRpcResult;
    }

    /*
     * 生成轮播图日志
     */
    private String generateTopicCardLog(String blockId, List<TopicItem> topics, String aid) {
        JSONArray topicCardsLog = new JSONArray();
        int position = 0;
        for (TopicItem topic : topics) {
            JSONObject item = new JSONObject();
            item.put("pos", position++);

            topicCardsLog.add(item);
        }

        JSONObject result = new JSONObject();
        result.put("blockId", blockId);
        result.put("aid", aid);
        result.put("result", topicCardsLog);

        return result.toJSONString();
    }

    /**
     * 生成商品展现日志
     *
     * @param query
     * @param blockId
     * @param topicId
     * @param products
     * @param aid
     * @param
     **/
    private String generateProductResultLog(String query, String blockId, Integer topicId, List<SearchProduct> products,
                                            String aid) {
        JSONArray productsLog = new JSONArray();
        int position = 0;
        for (SearchProduct product : products) {
            JSONObject item = new JSONObject();
            item.put("pos", position++);
            if (topicId == null) {
                item.put("sc", product.getScore() == 0 ? "0" : df.format(product.getScore()));
            }
            item.put("suid", product.getSuId());
            productsLog.add(item);
        }

        JSONObject result = new JSONObject();
        if (topicId == null) {
            result.put("q", query);
        }
        result.put("blockId", blockId);
        if (topicId != null) {
            result.put("topicId", topicId);
        }
        result.put("aid", aid);
        result.put("result", productsLog);

        return result.toJSONString();
    }

    /*
     * 获取排序面板是否展示的配置
     * @return 0:不显示  1:显示
     */
    private Integer getTopPanelOnOff() {
        if (redisDataCache.getPanelConfig() != null) {
            return redisDataCache.getPanelConfig().getOnOff();
        } else {
            return 0;
        }
    }


    /**
     * 底部是否显示浮层
     *
     * @return 0:不显示  1:显示
     */
    private Integer getBottomHanging() {
        if (redisDataCache.getPanelConfig() != null) {
            return redisDataCache.getPanelConfig().getBottomHanging();
        } else {
            return 0;
        }
    }

    /**
     * 生成搜索无结果时的提示标题
     *
     * @param query
     * @return
     * @author: luozhuo
     * @date: 2018年7月26日 上午11:21:43
     */
    private List<SearchTitle> generateTips(String query) {
        List<SearchTitle> result = new ArrayList<SearchTitle>();

        SearchTitle title1 = new SearchTitle();
        List<TitleText> oneLine = new ArrayList<>();
        oneLine.add(new TitleText("没有找到", ColorCodeConsts.TITLE_COLOR_GRAY, 0));
        oneLine.add(new TitleText("“" + truncateQuery(query, 8) + "”", ColorCodeConsts.TITLE_COLOR_PURPLE, 1));
        oneLine.add(new TitleText("相关的商品", ColorCodeConsts.TITLE_COLOR_GRAY, 0));

        title1.setPicType(0);
        title1.setContents(oneLine);

        result.add(title1);

        SearchTitle title2 = new SearchTitle();
        List<TitleText> twoLine = new ArrayList<>();
        twoLine.add(new TitleText("为您推荐以下商品", ColorCodeConsts.TITLE_COLOR_GRAY, 0));

        title2.setPicType(0);
        title2.setContents(twoLine);

        result.add(title2);

        return result;
    }

    /**
     * 生成无完全匹配、无部分匹配结果的提示语
     *
     * @param query
     * @return
     */
    private List<SearchTitle> genNoMatchDataTips(String query) {
        List<SearchTitle> result = new ArrayList<SearchTitle>();

        SearchTitle title = new SearchTitle();
        List<TitleText> oneLine = new ArrayList<>();
        oneLine.add(new TitleText("没有找到", ColorCodeConsts.TITLE_COLOR_GRAY, 0));
        oneLine.add(new TitleText("“" + truncateQuery(query, 8) + "”", ColorCodeConsts.TITLE_COLOR_PURPLE, 1));
        oneLine.add(new TitleText("相关的商品", ColorCodeConsts.TITLE_COLOR_GRAY, 0));

        title.setPicType(0);
        title.setContents(oneLine);

        result.add(title);
        return result;
    }

    /**
     * 生成筛选后无结果时的提示标题
     *
     * @param query
     * @return
     * @author: luozhuo
     * @date: 2018年7月26日 上午11:22:08
     */
    private List<SearchTitle> generateTipsWhenFilter(String query) {
        List<SearchTitle> result = new ArrayList<SearchTitle>();

        SearchTitle title1 = new SearchTitle();
        List<TitleText> oneLine = new ArrayList<>();
        oneLine.add(new TitleText("没有找到匹配的商品 ", ColorCodeConsts.TITLE_COLOR_GRAY, 0));

        title1.setPicType(0);
        title1.setContents(oneLine);

        result.add(title1);

        SearchTitle title2 = new SearchTitle();
        List<TitleText> twoLine = new ArrayList<>();
        twoLine.add(new TitleText("为您推荐相关商品", ColorCodeConsts.TITLE_COLOR_GRAY, 0));

        title2.setPicType(0);
        title2.setContents(twoLine);

        result.add(title2);

        return result;
    }

    /**
     * 生成筛选后无完全匹配、无部分匹配的提示标题
     *
     * @param query
     * @return
     * @author: luozhuo
     * @date: 2018年7月26日 上午11:22:08
     */
    private List<SearchTitle> genNoMatchTipsWithFilter(String query) {
        List<SearchTitle> result = new ArrayList<SearchTitle>();

        SearchTitle title1 = new SearchTitle();
        List<TitleText> oneLine = new ArrayList<>();
        oneLine.add(new TitleText("没有找到匹配的商品 ", ColorCodeConsts.TITLE_COLOR_GRAY, 0));

        title1.setPicType(0);
        title1.setContents(oneLine);

        result.add(title1);

        return result;
    }

    /**
     * 生成部分匹配块的标题
     *
     * @param query
     * @return
     * @author: luozhuo
     * @date: 2018年7月26日 上午11:22:33
     */
    private List<SearchTitle> generateBlockTitleTextList(String query) {
        List<SearchTitle> result = new ArrayList<>();

        SearchTitle title = new SearchTitle();

        List<TitleText> oneLine = new ArrayList<>();
        oneLine.add(new TitleText("和“", ColorCodeConsts.TITLE_COLOR_BLACK_333333, 1));
        oneLine.add(new TitleText(truncateQuery(query, 8), ColorCodeConsts.TITLE_COLOR_BLACK_333333, 1));
        oneLine.add(new TitleText("”相关的商品", ColorCodeConsts.TITLE_COLOR_BLACK_333333, 1));

        title.setContents(oneLine);

        title.setPicType(1);
        result.add(title);

        return result;
    }


    /**
     * @param request
     * @return
     * @description: 搜索结果页-区块内翻页接口
     * @author: luozhuo
     * @date: 2018年7月26日 上午11:08:29
     * @see com.biyao.search.ui.remote.UISearchService#searchPage(com.biyao.search.ui.remote.request.UISearchPageRequest)
     */
    @Override
    @GET
    @Path("search/page")
    public HttpResult2<BlockData> searchPage(@BeanParam UISearchPageRequest request) {
        logger.info("翻页请求原始参数：" + JSONObject.toJSONString(request));
        request.preHandleParam();
        logger.info("翻页请求处理后参数：" + JSONObject.toJSONString(request));
        CommonRequestParam commonParam = new CommonRequestParam();
        commonParam.setPlatform(request.getPlatform());
        commonParam.setSid(request.getSid());
        commonParam.setUid(request.getUid());
        commonParam.setUuid(request.getUuid());

        BlockData blockData = pageAndCacheService.getPageBlockData(request);
        //fillRpcInfoService.fillProductInfo(Arrays.asList(blockData));//封装展示视频标识旧逻辑弃用
        return new HttpResult2<>(blockData);
    }

    /**
     * 标题词语进行长度限制，超过长度的部分用...代替
     *
     * @param query
     * @param targetLen
     * @return
     * @author: luozhuo
     * @date: 2018年7月26日 上午11:24:44
     */
    private static String truncateQuery(String query, int targetLen) {
        if (query == null) {
            return "";
        } else if (query.length() > targetLen) {
            return query.substring(0, targetLen) + "…";
        } else {
            return query;
        }
    }

    /**
     * 根据query match的搜索词和前端传的原始搜索词，生成搜索的block标识
     *
     * @param originalQuery
     * @param curQuery
     * @param blockPos
     * @return
     */
    private String generateMatchBlockId(String originalQuery, String curQuery, int blockPos) {
        if (originalQuery != null && originalQuery.equals(curQuery)) {
            return "qmatch";
        } else {
            return "qmatch" + blockPos;
        }
    }

    /**
     * 处理图片筛选项条件
     *
     * @param request
     * @param response
     * @param productList
     */
    private void dealImageFilter(UISearchRequest request, UISearchResponse response, List<SearchProduct> productList) {
        try {
            List<FilterItem> imageFilterList = new ArrayList<>();
            Set<String> fCategoryNameSet = new HashSet<>();
            String imageFilterParam = request.getImageFilterParam();
            String filterFCategoryName = "";
            boolean hasFilter = false;
            if (StringUtils.isNotEmpty(imageFilterParam)) {
                String[] kv = URLDecoder.decode(imageFilterParam).split("=");
                if (kv.length == 2) {
                    filterFCategoryName = kv[1];
                    hasFilter = true;
                }
            }
            Iterator<SearchProduct> iterator = productList.iterator();
            while (iterator.hasNext()) {
                SearchProduct searchItem = iterator.next();
                SearchProductInfo searchProductInfo = productCache.getSearchProductInfo((long) searchItem.getProductId());
                if (searchProductInfo == null) {
                    iterator.remove();
                    continue;
                }
                String fCategory3Name = "";
                if (searchProductInfo.getFCategory3Names() != null && searchProductInfo.getFCategory3Names().size() > 0) {
                    fCategory3Name = searchProductInfo.getFCategory3Names().get(0);
                }
                // 添加筛选项
                if (!fCategoryNameSet.contains(fCategory3Name)) {
                    FilterItem imageFilter = new FilterItem();
                    imageFilter.setImage(searchProductInfo.getSquarePortalImg());
                    imageFilter.setText(fCategory3Name);
                    imageFilter.setParam(URLEncoder.encode("fc3n=" + fCategory3Name));
                    // 设置是否选中
                    if (hasFilter && filterFCategoryName.equals(fCategory3Name)) {
                        imageFilter.setSelected(1);
                    } else {
                        imageFilter.setSelected(0);
                    }
                    imageFilterList.add(imageFilter);
                    fCategoryNameSet.add(fCategory3Name);
                }
                // 如果有过滤条件，且商品前台类目集合不包含选中的一级类目，则需要删除该商品
                boolean shouldRemove = hasFilter && (searchProductInfo.getFCategory3Names() == null || !searchProductInfo.getFCategory3Names().contains(filterFCategoryName));
                if (shouldRemove) {
                    iterator.remove();
                }
            }

            if (imageFilterList.size() > 1) {
                if (null == response.getImageFilters() || response.getImageFilters().size() == 0) {
                    response.setImageFilters(imageFilterList);
                } else {
                    List<FilterItem> oriItems = response.getImageFilters();
                    List<String> paramList = oriItems.stream().map(FilterItem::getParam).collect(Collectors.toList());
                    imageFilterList.forEach(item -> {
                        if (!paramList.contains(item.getParam())) {
                            oriItems.add(item);
                        }
                    });
                    response.setImageFilters(oriItems);
                }

            }
        } catch (Exception e) {
            logger.error("[严重异常]处理图片筛选项失败,productList={},response={}", JSON.toJSONString(productList), JSON.toJSONString(response), e);
        }
    }


    @Override
    @Path("testonline")
    @GET
    @ResponseBody
    public Map<String, String> testonline() {
        Map<String, String> result = new HashMap<>();
        result.put("state", "yes");
        return result;
    }

    /**
     * 构造托底数据
     *
     * @param request
     * @return response
     */
    private UISearchResponse buildFullbackResponse(UISearchRequest request) {
        UISearchResponse response = new UISearchResponse();
        try {
            List<SearchTitle> result = new ArrayList<SearchTitle>();

            SearchTitle title1 = new SearchTitle();
            List<TitleText> oneLine = new ArrayList<>();
            oneLine.add(new TitleText("没有找到您搜索的商品", ColorCodeConsts.TITLE_COLOR_GRAY, 0));

            title1.setPicType(0);
            title1.setContents(oneLine);

            result.add(title1);
            // 搜索无结果提示
            response.setTips(result);
            // 顶部面板，初始化时为不显示 topPanel.onOff == 0
            TopPanel topPanel = new TopPanel();
            response.setTopPanel(topPanel);
            response.setQuery(request.getOriginalQuery());
            response.setSid(request.getSid());
            response.setBottomHanging(0);

        } catch (Exception e) {
            logger.error("[严重异常]生成托底结果失败:[uuid={}, request={}]", request.getUuid(), JSON.toJSONString(request), e);
        }

        return response;
    }
}
