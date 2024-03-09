package com.biyao.moses.model;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

/**
 * 双排商品样式（首页feed流）
 * 
 * @author monkey
 * @date 2018年8月30日
 */
@Setter
@Getter
public class DoubleRowListTemplateInfo extends ProductTemplateInfo implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 直播状态标签
	 * “0”：不展示标签， “1”：展示直播中标签
	 */
	private String liveStatus;
}