package com.biyao.moses.model.template;

import java.io.Serializable;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * 区块模板,包含不同模板
 * 
 * @author monkey
 * @date 2018年8月30日
 */
@Setter
@Getter
public class Block<T> implements Serializable {

	private static final long serialVersionUID = 1L;

	private String bid;

	// 是否动态数据
	private boolean dynamic;

	// 是否feed流
	private boolean feed;

	// 包含模板列表
	private List<Template<T>> block;
	
	// 当前block下totalTemplateInfo的个数
	@Deprecated
	private Integer templateInfoNums;
	
	@Deprecated
	private boolean exp;
	
	@Deprecated
	private String expId;

}