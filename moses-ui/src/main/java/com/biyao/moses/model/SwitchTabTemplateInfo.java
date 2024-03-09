package com.biyao.moses.model;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

import com.biyao.moses.model.template.TemplateInfo;

/**
 * tab模板
 * 
 * @author monkey
 * @date 2018年8月30日
 */
@Setter
@Getter
public class SwitchTabTemplateInfo extends TemplateInfo implements Serializable {

	private static final long serialVersionUID = 1L;

	private String mainTitle;

	private String mainColor;

	// 选中颜色
	private String selectColor;
	// 是否选中（0没选中，1选中）
	private String isSelect;

}