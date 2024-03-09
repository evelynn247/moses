package com.biyao.moses.model;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

/**
 * 单排模板-样式2（专题）
 * 
 * @author monkey
 * @date 2018年8月30日
 */
@Setter
@Getter
public class SpecialTemplateInfo extends ImageTemplateInfo implements Serializable {

	private static final long serialVersionUID = 1L;

	// ¥123元起
	private String priceStr;
	// 12300.00
	private String priceCent;

	private String priceColor;

}
