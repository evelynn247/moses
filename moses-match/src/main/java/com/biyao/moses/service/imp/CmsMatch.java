package com.biyao.moses.service.imp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.biyao.moses.cache.CmsTopicCache;
import com.biyao.moses.model.cms.CmsTopic;
import com.biyao.moses.model.exp.MatchDataSourceTypeConf;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.service.RecommendMatch;

/**
 * cms获取数据match
 * 
 * @Description
 * @author houkun
 * @Date 2019年03月02日
 */
@Slf4j
@Component("cmsMatch")
public class CmsMatch implements RecommendMatch {

	@Autowired
	CmsTopicCache cmsTopicCache;

	@BProfiler(key = "com.biyao.moses.service.imp.CmsMatch.executeRecommendMatch",
			monitorType = {MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR})
	@Override
	public Map<String, List<TotalTemplateInfo>> executeRecommendMatch(String key, MatchDataSourceTypeConf mdst,
			String uuId) {

		Map<String, List<TotalTemplateInfo>> resultMap = new HashMap<>();

		List<TotalTemplateInfo> totalList = new ArrayList<>();
		resultMap.put(key, totalList);
		try {
			CmsTopic cmsTopic = cmsTopicCache.getCmsTopic();
			TotalTemplateInfo totalTemplateInfo = new TotalTemplateInfo();
			totalTemplateInfo.setSubtitle(cmsTopic.getSubtitle());
			totalTemplateInfo.setMainTitle(cmsTopic.getTitle());
			String entryImageUrl = cmsTopic.getEntryImageUrl();
			List<String> images = new ArrayList<>();
			images.add(entryImageUrl);
			totalTemplateInfo.setImages(images);
			
			totalTemplateInfo.setPriceCent(cmsTopic.getPrice());
			totalTemplateInfo.setPriceStr(cmsTopic.getPrice());
			
			Map<String, String> routerParams = new HashMap<>();
			routerParams.put("cmsTopicId", cmsTopic.getTopicId()+"");
			totalTemplateInfo.setRouterParams(routerParams );
			totalList.add(totalTemplateInfo);
		} catch (Exception e) {
			log.error("[严重异常]CmsMatch获取cms专题数据出现异常，uuid {}", uuId, e);
		}
		return resultMap;
	}

}