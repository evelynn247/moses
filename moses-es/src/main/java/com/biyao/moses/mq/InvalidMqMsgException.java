package com.biyao.moses.mq;

/**
 * @program: moses-parent-online
 * @description:  无效消息异常
 * @author: changxiaowei
 * @Date: 2021-12-02 20:36
 **/
public class InvalidMqMsgException extends RuntimeException{


    /**
     *
     */
    private static final long serialVersionUID = -2230269058309828940L;

    public InvalidMqMsgException(String msg) {
        super(msg);
    }

}
