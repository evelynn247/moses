package com.biyao.moses.mq;

import com.alibaba.fastjson.JSONObject;
import org.apache.rocketmq.common.message.MessageExt;

/**
 * @program: moses-parent-online
 * @description: mq消费者
 * @author: changxiaowei
 * @create: 2021-12-03 14:51
 **/
public interface IConsumerHandler {

    /**
     *  消费处理消息
     * @param messageExt
     * @param body
     * @throws InvalidMqMsgException
     * @throws Exception
     */
    void handle(MessageExt messageExt, JSONObject body) throws InvalidMqMsgException,Exception;


    /**
     * 获取body信息，用途是记录日志
     * @param body
     * @return
     */
    BodyInfo getBodyInfo(JSONObject body);

}
