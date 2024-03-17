package com.biyao.search.ui.rest.impl;

import com.alibaba.dubbo.rpc.protocol.rest.support.ContentType;
import com.alibaba.fastjson.JSON;
import com.biyao.search.as.service.ASSearchService;
import com.biyao.search.as.service.model.response.ASHiResponse;
import com.biyao.search.common.constant.SearchStatus;
import com.biyao.search.common.model.HttpResult;
import com.biyao.search.common.model.RPCResult;
import com.biyao.search.ui.exp.UIExperimentSpace;
import com.biyao.search.ui.model.RequestBlock;
import com.biyao.search.ui.rest.UISearchRestService;
import com.biyao.search.ui.rest.impl.module.UIModule;
import com.biyao.search.ui.rest.request.UIRequest;
import com.biyao.search.ui.rest.response.UIHiResponse;
import com.biyao.search.ui.rest.response.UIResponse;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("/")
public class UISearchRestServiceImpl implements UISearchRestService {
    @Autowired
    private  ASSearchService asSearchService;
    
    @Autowired
    private UIExperimentSpace experimentSpace;

    /**
     * 参数校验、安全、重写参数
     */
    @Autowired
    private UIModule modInitRequest;
    /**
     * 请求AS数据
     */
    @Autowired
    private UIModule modRequestAs;
    /**
     * 组装返回结果
     */
    @Autowired
    private UIModule modResponse;
    /**
     * 打日志
     */
    @Autowired
    private UIModule modRecordLog;

    @Path("/search")
    @POST
    @Produces({ ContentType.APPLICATION_JSON_UTF_8 })
    @Override
    public HttpResult<UIResponse> search(String requestJson) {

        UIRequest uiRequest = null;
        try {
            uiRequest = JSON.parseObject(requestJson, UIRequest.class);
        } catch (Exception e) {
            System.err.println("搜索请求参数转换时发生异常：" + requestJson );
            e.printStackTrace();
        }
        
        if( uiRequest == null ) {
            uiRequest = new UIRequest();
        }

        // 初始化 Request Block
        RequestBlock request = new RequestBlock();
        request.initWithUIRequest(uiRequest);
        
        // abtest
        request = experimentSpace.divert(request);

        // 各模块进行处理
        UIModule[] modules = { modInitRequest, modRequestAs };
        for (UIModule module : modules) {
            request.setStatus( module.run(request) );
            if (!SearchStatus.OK.equals(request.getStatus())) {
                break;
            }
        }
        
        // 组装返回结果
        modResponse.run(request);
        // 打印日志
        modRecordLog.run(request);

        return new HttpResult<UIResponse>( request.getUiResponse() );
    }
    
    /**
     * UI Hi接口
     */
    @Path("/hi")
    @GET
    @Produces({ ContentType.APPLICATION_JSON_UTF_8 })
    @Override
    public HttpResult<UIHiResponse> hi(){
        UIHiResponse uiHiResponse = new UIHiResponse();
        RPCResult<ASHiResponse> asResult = null;
        
        try {
            asResult = asSearchService.hi();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        if( asResult != null ){
            ASHiResponse asResponse= asResult.getData();
            uiHiResponse.setAsHiResponse( asResponse );
        }
        
        return new HttpResult<UIHiResponse>(uiHiResponse);
    }
    
}
