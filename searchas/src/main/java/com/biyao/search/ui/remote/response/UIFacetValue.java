package com.biyao.search.ui.remote.response;

public class UIFacetValue {
	/**
	 * 筛选值描述
	 */
	private String desc;
	
	/**
	 * 筛选值实际code
	 */
	private String code;
	
	/**
	 * 筛选值是否被选中
	 * 1 - 是
	 * 0 - 否
	 */
	private Integer selected = 0;

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public Integer getSelected() {
		return selected;
	}

	public void setSelected(Integer selected) {
		this.selected = selected;
	}
	
	
}
