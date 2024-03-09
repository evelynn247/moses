package com.biyao.moses.model;

import java.io.Serializable;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * 带图片的模板信息类 类似首页大部分的topic模板
 * 
 * @author monkey
 * @date 2018年8月30日
 */
@Setter
@Getter
public abstract class ImageTemplateInfo extends BaseTemplateInfo implements Serializable {

	private static final long serialVersionUID = 1L;
	// 图片列表
	private List<String> images;
	//webp图片列表
	private List<String> imagesWebp;
}