package com.biyao.moses.model;

import lombok.Getter;
import lombok.Setter;

/**
 * 商品模板进阶
 * 带附加信息的商品模板
 * @author monkey
 * @date 2018年8月30日
 */
@Setter
@Getter
public class ProductTemplateInfo extends BaseProductTemplateInfo {

	private static final long serialVersionUID = 1L;

	// 0不展示 1新品 2拼团 3一起拼
	private String isShowIcon;
	// 朋友买过及好评的拼接字段
	private String thirdContent;
	// 制造商背景或卖点等展示位置的文字内容
	private String subtitle;

}