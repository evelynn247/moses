package com.biyao.moses.mq;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.MessageListener;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;

import javax.annotation.PreDestroy;

/**
 * @program: moses-parent-online
 * @description: 消费者抽象类
 * @author: changxiaowei
 * @Date: 2021-12-03 15:14
 **/
@Slf4j
public abstract class AbstractConsumer {


    /**
     * 构造
     * @param name 名称，用于打印日志
     */
    public AbstractConsumer(String name) {
        super();
        this.name = name;
    }

    /**
     *  名称，用于打印日志
     */
    private String name;

    /**
     * 服务地址
     */
    protected String namesrvAddr;

    /**
     * 消费组
     */
    protected String consumerGroup;

    /**
     * 主题
     */
    protected String topic;


    /**
     * 消费者->推
     */
    private DefaultMQPushConsumer pushConsumer;

    /**
     * 消息监听器
     */
    protected MessageListener messageListener;

    /**
     * 标签
     */
    protected String tag;




    /**
     * 启动监控客户端
     *
     * @author liyawei
     * @date 2018年4月3日 下午4:04:40
     * @throws Exception
     */
    @SuppressWarnings("deprecation")
    public void start() throws Exception {

        /*
         * 启动消费者
         */
        try {
            pushConsumer = new DefaultMQPushConsumer();
            //name server
            pushConsumer.setNamesrvAddr(this.namesrvAddr);
            //消费组
            pushConsumer.setConsumerGroup(this.consumerGroup);
            //集群消费
            pushConsumer.setMessageModel(MessageModel.CLUSTERING);
            //从第一个开始消费
            pushConsumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
            //实例名称
            pushConsumer.setInstanceName(String.valueOf(System.currentTimeMillis()));
            //消息监听器
            pushConsumer.registerMessageListener(this.messageListener);
            //订阅主题
            pushConsumer.subscribe(this.topic, this.tag);
            //一次消费最大数量1
            pushConsumer.setConsumeMessageBatchMaxSize(1);
            //启动
            pushConsumer.start();
            log.info("[操作日志]{} MQ consumer 初始化成功,namesrvAddr:{},topic:{},tag:{},consumerGroup:{}"
                    , this.name, this.namesrvAddr, this.topic, this.tag, this.consumerGroup);
        }catch (Exception e) {
            log.error("[操作日志]{} MQ consumer 初始失败,namesrvAddr:{},topic:{},tag:{},consumerGroup:{}"
                    , this.name, this.namesrvAddr, this.topic, this.tag,this.consumerGroup,  e);
            throw e;
        }

    }


    /**
     * 设置服务地址
     *
     * @author liyawei
     * @date 2018年4月3日 下午4:36:51
     * @param namesrvAddr 服务地址
     */
    protected abstract void setNamesrvAddr(String namesrvAddr);
    /**
     * 设置消费者组
     *
     * @author liyawei
     * @date 2018年4月3日 下午4:37:15
     * @param consumerGroup 消费者组
     */
    protected abstract void setConsumerGroup(String consumerGroup);
    /**
     * 设置mq主题
     *
     * @author liyawei
     * @date 2018年4月3日 下午4:37:03
     * @param topic mq主题
     */
    protected abstract void setTopic(String topic);
    /**
     * 设置消息监听器
     *
     * @author liyawei
     * @date 2018年4月3日 下午4:37:40
     * @param messageListener 消息监听器
     */
    protected abstract void setMessageListener(MessageListener messageListener);

    /**
     * 设置mq标签
     * @param tag
     */
    protected abstract void setTag(String tag);

    /**
     * 获取标签
     * @return
     */
    public String getTag() {
        return tag;
    }

    /**
     * 获取消费者组
     *
     * @author liyawei
     * @date 2018年4月4日 下午2:15:26
     * @return
     */
    public String getConsumerGroup() {
        return consumerGroup;
    }


    /**
     * 获取topic
     * @author liyawei
     * @date 2018年4月4日 下午2:15:36
     * @return
     */
    public String getTopic() {
        return topic;
    }


    /**
     * 销毁方法
     * @author liyawei
     * @date 2018年4月3日 下午4:15:11
     */
    @PreDestroy
    public void destroy() {
        if(pushConsumer != null) {
            pushConsumer.shutdown();
        }
    }


}

