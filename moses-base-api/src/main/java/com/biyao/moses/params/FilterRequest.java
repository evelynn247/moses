package com.biyao.moses.params;

import java.io.Serializable;
import java.util.List;

import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.params.ProductInfo;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class FilterRequest implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	//未过滤的原始数据
	private List<TotalTemplateInfo> sourceList;
	//商品详情
	private List<ProductInfo> productInfoList;

	private String siteId;
}
