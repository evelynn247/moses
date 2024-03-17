package com.biyao.search.ui.rest.response;

import java.util.List;

import com.google.common.collect.Lists;

public class SearchPanel {
	/**
	 * 面板开关 on-展示 off-关闭
	 */
	private String onOff = "off";
	
	/**
	 * 排序项列表
	 */
	private List<SearchOrderBy> orderByList = Lists.newArrayList(SearchOrderByConsts.NORMAL, SearchOrderByConsts.SALE,SearchOrderByConsts.NEW, SearchOrderByConsts.PRICE);
	
	/**
	 * 默认展示风格 single-一行一个 double-一行两个
	 */
	private String showStyle = "double";

	public String getOnOff() {
		return onOff;
	}

	public void setOnOff(String onOff) {
		this.onOff = onOff;
	}

	public List<SearchOrderBy> getOrderByList() {
		return orderByList;
	}

	public void setOrderByList(List<SearchOrderBy> orderByList) {
		this.orderByList = orderByList;
	}

	public String getShowStyle() {
		return showStyle;
	}

	public void setShowStyle(String showStyle) {
		this.showStyle = showStyle;
	}
	
	
}
