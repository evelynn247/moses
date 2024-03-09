package com.biyao.moses.consumer;

import com.alibaba.fastjson.JSONObject;
import com.biyao.moses.bean.KafkaConfigure;
import com.biyao.moses.consumer.template.MqConsumerTemplate;
import com.biyao.orderquery.client.tob.IBOrderDetailQueryService;
import com.biyao.orderquery.client.tob.beans.BOrderDetailDTO;
import com.biyao.orderquery.client.tob.beans.BOrderSuSnapDTO;
import com.biyao.orderquery.client.tob.beans.Result;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import com.google.common.base.Charsets;
import com.uc.domain.constant.UserFieldConstants;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @Description 消费支付消息
 * @date 2019年10月12日上午11:35:03
 * @version V1.0 
 * @author 邹立强 (zouliqiang@idstaff.com)
 * <p>Copyright (c) Department of Research and Development/Beijing.</p>
 */
@Component
public class UserOrderConsumer extends MqConsumerTemplate {

	private Logger logger = LoggerFactory.getLogger(UserOrderConsumer.class);

	@Value("${rocketmq.server.namesrvAddr}")
	private String namesrvAddr;
	@Value("${rocketmq.user.pay.product.consumer.group}")
	private String customerGroup;
	@Value("${rocketmq.user.pay.product.topic}")
	private String topic;
	@Value("${rocketmq.user.pay.product.tags}")
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
	@Autowired
	private IBOrderDetailQueryService iBOrderDetailQueryService;
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
			        JSONObject json = JSONObject.parseObject(msgBody);
			        Long customerId = json.getLong("customerId");
			        Long orderId = json.getLong("orderId");
			        Long paidTime = json.getLong("paidTime");
					if (customerId!=null && orderId!=null&& paidTime!=null ) {
						HashSet<Long> orderSet = new HashSet<Long>();
						orderSet.add(orderId);
						Result<Map<Long, List<BOrderDetailDTO>>> queryByOrderIds = null;
						try {
							queryByOrderIds = iBOrderDetailQueryService.queryByOrderIds(orderSet);
						} catch (Exception e) {
							logger.error("通过orderId查询订单信息失败:msg={} ,e：{}", messageExt, e);
							e.printStackTrace();
						}
						if (queryByOrderIds == null) {
							continue;
						}
						Map<Long, List<BOrderDetailDTO>> map = queryByOrderIds.getObj();
						if (map != null) {
							List<BOrderDetailDTO> list = map.get(orderId);
							if (CollectionUtils.isNotEmpty(list)) {
								for (BOrderDetailDTO bOrderDetailDTO : list) {
									BOrderSuSnapDTO suSnapInfo = bOrderDetailDTO.getSuSnapInfo();
									if (suSnapInfo == null) {
										continue;
									}
									long suId = suSnapInfo.getSuId();
									if (suId != 0L) {
										String pid = Long.valueOf(suId).toString().substring(0, 10);
										StringBuffer msgStr = new StringBuffer();
										msgStr.append("uid=").append(customerId).append("\t")
												.append(UserFieldConstants.ORDERPIDS).append("=").append(pid)
												.append(":").append(paidTime);
										sendKafka(producer, producerTopic, msgStr.toString());
									}
								}
							}
						}
					}
				}
			} catch (Exception e) {
				logger.error("消费下单支付消息失败:msg={} ,e：{}", messageExt, e);
			}
		}
	}
}