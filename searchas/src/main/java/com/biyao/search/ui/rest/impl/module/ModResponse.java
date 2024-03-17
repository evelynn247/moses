package com.biyao.search.ui.rest.impl.module;

import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.biyao.orderquery.client.tob.IBOrderBaseQueryService;
import com.biyao.orderquery.client.tob.beans.BFirstOrderInfoDTO;
import com.biyao.orderquery.client.tob.beans.Result;
import com.biyao.search.as.service.ASSearchService;
import com.biyao.search.as.service.model.response.ASSearchResponse;
import com.biyao.search.common.constant.SearchStatus;
import com.biyao.search.common.enums.PlatformEnum;
import com.biyao.search.common.model.*;
import com.biyao.search.ui.cache.ProductCache;
import com.biyao.search.ui.constant.CommonConstant;
import com.biyao.search.ui.constant.UIResultType;
import com.biyao.search.ui.model.RequestBlock;
import com.biyao.search.ui.model.SearchProductInfo;
import com.biyao.search.ui.rest.impl.cache.CommonCache;
import com.biyao.search.ui.rest.response.SearchOrderBy;
import com.biyao.search.ui.rest.response.SearchOrderByConsts;
import com.biyao.search.ui.rest.response.SearchPanel;
import com.biyao.search.ui.rest.response.UIResponse;
import com.biyao.search.ui.util.RedisUtil;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 组装前端返回结果UISearchResponse
 */
public class ModResponse implements UIModule {
	
    // 高模商品
	private static final String H_PRODUCT_URL_PREFIX = "biyao://product/browse/productDetail?goodsID=";
	// 无模型或者低模商品
	private static final String L_PRODUCT_URL_PREFIX = "biyao://product/browse/goodsDetail?goodsID=";
	// 拼团商品
	private static final String GROUP_PRODUCT_URL_PREFIX = "biyao://product/group/goodsDetail?goodsId=";
	// 同事一起拼商品
	private static final String TOGETHER_PRODUCT_URL_PREFIX = "biyao://product/togetherGroup/goodsDetail?suId=";
	
	private static final int FALLBACK_PRODUCT_NUM = 20; // 托底商品数量
	
	private static final String LABEL_CONFIG_CACHE = "search:product_label_info";
	private static final String DEFAULT_LABEL_COLOR = "#D6B98C";
	private static final String SEARCH_CONFIG = "search:search_show_config";
	private static final String SEARCH_PANEL_CONFIG = "search:top_panel_config";
	
    @Autowired
    private  ASSearchService asSearchService;
    
    @Autowired
    private CommonCache commonCache;
    
    @Autowired
    private IBOrderBaseQueryService orderBaseService;

    @Autowired
    private RedisUtil redisUtil;
    
    @Autowired
    private ProductCache productCache;

    private Logger logger = LoggerFactory.getLogger(getClass());

    private final static DecimalFormat decimalFormat = new DecimalFormat("###################.##");
    
    /**
     * 初始化操作
     * 程序启动时执行一次
     */
    public void init(){}
    
    /**
     * 清理操作
     * 启动关闭时执行一次
     */
    public void destroy(){}

    // 处理请求
    public Status run(RequestBlock request ) {
        Status status = request.getStatus();
        
        // 搜索正常时，组装返回结果
        UIResponse response = null;
        if( status.equals( SearchStatus.OK ) && request.getAsResponse().getHitTotal() > 0 ) {
            response = createNormalResult(request);
        
        // 搜索无结果，或者搜索出错  增加条件：当前为首页时，防止翻页时遇到缓存错误 20180510
        } else if (request.getPageIndex() == 1){
            response = createFallbackResult(request);
        }
        
        if( response == null ) {
            response = new UIResponse();
        }
        
        // UI的返回结果
        request.setUiResponse( response );
        
        return SearchStatus.OK;
    }
    
    /**
     * 搜索正常时，组装返回结果
     */
    private UIResponse createNormalResult( RequestBlock request ) {
        
        ASSearchResponse<ASProduct> asResponse = request.getAsResponse();
        
        // 根据AS的返回结果获取返回数据
        int basePosition = request.getPageSize() * (request.getPageIndex()-1);
        
        List<ASProduct> asProducts = asResponse.getResult();
        List<UIProduct> uiProducts = new ArrayList<UIProduct>(asProducts.size());
        
        // 缓存为0时才认为不显示，缓存不为0或缓存不存在认为需要显示
        boolean productDesShow = !"0".equals(redisUtil.hgetString(SEARCH_CONFIG, "productdeson"));
        boolean labelsShow = !"0".equals(redisUtil.hgetString(SEARCH_CONFIG, "labelson"));
        
        boolean showYiqipinIcon = getStatusOfYiqipinIcon(request);
      
        for( int i = 0; i < asProducts.size(); i++ ) {
            ASProduct asProduct = asProducts.get(i);
            Long productId = Long.valueOf(asProduct.getSuId().substring(0, 10));

            SearchProductInfo searchProductInfo = productCache.getSearchProductInfo(productId);
            UIProduct uiProduct = new UIProduct();
            
            uiProduct.setPosition( basePosition + i );
            uiProduct.setImage( asProduct.getImage() );
            uiProduct.setName( asProduct.getShortTitle() );
            uiProduct.setFullTitle(asProduct.getFullTitle());
            uiProduct.setSalePoint(asProduct.getSalePoint());
            uiProduct.setPrice( getPrice(request, asProduct) );
            uiProduct.setSuId( asProduct.getSuId() );
            if( asProduct.getSaleMode() == 2 ) {
                uiProduct.setIsShowIcon(2);    // 左上角提示类型。0没有，1新品 2 团购 3 一起拼
            } else if (showYiqipinIcon && asProduct.getActivities() != null && asProduct.getActivities().contains(CommonConstant.YIQIPIN_ACTIVITY)) {
            	uiProduct.setIsShowIcon(3);
            } else {
                uiProduct.setIsShowIcon(0);    // 左上角提示类型。0没有，1新品 2 团购 3 一起拼
            }
            uiProduct.setRedirectUrl(getRedirectUrlBySuId(request, asProduct.getSaleMode(), asProduct.getSuId()+"", uiProduct.getPosition(), uiProduct.getIsShowIcon()));
            uiProduct.setScore(asProduct.getScore());
            uiProduct.setAlgoScore(asProduct.getAlgoReScore());
            if (searchProductInfo != null && searchProductInfo.getGoodCommentToAll() != null && searchProductInfo.getGoodCommentToAll() >= 1) { // 只要有评价即展示
            	uiProduct.setCommentInfo(fillNewGoodComment(searchProductInfo.getGoodCommentToAll())+ "条好评");
            } else {
            	uiProduct.setCommentInfo("");
            }
            
            if (labelsShow) {
            	uiProduct.setLables(asProduct.getLabels().size() <= 2 ? asProduct.getLabels()
            			: asProduct.getLabels().subList(0, 2)); // 最多展示两个标签
            } else {
            	uiProduct.setLables(new ArrayList<>());
            }
            
            uiProduct.setProductDes(productDesShow ? asProduct.getSalePoint() : ""); // 先默认为卖点

            /**
             * pc改版搜索结果增加节点,
             * 新品 isNew
             * 好评数 evaluate
             * 标签 labels
             * 价格字符串 priceStr
             * 制造商背景 supplierBackground
             */
            if (PlatformEnum.PC.getName().equals(request.getPlatform())) {
                String priceStr = decimalFormat.format(asProduct.getPrice());
                uiProduct.setPriceStr(priceStr);
                uiProduct.setSupplierBackground("");
                if (searchProductInfo != null) {
                	Date firstOnshelfDate = searchProductInfo.getFirstOnShelfTime();
                    int hours = 0;
                    if (firstOnshelfDate != null) {
                        hours = (int) ((System.currentTimeMillis() - firstOnshelfDate.getTime()) / (1000 * 3600));
                    }
                    if (hours <= 48) {
                        uiProduct.setIsNew(1);
                    } else {
                        uiProduct.setIsNew(0);
                    }
                    Integer evaluate = searchProductInfo.getPositiveComment();
                    if(evaluate != null) {                    	
                    	uiProduct.setEvaluate(evaluate);
                    }

                    String supplierBackground = searchProductInfo.getSupplierBackground();
                    supplierBackground = StringUtils.isEmpty(supplierBackground) ? "" : supplierBackground;
                    uiProduct.setSupplierBackground(supplierBackground);
                    /**
                     * 标签处理，如果支持一起拼，则增加一起拼标签
                     */
                    boolean yqp = searchProductInfo.getIsToggroupProduct() == 1;
                    List<LabelTag> labelList = getPcSearchLabels(searchProductInfo.getSearchLabels(), yqp);
                    uiProduct.setLables(labelList);
                }
            }
            uiProducts.add( uiProduct );
        }

        /*
         *  决定productDes返回制造商背景还是卖点
         */
        if (productDesShow) {
        	// 具有制造商背景的商品列表
            List<ASProduct> asProductsHavingSBG = asProducts.stream().filter(i -> !Strings.isNullOrEmpty(i.getSupplierBackground())).collect(Collectors.toList());
            // 选取需要展示制造商背景的商品
            int destNum = (int) Math.ceil((float) asProducts.size() / 10);
            List<ASProduct> showSBGproducts = selectByRandom(asProductsHavingSBG, destNum);
            Map<String, ASProduct> showSBGproductMap = showSBGproducts.stream().collect(
            		Collectors.toMap(ASProduct :: getSuId, v -> v)); // key -> suId  value -> v
            // 遍历ui结果，设置制造商背景
            for (int i = 0; i < uiProducts.size(); i ++) {
            	UIProduct product = uiProducts.get(i);
            	if (!showSBGproductMap.containsKey(product.getSuId())) {
            		continue;
            	}
            	
            	product.setProductDes(showSBGproductMap.get(product.getSuId()).getSupplierBackground());
            }
        }
        
        // label颜色处理
        // zhaiweixi 20180903 pc不处理
        if (labelsShow && !PlatformEnum.PC.getName().equals(request.getPlatform())) {
        	 String labelConfigStr = redisUtil.getString(LABEL_CONFIG_CACHE);
             JSONObject labelConfig = Strings.isNullOrEmpty(labelConfigStr) ? new JSONObject() : JSONObject.parseObject(labelConfigStr);
             for (int i = 0; i < uiProducts.size(); i ++) {
             	UIProduct product = uiProducts.get(i);
             	List<LabelTag> labels = product.getLables();
             	for (LabelTag label : labels) {
             		if (!labelConfig.containsKey(label.getContent())) {
             			label.setColor(DEFAULT_LABEL_COLOR);
             		} else {
             			label.setColor(labelConfig.getJSONObject(label.getContent()).getString("color"));
             		}
             	}
             }
        }

        // 组装返回结果
        UIResponse response = new UIResponse();
        response.setQuery(request.getQuery());
        response.setSid(request.getSid());
        response.setPageCount( calPageCount(request.getPageSize(), asResponse.getHitTotal()) );
        response.setPageIndex( request.getPageIndex() );
        response.setResultType(UIResultType.NORMAL_SEARCH);
        response.setOrderBy(request.getOrderBy().getCode());
        response.setTopPanel(genSearchPanelData(request));
        
        response.setProducts(uiProducts);
        
        return response;
    }

    /**
     * 处理新好评数
     * @param goodCommentToAll
     * @return
     */
    private static String fillNewGoodComment(Integer goodCommentToAll) {
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
                return val.substring(0,val.length()-1)+"."+val.substring(val.length()-1,val.length())+"w+";
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
     * 设置搜索结果的标签，优先展示一起拼
     */
    private List<LabelTag> getPcSearchLabels(List<String> searchLabelList, boolean yqp){
        List<LabelTag> result = new ArrayList<>();
        if (yqp){
            LabelTag showLabel = new LabelTag();
            showLabel.setContent("一起拼");
            showLabel.setColor("#FFFFFF");
            showLabel.setTextColor("#FB4C81");
            showLabel.setRoundColor("#FB4C81");
            result.add(showLabel);
        }
        if (searchLabelList != null && searchLabelList.size() > 0) {
            for (String label : searchLabelList) {
                if (label.equals("必粉最爱")) {
                    LabelTag showLabel = new LabelTag();
                    showLabel.setContent("必粉最爱");
                    showLabel.setColor("#AB7FD1");
                    showLabel.setTextColor("#FFFFFF");
                    showLabel.setRoundColor("3AB7FD1");
                    result.add(showLabel);
                } else if (label.equals("爆品")) {
                    LabelTag showLabel = new LabelTag();
                    showLabel.setContent("爆品");
                    showLabel.setColor("#AB7FD1");
                    showLabel.setTextColor("#FFFFFF");
                    showLabel.setRoundColor("#AB7FD1");
                    result.add(showLabel);
                } else if (label.equals("精选")) {
                    LabelTag showLabel = new LabelTag();
                    showLabel.setContent("精选");
                    showLabel.setColor("#AB7FD1");
                    showLabel.setTextColor("#FFFFFF");
                    showLabel.setRoundColor("#AB7FD1");
                    result.add(showLabel);
                } else if (label.equals("1天生产")) {
                    LabelTag showLabel = new LabelTag();
                    showLabel.setContent("1天生产");
                    showLabel.setColor("#D6B98C");
                    showLabel.setTextColor("#FFFFFF");
                    showLabel.setRoundColor("#D6B98C");
                    result.add(showLabel);
                } else if (label.equals("2天生产")) {
                    LabelTag showLabel = new LabelTag();
                    showLabel.setContent("2天生产");
                    showLabel.setColor("#D6B98C");
                    showLabel.setTextColor("#FFFFFF");
                    showLabel.setRoundColor("#D6B98C");
                    result.add(showLabel);
                } else if (label.equals("3天生产")) {
                    LabelTag showLabel = new LabelTag();
                    showLabel.setContent("3天生产");
                    showLabel.setColor("#D6B98C");
                    showLabel.setTextColor("#FFFFFF");
                    showLabel.setRoundColor("#D6B98C");
                    result.add(showLabel);
                } else if (label.equals("签名定制")) {
                    LabelTag showLabel = new LabelTag();
                    showLabel.setContent("签名定制");
                    showLabel.setColor("#D6B98C");
                    showLabel.setTextColor("#FFFFFF");
                    showLabel.setRoundColor("#D6B98C");
                    result.add(showLabel);
                } else if (label.equals("新品")) {
                    LabelTag showLabel = new LabelTag();
                    showLabel.setContent("新品");
                    showLabel.setColor("#D6B98C");
                    showLabel.setTextColor("#FFFFFF");
                    showLabel.setRoundColor("#D6B98C");
                    result.add(showLabel);
                }
            }
        }

        return result;
    }

    private SearchPanel genSearchPanelData(RequestBlock request) {
    	SearchPanel panel = new SearchPanel();
    	
    	String panelConfigCache = redisUtil.getString(SEARCH_PANEL_CONFIG);
    	if (Strings.isNullOrEmpty(panelConfigCache)) {
    		  return panel; // 有默认值
    	}
    	
    	JSONObject panelConfig = JSONObject.parseObject(panelConfigCache);
    	
    	String showStyle = panelConfig.getString("showStyle");
    	panel.setShowStyle(Strings.isNullOrEmpty(showStyle) ? "double" : showStyle);
    	
    	String onoff = panelConfig.getString("onOff");
    	panel.setOnOff(Strings.isNullOrEmpty(onoff) ? "off" : onoff);
    	
    	if ("off".equals(panel.getOnOff())) {
    		return panel;
    	} else if ("OFF".equals(request.getStringFlag("sflag_search_panelon"))) {// 面板开关实验
    		panel.setOnOff("off");
    		return panel;
    	}
    	
    	JSONArray orderByConfig = panelConfig.getJSONArray("orderByList");
    	if (orderByConfig == null || orderByConfig.size() == 0) {
    		return panel;
    	}
    	
    	List<SearchOrderBy> returnOrderBys = Lists.newArrayList();
    	for (int i = 0; i < orderByConfig.size(); i++) {
    		String orderByName = orderByConfig.getString(i);
    		returnOrderBys.add(SearchOrderByConsts.getSearchOrderBy(orderByName));
    	}
    	panel.setOrderByList(returnOrderBys);
    	
    	return panel;
	}

	private List<ASProduct> selectByRandom(List<ASProduct> candidates,
			int destNum) {
    	List<ASProduct> tmp = new ArrayList<>(candidates);
    	List<ASProduct> result = new ArrayList<ASProduct>();
    	
    	if (candidates.size() <= destNum) {
    		result.addAll(tmp);
    	} else {
    		Random random = new Random();
    		for (int i = 0; i < destNum; i ++) {
    			int index = random.nextInt(tmp.size());
        		result.add(tmp.get(index));
        		tmp.remove(index);
    		}
    	}
    	
    	return result;
	}

	public static Float getPrice(RequestBlock request, ASProduct product) {
        /** APP **/
        if( "ios".equals(request.getPlatform()) || "android".equals(request.getPlatform()) ){
            if( product.getSaleMode() == 2 ) {   // 团购方式售卖
                // IOS和Android的拼团新版本，拼团商品按照拼团的形式展示
                if( ("ios".equals(request.getPlatform()) && request.getAvn()>=CommonConstant.IOS_GROUP_VERSION) 
                        || ("android".equals(request.getPlatform()) && request.getAvn()>=CommonConstant.ANDROID_GROUP_VERSION) ) { 
                    return product.getGroupPrice();
                // 旧的APP版本、M站、PC按照普通商品进行展示
                } else {
                    return product.getPrice();
                }
            } else {
                return product.getPrice();
            }
        // PC、M返回链接
        } else if( "mweb".equals( request.getPlatform()) ) {
            if( product.getSaleMode() == 2 ) {   // 团购方式售卖
                return product.getGroupPrice();
            } else {
                return product.getPrice();
            }
        } else if( "pc".equals( request.getPlatform()) ) {
            return product.getPrice();
        } else {
            return product.getPrice();
        }
    }
    
    /**
     * 根据suID组装跳转url
     * @param suId
     * @return
     */
    public static String getRedirectUrlBySuId(RequestBlock request, Integer saleMode, String suId, Integer pos, Integer isShowIcon) {
        String trackParam = String.format("sid=%s&pos=%d", request.getSid(), pos);
        /** APP返回路由表 **/
        if( "ios".equals(request.getPlatform()) || "android".equals(request.getPlatform()) ){
            if( saleMode == 2 ) {   // 团购方式售卖
                // IOS和Android的拼团新版本，拼团商品按照拼团的形式展示
                if( ("ios".equals(request.getPlatform()) && request.getAvn()>=CommonConstant.IOS_GROUP_VERSION) 
                        || ("android".equals(request.getPlatform()) && request.getAvn()>=CommonConstant.ANDROID_GROUP_VERSION) ) { 
                    return GROUP_PRODUCT_URL_PREFIX + suId + "&"+trackParam;
                // 旧的APP版本、M站、PC按照普通商品进行展示
                } else {
                    return L_PRODUCT_URL_PREFIX + suId+ "&"+trackParam;
                }
            } else if (isShowIcon == 3){ // 同事一起拼
            	// 新的APP版本按照同事一起拼商品展示
            	if( ("ios".equals(request.getPlatform()) && request.getAvn()>=CommonConstant.IOS_TOGETHER_VERSION) 
                        || ("android".equals(request.getPlatform()) && request.getAvn()>=CommonConstant.ANDROID_TOGETHER_VERSION) ) { 
                    return TOGETHER_PRODUCT_URL_PREFIX + suId + "&"+trackParam;
                // 旧的APP版本按照普通商品进行展示
                } else {
                    return L_PRODUCT_URL_PREFIX + suId+ "&"+trackParam;
                }
            } else {
                if(suId.endsWith("0")) {  //高模
                    return H_PRODUCT_URL_PREFIX + suId+ "&"+trackParam;
                }else if(suId.endsWith("1") || suId.endsWith("2")) {//低模或无模
                    return L_PRODUCT_URL_PREFIX + suId+ "&"+trackParam;
                }
            }
        // PC、M返回链接
        } else if( "mweb".equals( request.getPlatform()) ) {
            return "https://m.biyao.com/products/"+suId+".html"+ "?"+trackParam;
        } else if( "pc".equals( request.getPlatform()) ) {
            return "https://www.biyao.com/products/"+suId+"-0.html"+ "?"+trackParam;
        } else if ( "miniapp".equals( request.getPlatform())) { // 小程序和m站一致
        	return "https://m.biyao.com/products/"+suId+".html"+ "?"+trackParam;
        }
        
    	return "";
	}

    /**
     * 有错误发生时，或者搜索无结果时，组装托底数据
     */
    private UIResponse createFallbackResult( RequestBlock request ) {
        
        // 从Redis里获取周排行TOP50
        SearchFallback searchFallback = commonCache.getSearchFallback();
        if( searchFallback == null ) {
            searchFallback = new SearchFallback();
        }

        boolean showYiqipinIcon = getStatusOfYiqipinIcon(request);
        
        int basePosition = 0;
        
        List<ASProduct> asProducts = searchFallback.getProducts();
        List<UIProduct> uiProducts = new ArrayList<UIProduct>(asProducts.size());
        for( int i = 0; i < asProducts.size(); i++ ) {
        	if (uiProducts.size() >= FALLBACK_PRODUCT_NUM) {
        		break;
        	}
        	
        	// 小程序过滤高模商品
        	if ("miniapp".equals(request.getPlatform()) && asProducts.get(i).getSuId().endsWith("0")) {
        		continue;
        	}
        	
            ASProduct asProduct = asProducts.get(i);
            UIProduct uiProduct = new UIProduct();
            
            uiProduct.setPosition( basePosition + i );
            uiProduct.setImage( asProduct.getImage() );
            uiProduct.setName( asProduct.getShortTitle() );
            uiProduct.setPrice( asProduct.getPrice() );
            uiProduct.setSuId( asProduct.getSuId() );
            uiProduct.setIsShowIcon((showYiqipinIcon && asProduct.getActivities() != null && asProduct.getActivities().contains(CommonConstant.YIQIPIN_ACTIVITY)) ? 3 : 0);    // 左上角提示类型。0没有，1新品 2 团购  3 一起拼    托底只管一起拼图标是否展示
            uiProduct.setRedirectUrl(getRedirectUrlBySuId(request, asProduct.getSaleMode(), asProduct.getSuId()+"", uiProduct.getPosition(), uiProduct.getIsShowIcon()));
            uiProduct.setScore(0F);
            
            uiProducts.add( uiProduct );
        }

        // 组装返回结果
        UIResponse response = new UIResponse();
        response.setQuery(request.getQuery());
        response.setSid(request.getSid());
        response.setPageCount(1);
        response.setPageIndex(1);
        response.setResultType(UIResultType.WEEK_HOT);
        response.setProducts(uiProducts);
        
        return response;
    }
    
    /**
     * 判断当前用户能否展示一起拼标识
     */
    private boolean getStatusOfYiqipinIcon(RequestBlock request) {
    	// 未登录的用户不展示
    	if (request.getUid() == null || request.getUid() <= 0) {
    		return false;
    	}
    	
    	// 没有首单的用户不展示
    	Result<List<BFirstOrderInfoDTO>> rpcResult = null;
    	try {
    		rpcResult = orderBaseService.firstPaidOrderByCustomerId(Sets.newHashSet(request.getUid().longValue()));
    	} catch (Exception e) {
    		logger.error("[严重异常]从border服务查询首单信息时发生异常，uid:{}" ,request.getUid(), e);
    		return false;
    	}
    	
    	if (!rpcResult.isSuccess()) {
    		return false;
    	}
    	
    	if (rpcResult.getObj().size() == 0 || rpcResult.getObj().get(0).getOrderId() == null) {
    		return false;
    	}
    	
    	return true;
	}

	/**
     * @param pageSize : 页面大小
     * @param itemTotal： 总记录数
     * @return 总页数
     */
    public static int calPageCount( int pageSize, int itemTotal ) {
        int pageCount = itemTotal / pageSize;
        if( itemTotal % pageSize != 0 ) {
            pageCount += 1;
        }
        return pageCount;
    }

}
