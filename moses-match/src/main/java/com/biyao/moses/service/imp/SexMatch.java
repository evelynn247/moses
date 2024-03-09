package com.biyao.moses.service.imp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.biyao.moses.common.constant.CommonConstants;
import com.biyao.moses.model.exp.MatchDataSourceTypeConf;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.service.RecommendMatch;
import com.biyao.moses.util.ProductWeightUtil;
import com.biyao.moses.util.RedisUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * 性别match
 *
 */
@Slf4j
@Component("SM")
public class SexMatch implements RecommendMatch {

	// # 性别match
	// > 根据uuid获取性别，hget moses:visitorSexLabel uuid。返回值 0:男，1:女，-1:中性。获取不到也算作中性
	// > 性别match的topicId为10300147。男性match的数据号为1000，女性match的数据号为1001，中性match的数据号为1002

	@Autowired
	RedisUtil redisUtil;

	private static final String MAN = "1000";
	private static final String WOMAN = "1001";
	private static final String NEUTRAL = "1002";

	@BProfiler(key = "com.biyao.moses.service.imp.SexMatch.executeRecommendMatch",
			monitorType = {MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR})
	@Override
	public Map<String, List<TotalTemplateInfo>> executeRecommendMatch(String dataKey, MatchDataSourceTypeConf mdst,
			String uuId) {

		Map<String, List<TotalTemplateInfo>> resultMap = new HashMap<>();
		try {
			String newDataKey=dataKey;
			
			int lastIndexOf = newDataKey.lastIndexOf("_");
			String sex = redisUtil.hget(CommonConstants.SEX_LABEL, uuId);
			if (StringUtils.isNotBlank(sex) && "0".equals(sex)) {// 男
				newDataKey = newDataKey.substring(0, lastIndexOf + 1) + MAN;
			} else if (StringUtils.isNotBlank(sex) && "1".equals(sex)) {// 女
				newDataKey = newDataKey.substring(0, lastIndexOf + 1) + WOMAN;
			} else {// 中性
				newDataKey = newDataKey.substring(0, lastIndexOf + 1) + NEUTRAL;
			}
			List<TotalTemplateInfo> totalList = new ArrayList<>();
			Object matchData = redisUtil.getString(newDataKey);

			if (!org.springframework.util.StringUtils.isEmpty(matchData)) {
				// 数据格式，
				String[] splitStr = matchData.toString().split(",");
				for (String str : splitStr) {
					TotalTemplateInfo tti = new TotalTemplateInfo();
					Object[] idAndScore = str.split(":");
					tti.setId(idAndScore[0].toString());
					if (org.springframework.util.StringUtils.isEmpty(idAndScore[1])) {
						tti.setScore(0D);
					} else {
						tti.setScore(Double.valueOf(idAndScore[1].toString()));
					}
					totalList.add(tti);
				}
				// 计算权重
				ProductWeightUtil.calculationWeight(mdst.getWeight(), totalList);
				resultMap.put(dataKey, totalList);
			}
		} catch (Exception e) {
			log.error("[严重异常]SexMatch获取性别数据异常，uuid {} ", uuId, e);
		}
		return resultMap;
	}

}
