package com.biyao.moses.model.template;

import java.io.Serializable;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

/**
 * 模板信息类root父类,所有模板都有routerType，routerParams
 * 
 * @author monkey
 * @date 2018年8月30日
 */
@Setter
@Getter
public class TemplateInfo implements Serializable {

	private static final long serialVersionUID = 1L;
	// 跳转类型，见TemplateTypeEnum
	private Integer routerType;
	//跳转参数 
	private Map<String, String> routerParams;

}