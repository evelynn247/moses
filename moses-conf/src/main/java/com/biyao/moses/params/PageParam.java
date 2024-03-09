package com.biyao.moses.params;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;

import lombok.Getter;
import lombok.Setter;

/**
 * page参数校验
 * @Description 
 * @author zyj
 * @Date 2018年9月27日
 */
@Setter
@Getter
public class PageParam {

	@NotEmpty(message = "blockList不能为空")
	private Object[] blockList;
	
	//@NotBlank(message = "pageName不能为空")
	private String pageName;
	
	private String pid;
 

}
