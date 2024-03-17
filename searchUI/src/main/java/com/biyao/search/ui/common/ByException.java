package com.biyao.search.ui.common;

/**
 * 业务运行时异常
 * @author biyao
 *
 */
public class ByException extends RuntimeException {
 

	/**
	 * 
	 */
	private static final long serialVersionUID = 2124504809392972881L;

	private String errorCode;

	private String errorMsg;

	public ByException(ByErrorCode errorCode) {
		super(errorCode.msg);
		this.errorCode = errorCode.code;
		// 使用默认错误信息
		this.errorMsg = errorCode.msg;
	}

	public ByException(ByErrorCode errorCode, String errorMsg) {
		super(errorMsg);
		this.errorCode = errorCode.code;
		// 定制错误信息
		this.errorMsg = errorMsg;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public String getErrorMsg() {
		return errorMsg;
	}
}
