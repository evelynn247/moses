package com.biyao.search.ui.rest;

import com.biyao.search.common.model.HttpResult;
import com.biyao.search.ui.rest.response.UIDesignerProductResponse;

public interface UIDesignerProductSearchRestService {
    
	public HttpResult<UIDesignerProductResponse> search( String request );
	
}
