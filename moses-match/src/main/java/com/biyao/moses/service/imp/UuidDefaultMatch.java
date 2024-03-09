package com.biyao.moses.service.imp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSONArray;
import com.biyao.moses.common.constant.CommonConstants;
import com.biyao.moses.model.exp.MatchDataSourceTypeConf;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.service.RecommendMatch;
import com.biyao.moses.util.ProductWeightUtil;
import com.biyao.moses.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 根据用户获取数据的方法
 * 
 * @Description
 * @author zyj
 * @Date 2018年12月21日
 */
@Slf4j
@Component("UDM")
public class UuidDefaultMatch implements RecommendMatch {

	@Autowired
	RedisUtil redisUtil;

	@Autowired
	DefaultMatch defaultMatch;

	@BProfiler(key = "com.biyao.moses.service.imp.UuidDefaultMatch.executeRecommendMatch",
			monitorType = {MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR})

	@Override
	public Map<String, List<TotalTemplateInfo>> executeRecommendMatch(String dataKey, MatchDataSourceTypeConf mdst,
			String uuId) {
		Map<String, List<TotalTemplateInfo>> resultMap = new HashMap<>();
		try {
			String oriKey = dataKey;
			String defaultkey = dataKey;
			dataKey = dataKey + "_" + uuId;


			// redis的key=dataSourceType+matchName_0001_uuid
			Object matchData = redisUtil.getString(dataKey);
			if (!mdst.isPersonalizedRecommendActSwitch() || StringUtils.isEmpty(matchData)) {
				// 查询UDM无数据，执行兜底方法
				log.error("[一般异常]查询UDM无数据! uuid {}， dataKey:{}", uuId, dataKey);
				defaultkey = defaultkey.replace(CommonConstants.UDM_MATCH_NAME, CommonConstants.DEFAULT_MATCH_NAME);
				int lastIndexOf = defaultkey.lastIndexOf("_");
				defaultkey = defaultkey.substring(0, lastIndexOf + 1) + CommonConstants.DEFAULT_EXPNUM;
				mdst.setDefalutData(true);
				Map<String, List<TotalTemplateInfo>> executeRecommendMatch = defaultMatch
						.executeRecommendMatch(defaultkey, mdst, uuId);
				if (executeRecommendMatch.isEmpty()) {
					log.error("[严重异常]UDM查询兜底无数据! uuid{}, defaultKey:{}", uuId, defaultkey);
				}
				resultMap.put(oriKey, executeRecommendMatch.get(defaultkey));
				return resultMap;
			}
			List<TotalTemplateInfo> totalList = new ArrayList<>();
			if (isjson(matchData)) {
				totalList = JSONArray.parseArray(matchData.toString(), TotalTemplateInfo.class);
			} else {
				// 数据格式，
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

			}
			//计算权重
			ProductWeightUtil.calculationWeight(mdst.getWeight(), totalList);

			resultMap.put(oriKey, totalList);
		} catch (Exception e) {
			log.error("[严重异常]UDM出现异常，uuid {}，dataKey {}", uuId, dataKey, e);
		}
		return resultMap;

	}

	private boolean isjson(Object obj) {
		try {
			JSONArray.parseArray(obj.toString());
			return true;
		} catch (Exception e) {
			return false;
		}
	}

}
