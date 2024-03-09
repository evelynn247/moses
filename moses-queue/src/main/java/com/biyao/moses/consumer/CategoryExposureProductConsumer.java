package com.biyao.moses.consumer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.biyao.moses.consumer.template.MqConsumerTemplate;
import com.biyao.moses.util.CacheRedisUtil;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import com.google.common.base.Charsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author 邹立强 (zouliqiang@idstaff.com)
 * @version V1.0
 * @Description 曝光商品消费
 * @date 2019年6月13日下午7:22:53
 */
@Component
public class CategoryExposureProductConsumer extends MqConsumerTemplate {

	private Logger logger = LoggerFactory.getLogger(CategoryExposureProductConsumer.class);

	@Autowired
	private CacheRedisUtil cacheRedisUtil;
	@Value("${rocketmq.server.namesrvAddr}")
	private String namesrvAddr;
	@Value("${rocketmq.category.exposure.product.consumer.group}")
	private String customerGroup;
	@Value("${rocketmq.category.exposure.product.topic}")
	private String topic;
	@Value("${rocketmq.category.exposure.product.tags}")
	private String tags = "*";
	private static final String KEY_PREFIEX = "moses:user_category_";
	private static final int NUMBER = 80;
	private static final int TIME_EXPIRE = 259200;// 60*60*24*3 3天秒数

	@PostConstruct
	public void init() {
		initConsumer(namesrvAddr, customerGroup, topic, tags);
	}

	@BProfiler(key = "CategoryExposureProductConsumer.handleMessage", monitorType = { MonitorType.TP,
			MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR })
	public void handleMessage(List<MessageExt> msgs) {

		for (MessageExt messageExt : msgs) {
			try {
				// 日志格式
				/*
				 * pageId=moses:pid_30 realPid=moses:pid_30 fcid=4543 pageIndex=1
				 * blockId=moses:bid_202 topicId=10300148 u=10364868 avn=256
				 * pvid=1f7d8ab7-1560418186604-dc7asi1o5j did=1f7d8ab7c9e985bf d=
				 * uniqid=5120984ac818690a.1560418188281 catIds=32205,80032213,80032214
				 * trace={"9d860ba6ae01f983.1560418188290":{"expId":
				 * "moses:10300148_match.nocache.category.04121136","keys":[
				 * "moses:10300148_CM_0000"],"pids":
				 * "1301075516,1301075396,1302505022,1301515073,1301075372,1302505021,1301515052,1301045263,1302505038,1300835405,1301075366,1301075398,1301075354,1301075348,1301515081,1301515060,1301515184,1301515056,1301075386,1301075506,"
				 * }} rankTrace={"9d860ba6ae01f983.1560418188290":{"expId":
				 * "rank.nocache.category.04121136","keys":["sr_0100"]}}
				 * ctp={"av":"5.2.0","avn":"256","b":"","d":"XiaomiMI 9 Transparent Edition"
				 * ,"dh":"2135","did":"1f7d8ab7c9e985bf","dw":"1080","lat":"","lng":"",
				 * "os":"android 9","p":"9-100004-500027","pf":"android","pvid":
				 * "1f7d8ab7-1560418186604-dc7asi1o5j","ssid":"1f7d8ab7c9e985bf-1560417415508",
				 * "stid":"9","u":"10364868","utm":"","uu":
				 * "9190613171655f58c799e3e407ddd0000000"}
				 * stp={"spm":"9.500002.category_tree.2_1","rpvid":
				 * "1f7d8ab7-1560418176884-oggw4n3dwe"}
				 */
				String msgBody = new String(messageExt.getBody(), Charsets.UTF_8);
				Map<String, String> logMap = getMapByLogStr(msgBody);
				String pageIndex = logMap.get("pageIndex");
				String topicId = logMap.get("topicId");
				String uuid = logMap.get("uu");
				String fcid = logMap.get("fcid");
				String traceJson = logMap.get("trace");
				// 过滤非类别数据和第一页数据
				if (StringUtils.isBlank(pageIndex) || StringUtils.isBlank(topicId) || !"1".equals(pageIndex)
						|| !"10300148".equals(topicId)) {
					continue;
				}
				// 过滤非空数据
				if (StringUtils.isBlank(uuid) || StringUtils.isBlank(fcid) || StringUtils.isBlank(traceJson)) {
					continue;
				}
				JSONObject jsonObject = JSON.parseObject(traceJson);
				Collection<Object> collections = jsonObject.values();
				String traceJsonValue = null;
				JSONObject traceJsonValueObject = null;
				String pids = null;
				String[] pidsSZ = null;
				// 获取商品集合
				if (collections != null && collections.size() > 0) {
					for (Object traceValue : collections) {
						if (traceValue != null) {
							traceJsonValue = traceValue.toString();
							traceJsonValueObject = JSON.parseObject(traceJsonValue);
						}
						if (traceJsonValueObject != null) {
							Object pidsO = traceJsonValueObject.get("pids");
							if (pidsO != null) {
								pids = (String) pidsO;
								List<String> list = Arrays.asList(pids.split(","));
								if (list.size() >= 8) {
									pidsSZ = list.subList(0, 8).toArray(new String[0]);
								} else {
									pidsSZ = list.toArray(new String[0]);
								}
								break;
							}
						}
					}
				}

				if (pidsSZ != null && pidsSZ.length > 0) {
					String key = KEY_PREFIEX + uuid + "_" + fcid;
					Set<String> pidSet = cacheRedisUtil.smems(key);
					if (pidSet != null && pidSet.size() > NUMBER) {
						cacheRedisUtil.del(key);
					} else {
						cacheRedisUtil.batchSadd(key, pidsSZ);
						cacheRedisUtil.expire(key, TIME_EXPIRE);
					}
				}
			} catch (Exception e) {
				logger.error("消费类别曝光商品消息失败:msg={}.", messageExt, e);
			}
		}
	}
}
