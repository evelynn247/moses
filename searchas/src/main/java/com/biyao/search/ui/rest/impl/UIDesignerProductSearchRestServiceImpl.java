package com.biyao.search.ui.rest.impl;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.alibaba.dubbo.rpc.protocol.rest.support.ContentType;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.biyao.dclog.service.DCLogger;
import com.biyao.search.as.service.ASDesignerProductSearchService;
import com.biyao.search.as.service.model.request.ASSearchRequest;
import com.biyao.search.as.service.model.response.ASSearchResponse;
import com.biyao.search.common.constant.SearchLimit;
import com.biyao.search.common.constant.SearchStatus;
import com.biyao.search.common.model.ASDesignerProduct;
import com.biyao.search.common.model.HttpResult;
import com.biyao.search.common.model.RPCResult;
import com.biyao.search.common.model.Status;
import com.biyao.search.ui.constant.UIResultType;
import com.biyao.search.ui.model.RequestBlock;
import com.biyao.search.ui.model.UIDesignerProduct;
import com.biyao.search.ui.rest.UIDesignerProductSearchRestService;
import com.biyao.search.ui.rest.impl.module.ModInitRequest;
import com.biyao.search.ui.rest.impl.module.ModRecordLog;
import com.biyao.search.ui.rest.impl.module.ModResponse;
import com.biyao.search.ui.rest.impl.module.UIModule;
import com.biyao.search.ui.rest.request.UIRequest;
import com.biyao.search.ui.rest.response.UIDesignerProductResponse;

@Path("/designerp")
public class UIDesignerProductSearchRestServiceImpl implements UIDesignerProductSearchRestService {
    
    public static  Logger logger = LoggerFactory.getLogger(UIDesignerProductSearchRestServiceImpl.class);
    private static DCLogger remoteLogger = DCLogger.getLogger("designer_search_log");
    
    @Autowired
    private  ASDesignerProductSearchService asSearchService;
    
    /** 参数校验、安全、重写参数 **/
    private UIModule modInitRequest = new ModInitRequest(); 
    
    private static final String DESIGNER_PRODUCT_URL = "biyao://product/designer/goodsDetail";

    @Path("/")
    @POST
    @Produces({ ContentType.APPLICATION_JSON_UTF_8 })
    public HttpResult<UIDesignerProductResponse> search(String requestJson) {
        logger.info("设计师商品搜索请求参数，request="+requestJson);
        /**
         *  初始化Request Block
         */
        UIRequest uiRequest = null;
        try {
            uiRequest = JSON.parseObject(requestJson, UIRequest.class);
        } catch (Exception e) {
            logger.error("[严重异常]设计师商品搜索异常，requestJson = {}", requestJson, e);
        }
        if( uiRequest == null ) {
            uiRequest = new UIRequest();
        }

        RequestBlock request = new RequestBlock();
        request.initWithUIRequest(uiRequest);
        
        /**
         *  参数校验、重写参数等
         */
        Status status = modInitRequest.run( request );
        request.setStatus(status);
        if (!SearchStatus.OK.equals(status)) {  // 返回失败
            UIDesignerProductResponse uiResponse = createFallbackResult(request);
            printLog(request, new ASSearchResponse<ASDesignerProduct>(), uiResponse );
            return new HttpResult<UIDesignerProductResponse>(uiResponse);
        }
        
        // 100条数据一次返回即可  20180524 ASBS拆分时       by luozhuo
        request.setPageSize(100);
        
        /**
         * 请求AS
         */
        ASSearchResponse<ASDesignerProduct> asResponse = requestAS( request );
        if( asResponse == null ) { // 返回失败
            UIDesignerProductResponse uiResponse = createFallbackResult(request);
            printLog(request, new ASSearchResponse<ASDesignerProduct>(), uiResponse );
            return new HttpResult<UIDesignerProductResponse>(createFallbackResult(request));
        }
        
        /**
         * 组装返回结果
         */
        status = request.getStatus();
        UIDesignerProductResponse uiResponse = null;
        if( status.equals( SearchStatus.OK ) && asResponse.getHitTotal() > 0 ) {
            uiResponse = createNormalResult(request, asResponse);
        } else {
            uiResponse = createFallbackResult(request);
        }
        
        /**
         * 打印日志
         */
        printLog(request, asResponse, uiResponse );
        
        return new HttpResult<UIDesignerProductResponse>(uiResponse);
    }
    
    
    /**
     * 搜索正常时，组装返回结果
     */
    private UIDesignerProductResponse createNormalResult( RequestBlock request, ASSearchResponse<ASDesignerProduct> asResponse ) {
        
        // 根据AS的返回结果获取返回数据
        int basePosition = request.getPageSize() * (request.getPageIndex()-1);
        
        List<ASDesignerProduct> asDesignerProducts = asResponse.getResult();
        List<UIDesignerProduct> uiDesignerProducts = new ArrayList<UIDesignerProduct>(asDesignerProducts.size());
        for( int i = 0; i < asDesignerProducts.size(); i++ ) {
            ASDesignerProduct asDesignerProduct = asDesignerProducts.get(i);
            UIDesignerProduct uiDesignerProduct = new UIDesignerProduct();
            uiDesignerProduct.setPrice( asDesignerProduct.getPrice() );
            uiDesignerProduct.setName( asDesignerProduct.getName() );
            uiDesignerProduct.setImage( asDesignerProduct.getImage() );
            int pos = basePosition + i;  // 位置
            uiDesignerProduct.setRedirectUrl(
                    String.format("%s?goodsID=%s&designID=%s&pos=%d", DESIGNER_PRODUCT_URL, 
                            asDesignerProduct.getSuId(), asDesignerProduct.getDesignId(), pos ) );
            
            uiDesignerProducts.add( uiDesignerProduct );
        }

        // 组装返回结果
        UIDesignerProductResponse response = new UIDesignerProductResponse();
        response.setQuery(request.getQuery());
        response.setSid(request.getSid());
        response.setPageCount( ModResponse.calPageCount(request.getPageSize(), asResponse.getHitTotal()) );
        response.setPageIndex( request.getPageIndex() );
        response.setResultType(UIResultType.NORMAL_SEARCH);
        
        response.setProducts(uiDesignerProducts);
        return response;
    }
    
    
    /**
     * 有错误发生时，或者搜索无结果时，组装托底数据
     */
    private UIDesignerProductResponse createFallbackResult( RequestBlock request ) {
        
        UIDesignerProductResponse response = new UIDesignerProductResponse();
        response.setQuery(request.getQuery());
        response.setSid(request.getSid());
        response.setPageCount(0);
        response.setPageIndex(0);
        response.setProducts( new ArrayList<UIDesignerProduct>() );
        response.setResultType(UIResultType.NO_RESULT);
        
        return response;
    }
    
    /**
     * 向AS发起请求，成功返回AS结果，失败返回null，失败原因记录在requestBlock中
     * @param request
     */
    private ASSearchResponse<ASDesignerProduct> requestAS( RequestBlock request ){
        /**
         *  超出条数限制
         */
        if( (request.getPageIndex()-1) * request.getPageSize() >= SearchLimit.MAX_HIT_COUNT ) {
            ASSearchResponse<ASDesignerProduct> asResponse= new ASSearchResponse<ASDesignerProduct>();
            asResponse.setHitTotal( SearchLimit.MAX_HIT_COUNT );
            request.setStatus( SearchStatus.OK );
            return asResponse;
        }
        
        /**
         *  向AS发起请求
         */
        ASSearchRequest asRequest = new ASSearchRequest();
        asRequest.setPageIndex( request.getPageIndex() );
        asRequest.setPageSize( request.getPageSize() );
        asRequest.setQuery( request.getQuery() );
        
        RPCResult<ASSearchResponse<ASDesignerProduct>> asResult = null;
        
        long start = System.currentTimeMillis();
        try {
            asResult = asSearchService.search(asRequest);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // AS请求消耗的时间
        request.setAsTookTime( (int)(System.currentTimeMillis()-start) );
        
        if( asResult == null ) {
            request.setStatus( SearchStatus.AS.UNKNOWN );
            return null;
        }
        
        if( ! SearchStatus.OK.equals( asResult.getStatus() ) ) {
            request.setStatus( asResult.getStatus() );
            return null;
        }
        
        ASSearchResponse<ASDesignerProduct> asResponse= asResult.getData();
        
        /**
         *  返回AS的返回结果
         */
        if( asResponse.getHitTotal() > SearchLimit.MAX_HIT_COUNT ) {
            asResponse.setHitTotal( SearchLimit.MAX_HIT_COUNT );
        }
        
        request.setStatus( SearchStatus.OK );
        return asResponse;
    }
    
    
    /**
     * 打印日志
     */
    private void printLog (RequestBlock request, ASSearchResponse<ASDesignerProduct> asResponse, UIDesignerProductResponse uiResponse){
        StringBuilder sb = new StringBuilder(1024);
        
        JSONArray results = new JSONArray();
        List<ASDesignerProduct> products = asResponse.getResult();
        for( ASDesignerProduct product : products ) {
            JSONObject result = new JSONObject();
            result.put("suid", product.getSuId() );
            result.put("dspid", product.getDesignId() );
            result.put("dspn", product.getName() );
            results.add(result);
        }
        
        String uid = "";
        if( request.getUid() > 0 ){
            uid = request.getUid() + "";
        }
        sb.append("lt=search-designerp");
        sb.append("&lv=1.0");
        sb.append("&tp=dp"); // 搜索design-product
        sb.append("&pf="); sb.append( request.getPlatform() );
        sb.append("&uu="); sb.append( request.getUuid() );
        sb.append("&u="); sb.append( uid );
        sb.append("&av="); sb.append( request.getAppVersion() );
        sb.append("&d="); sb.append( request.getDevice() );
        sb.append("&osv="); sb.append( request.getOsVersion() );
        sb.append("&dw="); sb.append( request.getDeviceWidth() );
        sb.append("&dh="); sb.append( request.getDeviceHeight() );
        sb.append("&ip="); sb.append( request.getIp() );
        sb.append("&reqt="); sb.append( request.getRequestTime() );
        sb.append("&sid="); sb.append( request.getSid() );
        sb.append("&q="); sb.append( request.getQuery() );
        sb.append("&uit="); sb.append( System.currentTimeMillis() - request.getRequestTime() );
        sb.append("&ast="); sb.append( request.getAsTookTime() );
        sb.append("&bst="); sb.append( asResponse.getBsTookTime() );
        sb.append("&est="); sb.append( asResponse.getEsTookTime() );
        sb.append("&code="); sb.append( request.getStatus().getCode() );
        sb.append("&rttype="); sb.append( uiResponse.getResultType() );
        sb.append("&result="); sb.append( results.toJSONString() );
        
        String log = sb.toString();
        remoteLogger.printDCLog(log);
    }
}
