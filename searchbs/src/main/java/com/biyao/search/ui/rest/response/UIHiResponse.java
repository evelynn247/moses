package com.biyao.search.ui.rest.response;

import com.biyao.search.as.service.model.response.ASHiResponse;


/**
 * 搜索返回对象
 */
public class UIHiResponse {
	
    private String app = "Search-UI";
    private String version = "1.0.0";
    private ASHiResponse asHiResponse = new ASHiResponse();
    
    public String getApp() {
        return app;
    }
    public void setApp(String app) {
        this.app = app;
    }
    public String getVersion() {
        return version;
    }
    public void setVersion(String version) {
        this.version = version;
    }
    public ASHiResponse getAsHiResponse() {
        return asHiResponse;
    }
    public void setAsHiResponse(ASHiResponse asHiResponse) {
        this.asHiResponse = asHiResponse;
    }
}
