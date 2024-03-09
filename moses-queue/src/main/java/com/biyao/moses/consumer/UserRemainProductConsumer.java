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
import java.util.Map;
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
 * @Description 离开商详页
 * @date 2020年1月1日下午10:21:27
 * @version V1.0
 * @author 邹立强 (zouliqiang@idstaff.com)
 */
@Component
public class UserRemainProductConsumer extends KafkaConsumerTemplate implements KafkaConsumerService {

	private Logger logger = LoggerFactory.getLogger(UserRemainProductConsumer.class);

	@Value("${kafka.bootstrapServers}")
	private String bootstrapServers;
	@Value("${kafka.consumer.remain.groupId}")
	private String groupId;
	@Value("${kafka.consumer.remain.topic}")
	private String topic;
	@Value("${kafka.consumer.remain.threadNum}")
	private Integer threadNum;
	private ExecutorService executorService;
	DclogProducer dclog = DclogProducer.getLogger("deepview_log");
	private static CacheRedisUtil cacheRedisUtil;

	public void init() {
		try {
			executorService = Executors.newFixedThreadPool(threadNum);
			for (int i = 0; i < threadNum; i++) {
				executorService.execute(new UserRemainProductConsumer().setBootstrapServers(bootstrapServers)
						.setGroupId(groupId).setTopic(topic));
			}
			cacheRedisUtil = FrameSpringBeanUtil.getBean("cacheRedisUtil");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@BProfiler(key = "com.biyao.moses.consumer.UserRemainProductConsumer.handleMessage", monitorType = { MonitorType.TP,
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
				String pvid = mapByLogStr.get("pvid");
				String pid = mapByLogStr.get("pid");
				String u = mapByLogStr.get("u");
				String stid = mapByLogStr.get("stid");
				String spm = mapByLogStr.get("spm");
				String it = mapByLogStr.get("it");
				String ot = mapByLogStr.get("ot");
				if (StringUtils.isNotBlank(pid) && StringUtils.isNotBlank(pvid) && StringUtils.isNotBlank(uuid)) {
					String deepViewKey = CommonConstants.UUID_DEEPVIEW_KEY_PREFIEX + uuid;
					Map<String, String> viewMsg = cacheRedisUtil.hgetAll(deepViewKey);
					if (viewMsg != null) {
						for (Map.Entry<String, String> m : viewMsg.entrySet()) {
							String odPvid = m.getKey();
							String deepViewJson = m.getValue();
							String otStr = "0";
							String itStr = "0";
							if (pvid.equalsIgnoreCase(odPvid)) {
								otStr = ot;
								itStr = it;
							}
							if (!StringUtil.isBlank(deepViewJson)) {
								DeepViewLog deepViewLog = JSON.parseObject(deepViewJson, DeepViewLog.class);
								printViewLog(deepViewLog, uuid, u, pid, odPvid, stid, spm, itStr, otStr);
							}
						}
						cacheRedisUtil.del(deepViewKey);
					}
				}
			} catch (Exception e) {
				logger.error("停留日志数据异常：{}，数据内容：{}", e, JSON.toJSONString(record));
			}
		}
	}

	/**
	 * @Description 打印dclog
	 * @param deepViewLog
	 * @param uuid
	 * @param u
	 * @param pid
	 * @param pvid
	 * @param stid
	 * @param spm
	 * @param it
	 * @param ot
	 * @param st
	 * @return void
	 * @version V1.0
	 * @auth 邹立强 (zouliqiang@idstaff.com)
	 */
	private void printViewLog(DeepViewLog deepViewLog, String uuid, String u, String pid, String pvid, String stid,
			String spm, String it, String ot) {
		StringBuffer sb = new StringBuffer();
		String uStr = u == null ? "" : u.toString();
		uuid = StringUtil.isBlank(uuid) ? "" : uuid;
		pid = StringUtil.isBlank(pid) ? "" : pid;
		pvid = StringUtil.isBlank(pvid) ? "" : pvid;
		stid = StringUtil.isBlank(stid) ? "" : stid;
		spm = StringUtil.isBlank(spm) ? "" : spm;
		it = StringUtil.isBlank(it) ? "" : it;
		ot = StringUtil.isBlank(ot) ? "" : ot;
		String st = StringUtil.isBlank(deepViewLog.getSt()) ? "" : deepViewLog.getSt();
		Integer pslipNum = deepViewLog.getPslipNum();
		Integer cslipNum = deepViewLog.getCslipNum();
		Integer cimgNum = deepViewLog.getCimgNum();
		Integer fav = deepViewLog.getFav();
		Integer cart = deepViewLog.getCart();
		Integer cmt = deepViewLog.getCmt();
		Integer pcmt = deepViewLog.getPcmt();
		Integer pd = deepViewLog.getPd();
		Integer mpic = deepViewLog.getMpic();
		Integer buy = deepViewLog.getBuy();
		sb.append("lt=deepview_log").append("\tst=").append(st).append("\tuu=").append(uuid).append("\tu=").append(uStr)
				.append("\tpid=").append(pid).append("\tpvid=").append(pvid).append("\tstid=").append(stid)
				.append("\tspm=").append(spm).append("\tpslipNum=").append(pslipNum).append("\tcslipNum=")
				.append(cslipNum).append("\tcimgNum=").append(cimgNum).append("\tfav=").append(fav).append("\tcart=")
				.append(cart).append("\tcmt=").append(cmt).append("\tpcmt=").append(pcmt).append("\tpd=").append(pd)
				.append("\tmpic=").append(mpic).append("\tbuy=").append(buy).append("\tit=").append(it).append("\tot=")
				.append(ot);
		dclog.printDCLog(sb.toString());
	}
}
