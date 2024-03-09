package com.biyao.moses.consumer;

import com.alibaba.dubbo.common.utils.StringUtils;
import com.biyao.moses.common.constant.CommonConstants;
import com.biyao.moses.service.imp.FootPrintExecutor;
import com.biyao.moses.util.RedisUtil;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import com.google.common.base.Charsets;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * mq消费
 * 
 * @Description
 */
@Service
public class ViewProductConsumer extends DefaultMQPushConsumer {

	@Value("${mosesui.rocketmq.nameserver}")
	private String nameServer;

	@Autowired
	private RedisUtil redisUtil;

	private static final String TOPIC = "DC_LOG";
	private static final String TAGS = "appapi.biyao.com:raw_pdetail || api.biyao.com:raw_pdetail || apiplus.biyao.com:raw_pdetail";
	private static final String GROUP = "consumer_moses_ui";
	private static final int BATCH_SIZE = 100;

	private Logger logger = LoggerFactory.getLogger(ViewProductConsumer.class);

	private static ExecutorService fixedThreadPool = Executors.newFixedThreadPool(10);

	@PostConstruct
	public void init() throws MQClientException {
		this.setConsumerGroup(GROUP);
		this.setNamesrvAddr(this.nameServer);
		this.subscribe(TOPIC, TAGS);
		this.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
		this.setConsumeMessageBatchMaxSize(BATCH_SIZE);
		this.setMessageModel(MessageModel.CLUSTERING);

		//
		this.registerMessageListener(new MessageListenerConcurrently() {
			@Override
			@BProfiler(key = "com.biyao.moses.consumer.ViewProductConsumer#consumeMessage", monitorType = { MonitorType.TP,
					MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR })
			public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> messageExtList,
					ConsumeConcurrentlyContext context) {
				consume(messageExtList);
				return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
			}
		});

		this.start();
	}

	/**
	 * 消费用户浏览商品消息
	 * 消息内容 e.g.
	 * 2019-09-15 00:00:24	172.16.16.53		lt=raw_pdetail	lv=1.0	pvid=	uu=91904230048583a7e036381dd6e1c0000000	u=9211889	pf=android	av=4.8.0	d=vivo_vivo Xplay6	os=android 7.1.1	ip=111.29.239.217	suid=1300945021010000001	pid=1300945021	sid=130094	uprice=69.00	dur=3	tt=除臭护脚透气减震运动休闲袜5双装	psp=防磨护脚垫 减震毛圈底 橡筋稳固袜身	ext={"editor":2,"money":"0","ticket":0,"type":1}
	 * 判断app version版本小于5的才进行逻辑操作，后续慢慢废弃本消费
	 * @param messageExtList
	 */
	private void consume(List<MessageExt> messageExtList) {
		for (MessageExt messageExt : messageExtList) {
			try {
				String msgBody = new String(messageExt.getBody(), Charsets.UTF_8);
				// 如果是安卓或者ios，且版本是4.x.x或者是3.x.x，则处理消息，否则直接跳过
				String msgLowerCase = msgBody.toLowerCase();
				boolean appBefore5_0 = (msgLowerCase.contains("pf=android") || msgLowerCase.contains("pf=ios")) && (msgLowerCase.contains("av=4") || msgLowerCase.contains("av=3"));
				if (!appBefore5_0){
					continue;
				}

				String[] items = msgBody.split("\t");
				String uuid = null, pid = null;

				for (String item : items) {
					if (item.startsWith("uu=")) {
						uuid = item.substring(3);
					} else if (item.startsWith("pid=")) {
						pid = item.substring(4);
					}
					if (uuid != null && pid != null) {
						break;
					}
				}

				// 如果pid为空或uuid为空，则抛出异常
				if (StringUtils.isBlank(uuid) || "null".equalsIgnoreCase(uuid) || uuid.length() <= 10 || StringUtils.isBlank(pid)) {
					throw new Exception("uuid or pid is null, msg=" + msgBody);
				}

				String redisKey = CommonConstants.MOSES_FOOT_PRINT + uuid;

				Double zscore = 0.0;
				Set<String> zrevrange = redisUtil.zrevrange(redisKey, 0, 0);
				for (String member : zrevrange) {
					zscore = redisUtil.zscore(redisKey, member);
				}
				Long lresult = redisUtil.zadd(redisKey, ++zscore, pid);
				if (lresult != null) {
					redisUtil.expire(redisKey, CommonConstants.EXEPIRE_TIME);
					fixedThreadPool.execute(new FootPrintExecutor(uuid, pid, redisUtil));
				}

			} catch (Exception e) {
				logger.error("消费用户浏览商品消息失败:", e.getMessage());
			}
		}
	}
}
