package com.biyao.moses.consumer;

import com.alibaba.fastjson.JSONObject;
import com.biyao.moses.bean.KafkaConfigure;
import com.biyao.moses.consumer.template.MqConsumerTemplate;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import com.google.common.base.Charsets;
import com.uc.domain.constant.UserFieldConstants;
import java.util.List;
import javax.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
* @Description 用户首单支付消费
* @date 2019年7月10日上午11:27:16
* @version V1.0 
* @author 邹立强 (zouliqiang@idstaff.com)
 */
@Component
public class UserPayFirstOrderConsumer extends MqConsumerTemplate {

	private Logger logger = LoggerFactory.getLogger(UserPayFirstOrderConsumer.class);
	
	@Value("${kafka.bootstrapServers}")
	private String bootstrapServers;
	@Value("${kafka.producer.acks}")
	private String acks;
	@Value("${kafka.producer.retries}")
	private Integer retries;
	@Value("${kafka.producer.batch.size}")
	private Integer batchSize;
	@Value("${kafka.producer.linger.ms}")
	private Integer lingerMs;
	@Value("${kafka.producer.buffer.memory}")
	private Long bufferMemory;
	@Value("${kafka.producer.compression.type}")
	private String compressionType;
	@Value("${kafka.producer.uc.topic}")
	private String kafkaTopic;
	@Value("${rocketmq.server.namesrvAddr}")
	private String namesrvAddr;
	@Value("${rocketmq.first.order.pay.consumer.group}")
	private String customerGroup;
	@Value("${rocketmq.first.order.pay.topic}")
	private String topic;
	@Value("${rocketmq.first.order.pay.tags}")
	private String tags = "*";
	private static KafkaProducer<String, String> producer;


	@PostConstruct
	public void init() {
		initConsumer(namesrvAddr, customerGroup, topic, tags);
		producer = initProducerInstance(KafkaConfigure.builder().acks(acks).batchSize(batchSize)
				.bootstrapServers(bootstrapServers).bufferMemory(bufferMemory).compressionType(compressionType)
				.lingerMs(lingerMs).retries(retries).build());
	}

	@BProfiler(key = "UserPayFirstOrderConsumer.handleMessage", monitorType = { MonitorType.TP,
			MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR })
	public void handleMessage(List<MessageExt> msgs) {
		for (MessageExt messageExt : msgs) {
			try {
				//日志格式
			/*	{
					"createTime": 1562247537687,
					"data": {
						"customerId": 10801305,
						"orderIds": [
							302016227631700001
						],
						"orderType": 1,
						"payCode": 1701622763170000109,
						"payType": 990,
						"successTime": 1562247536000
					}
				}*/
				String msgBody = new String(messageExt.getBody(), Charsets.UTF_8);
				if (StringUtils.isNotBlank(msgBody)) {
					JSONObject parseObject = JSONObject.parseObject(msgBody);
					JSONObject jsonObject = parseObject.getJSONObject("data");
					String uid = jsonObject.getString("customerId");
					String payType = jsonObject.getString("payType");
					//实物红包不记录老客
					if(StringUtils.isNotBlank(payType)&&"103".equals(payType)) {
						return ;
					}
					if (StringUtils.isNotBlank(uid)) {
						StringBuffer msg = new StringBuffer();
						msg.append("uid=").append(uid).append("\t");
						msg.append(UserFieldConstants.CUSTOMERSTATUS).append("=").append(2);
						sendKafka(producer, kafkaTopic, msg.toString());
					}
				}

			} catch (Exception e) {
				logger.error("消费首单支付消息失败:msg={} ,e：{}", messageExt, e);
			}
		}
	}
}