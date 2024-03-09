package com.biyao.moses.consumer;

import com.alibaba.fastjson.JSON;
import com.biyao.moses.bean.DeepViewLog;
import com.biyao.moses.bean.KafkaConfigure;
import com.biyao.moses.consumer.template.KafkaConsumerService;
import com.biyao.moses.consumer.template.KafkaConsumerTemplate;
import com.biyao.moses.util.StringUtil;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import com.uc.domain.constant.UserFieldConstants;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @Description 新深度浏览操作
 * @date 2020年2月4日下午6:06:33
 * @version V1.0
 * @author 邹立强 (zouliqiang@idstaff.com)
 */
@Component
public class DeepViewNewConsumer extends KafkaConsumerTemplate implements KafkaConsumerService {

	private Logger logger = LoggerFactory.getLogger(DeepViewNewConsumer.class);

	@Value("${kafka.bootstrapServers}")
	private String bootstrapServers;
	@Value("${kafka.new.bootstrapServers}")
	private String newBootstrapServers;
	@Value("${kafka.consumer.newdeepview.groupId}")
	private String groupId;
	@Value("${kafka.consumer.newdeepview.topic}")
	private String topic;
	@Value("${kafka.consumer.newdeepview.threadNum}")
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
	
	@Override
	public void init() {
		try {
			executorService = Executors.newFixedThreadPool(threadNum);
			for (int i = 0; i < threadNum; i++) {
				executorService.execute(new DeepViewNewConsumer().setBootstrapServers(newBootstrapServers)
						.setGroupId(groupId).setTopic(topic));
			}
			producer = initProducerInstance(KafkaConfigure.builder().acks(acks).batchSize(batchSize)
					.bootstrapServers(bootstrapServers).bufferMemory(bufferMemory).compressionType(compressionType)
					.lingerMs(lingerMs).retries(retries).build());
			producerTopic = kafkaTopic;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@BProfiler(key = "com.biyao.moses.consumer.DeepViewNewConsumer.handleMessage", monitorType = { MonitorType.TP,
			MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR })
	public void handleMessage(ConsumerRecords<String, String> records) {
		for (ConsumerRecord<String, String> record : records) {
			try {
				String message = record.value();
				if (StringUtils.isBlank(message)) {
					continue;
				}
				HashMap<String, String> mapByLogStr = getMapByLogStr(message);
				String uuid = mapByLogStr.get("uu");
				String pid = mapByLogStr.get("pid");
				String st = mapByLogStr.get("st");
				Long time = StringUtil.isBlank(st) ? System.currentTimeMillis() :formatter.parse(st).getTime() ;
				String stid = mapByLogStr.get("stid");
				if (!StringUtil.isBlank(uuid) && !StringUtil.isBlank(pid) && !StringUtil.isBlank(stid)
						&& ("7".equals(stid) || "9".equals(stid))) {
					String pslipNumStr = mapByLogStr.get("pslipNum");
					Integer pslipNum = StringUtil.isBlank(pslipNumStr) ? 0 : Integer.parseInt(pslipNumStr);
					String cslipNumStr = mapByLogStr.get("cslipNum");
					Integer cslipNum = StringUtil.isBlank(cslipNumStr) ? 0 : Integer.parseInt(cslipNumStr);
					String cimgNumStr = mapByLogStr.get("cimgNum");
					Integer cimgNum = StringUtil.isBlank(cimgNumStr) ? 0 : Integer.parseInt(cimgNumStr);
					String favStr = mapByLogStr.get("fav");
					Integer fav = StringUtil.isBlank(favStr) ? 0 : Integer.parseInt(favStr);
					String cartStr = mapByLogStr.get("cart");
					Integer cart = StringUtil.isBlank(cartStr) ? 0 : Integer.parseInt(cartStr);
					String cmtStr = mapByLogStr.get("cmt");
					Integer cmt = StringUtil.isBlank(cmtStr) ? 0 : Integer.parseInt(cmtStr);
					String pcmtStr = mapByLogStr.get("pcmt");
					Integer pcmt = StringUtil.isBlank(pcmtStr) ? 0 : Integer.parseInt(pcmtStr);
					String pdStr = mapByLogStr.get("pd");
					Integer pd = StringUtil.isBlank(pdStr) ? 0 : Integer.parseInt(pdStr);
					String mpicStr = mapByLogStr.get("mpic");
					Integer mpic = StringUtil.isBlank(mpicStr) ? 0 : Integer.parseInt(mpicStr);
					String buyStr = mapByLogStr.get("buy");
					Integer buy = StringUtil.isBlank(buyStr) ? 0 : Integer.parseInt(buyStr);
					DeepViewLog deepView = new DeepViewLog(pslipNum, cslipNum, cimgNum, fav, cart, cmt, pcmt, pd, mpic,
							buy);
					Boolean isDeepView = checkDeepView(deepView);
					if(isDeepView) {
						StringBuffer msg = new StringBuffer();
						msg.append("uuid=").append(uuid).append("\t");
						msg.append(UserFieldConstants.VIEWPIDS).append("=").append(pid).append(":")
								.append(time);
						sendKafka(producer, producerTopic, msg.toString());
					}
				}
			} catch (Exception e) {
				logger.error("消费深度浏览数据异常：{}，数据内容：{}", e, JSON.toJSONString(record));
			}
		}
	}

	/**
	 * @Description 校验是否深度浏览
	 * @param deepView
	 * @return Boolean
	 * @version V1.0
	 * @auth 邹立强 (zouliqiang@idstaff.com)
	 */
	private Boolean checkDeepView(DeepViewLog deepView) {
		if (deepView.getPd() > 0 || deepView.getMpic() > 0 || deepView.getCmt() > 0 || deepView.getPcmt() > 0
				|| deepView.getCart() > 0 || deepView.getFav() > 0 || deepView.getBuy() > 0
				|| deepView.getPslipNum() > 3 || deepView.getCimgNum() > 0 || deepView.getCslipNum() > 0) {
			return true;
		}
		return false;
	}

}
