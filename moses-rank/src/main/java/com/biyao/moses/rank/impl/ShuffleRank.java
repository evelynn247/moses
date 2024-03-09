package com.biyao.moses.rank.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.biyao.moses.common.constant.CommonConstants;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.rank.RankRequest;

@Slf4j
@Component("sr")
public class ShuffleRank extends DefaultRank {

	@BProfiler(key = "ShuffleRank.executeRecommend", monitorType = {
			MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR })
	@Override
	public List<TotalTemplateInfo> executeRecommend(RankRequest rankRequest) {

		log.info("ShuffleRank请求参数，uuid={}", rankRequest.getUuid());
		rankRequest.setRankName("dr");
		List<TotalTemplateInfo> executeRecommend = super.executeRecommend(rankRequest);
		breakRank(executeRecommend);
		return executeRecommend;
	}

	// 按最小分页数打乱排序
	public void breakRank(List<TotalTemplateInfo> rankResult) {
		if (rankResult != null && !rankResult.isEmpty()) {
			int totalPage = rankResult.size() % CommonConstants.PAGENUM == 0
					? rankResult.size() / CommonConstants.PAGENUM
					: rankResult.size() / CommonConstants.PAGENUM + 1;
			for (int i = 1; i <= totalPage; i++) {
				int fromIndex = (i - 1) * CommonConstants.PAGENUM;
				int toIndex = i * CommonConstants.PAGENUM;
				toIndex = toIndex > rankResult.size()
						? rankResult.size()
						: toIndex;
				List<TotalTemplateInfo> subList = rankResult.subList(fromIndex,	toIndex);
				Collections.shuffle(subList);
			}
		}
	}

	public static void main(String[] args) {
		ArrayList<TotalTemplateInfo> arrayList = new ArrayList<>();
		
		for (int i = 0; i < 50; i++) {
			TotalTemplateInfo totalTemplateInfo = new TotalTemplateInfo();
			totalTemplateInfo.setId(i+"");
			totalTemplateInfo.setHeight(null);
			arrayList.add(totalTemplateInfo);
		}
		System.out.println(JSONObject.toJSONString(arrayList));
		new ShuffleRank().breakRank(arrayList);
		
		HashSet<String> hashSet = new HashSet<String>();
		
		for (int i = 0; i < arrayList.size(); i++) {
			hashSet.add(arrayList.get(i).getId());
		}
		System.out.println(hashSet.size());
		System.out.println(JSONObject.toJSONString(arrayList));
		
	}
	
}
