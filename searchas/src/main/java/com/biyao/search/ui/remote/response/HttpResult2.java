package com.biyao.search.ui.remote.response;

import com.biyao.search.common.model.Status;

/**
 * Dubbo PRC接口返回结果
 * 此结果正常时error节点会返回null
 */
public class HttpResult2<T> {
    
	/**
	 * 1:成功；0失败
	 */
    private Integer success;
    
	/**
	 * 错误信息
	 */
	private Status error;
	
	/**
	 * 逻辑返回值（success为true时使用）
	 */
	private T data;

	public HttpResult2() {
	}
	
	public HttpResult2(T data) {
		this.error = null;
		this.data   = data;
		this.success = 1;
	}
	
	public HttpResult2(Status error) {
		this.error = error;
		this.success = 0;
	}

    public Integer getSuccess() {
        return success;
    }

    public void setSuccess(Integer success) {
        this.success = success;
    }

    public Status getError() {
        return error;
    }

    public void setError(Status error) {
        this.error = error;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}