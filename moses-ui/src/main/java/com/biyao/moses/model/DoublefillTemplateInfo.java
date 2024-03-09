package com.biyao.moses.model;

import java.io.Serializable;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

import com.biyao.moses.model.template.TemplateInfo;

/**
 * 双排模板-样式1（左1右1）
 * 
 * @author monkey
 * @date 2018年8月30日
 */
@Setter
@Getter
public class DoublefillTemplateInfo extends TemplateInfo implements Serializable {

	private static final long serialVersionUID = 1L;

	// 图片列表
	private List<String> images;
	
	//webp图片列表
	private List<String> imagesWebp;

}
