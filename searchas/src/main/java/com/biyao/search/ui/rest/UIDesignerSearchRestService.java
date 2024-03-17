package com.biyao.search.ui.rest;

import com.biyao.search.common.model.HttpResult;
import com.biyao.search.ui.rest.response.UIDesignerResponse;

public interface UIDesignerSearchRestService {
    
	public HttpResult<UIDesignerResponse> search( String request );
	
}
