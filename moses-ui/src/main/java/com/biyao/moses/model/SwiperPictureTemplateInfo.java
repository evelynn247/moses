package com.biyao.moses.model;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

import com.biyao.moses.model.template.TemplateInfo;

/**
 * 轮播图模板
 * 
 * @author monkey
 * @date 2018年8月30日
 */
@Setter
@Getter
public class SwiperPictureTemplateInfo extends TemplateInfo implements Serializable {

	private static final long serialVersionUID = 1L;

	private String imageUrl;
	
	private String imageUrlWebp;

}