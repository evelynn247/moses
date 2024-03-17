package com.biyao.search.ui.rpc;

import com.biyao.search.ui.common.ByErrorCode;
import com.biyao.search.ui.common.ByException;

/**
 * rpc异常
 * @author biyao
 *
 */
public class ByRpcException extends ByException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1597395777050076323L;

	/**
	 * 发生rpc异常时抛出的真正异常
	 */
	private Throwable throwable;

	/**
	 * 发生rpc错误时，调用rpc接口传入的参数
	 */
	private String param;

	/**
	 * 发生rpc错误时，调用rpc接口返回结果
	 */
	private String result;

	public ByRpcException(String errorMsg, String param) {
		super(ByErrorCode.ERROR_RPC_UNSUCCESS, errorMsg);
		this.param = param;
	}

	public ByRpcException(String errorMsg, String param, String result) {
		super(ByErrorCode.ERROR_RPC_UNSUCCESS, errorMsg);
		this.param = param;
		this.result = result;
	}

	public ByRpcException(String errorMsg, String param, Throwable e) {
		super(ByErrorCode.ERROR_RPC_UNSUCCESS, errorMsg);
		this.param = param;
		this.throwable = e;
	}

	public Throwable getThrowable() {
		return throwable;
	}

	public void setThrowable(Throwable throwable) {
		this.throwable = throwable;
	}

	public String getParam() {
		return param;
	}

	public String getResult() {
		return result;
	}

}