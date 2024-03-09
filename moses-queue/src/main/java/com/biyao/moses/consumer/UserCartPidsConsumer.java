package com.biyao.moses.consumer;

import com.alibaba.fastjson.JSON;
import com.biyao.moses.bean.KafkaConfigure;
import com.biyao.moses.consumer.template.MqConsumerTemplate;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import com.google.common.base.Charsets;
import com.uc.domain.constant.UserFieldConstants;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
* @Description 消费加入购物车消息
* @date 2019年12月12日下午2:43:26
* @version V1.0 
* @author 邹立强 (zouliqiang@idstaff.com)
 */
@Component
public class UserCartPidsConsumer extends MqConsumerTemplate {

	private Logger logger = LoggerFactory.getLogger(UserOrderConsumer.class);
	
	@Value("${rocketmq.server.namesrvAddr}")
	private String namesrvAddr;
	@Value("${rocketmq.user.shopcart.product.consumer.group}")
	private String customerGroup;
	@Value("${rocketmq.user.shopcart.product.topic}")
	private String topic;
	@Value("${rocketmq.user.shopcart.product.tags}")
	private String tags = "*";
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
	private static String producerTopic;
	private static KafkaProducer<String, String> producer;


	@PostConstruct
	public void init() {
		initConsumer(namesrvAddr, customerGroup, topic, tags);
		producer = initProducerInstance(KafkaConfigure.builder().acks(acks).batchSize(batchSize)
				.bootstrapServers(bootstrapServers).bufferMemory(bufferMemory).compressionType(compressionType)
				.lingerMs(lingerMs).retries(retries).build());
		producerTopic=kafkaTopic;
	}

	@BProfiler(key = "com.biyao.moses.consumer.UserOrderConsumer.handleMessage", monitorType = { MonitorType.TP,
			MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR })
	public void handleMessage(List<MessageExt> msgs) {
		for (MessageExt messageExt : msgs) {
			try {
 				String msgBody = new String(messageExt.getBody(), Charsets.UTF_8);
				if (StringUtils.isNotBlank(msgBody)) {
					Map<String, String> logMap = getMapByLogStr(msgBody);
					String tp = logMap.get("tp");
					String pid = logMap.get("pid");
					String time = logMap.get("st");
					if (StringUtils.isNotBlank(tp)&&StringUtils.isNotBlank(pid)&&StringUtils.isNotBlank(time)) {
						Map tpMap = JSON.parseObject(tp,Map.class);  
				        Object ub = tpMap.get("u");
				        if(ub!=null&&StringUtils.isNotBlank(ub.toString())) {
				        	StringBuffer msgStr = new StringBuffer();
							msgStr.append("uid=").append(ub.toString()).append("\t").append(UserFieldConstants.CARTPIDS).append("=").append(pid).append(":").append(time);
							sendKafka(producer, producerTopic, msgStr.toString());
				        }
					}
				}

			} catch (Exception e) {
				logger.error("消费加购消息失败:msg={} ,e：{}", messageExt, e);
			}
		}
	}
}