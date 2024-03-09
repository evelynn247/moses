package com.biyao.moses.cache;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.biyao.moses.common.constant.RedisKeyConstant;
import com.biyao.moses.util.RedisUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BlackListCacheNoCron {

	@Autowired
	RedisUtil redisUtil;

	// 全局黑名单
	private static Map<String, String> blackListAllMapStatic = new HashMap<>();
	// page黑名单
	private static Map<String, Map<String,String>> blackListPageMapStatic = new HashMap<>();
	// topic黑名单
	private static Map<String, Map<String,String>> blackListTopicMapStatic = new HashMap<>();
	// uuid黑名单
	private static Map<String, Map<String,String>> blackListUserMapStatic = new HashMap<>();
	
	// @PostConstruct
	public void init() {
		refreshBlackCache();
	}

	public static boolean getBlackListAllProduct(String spuId) {
		if(blackListAllMapStatic!=null &&!StringUtils.isEmpty(blackListAllMapStatic.get(spuId))) {
			return true;
		}
		return false;
		
	}
	
	public static boolean getBlackListPageProduct(String pageId,String spuId) {
		boolean flag=false;
		if(blackListPageMapStatic!=null &&blackListPageMapStatic.containsKey(pageId) ) {
			if(!StringUtils.isEmpty(blackListPageMapStatic.get(pageId).get(spuId))) {
				flag=true;
			}
		}
		return flag;
	}
	
	
	public static boolean getBlackListUserProduct(String uuid,String spuId) {
		boolean flag=false;
		if(blackListUserMapStatic!=null &&blackListUserMapStatic.containsKey(uuid) ) {
			if(!StringUtils.isEmpty(blackListUserMapStatic.get(uuid).get(spuId))) {
				flag=true;
			}
		}
		return flag;
	}
	
	public static boolean getBlackListTopicProduct(String topicId,String spuId) {
		boolean flag=false;
		if(blackListTopicMapStatic!=null &&blackListTopicMapStatic.containsKey(topicId) ) {
			if(!StringUtils.isEmpty(blackListTopicMapStatic.get(topicId).get(spuId))) {
				flag=true;
			}
		}
		return flag;
	}
	
	
	// @Scheduled(cron = "0 0/2 * * * ?")
	public void refreshBlackCache() {

		String redisBlackListall = redisUtil.getString(RedisKeyConstant.MOSES_BLACK_lIST_ALL_KEY);
		if (!StringUtils.isEmpty(redisBlackListall)) {
			String[] blackListAll = redisBlackListall.split(",");
			List<String> asList = Arrays.asList(blackListAll);
			Map<String, String> blackListAllMaps = asList.stream()
					.collect(Collectors.toMap(i -> i, Function.identity(), (key1, key2) -> key2));
			blackListAllMapStatic=blackListAllMaps;
		}else {
			blackListAllMapStatic = new HashMap<>();
		}
		
		Map<String, String> pageMap = redisUtil.hgetAll(RedisKeyConstant.MOSES_BLACK_lIST_PAGE_KEY);
		if (pageMap != null && !pageMap.isEmpty()) {
			
			Map<String, Map<String,String>> newPageMap=new HashMap<>();
			for (Map.Entry<String, String> spuIdsMap : pageMap.entrySet()) {
				Map<String,String> map= new HashMap<>();
				String [] spuIds=spuIdsMap.getValue().split(",");
				for (int j = 0; j < spuIds.length; j++) {
					map.put(spuIds[j], spuIds[j]);
				}
				newPageMap.put(spuIdsMap.getKey(), map);
			 }
			blackListPageMapStatic=newPageMap;
		}else {
			blackListPageMapStatic= new HashMap<>(); 
		}

		Map<String, String> topicMap = redisUtil.hgetAll(RedisKeyConstant.MOSES_BLACK_lIST_TOPIC_KEY);
		if (topicMap != null && !topicMap.isEmpty()) {
			Map<String, Map<String,String>> newTopicMap=new HashMap<>();
			for (Map.Entry<String, String> spuIdsMap : topicMap.entrySet()) {
				Map<String,String> map= new HashMap<>();
				String [] spuIds=spuIdsMap.getValue().split(",");
				for (int j = 0; j < spuIds.length; j++) {
					map.put(spuIds[j], spuIds[j]);
				}
				newTopicMap.put(spuIdsMap.getKey(), map);
			 }
			blackListTopicMapStatic=newTopicMap;
		}else {
			blackListTopicMapStatic= new HashMap<>();
		}

		Map<String, String> userMap = redisUtil.hgetAll(RedisKeyConstant.MOSES_BLACK_lIST_USER_KEY);
		if (userMap != null && !userMap.isEmpty()) {
			
			Map<String, Map<String,String>> newUserMap=new HashMap<>();
			for (Map.Entry<String, String> spuIdsMap : userMap.entrySet()) {
				Map<String,String> map= new HashMap<>();
				String [] spuIds=spuIdsMap.getValue().split(",");
				for (int j = 0; j < spuIds.length; j++) {
					map.put(spuIds[j], spuIds[j]);
				}
				newUserMap.put(spuIdsMap.getKey(), map);
			 }
			
			blackListUserMapStatic=newUserMap;
		}else {
			blackListUserMapStatic=new HashMap<>();
		}

	}
}