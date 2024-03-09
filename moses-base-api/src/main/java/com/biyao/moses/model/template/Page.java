package com.biyao.moses.model.template;

import java.io.Serializable;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * 页面
 * 
 * @author monkey
 * @date 2018年8月30日
 */
@Setter
@Getter
public class Page<T> implements Serializable {

	private static final long serialVersionUID = 1L;
	
	//页面id，通过此id获取模板
	private String pid;
	
	//页面名称，展示使用
	private String pageName;
	
	//页面楼层数据
	private List<Block<T>> blockList;

}
