package com.biyao.moses.service;

import com.biyao.moses.util.AlgorithmRedisUtil;
import com.biyao.moses.util.ApplicationContextProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Future;

@Slf4j
@Service
public class AlgorithmRedisAnsyService {

	@Autowired
	AlgorithmRedisUtil algorithmRedisUtil;

	@Autowired
	ApplicationContextProvider applicationContextProvider;

	private final static String U2SHOP = "us";
	private final static String SHOP = "s";
	private final static String U2TOP = "ut";
	private final static String TOPIC = "t";

	@Async
	public Future<HashMap<String, Double>> redisHmget(String key,
			String[] pids, String factor, String topicId) {
		HashMap<String, Double> hashMap = new HashMap<String, Double>();
		try {

			String[] pidKeys = null;
			if (U2SHOP.equals(factor) || SHOP.equals(factor)) {
				pidKeys = new String[pids.length];
				for (int i = 0; i < pidKeys.length; i++) {
					pidKeys[i] = pids[i].substring(0, 6);
				}
			} else if (U2TOP.equals(factor) || TOPIC.equals(factor)) {
				pidKeys = new String[1];
				pidKeys[0] = topicId;
			} else {
				pidKeys = pids;
			}

			List<String> hmget = algorithmRedisUtil.hmget(key, pidKeys);

 			if (hmget == null) {
				log.error("[严重异常]redis获取无商品数据，key={}", key);
				return new AsyncResult<HashMap<String, Double>>(hashMap);
			}

			boolean flag = U2TOP.equals(factor) || TOPIC.equals(factor);
			double topicScore = 0d;
			if (flag) {
				topicScore = StringUtils.isNotBlank(hmget.get(0)) ? Double.valueOf(hmget.get(0)) : 0d;
			}

			for (int i = 0; i < pids.length; i++) {

				if (flag) {
					if (topicScore!=0d) {
						hashMap.put(pids[i], topicScore);
					}
				} else {
					if (StringUtils.isNotEmpty(hmget.get(i))) {
						hashMap.put(pids[i], Double.valueOf(hmget.get(i)));
					}
				}

			}
		} catch (Exception e) {
			log.error("[严重异常]redis获取商品数据错误", e);
			return new AsyncResult<HashMap<String, Double>>(
					new HashMap<String, Double>());
		}
		return new AsyncResult<HashMap<String, Double>>(hashMap);
	}

	@Async
	public Future<HashMap<String, String>> redisHmget(String key,
													  String[] pids) {
		HashMap<String, String> hashMap = new HashMap<String, String>();
		try {

			List<String> scoreList = algorithmRedisUtil.hmget(key, pids);
			for(int i = 0; i < pids.length; i++){
				if(scoreList.get(i) !=null){
					hashMap.put(pids[i],scoreList.get(i));
				}
			}
			if (scoreList == null) {
				log.error("[严重异常]redis获取无商品数据，key={}", key);
				return new AsyncResult<HashMap<String, String>>(hashMap);
			}

		} catch (Exception e) {
			log.error("[严重异常]redis获取商品数据错误", e);
			return new AsyncResult<HashMap<String, String>>(
					new HashMap<String, String>());
		}
		return new AsyncResult<HashMap<String, String>>(hashMap);
	}

}