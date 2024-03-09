package com.biyao.moses.consumer.template;

import com.biyao.moses.bean.KafkaConfigure;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;
/**
* @Description MQ基类
* @date 2019年6月5日上午11:51:45
* @version V1.0 
* @author 邹立强 (zouliqiang@idstaff.com)
 */
public abstract class MqConsumerTemplate{
	
	private static final int BATCH_SIZE = 100;
	
	/**
	* @Description 初始化消费者   
	* @return void 
	* @version V1.0
	* @auth 邹立强 (zouliqiang@idstaff.com)
	 */
	public void init() {
		
	}
    
	/**
	* @Description Mq消费数据逻辑处理 
	* @param messageExtList 
	* @return void 
	* @version V1.0
	* @auth 邹立强 (zouliqiang@idstaff.com)
	 */
	public void handleMessage(List<MessageExt> messageExtList) {
		
	}
	
	
	/**
	* @Description 初始化模板方法
	* @param namesrvAddr
	* @param customerGroup
	* @param topic
	* @param tags
	* @return DefaultMQPushConsumer 
	* @version V1.0
	* @auth 邹立强 (zouliqiang@idstaff.com)
	 */
	public DefaultMQPushConsumer initConsumer(String namesrvAddr, String customerGroup, String topic, String tags) {
		DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(customerGroup);
		try {
			consumer = new DefaultMQPushConsumer(customerGroup);
			consumer.setNamesrvAddr(namesrvAddr);
			consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
			consumer.subscribe(topic, tags);
			consumer.setMessageModel(MessageModel.CLUSTERING);
			consumer.setConsumeMessageBatchMaxSize(BATCH_SIZE);
			consumer.registerMessageListener((List<MessageExt> msgs, ConsumeConcurrentlyContext ctx) -> {
				handleMessage(msgs);
				return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
			});
			consumer.start();
		} catch (MQClientException e) {
			e.printStackTrace();
		}
		return consumer;
	}
	
	/**
	 * @Description 字符串转化Map
	 * @param logStr
	 * @return HashMap<String,String>
	 * @version V1.0
	 * @auth 邹立强 (zouliqiang@idstaff.com)
	 */
	public HashMap<String, String> getMapByLogStr(String logStr) {
		HashMap<String, String> result = new HashMap<String, String>();
		if(StringUtils.isBlank(logStr)) {
			return result;
		}
		try {
			String[] qs = logStr.split("\t");
			for (String q : qs) {
				int indexOf = q.indexOf("=");
				if (indexOf < 0) {
					continue;
				}
				String key = q.substring(0, indexOf);
				String value = q.substring(indexOf + 1);
				result.put(key, value);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	
	/**
	 * @Description 初始化kafka生产者
	 * @return void
	 * @version V1.0
	 * @auth 邹立强 (zouliqiang@idstaff.com)
	 */
	public KafkaProducer<String,String> initProducerInstance(KafkaConfigure kafkaConfigure) {
		Properties props = new Properties();
		props.put("bootstrap.servers", kafkaConfigure.getBootstrapServers());
		props.put("acks", kafkaConfigure.getAcks());
		props.put("retries", kafkaConfigure.getRetries());
		props.put("batch.size", kafkaConfigure.getBatchSize());
		props.put("linger.ms", kafkaConfigure.getLingerMs());
		props.put("buffer.memory", kafkaConfigure.getBufferMemory());
		props.put("compression.type", kafkaConfigure.getCompressionType());
		props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		KafkaProducer<String, String> producer = new KafkaProducer<>(props);
		return producer;
	}
	
	
	/**
	* @Description kafka生产消息 
	* @param producer
	* @param kafkaTopic
	* @param msg 
	* @version V1.0
	* @auth 邹立强 (zouliqiang@idstaff.com)
	 */
	public void sendKafka(KafkaProducer<String, String> producer, String kafkaTopic, String msg) {
		ProducerRecord<String, String> record = new ProducerRecord<>(kafkaTopic, msg);
		producer.send(record, new Callback() {
			@Override
			public void onCompletion(RecordMetadata metadata, Exception exception) {
				if (exception != null) {
				}
			}
		});
	}
 
}
