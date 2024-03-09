package com.biyao.moses.mq.product;

import com.biyao.moses.mq.AbstractConsumer;
import org.apache.rocketmq.client.consumer.listener.MessageListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * @program: moses-parent-online
 * @description: 商品基本信息mq工具类
 * @author: changxiaowei
 * @Date: 2021-12-03 15:07
 **/

@Component
public class ProductConsumeUtil extends AbstractConsumer {

    public ProductConsumeUtil() {
        super("商品基础信息消费者工具类");
    }

    @Override
    @Value("${mq.normal.server.namesrvAddr}")
    protected void setNamesrvAddr(String namesrvAddr) {
        this.namesrvAddr = namesrvAddr;
    }

    @Override
    @Value("${product_consumer_group}")
    protected void setConsumerGroup(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }

    @Override
    @Value("${product_consumer_topic}")
    protected void setTopic(String topic) {
        this.topic = topic;
    }

    @Override
    @Value("${product_consumer_tag}")
    protected void setTag(String tag) {
        this.tag = tag;
    }

    @Override
    @Resource(name="productConsumerMassageListener")
    protected void setMessageListener(MessageListener messageListener) {
        this.messageListener = messageListener;
    }


    /**
     * spring初始化
     * @throws Exception
     */
    @PostConstruct
    public void init() throws Exception {
        super.start();
    }
}
