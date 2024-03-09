package com.biyao.moses.params;

import javax.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * 批量创建页面参数
 * 
 * @Description
 * @author zyj
 * @Date 2018年9月27日
 */
@Setter
@Getter
public class BuildPageParam {

	@NotBlank(message = "pageNames不能为空")
	private String pageNames;

	@NotBlank(message = "templateType不能为空")
	private String templateType;

	@NotBlank(message = "expId不能为空")
	private String expId;
	
	@NotBlank(message = "pageCount不能为空")
	private String pageCount;
}
