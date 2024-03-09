package com.biyao.moses.params;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * 模板参数校验
 * 
 * @Description
 * @author zyj
 * @Date 2018年9月27日
 */
@Setter
@Getter
public class TemplateParam {

	@NotNull(message = "templateType不能为空")
	private int templateType;

	@NotBlank(message = "templateName不能为空")
	private String templateName = "";

	private Object[] data;
	
	private String tid;

	// 是否为动态数据，标题,空白，分割线模板为false(type=1,11,12)，其他都为true
	@NotNull(message = "dynamic不能为空")
	private Boolean dynamic;

	// 此类型由需求方和算法提出，例如新增低价排行数据源。将此数据源维护到svn
	@NotBlank(message = "dataSourceType不能为空")
	private String dataSourceType;
	
}
