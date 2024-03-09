package com.biyao.moses.service.imp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.biyao.moses.common.constant.CommonConstants;
import com.biyao.moses.model.exp.MatchDataSourceTypeConf;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.service.RecommendMatch;
import com.biyao.moses.util.RedisUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * 足迹横滑模板match
 * 
 * @Description
 * @author zyj
 * @Date 2018年10月10日
 */
@Slf4j
@Component("FootPrintMatch")
public class FootPrintMatch implements RecommendMatch {

	@Autowired
	RedisUtil redisUtil;

	@BProfiler(key = "com.biyao.moses.service.imp.FootPrintMatch.executeRecommendMatch",
			monitorType = {MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR})
	@Override
	public Map<String, List<TotalTemplateInfo>> executeRecommendMatch(String key, MatchDataSourceTypeConf mdst,
			String uuId) {

		Map<String, List<TotalTemplateInfo>> resultMap = new HashMap<>();

		List<TotalTemplateInfo> totalList = new ArrayList<>();
		try {

			String matchKey = CommonConstants.MOSES_FOOT_PRINT + uuId;

			Set<String> zrevrange = redisUtil.zrevrange(matchKey, 0, -1);

			if (zrevrange.size() == 0) {
				log.error("查询足迹match无数据! uuid:{}", uuId);
				return resultMap;
			}

			int score = zrevrange.size();
			for (String productId : zrevrange) {
				TotalTemplateInfo templateInfo = new TotalTemplateInfo();
				templateInfo.setId(productId);
				templateInfo.setScore(Double.valueOf(--score));
				totalList.add(templateInfo);
			}
			resultMap.put(key, totalList);
		} catch (Exception e) {
			log.error("FootPrintMatch 异常！", e);
		}
		return resultMap;
	}

}
