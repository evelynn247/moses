package com.biyao.search.ui.rest.impl.module;

import java.text.DecimalFormat;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.biyao.dclog.service.DCLogger;
import com.biyao.search.common.constant.SearchStatus;
import com.biyao.search.common.model.Status;
import com.biyao.search.common.model.UIProduct;
import com.biyao.search.ui.model.RequestBlock;
import com.biyao.search.ui.rest.response.UIResponse;

/**
 * 打印日志（搜索请求日志、搜索展示日志）
 */
public class ModRecordLog implements UIModule {
    
    public static  Logger logger = LoggerFactory.getLogger(ModRecordLog.class);
    
    // 搜索请求日志和搜索展示日志
    public static  Logger requestLogger = LoggerFactory.getLogger("search_request");
    public static  Logger showLogger    = LoggerFactory.getLogger("search_show"); 
    
    //数据中心dclog
    private static DCLogger requestDCLogger = DCLogger.getLogger("search_request");
    private static DCLogger showDCLogger = DCLogger.getLogger("search_show");

    DecimalFormat df = new DecimalFormat("#.000000");
    
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
        // 新的搜索请求才会记录请求日志
        if( ! request.isScroll() ) { 
            printRequestLog(request);
        }
        // 检索结果日志
        printShowLog(request);
        
        return SearchStatus.OK;
    }
    
    /**
     * 请求日志
     */
    private void printRequestLog (RequestBlock request){
        StringBuilder sb = new StringBuilder(1024);
        
        String uid = "";
        if( request.getUid() > 0 ){
            uid = request.getUid() + "";
        }
        
        sb.append("lt=search-req");
        sb.append("&lv=1.0");
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
        sb.append("&pvid="); sb.append( request.getPvid() );
        sb.append("&ubid="); sb.append( request.getUbid() );
        sb.append("&qf="); sb.append(request.getQueryFrom());
        sb.append("&q="); sb.append( request.getQuery() );
        
        String log = sb.toString();
        requestLogger.info( log );
        requestDCLogger.printDCLog(log);
    }
    
    /**
     * 搜索展现日志
     */
    private void printShowLog (RequestBlock request){
        
        // 搜索结果
        JSONArray results = new JSONArray();
        UIResponse response = request.getUiResponse();
        List<UIProduct> products = response.getProducts();
        for( UIProduct product : products ) {
            JSONObject result = new JSONObject();
            result.put("suid", product.getSuId() );
            result.put("pos", product.getPosition() );
            result.put("score", product.getScore());
            result.put("algoScore", product.getAlgoScore() == null ? -999999.0 : df.format(product.getAlgoScore()));
            result.put("lables", product.getLables());
            results.add(result);
        }
        
        // 打印日志
        StringBuilder sb = new StringBuilder(1024);
        sb.append("lt=search-show");
        sb.append("&lv=1.0");
        sb.append("&sid="); sb.append( request.getSid() );
        sb.append("&pvid="); sb.append( request.getPvid() );
        sb.append("&ubid="); sb.append( request.getUbid() );
        sb.append("&uit="); sb.append( System.currentTimeMillis() - request.getRequestTime() );
        sb.append("&ast="); sb.append( request.getAsTookTime() );
        sb.append("&bst="); sb.append( request.getAsResponse().getBsTookTime() );
        sb.append("&est="); sb.append( request.getAsResponse().getEsTookTime() );
        sb.append("&showt="); sb.append( request.getRequestTime() );
        sb.append("&code="); sb.append( request.getStatus().getCode() );
        sb.append("&rttype="); sb.append( request.getUiResponse().getResultType() );
        sb.append("&odby=");sb.append(request.getOrderBy().getCode());
        sb.append("&panelOn=");sb.append(response.getTopPanel().getOnOff());
        sb.append("&result="); sb.append( results.toJSONString() );
        
        String log = sb.toString();
        showLogger.info( log );
        showDCLogger.printDCLog(log);
    }
}
