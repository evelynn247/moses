package com.biyao.moses.mq;

import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.fastjson.JSONObject;
import com.biyao.moses.Enum.EMqLogType;
import com.biyao.moses.pdc.IMqLogDao;
import com.biyao.moses.pdc.domain.MqLogDomain;
import com.biyao.moses.util.CommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.List;

import static com.biyao.moses.common.constant.CommonConstant.MQ_LOG_STATUS_FAILED;
import static com.biyao.moses.common.constant.CommonConstant.MQ_LOG_STATUS_SUCC;

/**
 * @program: moses-parent-online
 * @description: 消息监听器抽象类
 * @author: changxiaowei
 * @Date: 2021-12-02 16:27
 **/
@Slf4j
public abstract class AbstractConsumerMessageListener implements MessageListenerOrderly {

    /**
     * 重试达到3次，发送邮件
     */
    private static final int RETRY_NUM_SEND_EMAIL = 3;


    /**
     * 最大重试6次
     */
    private static final int RETRY_NUM_MAX = 6;

    /**
     * 构造
     * @param name 名称，用于打印日志
     */
    public AbstractConsumerMessageListener(String name) {
        super();
        this.name = name;
    }

    /**
     * 名称，用于打印日志
     */
    protected String name;

    /**
     * 消费日志dao
     */
    @Autowired
    protected IMqLogDao mqLogDao;

    /**
     * 消息处理者
     */
    protected IConsumerHandler consumerHandler;

    /**
     * 设置消息处理者
     *
     * @author liyawei
     * @date 2018年4月13日 下午7:32:01
     * @param consumerHandler
     */
    public abstract void setConsumerHandler(IConsumerHandler consumerHandler);


    @Override
    public ConsumeOrderlyStatus consumeMessage(List<MessageExt> msgs, ConsumeOrderlyContext context) {

        MessageExt messageExt = msgs.get(0);
        try {
            //body
            String bodyStr = new String(messageExt.getBody(), "UTF-8");
            if (StringUtils.isEmpty(bodyStr)) {
                throw new InvalidMqMsgException("body为空");
            }
            JSONObject body = JSONObject.parseObject(bodyStr);
            if (null == body || body.isEmpty()) {
                throw new InvalidMqMsgException("body为空");
            }

            //处理消息
            this.consumerHandler.handle(messageExt, body);

            //保存消费成功日志
            this.saveMqLog(true, messageExt, null);
            log.info("[操作记录]{} 消费MQ消息成功，topic：{}，消息：{}， 内容：{}", this.name, messageExt.getTopic(), messageExt, body.toJSONString());

        }catch (InvalidMqMsgException e) {
            log.error("[严重异常]{} 消费MQ消息时发生错误，消息格式错误, topic：{}，消息：{}， 内容：{}", this.name, messageExt.getTopic(), messageExt, new String(messageExt.getBody()), e);
            return ConsumeOrderlyStatus.SUCCESS;
        }catch (Exception e) {
            /*
             * 未知异常
             */
            log.error("[一般异常]{} 消费MQ消息时发生未知异常, 失败达到3次时，将会邮件报警 topic：{}，消息：{}， 内容：{}", this.name, messageExt.getTopic(), messageExt, new String(messageExt.getBody()), e);
            //记录消费失败
            try {
                this.saveMqLog(false, messageExt, e.getMessage());
            } catch (Exception e2) {
                log.error("[严重异常]{} 消费消息失败，插入异常表失败,msgId:{}", this.name, messageExt.getMsgId(), e2);
            }

            //重试3次(发邮件报警)
            if(messageExt.getReconsumeTimes() + 1 == RETRY_NUM_SEND_EMAIL) {
                log.error("[严重异常]同一个MQ消息已连续3次消费失败，topic：{},消息:{},内容:{}." +
                        "当第6次消费失败后，系统将跳过该消息不再重试。请关注日志,如果6次后依然消费失败，请人工检查并修复数据",
                        messageExt.getTopic(),messageExt,new String(messageExt.getBody()));
                //暂停当前队列（会在一段时间之后重试消费消息）
                return ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT;
            }

            //重试大于等于6次，将消息修改为成功
            if(messageExt.getReconsumeTimes() + 1 >= RETRY_NUM_MAX) {
                log.error("[操作记录]{} 消息消费失败达到6次，跳过该条消息，topic：{}，消息：{}， 内容：{}", this.name, messageExt.getTopic(), messageExt, new String(messageExt.getBody()));
                return ConsumeOrderlyStatus.SUCCESS;
            }

            //暂停当前队列（会在一段时间之后重试消费消息）
            return ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT;
        }

        return ConsumeOrderlyStatus.SUCCESS;
    }


    /**
     * 保存消费日志
     *
     * @param success 标识消费是否成功
     * @param messageExt 消息
     * @param remark
     * @throws UnsupportedEncodingException
     */
    protected void saveMqLog(boolean success, MessageExt messageExt, String remark) throws UnsupportedEncodingException {

        JSONObject body = JSONObject.parseObject(new String(messageExt.getBody(), "UTF-8"));

        BodyInfo bodyInfo = this.consumerHandler.getBodyInfo(body);
        if(bodyInfo == null) {
            return;
        }
        //消息创建时间
        Long createTime = bodyInfo.getCreateTime();
        if(null == createTime) {
            return;
        }
        //唯一ID
        String uuid = CommonUtil.createUniqueId();
        //业务id
        String businessId = messageExt.getMsgId();
        //查询是否存在日志
        MqLogDomain mqLogDomain = this.mqLogDao.selectByUuidAndMsgType(uuid,(byte)0);
        /*
         * 新增
         */
        if(mqLogDomain == null) {
            mqLogDomain = new MqLogDomain();
            mqLogDomain.setUuid(uuid);
            mqLogDomain.setType(EMqLogType.PRODUCT.getType());
            mqLogDomain.setMqContext(body.toJSONString());
            mqLogDomain.setMqTag(messageExt.getTags());
            mqLogDomain.setCreateTime(new Date());
            mqLogDomain.setRemark(remark);
            mqLogDomain.setReconsumeTimes(messageExt.getReconsumeTimes());
            mqLogDomain.setSendMsgTime(new Date(createTime));
            mqLogDomain.setBusinessId(businessId);
            mqLogDomain.setMqTopic(messageExt.getTopic());
            if(success) {
                mqLogDomain.setConsumeSuccTime(new Date());
                mqLogDomain.setStatus(MQ_LOG_STATUS_SUCC);

            }else {
                mqLogDomain.setStatus(MQ_LOG_STATUS_FAILED);
            }

            log.info("[操作日志][SQL操作]mq消费信息入库--------");
            this.mqLogDao.insert(mqLogDomain);
            log.info("[操作日志][SQL操作]mq消费信息入库结束--------");
            return;
        }

        /*
         * 修改
         */
        if(success) {
            //更新消费成功
            mqLogDomain.setRemark(remark);
            mqLogDomain.setConsumeSuccTime(new Date());
            mqLogDomain.setReconsumeTimes(messageExt.getReconsumeTimes());
            this.mqLogDao.updateSuccess(mqLogDomain);

        }else {
            //更新消费失败
            mqLogDomain.setRemark(remark);
            mqLogDomain.setReconsumeTimes(messageExt.getReconsumeTimes());
            this.mqLogDao.updateFailure(mqLogDomain);
        }
    }
}
