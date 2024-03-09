package com.biyao.moses.consumer;

import com.biyao.moses.bean.KafkaConfigure;
import com.biyao.moses.consumer.template.MqConsumerTemplate;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import com.google.common.base.Charsets;
import com.uc.domain.constant.UserFieldConstants;
import java.text.SimpleDateFormat;
import java.util.Date;
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
 * @Description 消费下单消息
 * @date 2019年12月11日下午4:02:28
 * @version V1.0
 * @author 邹立强 (zouliqiang@idstaff.com)
 */
@Component
public class UserOdPidsConsumer extends MqConsumerTemplate {

	private Logger logger = LoggerFactory.getLogger(UserOrderConsumer.class);

	@Value("${rocketmq.server.namesrvAddr}")
	private String namesrvAddr;
	@Value("${rocketmq.user.order.product.consumer.group}")
	private String customerGroup;
	@Value("${rocketmq.user.order.product.topic}")
	private String topic;
	@Value("${rocketmq.user.order.product.tags}")
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
		producerTopic = kafkaTopic;
	}

	@BProfiler(key = "com.biyao.moses.consumer.UserOrderConsumer.handleMessage", monitorType = { MonitorType.TP,
			MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR })
	public void handleMessage(List<MessageExt> msgs) {
		for (MessageExt messageExt : msgs) {
			try {
				String msgBody = new String(messageExt.getBody(), Charsets.UTF_8);
				if (StringUtils.isNotBlank(msgBody)) {
					Map<String, String> logMap = getMapByLogStr(msgBody);
					String uid = logMap.get("u");
					String pid = logMap.get("pid");
					String time = logMap.get("t");
					if (StringUtils.isNotBlank(uid) && StringUtils.isNotBlank(pid) && StringUtils.isNotBlank(time)) {
						StringBuffer msgStr = new StringBuffer();
						SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						Date date = format.parse(time);
						Long timestamp = date.getTime();
						msgStr.append("uid=").append(uid).append("\t").append(UserFieldConstants.ODPIDS).append("=")
								.append(pid).append(":").append(timestamp);
						sendKafka(producer, producerTopic, msgStr.toString());
					}
				}
			} catch (Exception e) {
				logger.error("消费下单消息失败:msg={} ,e：{}", messageExt, e);
			}
		}
	}
}