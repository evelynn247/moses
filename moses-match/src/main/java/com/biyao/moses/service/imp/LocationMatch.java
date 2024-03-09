package com.biyao.moses.service.imp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.biyao.moses.model.exp.MatchDataSourceTypeConf;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.service.RecommendMatch;
import com.biyao.moses.service.http.LocalServiceImpl;
import com.biyao.moses.util.ProductWeightUtil;
import com.biyao.moses.util.RedisUtil;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;

/**
 * 
 * 地理位置match
 */
@Slf4j
@Component("locMatch")
public class LocationMatch implements RecommendMatch {

	@Autowired
	RedisUtil redisUtil;
	
	@Autowired
	LocalServiceImpl localServiceImpl;
	
	@BProfiler(key = "com.biyao.moses.service.imp.LocationMatch.executeRecommendMatch",
			monitorType = {MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR})
	@Override
	public Map<String, List<TotalTemplateInfo>> executeRecommendMatch(String dataKey, MatchDataSourceTypeConf mdst,
			String uuId) {

		Map<String, List<TotalTemplateInfo>> resultMap = new HashMap<>();
		try {
			String key = dataKey;
			String lat = mdst.getLat();
			String lng = mdst.getLng();
			String cityNameByLoc = localServiceImpl.getCityNameByLoc(uuId, lat, lng);
			if (!StringUtils.isEmpty(cityNameByLoc)) {
				key = dataKey + "_" +cityNameByLoc;
			}
			
			// redis的key=dataSourceType+matchName+expNum
			Object matchData = redisUtil.getString(key);
			if (StringUtils.isEmpty(matchData)) {
				log.error("查询locMatch无数据 datakey={}", key);
				
				//获取城市数据为null，走默认数据
				if (!key.equals(dataKey)) {
					//城市数据为空，查询兜底数据
					matchData = redisUtil.getString(dataKey);
					if (StringUtils.isEmpty(matchData)) {
						log.error("查询locMatch无兜底数据 key={}", key);
						return resultMap;
					}
				}else{
					log.error("查询locMatch无兜底数据 key={}", key);
					return resultMap;
				}
			}
			
			
			
			List<TotalTemplateInfo> totalList = new ArrayList<>();
			String[] splitStr = matchData.toString().split(",");
			for (String str : splitStr) {
				TotalTemplateInfo tti = new TotalTemplateInfo();
				Object[] idAndScore = str.split(":");
				tti.setId(idAndScore[0].toString());
				if (StringUtils.isEmpty(idAndScore[1])) {
					tti.setScore(0D);
				} else {
					tti.setScore(Double.valueOf(idAndScore[1].toString()));
				}
				totalList.add(tti);
			}

			//计算权重
			ProductWeightUtil.calculationWeight(mdst.getWeight(), totalList);
			resultMap.put(dataKey, totalList);
		} catch (Exception e) {
			log.error("locMatch 异常！", e);
		}
		return resultMap;
	}

}
