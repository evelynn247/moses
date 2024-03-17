package com.biyao.search.ui.remote.response;

public class ImageTemplateData extends TemplateData{
	/**
	 * 图片地址
	 */
	private String imageUrl;
	
	/**
	 * 跳转信息
	 */
	private TemplateRoute route;

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public TemplateRoute getRoute() {
		return route;
	}

	public void setRoute(TemplateRoute route) {
		this.route = route;
	}
	
	
}