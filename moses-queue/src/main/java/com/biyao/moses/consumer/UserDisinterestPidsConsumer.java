package com.biyao.moses.consumer;

import com.alibaba.fastjson.JSON;
import com.biyao.moses.bean.KafkaConfigure;
import com.biyao.moses.consumer.template.KafkaConsumerService;
import com.biyao.moses.consumer.template.KafkaConsumerTemplate;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import com.uc.domain.constant.UserFieldConstants;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
* @Description 处理不感兴趣商品
* @date 2020年1月6日下午2:32:47
* @version V1.0 
* @author 邹立强 (zouliqiang@idstaff.com)
 */
@Component
public class UserDisinterestPidsConsumer extends KafkaConsumerTemplate implements KafkaConsumerService{
	
	private Logger logger = LoggerFactory.getLogger(UserDisinterestPidsConsumer.class);
	
	@Value("${kafka.bootstrapServers}")
	private String bootstrapServers;
	@Value("${kafka.consumer.disinterest.groupId}")
	private String groupId;
	@Value("${kafka.consumer.disinterest.topic}")
	private String topic;
	@Value("${kafka.consumer.disinterest.threadNum}")
	private Integer threadNum;
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
	private  String kafkaTopic;
	private  static String producerTopic;
	private  static KafkaProducer<String, String> producer;
	
	private static final Set<String> pclks = new HashSet<String>() {
		private static final long serialVersionUID = -5428011865781096966L;
		{
			add("not_interest");
		}
	};
	private ExecutorService executorService;
	
	@Override
	public void init() {
		try {
			executorService = Executors.newFixedThreadPool(threadNum);
			for (int i = 0; i < threadNum; i++) {
				executorService.execute(new UserDisinterestPidsConsumer().setBootstrapServers(bootstrapServers)
						.setGroupId(groupId).setTopic(topic));
			}
			producer = initProducerInstance(KafkaConfigure.builder().acks(acks).batchSize(batchSize)
					.bootstrapServers(bootstrapServers).bufferMemory(bufferMemory).compressionType(compressionType)
					.lingerMs(lingerMs).retries(retries).build());
			producerTopic=kafkaTopic;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
    @Override
    @BProfiler(key = "UserDisinterestPidsConsumer.handleMessage", monitorType = { MonitorType.TP,
			MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR })
	public void handleMessage(ConsumerRecords<String, String> records) {
    	for (ConsumerRecord<String, String> record : records) {
			try {
				String message = record.value();
				if (StringUtils.isBlank(message)) {
					continue;
				}
				HashMap<String, String> mapByLogStr = getMapByLogStr(message);
				String pclk = mapByLogStr.get("pclk");
				String pclkp = mapByLogStr.get("pclkp");
				String uuid = mapByLogStr.get("uu");
				if (StringUtils.isNotBlank(pclk) && pclks.contains(pclk)
						&& StringUtils.isNotBlank(uuid)&& StringUtils.isNotBlank(pclkp) && pclkp.contains("spuid")) {
					StringBuffer msg = new StringBuffer();
					HashMap<String, String> pclkpParam = urlParam(pclkp);
					msg.append("uuid=").append(uuid).append("\t");
					String spuid = pclkpParam.get("spuid");
					if (StringUtils.isNotBlank(spuid)) {
						msg.append(UserFieldConstants.DISINTERESTPIDS).append("=").append(spuid).append(":")
								.append(System.currentTimeMillis());
						sendKafka(producer, producerTopic, msg.toString());
					}
				}
			} catch (Exception e) {
				logger.error("消费不感兴趣商品数据异常：{}，数据内容：{}", e, JSON.toJSONString(record));
			}
		}
	}


}
