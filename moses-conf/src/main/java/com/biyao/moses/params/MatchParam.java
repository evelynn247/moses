package com.biyao.moses.params;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;

import lombok.Getter;
import lombok.Setter;

/**
 * 实验参数校验
 * 
 * @Description
 * @author zyj
 * @Date 2018年9月27日
 */
@Setter
@Getter
public class MatchParam {

	@NotEmpty(message = "data不能为空")
	private Object[] data;
	// id
	@NotBlank(message = "matchId不能为空")
	private String matchId;
}
