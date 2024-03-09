package com.biyao.moses.params;

import javax.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * 版本设置参数
 * @Description
 * @author zyj
 * @Date 2018年9月27日
 */
@Setter
@Getter
public class AvnParam {

	// 来源
	@NotBlank(message = "source不能为空")
	private String source;
	
	//平台
	@NotBlank(message = "platform不能为空")
	private String platform;
	
	//对应版本 minAvn-maxAvn:version,minAvn-maxAvn:version
	@NotBlank(message = "versionValue不能为空")
	private String versionValue;
	
}
