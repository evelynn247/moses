package com.biyao.search.ui.remote;

import javax.ws.rs.BeanParam;

import com.biyao.search.ui.remote.request.UISearchPageRequest;
import com.biyao.search.ui.remote.request.UISearchRequest;
import com.biyao.search.ui.remote.response.BlockData;
import com.biyao.search.ui.remote.response.HttpResult2;
import com.biyao.search.ui.remote.response.*;

import java.util.Map;

/**
 * 搜索中间页改版新接口  20180606
 * @author luozhuo
 *
 */
public interface UISearchService {
	HttpResult2<UISearchResponse> search(@BeanParam UISearchRequest reqeust);
	
	HttpResult2<BlockData> searchPage(@BeanParam UISearchPageRequest request);

	public Map<String,String> testonline();
}
