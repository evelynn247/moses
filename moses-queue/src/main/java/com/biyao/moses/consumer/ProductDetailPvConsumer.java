package com.biyao.moses.consumer;

import com.alibaba.fastjson.JSON;
import com.biyao.dclog.producer.service.DclogProducer;
import com.biyao.moses.bean.DeepViewLog;
import com.biyao.moses.common.constant.CommonConstants;
import com.biyao.moses.consumer.template.FrameSpringBeanUtil;
import com.biyao.moses.consumer.template.KafkaConsumerService;
import com.biyao.moses.consumer.template.KafkaConsumerTemplate;
import com.biyao.moses.util.CacheRedisUtil;
import com.biyao.moses.util.StringUtil;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
* @Description 通过pv日志记录浏览商品信息
* @date 2020年1月2日下午7:37:35
* @version V1.0 
* @author 邹立强 (zouliqiang@idstaff.com)
 */
@Component
public class ProductDetailPvConsumer extends KafkaConsumerTemplate implements KafkaConsumerService{
	

	private Logger logger = LoggerFactory.getLogger(ProductDetailPvConsumer.class);

	@Value("${kafka.bootstrapServers}")
	private String bootstrapServers;

	@Value("${kafka.consumer.pv.groupId}")
	private String groupId;

	@Value("${kafka.consumer.pv.topic}")
	private String topic;
	
	@Value("${kafka.consumer.pv.threadNum}")
	private Integer threadNum;
	DclogProducer dclog = DclogProducer.getLogger("deepview_log");
	private ExecutorService executorService;
	private static CacheRedisUtil cacheRedisUtil;
	
	@Override
	public void init() {
		try {
			executorService = Executors.newFixedThreadPool(threadNum);
			for (int i = 0; i < threadNum; i++) {
				executorService.execute(new ProductDetailPvConsumer().setBootstrapServers(bootstrapServers)
						.setGroupId(groupId).setTopic(topic));
			}
			cacheRedisUtil = FrameSpringBeanUtil.getBean("cacheRedisUtil");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
    @BProfiler(key = "ProductDetailPvConsumer.handleMessage", monitorType = { MonitorType.TP,
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
				String u = mapByLogStr.get("u");
				String pvid = mapByLogStr.get("pvid");
				String pid = mapByLogStr.get("pid");
				String spm = mapByLogStr.get("spm");
				String stid = mapByLogStr.get("stid");
				String st = mapByLogStr.get("st");
				if (StringUtils.isNotBlank(uuid)&&StringUtils.isNotBlank(pid)
					&&StringUtils.isNotBlank(stid)&&!"1".equals(stid)) {
					if("2".equals(stid)){
					    printViewLog(uuid,u,pid,pvid,stid,spm,st);
					}else if("7".equals(stid)||"9".equals(stid)){
						String deepViewKey = CommonConstants.UUID_DEEPVIEW_KEY_PREFIEX + uuid;
						DeepViewLog deepViewLog = new DeepViewLog();
						deepViewLog.setSt(st);
						cacheRedisUtil.hset(deepViewKey, pvid, JSON.toJSONString(deepViewLog));
						cacheRedisUtil.expire(deepViewKey, 3600);  
					}
					
				}
			} catch (Exception e) {
				logger.error("进入商品详情页事件数据异常：{}，数据内容：{}", e, JSON.toJSONString(record));
			}
		}
	}
    
    private void printViewLog( String uuid, String u, String pid, String pvid, String stid,
			String spm,String st) {
		StringBuffer sb = new StringBuffer();
		u = StringUtil.isBlank(u) ? "" : u;
		uuid = StringUtil.isBlank(uuid) ? "" : uuid;
		pid = StringUtil.isBlank(pid) ? "" : pid;
		pvid = StringUtil.isBlank(pvid) ? "" : pvid;
		stid = StringUtil.isBlank(stid) ? "" : stid;
		spm = StringUtil.isBlank(spm) ? "" : spm;
		sb.append("lt=deepview_log").append("\tst=").append(st).append("\tuu=").append(uuid).append("\tu=").append(u).append("\tpid=").append(pid).append("\tpvid=")
				.append(pvid).append("\tstid=").append(stid).append("\tspm=").append(spm).append("\tpslipNum=")
				.append(0).append("\tcslipNum=").append(0).append("\tcimgNum=").append(0)
				.append("\tfav=").append(0).append("\tcart=").append(0).append("\tcmt=").append(0)
				.append("\tpcmt=").append(0).append("\tpd=").append(0).append("\tmpic=").append(0)
				.append("\tbuy=").append(0).append("\tit=").append(0).append("\tot=").append(0);
		dclog.printDCLog(sb.toString());
	}

}
