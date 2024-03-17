package com.biyao.search.ui.remote.response;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Template implements Serializable{
	/**
	 * 模板类型
	 * textButton-文字按钮模板
	 * image-图片模板
	 * store-店铺模板
	 * singleProduct-商品单排
	 * doubleProduct-商品双排
	 */
	private String type;
	
	/**
	 * 当前模板的数据
	 */
	private List<TemplateData> data = new ArrayList<TemplateData>();

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public List<TemplateData> getData() {
		return data;
	}

	public void setData(List<TemplateData> data) {
		this.data = data;
	}
}
