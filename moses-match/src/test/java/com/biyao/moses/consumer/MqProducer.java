package com.biyao.moses.consumer;

import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class MqProducer {

    @Value("${rocketmq.server.namesrvAddr}")
    protected String namesrvAddr;

    @Value("${rocketmq.user.view.product.topic}")
    protected String topic;

    protected DefaultMQProducer producer;

    @PostConstruct
    public void init() {
        producer = new DefaultMQProducer("test");
        producer.setNamesrvAddr(namesrvAddr);
        producer.setInstanceName(String.valueOf(System.currentTimeMillis()));
        producer.setSendMsgTimeout(3000);
        producer.setRetryTimesWhenSendFailed(2);
        try {
            producer.start();
        } catch (MQClientException e) {
            e.printStackTrace();
        }
    }

    public SendResult sendMessageByTag(String messageBody, String msgTag) {
        Message message = new Message(topic, msgTag, messageBody.getBytes());
        SendResult sendResult = null;
        try {
            sendResult = producer.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (sendResult == null || sendResult.getSendStatus() != SendStatus.SEND_OK) {
            System.err.println("mq消息发送失败,messageBody=" + messageBody);
            return null;
        }
        return sendResult;
    }
}
