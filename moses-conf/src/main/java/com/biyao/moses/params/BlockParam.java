package com.biyao.moses.params;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * block参数校验 
 * @Description 
 * @Date 2018年9月27日
 */
@Setter
@Getter
public class BlockParam {

	@NotEmpty(message = "block不能为空")
	private Object[] block;

	private String bid;

	//是否为动态数据，决定是否在实验中获取数据
	@NotNull(message = "dynamic不能为空")
	private Boolean dynamic;
	
	//是否为feed流数据
	@NotNull(message = "feed不能为空")
	private Boolean feed;

}
