package com.biyao.moses.model.template;

import java.io.Serializable;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * 模板类
 * 
 * @author monkey
 * @date 2018年8月30日
 */
@Setter
@Getter
public class Template<T> implements Serializable {

	private static final long serialVersionUID = 1L;
	// 模板id，作为当前tid应用哪些match实验的key
	private String tid;
	// 模板类型
	private Integer templateType;
	// 模板名称
	private String templateName;
	// 当前模板是否是动态模板。分割线，空白行都不是动态的
	private boolean dynamic;
	// 当前模板的数据源,如男装，女装，排行榜。
	// 此类型由需求方和算法提出，例如新增低价排行数据源。将此数据源维护到svn
	private String dataSourceType;
	// 当前模板填充的数据类型，如商品，簇，商家，模板
	//private Integer dataType;
	// 模板数据
	private List<T> data;
	
	@Deprecated
	private Integer dataSize;

}