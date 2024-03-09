package com.biyao.moses.model;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

import com.biyao.moses.model.template.TemplateInfo;

/**
 * 大部分可以用的模板base类 基于标题模板
 * 
 * @author monkey
 * @date 2018年8月30日
 */
@Setter
@Getter
public abstract class BaseTemplateInfo extends TemplateInfo implements Serializable {

	private static final long serialVersionUID = 1L;

	private String mainTitle;

	private String mainTitleColor;

	private String subtitle;

	private String subtitleColor;

}
