package com.biyao.moses.consumer.template;

import com.biyao.moses.bean.KafkaConfigure;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Properties;
/**
* @Description MQ基类
* @date 2019年6月5日上午11:51:45
* @version V1.0 
* @author 邹立强 (zouliqiang@idstaff.com)
* <p>Copyright (c) Department of Research and Development/Beijing.</p>
 */
public class KafkaConsumerTemplate implements Runnable{
	
	private Logger logger = LoggerFactory.getLogger(KafkaConsumerTemplate.class);
	/**
	   * 是否自动提交offset
	 */
    private Boolean enableAutoCommit = false;
    /**
             *拉取数据频率
     */
    private String keyDeserializer = "org.apache.kafka.common.serialization.StringDeserializer";
    private String valueDeserializer = "org.apache.kafka.common.serialization.StringDeserializer";
    private Long pollTime=500L;
    
    private String bootstrapServers;
    
    private String groupId;
    
    private String topic;
    
    

	public KafkaConsumerTemplate setBootstrapServers(String bootstrapServers) {
		this.bootstrapServers = bootstrapServers;
		return this;
	}

	public KafkaConsumerTemplate setGroupId(String groupId) {
		this.groupId = groupId;
		return this;
	}
	
	public KafkaConsumerTemplate setTopic(String topic) {
		this.topic = topic;
		return this;
	}

	@Override
    public void run() {
		Properties props = new Properties();
		props.put("bootstrap.servers", bootstrapServers);
		props.put("group.id", groupId);
		props.put("enable.auto.commit", enableAutoCommit);
		props.put("key.deserializer", keyDeserializer);
		props.put("value.deserializer", valueDeserializer);
		KafkaConsumer<String, String> consumer = subscribeTopic(props, topic);
		try {
			while (true) {
				ConsumerRecords<String, String> records = consumer.poll(pollTime);
				handleMessage(records);
				consumer.commitAsync();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				consumer.commitSync();
			} finally {
				consumer.close();
			}
		}
    }
    
    /**
    * @Description kafka消费消息 
    * @param records 
    * @version V1.0
    * @auth 邹立强 (zouliqiang@idstaff.com)
     */
	public void handleMessage(ConsumerRecords<String, String> records) {
		
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
					logger.error("kafka生产消息异常：{}，消息内容：{}", exception, msg);
				}
			}
		});
	}
	
	/**
	* @Description 订阅topic 
	* @param props
	* @param topic
	* @return KafkaConsumer<String,String> 
	* @version V1.0
	* @auth 邹立强 (zouliqiang@idstaff.com)
	 */
    private KafkaConsumer<String, String> subscribeTopic(Properties props, String topic) {
        final KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(props);
        consumer.subscribe(Collections.singletonList(topic), new ConsumerRebalanceListener() {
            // 消费者平衡操作开始之前、消费者停止拉取消息之后被调用(可以提交偏移量以避免数据重复消费)
            @Override
            public void onPartitionsRevoked(Collection<TopicPartition> collection) {
                // 提交偏移量
                consumer.commitSync();
            }

            // 消费者平衡操作之后、消费者开始拉取消息之前被调用(可以在该方法中保证各消费者回滚到正确的偏移量，重置各消费者偏移量)
            @Override
            public void onPartitionsAssigned(Collection<TopicPartition> collection) {
                long committedOffset = -1;
                for (TopicPartition topicPartition : collection) {
                    // 获取该分区已消费的偏移量
                    if (consumer != null) {
                        committedOffset = consumer.committed(topicPartition).offset();
                        // 重置偏移量到上一次提交的偏移量开始消费
                        consumer.seek(topicPartition, committedOffset);
                    }
                }
            }
        });
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
		if (StringUtils.isBlank(logStr)) {
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
	 * @Description URL参数拼接&转换key=value
	 * @param query
	 * @return HashMap<String,String>
	 * @version V1.0
	 * @auth 赵晓峰 (zhaoxiaofeng@idstaff.com)
	 */
	public HashMap<String, String> urlParam(String query) {
		HashMap<String, String> result = new HashMap<String, String>();
		try {
			String[] qs = query.split("&");
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
}
