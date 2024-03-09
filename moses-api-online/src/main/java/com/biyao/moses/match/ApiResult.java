package com.biyao.moses.match;

import com.biyao.moses.common.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * @program: moses-parent-online
 * @description:
 * @author: changxiaowei
 * @create: 2021-09-14 10:23
 **/
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