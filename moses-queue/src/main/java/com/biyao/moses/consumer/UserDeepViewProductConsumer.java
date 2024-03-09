package com.biyao.moses.consumer;

import com.alibaba.fastjson.JSON;
import com.biyao.moses.bean.DeepViewLog;
import com.biyao.moses.bean.KafkaConfigure;
import com.biyao.moses.common.constant.CommonConstants;
import com.biyao.moses.consumer.template.FrameSpringBeanUtil;
import com.biyao.moses.consumer.template.KafkaConsumerService;
import com.biyao.moses.consumer.template.KafkaConsumerTemplate;
import com.biyao.moses.util.CacheRedisUtil;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import com.uc.domain.constant.UserFieldConstants;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
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

@Component
public class UserDeepViewProductConsumer extends KafkaConsumerTemplate implements KafkaConsumerService {

	private Logger logger = LoggerFactory.getLogger(UserDeepViewProductConsumer.class);

	@Value("${kafka.bootstrapServers}")
	private String bootstrapServers;

	@Value("${kafka.consumer.deepview.groupId}")
	private String groupId;

	@Value("${kafka.consumer.deepview.topic}")
	private String topic;

	@Value("${kafka.consumer.deepview.threadNum}")
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
	private String kafkaTopic;

	private static String producerTopic;

	private static KafkaProducer<String, String> producer;

	private ExecutorService executorService;

	private SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	// 1天有效期
	private static final int expireTime = 86400;// 60 * 60 * 24 1天有效

	private static CacheRedisUtil cacheRedisUtil;

	public void init() {
		try {
			executorService = Executors.newFixedThreadPool(threadNum);
			for (int i = 0; i < threadNum; i++) {
				executorService.execute(new UserDeepViewProductConsumer().setBootstrapServers(bootstrapServers)
						.setGroupId(groupId).setTopic(topic));
			}
			producer = initProducerInstance(KafkaConfigure.builder().acks(acks).batchSize(batchSize)
					.bootstrapServers(bootstrapServers).bufferMemory(bufferMemory).compressionType(compressionType)
					.lingerMs(lingerMs).retries(retries).build());
			producerTopic = kafkaTopic;
			cacheRedisUtil = FrameSpringBeanUtil.getBean("cacheRedisUtil");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@BProfiler(key = "com.biyao.moses.consumer.UserDeepViewProductConsumer.handleMessage", monitorType = {
			MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR })
	public void handleMessage(ConsumerRecords<String, String> records) {
		for (ConsumerRecord<String, String> record : records) {
			try {
				String message = record.value();
				if (StringUtils.isBlank(message)) {
					continue;
				}
				HashMap<String, String> mapByLogStr = getMapByLogStr(message);
				String pclk = mapByLogStr.get("pclk");
				String pid = mapByLogStr.get("pid");
				String uuid = mapByLogStr.get("uu");
				String pvid = mapByLogStr.get("pvid");
				String pf = mapByLogStr.get("pf");
				if (StringUtils.isNotBlank(pclk) && StringUtils.isNotBlank(pid)
						&& StringUtils.isNotBlank(uuid) && StringUtils.isNotBlank(pvid)) {
					if (StringUtils.isBlank(pf) || "android".equalsIgnoreCase(pf) || "ios".equalsIgnoreCase(pf)) {
						cacheDeepView(pclk, uuid, pvid);
					}
				}
			} catch (Exception e) {
				logger.error("消费深度浏览数据异常：{}，数据内容：{}", e, JSON.toJSONString(record));
			}
		}
	}

	/**
	 * @Description 存储浏览商品行为
	 * @param pclk
	 * @param uuid
	 * @param pvid
	 * @param pf
	 * @version V1.0
	 * @auth 邹立强 (zouliqiang@idstaff.com)
	 */
	private void cacheDeepView(String pclk, String uuid, String pvid) {
		String deepViewKey = CommonConstants.UUID_DEEPVIEW_KEY_PREFIEX + uuid;
		String deepViewJson = cacheRedisUtil.hget(deepViewKey, pvid);
		if (StringUtils.isNotBlank(deepViewJson)) {
			DeepViewLog deepViewLog = JSON.parseObject(deepViewJson, DeepViewLog.class);
			if ("pdetail.banner.slide".equals(pclk)) {
				deepViewLog.setPslipNum(deepViewLog.getPslipNum() + 1);
			} else if ("pdetail_comment.page".equals(pclk)) {
				deepViewLog.setCslipNum(deepViewLog.getCslipNum() + 1);
			} else if ("pdetail_comment.bigimg".equals(pclk)) {
				deepViewLog.setCimgNum(deepViewLog.getCimgNum() + 1);
			} else if ("click.favorsu".equals(pclk)) {
				deepViewLog.setFav(1);
			} else if ("pdetail.addshopcart".equals(pclk)) {
				deepViewLog.setCart(1);
			} else if ("pdetail.comment".equals(pclk)) {
				deepViewLog.setCmt(1);
			} else if ("pdetail.imgcomment".equals(pclk)) {
				deepViewLog.setPcmt(1);
			} else if ("pdetail.detail.tab".equals(pclk)) {
				deepViewLog.setPd(1);
			} else if ("pdetail.banner.bigimg".equals(pclk)) {
				deepViewLog.setMpic(1);
			} else {
				deepViewLog.setBuy(1);
			}
			cacheRedisUtil.hset(deepViewKey, pvid, JSON.toJSONString(deepViewLog));
			cacheRedisUtil.expire(deepViewKey, 3600);
		} else {
			DeepViewLog deepViewLog = new DeepViewLog();
			deepViewLog.setSt(formatter.format(new Date()));
			cacheRedisUtil.hset(deepViewKey, pvid, JSON.toJSONString(deepViewLog));
			cacheRedisUtil.expire(deepViewKey, 3600);
		}
	}
}