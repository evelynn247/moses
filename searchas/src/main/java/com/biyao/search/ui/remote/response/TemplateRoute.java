package com.biyao.search.ui.remote.response;

import java.io.Serializable;

public class TemplateRoute implements Serializable {
	/**
	 * 跳转类型
	 * searchPage - 当前页直接发起搜索
	 * newPage - 跳转到新的页面
	 */
	private String type;
	
	/**
	 * 跳转路由
	 * 当type为newPage时才会有值
	 */
	private String routeUrl;
	
	/**
	 * 当前跳转类型为searchPage时有值，用来发起搜索
	 * 当type为searchPage时才会有值
	 */
	private String query;
	
	/**
	 * 附加跟踪参数，在跳转页面时添加于请求末尾
	 * 格式 k=v&k=v
	 */
	private String trackParam = "";

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getRouteUrl() {
		return routeUrl;
	}

	public void setRouteUrl(String routeUrl) {
		this.routeUrl = routeUrl;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public String getTrackParam() {
		return trackParam;
	}

	public void setTrackParam(String trackParam) {
		this.trackParam = trackParam;
	}
	
	
}
