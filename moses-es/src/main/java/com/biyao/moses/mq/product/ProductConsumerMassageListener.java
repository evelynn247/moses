package com.biyao.moses.mq.product;

import com.biyao.moses.mq.AbstractConsumerMessageListener;
import com.biyao.moses.mq.IConsumerHandler;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyStatus;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * @program: moses-parent-online
 * @description: 商品基本信息mq监听器
 * @author: changxiaowei
 * @Date: 2021-12-03 15:07
 **/
@Component("productConsumerMassageListener")
public class ProductConsumerMassageListener extends AbstractConsumerMessageListener {

    public ProductConsumerMassageListener() {
        super("商品基础变更信息监听器");
    }


    @Override
    @BProfiler(key = "com.biyao.moses.mq.product.ProductConsumerMassageListener",
            monitorType = {MonitorType.TP, MonitorType.HEARTBEAT})
    public ConsumeOrderlyStatus consumeMessage(List<MessageExt> msgs, ConsumeOrderlyContext context) {
        return super.consumeMessage(msgs, context);
    }

    @Override
    @Resource(name = "productConsumerHandler")
    public void setConsumerHandler(IConsumerHandler consumerHandler) {
        this.consumerHandler = consumerHandler;
    }



}
