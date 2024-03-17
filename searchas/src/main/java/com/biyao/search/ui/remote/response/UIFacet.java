package com.biyao.search.ui.remote.response;

import java.util.ArrayList;
import java.util.List;

public class UIFacet {
	/**
	 * 筛选条件标题
	 */
	private String title;
	
	/**
	 * 筛选条件类型
	 * single - 单选
	 * multi - 多选
	 */
	private String type = "multi";
	
	/**
	 * 筛选条件key
	 */
	private String key;
	
	/**
	 * 筛选条件可选值
	 */
	private List<UIFacetValue> values = new ArrayList<UIFacetValue>();

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public List<UIFacetValue> getValues() {
		return values;
	}

	public void setValues(List<UIFacetValue> values) {
		this.values = values;
	}
	
	
	
}
