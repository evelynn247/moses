package com.biyao.search.ui.common;

/**
 * 异常 code
 * @author huangyq
 *
 */
public enum ByErrorCode {

    /**
     * 系统未知错误
     */
    ERROR_UNKNOWN("000001", "系统未知错误"),
    /**
     * 系统访问过于频繁
     */
    ERROR_VISIT_TOOMUCH("000005", "系统访问过于频繁"),
    /**
     * 参数不完整
     */
    ERROR_PARAM_INCOMPLETE("200002", "参数不完整"),
    /**
     * RPC调用异常
     */
    ERROR_RPC_UNSUCCESS("200005", "RPC调用异常"),
    /**
     * 数据不完整异常
     */
    ERROR_DATA_INCOMPLETE("200006", "数据不完整异常");



    public final String code;
    public final String msg;

    private ByErrorCode(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
