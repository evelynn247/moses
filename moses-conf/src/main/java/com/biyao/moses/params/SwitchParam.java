package com.biyao.moses.params;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * 开关和pid设置参数
 * @Description
 * @author zyj
 * @Date 2018年9月27日
 */
@Setter
@Getter
public class SwitchParam {

	//版本，二级key
	@NotBlank(message = "version不能为空")
	private String version;
	
	//开关
	@NotNull(message = "onoff不能为空")
	private Boolean onoff;
	
	//页面id
	@NotBlank(message = "pid不能为空")
	private String pid;
	//数据源
	private String topicId;
	
}
