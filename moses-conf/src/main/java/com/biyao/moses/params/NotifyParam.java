package com.biyao.moses.params;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * 回调测试
 * @Description 
 * @author zyj
 * @Date 2018年9月27日
 */
@Setter
@Getter
public class NotifyParam {

	@NotBlank(message = "参数name不能为空")
    private String name;

    @NotNull(message = "参数id不能为空")
    private Long id;
	
    private String code;

}
