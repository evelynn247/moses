package com.biyao.moses.consumer;

import com.alibaba.fastjson.JSON;
import com.biyao.moses.bean.KafkaConfigure;
import com.biyao.moses.consumer.template.KafkaConsumerService;
import com.biyao.moses.consumer.template.KafkaConsumerTemplate;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
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
* @Description 商品详情页事件转发
* @date 2019年7月16日上午11:21:36
* @version V1.0 
* @author 邹立强 (zouliqiang@idstaff.com)
 */
@Component
public class ProductDetailUbConsumer extends KafkaConsumerTemplate implements KafkaConsumerService{
	
	private Logger logger = LoggerFactory.getLogger(ProductDetailUbConsumer.class);
	
	@Value("${kafka.bootstrapServers}")
	private String bootstrapServers;
	@Value("${kafka.consumer.ub.groupId}")
	private String groupId;
	@Value("${kafka.consumer.ub.topic}")
	private String topic;
	@Value("${kafka.consumer.ub.threadNum}")
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
	@Value("${kafka.consumer.deepview.topic}")
	private  String kafkaTopic;
	private  static String producerTopic;
	private  static KafkaProducer<String, String> producer;
	
	/**
	 * pdetail.detail.tab  详情页tab切换
       pdetail.comment 详情页评论
       pdetail.imgcomment 点击有图评论
       pdetail_comment.bigimg 评论图点击放大
       yqp_kt_details.event_buy_directly 单独购买
       yqp_kt_details.event_buy_cheaper  邀请新人拼团
       yqp_kt_details.event_ct_button 一起拼开团
       yqp_ct_details.event_ct_button 一起拼参团
       pdetail.cservice  客服
       pdetail.buynow 立即购买
       pdetail.addshopcart 加入购物车
       click.favorsu 收藏
       newuser_details.event_click 新手价购买
       pdetail.banner.slide 滑动查看主图（新增）
       pdetail.banner.bigimg 点击主图放大（新增）
       pdetail_comment.page 评论页翻页（新增）
	 */
	private static final Set<String> pclks = new HashSet<String>() {
		private static final long serialVersionUID = -5428011865781096966L;
		{
			add("pdetail.detail.tab");
			add("pdetail.comment");
			add("pdetail.imgcomment");
			add("pdetail_comment.bigimg");
			add("pdetail.addshopcart");
			add("click.favorsu");
			add("pdetail.banner.slide");
			add("pdetail.banner.bigimg");
			add("pdetail_comment.page");
			add("yqp_kt_details.event_buy_directly");
			add("yqp_kt_details.event_buy_cheaper");
			add("yqp_kt_details.event_ct_button");
			add("yqp_ct_details.event_ct_button");
			add("pdetail.buynow");
			add("newuser_details.event_click");
			add("yqp_ct_details.event_ct1_button");
			add("yqp_ct_details.event_ct2_button");
			add("yqp_ct_details.event_ct3_button");
		}
	};
	private ExecutorService executorService;
	
	@Override
	public void init() {
		try {
			executorService = Executors.newFixedThreadPool(threadNum);
			for (int i = 0; i < threadNum; i++) {
				executorService.execute(new ProductDetailUbConsumer().setBootstrapServers(bootstrapServers)
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
    @BProfiler(key = "ProductDetailUbConsumer.handleMessage", monitorType = { MonitorType.TP,
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
				String pid = mapByLogStr.get("pid");
				String uuid = mapByLogStr.get("uu");
				String pf = mapByLogStr.get("pf");
				if (StringUtils.isNotBlank(pclk) && pclks.contains(pclk) && StringUtils.isNotBlank(pid)
						&& StringUtils.isNotBlank(uuid) && !"mweb".equalsIgnoreCase(pf)) {
					sendKafka(producer, producerTopic, message);
				}
			} catch (Exception e) {
				logger.error("商品详情页事件转发数据异常：{}，数据内容：{}", e, JSON.toJSONString(record));
			}
		}
	}


}
