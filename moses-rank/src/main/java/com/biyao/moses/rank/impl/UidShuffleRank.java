package com.biyao.moses.rank.impl;

import java.util.Collections;
import java.util.List;

import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.biyao.moses.common.constant.CommonConstants;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.rank.RankRequest;

@Slf4j
@Component("uidSr")
public class UidShuffleRank extends DefaultRank {

	@BProfiler(key = "UidShuffleRank.executeRecommend", monitorType = {
			MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR })
	@Override
	public List<TotalTemplateInfo> executeRecommend(RankRequest rankRequest) {

		log.info("UidShuffleRank请求参数，uuid={}", rankRequest.getUuid());
		rankRequest.setRankName("dr");
		// 复用default中的uuid字段，将uuid字段具体值赋值为 uid，如果没有uid 则使用uuid进行排序
		if (StringUtils.isNotBlank(rankRequest.getUid())) {
			rankRequest.setUuid(rankRequest.getUid());
		}
		List<TotalTemplateInfo> executeRecommend = super.executeRecommend(rankRequest);
		breakRank(executeRecommend);
		return executeRecommend;
	}

	// 按最小分页数打乱排序
	private void breakRank(List<TotalTemplateInfo> rankResult) {
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

}