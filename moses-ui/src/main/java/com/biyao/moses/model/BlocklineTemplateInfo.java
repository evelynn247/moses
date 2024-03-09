package com.biyao.moses.model;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

import com.biyao.moses.model.template.TemplateInfo;

/**
 * 空白行模板
 * 
 * @author monkey
 * @date 2018年8月30日
 */
@Setter
@Getter
public class BlocklineTemplateInfo extends TemplateInfo implements Serializable {

	private static final long serialVersionUID = 1L;

	// 空白行高度 20
	private String height = "20";

	private String color;

}
