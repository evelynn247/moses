package com.biyao.search.ui.remote.response;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SearchTitle implements Serializable {
	/**
	 * 文本两侧展示图片格式
	0-	不展示
	1-	横线加斜线（如猜你喜欢）  ----// 猜你喜欢 //----
	2-	横线（如选购热点） ---- 热门搜索 ----
	3	有背景色
	 */
	private Integer picType = 0;
	
	/**
	 * 标题文本列表
	 */
	private List<TitleText> contents = new ArrayList<>();

	public Integer getPicType() {
		return picType;
	}

	public void setPicType(Integer picType) {
		this.picType = picType;
	}

	public List<TitleText> getContents() {
		return contents;
	}

	public void setContents(List<TitleText> contents) {
		this.contents = contents;
	}
	
	
}
