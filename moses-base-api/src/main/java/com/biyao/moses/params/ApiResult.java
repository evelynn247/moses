package com.biyao.moses.params;

import java.io.Serializable;
import java.util.HashMap;

import com.alibaba.fastjson.JSONObject;
import com.biyao.moses.common.constant.ErrorCode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ApiResult
 * @Description 
 * @Date 2018年9月27日
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> implements Serializable {

	private static final long serialVersionUID = 1L;

	private Integer success = ErrorCode.SUCCESS_CODE; // 响应状态，默认0成功

	private String error = "success"; // 响应信息

	private T data = null;
	 
}
