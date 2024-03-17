package com.biyao.search.ui.rest;

import java.util.List;

import com.biyao.search.common.model.HttpResult;
import com.biyao.search.ui.home.model.HomeResponse;
import com.biyao.search.ui.home.model.HomeTemplate;
import com.biyao.search.ui.home.model.app.FeedPageData;
import com.biyao.search.ui.home.model.app.HomeFloor;
import com.biyao.search.ui.rest.response.UIHiResponse;
import com.biyao.search.ui.rest.response.UIResponse;

public interface UISearchRestService {
	
    public HttpResult<UIHiResponse> hi( );
    
	public HttpResult<UIResponse> search( String request );
}
