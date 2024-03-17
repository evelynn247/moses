package com.biyao.search.ui.remote.response;

import java.io.Serializable;

public class TitleText implements Serializable{
	/**
	 * 文本内容
	 */
	private String content;
	
	/**
	 * 文本颜色码
	 */
	private String color;
	
	/**
	 * 是否可缩写
	 */
	private Integer changeable = 0;
	
	public TitleText(String content, String color, Integer changeable) {
		super();
		this.content = content;
		this.color = color;
		this.changeable = changeable;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}

	public Integer getChangeable() {
		return changeable;
	}

	public void setChangeable(Integer changeable) {
		this.changeable = changeable;
	}
	
	
}
