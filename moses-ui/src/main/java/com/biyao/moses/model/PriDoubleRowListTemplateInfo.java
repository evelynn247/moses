package com.biyao.moses.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 *特权金双排样式
 */
@Setter
@Getter
public class PriDoubleRowListTemplateInfo extends ProductTemplateInfo implements Serializable {

	private static final long serialVersionUID = 1L;
	/**
	 * 抵扣金额
	 */
	private String priDeductAmount;

	/**
	 * 直播状态标签
	 * “0”：不展示标签， “1”：展示直播中标签
	 */
	private String liveStatus;
}