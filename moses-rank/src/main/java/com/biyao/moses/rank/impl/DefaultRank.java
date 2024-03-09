package com.biyao.moses.rank.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.biyao.moses.common.constant.CommonConstants;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.rank.RankRequest;
import com.biyao.moses.rank.RecommendRank;
import com.biyao.moses.service.RedisAnsyService;
import com.biyao.moses.util.RedisUtil;

/**
 * 基本rank
 * 当前 totalScore = productScore*a + supplierScore*b 
 * 					+ topicScore*c + user2productScore*d 
 * 					+ user2supplierScore*e + user2topicScore*f
 * 
 * redis使用hash存放基于uuid的分值  用户对商品=up 用户对商家=us 用户对topic的分值=ut
 * 用户对商品=up 
 * 一级key moses:rs_up_{rankName}_{dataNum}_{uuid}  二级key {productId}  值 0.89
 * 用户对商家=us
 * 一级key moses:rs_us_{rankName}_{dataNum}_{uuid}  二级key {shopId}  值 0.89   当前用户对商家的分值
 * 用户对商家=us
 * 一级key moses:rs_ut_{rankName}_{dataNum}_{uuid}  二级key {topicId}  值 0.89   当前用户对某个topic的分值
 * 
 * redis使用hash存放静态分值  商品分=p 商家分=s 主题分=t
 * 一级key moses:rs_p_{rankName}_{dataNum}   二级key {productId}  0.66
 * 一级key moses:rs_s_{rankName}_{dataNum}   二级key {shopId}  0.66
 * 一级key moses:rs_t_{rankName}_{dataNum}   二级key {topicId}  0.66    
 * 
 * 
 * redis使用string存放各分值系数 a,b,c,d,e,f....
 * 静态 key=moses:rs_fac_{rankName}_{dataNum} value={"p":"0.2","s":"0.4","t":"0.8"}
 * 动态 key=moses:rs_dfac_{rankName}_{dataNum} value={"up":"0.54","us":"0.43","ut":"0.33"}
 */
@Slf4j
@Component("dr")
public class DefaultRank implements RecommendRank {
	
	@Autowired
	RedisUtil redisUtil;
	
	@Autowired
	RedisAnsyService redisAnsyService;

	@BProfiler(key = "DefaultRank.executeRecommend", monitorType = {
			MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR })
	@Override
	public List<TotalTemplateInfo> executeRecommend(RankRequest rankRequest) {
		
		
		
		List<TotalTemplateInfo> oriData = rankRequest.getOriData();
		try {
			String uuid = rankRequest.getUuid();
			String dataNum = rankRequest.getDataNum();
			String rankName = rankRequest.getRankName();
			String topicId = rankRequest.getTopicId();
			
			log.info("defaultRank请求参数，uuid={},dataNum={},rankName={},topicId={}",uuid,dataNum,rankName,topicId);
			
			ArrayList<String> arrayList = new ArrayList<String>();
			for (TotalTemplateInfo totalTemplateInfo : oriData) {
				if (StringUtils.isNotBlank(totalTemplateInfo.getId())) {
					arrayList.add(totalTemplateInfo.getId());
				}
			}
			if (arrayList.size()==0) {
				return oriData;
			}
			
			String[] pids = new String[arrayList.size()];
			arrayList.toArray(pids);
			
			// 查询静态系数 
			String key = CommonConstants.RANKSCORE_FACTOR+rankName+CommonConstants.SPLIT_LINE+dataNum;
			String staticFactor = redisUtil.getString(key);
			Map<String,String> staticFactorMap = new HashMap<>();
			if (StringUtils.isNotEmpty(staticFactor)) {
				
				try {
					staticFactorMap = JSONObject.parseObject(staticFactor, Map.class);
				} catch (Exception e) {
					log.error("静态系数转换失败staticFactor={}",staticFactor);
				}
			}
			
			// 查询动态系数 
			String dkey = CommonConstants.RANKSCORE_DYNAMIC_FACTOR+rankName+CommonConstants.SPLIT_LINE+dataNum;
			String dynamicFactor = redisUtil.getString(dkey);
			
			Map<String,String> dynamicFactorMap = new HashMap<>();
			if (StringUtils.isNotEmpty(dynamicFactor)) {
				try {
					dynamicFactorMap = JSONObject.parseObject(dynamicFactor, Map.class);
				} catch (Exception e) {
					log.error("动态系数转换失败staticFactor={}",staticFactor);
				}
			}
			
			
			Map<String,Map<String,Double>> scoresMap = new HashMap<String,Map<String,Double>>();
			
			Map<String,Future<HashMap<String, Double>>> futureMap = new HashMap<String,Future<HashMap<String, Double>>>();
			// 查询pid对应静态分值
			for (Entry<String, String> staticFactorEntry : staticFactorMap.entrySet()) {
				String factor = staticFactorEntry.getKey();
				String keyP = CommonConstants.RANKSCORE_PID+factor+CommonConstants.SPLIT_LINE+rankName+CommonConstants.SPLIT_LINE+dataNum;
				
				Future<HashMap<String, Double>> future = redisAnsyService.redisHmget(keyP, pids, factor, topicId);
				futureMap.put(staticFactorEntry.getKey(), future);
			}
			
			// 查询pid对应动态分值
			for (Entry<String, String> dynamicFactorEntry : dynamicFactorMap.entrySet()) {
				String factor = dynamicFactorEntry.getKey();
				String keyP = CommonConstants.RANKSCORE_PID+dynamicFactorEntry.getKey()+CommonConstants.SPLIT_LINE+rankName+CommonConstants.SPLIT_LINE+dataNum+CommonConstants.SPLIT_LINE+uuid;
				Future<HashMap<String, Double>> future = redisAnsyService.redisHmget(keyP, pids, factor, topicId);      
				futureMap.put(dynamicFactorEntry.getKey(), future);
			}
			
			for (Entry<String, Future<HashMap<String, Double>>> futureEntry : futureMap.entrySet()) {
				
				Future<HashMap<String, Double>> future = futureEntry.getValue();
				HashMap<String, Double> result = new HashMap<>();
				try {
					result = future.get(20, TimeUnit.MILLISECONDS);
				} catch (Exception e) {
					log.error("redis获取分数超时");
				}
				scoresMap.put(futureEntry.getKey(), result);
			}
			
			boolean flag = false;
			
			for (TotalTemplateInfo info : oriData) {
				Double calculateScore = calculateScore(info.getId(),staticFactorMap,dynamicFactorMap,scoresMap);
				info.setScore(calculateScore);
				if (calculateScore!=0) {
					flag = true;
				}
			}
			
			//3. 排序
			if (flag) {
				Collections.sort(oriData, new Comparator<TotalTemplateInfo>() {
					@Override
					public int compare(TotalTemplateInfo o1, TotalTemplateInfo o2) {
						if (o1.getScore()>o2.getScore()) {
							return -1;
						}else if(o1.getScore()<o2.getScore()){
							return 1;
						}else{
							return 0;
						}
					}
				});
			}else{
				log.info("计算所有分数为0，无需排序");
			}
		} catch (Exception e) {
			log.error("rank未知错误",e);
		}
//		log.info("排序后商品:{}",JSONObject.toJSONString(oriData));
		return oriData;
	}
	
	private Double calculateScore(String pid,Map<String,String> staticFactorMap,Map<String,String> dynamicFactorMap,Map<String,Map<String,Double>> scoresMap){
		
		Double sum = 0d;
		
		// key1= {us}  key2={pid} value={score}
		//计算静态分值
		if (staticFactorMap!=null && !staticFactorMap.isEmpty()) {
			for (Entry<String, String> factorEntry : staticFactorMap.entrySet()) {
				String factor = factorEntry.getKey();
				Double score = 0d;
				if (scoresMap.containsKey(factor)&&scoresMap.get(factor).containsKey(pid)) {
					score = scoresMap.get(factor).get(pid);
					sum += Double.valueOf(factorEntry.getValue()) * score;
				}
			}
		}
		//计算动态分值
		if (dynamicFactorMap!=null && !dynamicFactorMap.isEmpty()) {
			for (Entry<String, String> dfactorEntry : dynamicFactorMap.entrySet()) {
				String factor = dfactorEntry.getKey();
				Double score = 0d;
				if (scoresMap.containsKey(factor)&&scoresMap.get(factor).containsKey(pid)) {
					score = scoresMap.get(factor).get(pid);
					sum += Double.valueOf(dfactorEntry.getValue()) * score;
				}
			}
		}
		
		return sum;
	}
	public static void main(String[] args) {
		
		ArrayList<Object> arrayList = new ArrayList<>();
		arrayList.add(0);
		arrayList.add(1);
//		arrayList.add(2);
//		arrayList.add(3);
//		arrayList.add(4);
//		arrayList.add(5);
//		arrayList.add(6);
//		arrayList.add(7);
//		arrayList.add(8);
//		arrayList.add(9);
		
		int pageNum = 3;
		int totalPage = arrayList.size()%pageNum == 0 ? arrayList.size()/pageNum : arrayList.size()/pageNum + 1;
		System.out.println(totalPage);
		
		System.out.println(JSONObject.toJSONString(arrayList));
		for (int i = 1; i <= totalPage; i++) {
			
			int fromIndex = (i - 1) * pageNum;
//			fromIndex = fromIndex > arrayList.size()-1?arrayList.size()-1:fromIndex;
			int toIndex = i * pageNum;
			toIndex = toIndex > arrayList.size()?arrayList.size():toIndex;
			List<Object> subList  = arrayList.subList(fromIndex, toIndex);
			Collections.shuffle(subList);
			System.out.println(fromIndex+"-"+toIndex+"==="+JSONObject.toJSONString(subList));
			
			
		}
		System.out.println(JSONObject.toJSONString(arrayList));
		
//		for (int i = 0; i < arrayList.size(); i++) {
//			
//			int fromIndex = i * pageNum;
//			fromIndex = fromIndex > arrayList.size()-1?arrayList.size()-1:fromIndex;
//			int toIndex = (i+1) * pageNum;
//			toIndex = toIndex > arrayList.size()-1?arrayList.size()-1:toIndex;
//			System.out.println(JSONObject.toJSONString(arrayList.subList(fromIndex, toIndex)));
//		}
		
		
	}
}