package com.biyao.moses.rank.impl;

import java.util.List;

import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.rank.RankRequest;

@Slf4j
@Component("uidDr")
public class UidDefaultRank extends DefaultRank {

	@BProfiler(key = "UidDefaultRank.executeRecommend", monitorType = {
			MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR })
	@Override
	public List<TotalTemplateInfo> executeRecommend(RankRequest rankRequest) {

		log.info("uidDefaultRank请求参数，uuid={},uid={}", rankRequest.getUuid(), rankRequest.getUid());
		rankRequest.setRankName("dr");
		// 复用default中的uuid字段，将uuid字段具体值赋值为 uid，如果没有uid 则使用uuid进行排序
		if (StringUtils.isNotBlank(rankRequest.getUid())) {
			rankRequest.setUuid(rankRequest.getUid());
		}
		
		List<TotalTemplateInfo> executeRecommend = super.executeRecommend(rankRequest);
		return executeRecommend;
	}

}