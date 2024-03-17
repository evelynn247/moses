package com.biyao.search.ui.remote.response;

import java.io.Serializable;

public class LinkButton implements Serializable {
	/**
	 * 当前按钮展示标题
	 */
	private String title;
	
	/**
	 * 跳转信息
	 */
	private TemplateRoute route;
	
	/**
	 * 字体颜色
	 */
	private String color="";

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public TemplateRoute getRoute() {
		return route;
	}

	public void setRoute(TemplateRoute route) {
		this.route = route;
	}

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}
	
	
}
