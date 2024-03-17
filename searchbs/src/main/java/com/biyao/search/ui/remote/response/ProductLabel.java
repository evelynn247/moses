package com.biyao.search.ui.remote.response;

import java.io.Serializable;

public class ProductLabel implements Serializable{
	private static final long serialVersionUID = -11696840076707620L;
	
	/**
	 * 标签文本内容
	 */
	private String content;
	
	/**
	 * 标签背景颜色码
	 */
	private String color;
	
	/**
	 * 标签文本颜色码
	 */
	private String textColor;
	
	/**
	 * 标签边框颜色码
	 */
	private String roundColor;

	/**
	 * 特殊标签类型  1：造物   2：津贴   默认是0，非特殊标签
	 */
	private int type = 0;


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

	public String getTextColor() {
		return textColor;
	}

	public void setTextColor(String textColor) {
		this.textColor = textColor;
	}

	public String getRoundColor() {
		return roundColor;
	}

	public void setRoundColor(String roundColor) {
		this.roundColor = roundColor;
	}

	public int getType() { return type; }

	public void setType(int type) { this.type = type; }
	
}
