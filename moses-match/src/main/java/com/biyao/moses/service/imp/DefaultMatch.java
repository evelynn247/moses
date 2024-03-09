package com.biyao.moses.service.imp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.params.ProductInfo;
import com.biyao.moses.util.FilterUtil;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSONArray;
import com.biyao.moses.model.exp.MatchDataSourceTypeConf;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.service.RecommendMatch;
import com.biyao.moses.util.ProductWeightUtil;
import com.biyao.moses.util.RedisUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * 默认实验
 * 
 * @Description
 * @author zyj
 * @Date 2018年9月27日
 */
@Slf4j
@Component("DefaultMatch")
public class DefaultMatch implements RecommendMatch {

	@Autowired
	RedisUtil redisUtil;

	@Autowired
	BaseMethod baseMatch;

	@Autowired
	ProductDetailCache productDetailCache;

	@BProfiler(key = "com.biyao.moses.service.imp.DefaultMatch.executeRecommendMatch",
			monitorType = {MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR})
	@Override
	public Map<String, List<TotalTemplateInfo>> executeRecommendMatch(String dataKey, MatchDataSourceTypeConf mdst,
			String uuId) {

		Map<String, List<TotalTemplateInfo>> resultMap = new HashMap<>();

		try {

			// redis的key=dataSourceType+matchName+expNum
			Object matchData = redisUtil.getString(dataKey);
			if (StringUtils.isEmpty(matchData)) {
				// 查询defaultmatch无数据，执行兜底方法
				log.error("[一般异常]查询defaultmatch无数据! uuid {}, key {}", uuId, dataKey);
				matchData = baseMatch.executeRecommendMatch(dataKey, mdst, uuId);
				if (StringUtils.isEmpty(matchData)) {
					log.error("[严重异常]查询兜底无数据! uuid{}，key {}", uuId, dataKey);
					return resultMap;
				}
			}
			List<TotalTemplateInfo> totalList = new ArrayList<>();
			if (isjson(matchData)) {
				totalList = JSONArray.parseArray(matchData.toString(), TotalTemplateInfo.class);
			} else {
				String[] splitStr = matchData.toString().split(",");
				for (String str : splitStr) {
					try {
						String[] idAndScore = str.split(":");
						String productId = idAndScore[0];
						String score = idAndScore[1];
						if(StringUtils.isEmpty(productId)){
							continue;
						}
						ProductInfo productInfo = productDetailCache.getProductInfo(Long.valueOf(productId));
						if(FilterUtil.isCommonFilter(productInfo)){
							continue;
						}
						TotalTemplateInfo tti = new TotalTemplateInfo();
						tti.setId(productId);
						if (StringUtils.isEmpty(score)) {
							tti.setScore(0D);
						} else {
							tti.setScore(Double.valueOf(score));
						}
						totalList.add(tti);
					}catch (Exception e){
						log.error("[严重异常]DefaultMatch解析商品信息{}时出现异常" ,str ,e);
					}
				}

			}
			//计算权重
			ProductWeightUtil.calculationWeight(mdst.getWeight(), totalList);

			resultMap.put(dataKey, totalList);
		} catch (Exception e) {
			log.error("[严重异常]DefaultMatch处理时出现异常，uuid {}， dataKey {}", uuId, dataKey, e);
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
