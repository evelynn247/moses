package com.biyao.moses.service.imp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.biyao.moses.common.constant.CommonConstants;
import com.biyao.moses.model.exp.MatchDataSourceTypeConf;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.service.RecommendMatch;
import com.biyao.moses.util.RedisUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * 发现好货,精选发现内容match
 * 
 * @Description
 * @author zyj
 * @Date 2019年1月3日
 */
@Slf4j
@Component("FGM")
public class FindGoodsMatch implements RecommendMatch {
	@Autowired
	RedisUtil redisUtil;

	private static final int SPLIT_NUM = 15;// 发现好货页面横滑模板数量上限

	private static final int JXFX_NUM = 5;// 精选发现-每个商品共现取的数量

	@BProfiler(key = "com.biyao.moses.service.imp.FindGoodsMatch.executeRecommendMatch",
			monitorType = {MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR})
	@Override
	public Map<String, List<TotalTemplateInfo>> executeRecommendMatch(String dataKey, MatchDataSourceTypeConf mdst,
			String  uuId) {

		Map<String, List<TotalTemplateInfo>> resultMap = new HashMap<>();
		List<TotalTemplateInfo> totalList = new ArrayList<>();

		try {

			// 展示根据用户足迹，有足迹取足迹商品共现。无足迹取热销商品共现
			String matchKey = CommonConstants.MOSES_FOOT_PRINT + uuId;
			Set<String> zrevrange = redisUtil.zrevrange(matchKey, 0, -1);
			if (zrevrange.size() < CommonConstants.PRODUCT_SPLIT_NUM) {
				// 无足迹，查询热销商品top15
				String matchDataStr = redisUtil.getString(CommonConstants.MOSES_TOTAL_HOT_SALE);
				String[] matchData = matchDataStr.split(",");
				int dataLength = 0;
				if (matchData.length >= SPLIT_NUM) {
					dataLength = SPLIT_NUM;
				} else {
					dataLength = matchData.length;
				}
				String[] productIds = new String[dataLength];
				System.arraycopy(matchData, 0, productIds, 0, dataLength);

				// 查询热销商品的商品共现
				List<String> res = redisUtil.hmget(dataKey, productIds);
				if (res == null || res.size() == 0) {
					log.error("查询热销商品共现异常!dataKey={} , productIds={}", dataKey, JSON.toJSON(productIds));
					resultMap.put(dataKey, totalList);
					return resultMap;
				}
				for (String pids : res) {
					if (!StringUtils.isEmpty(pids)) {

						String[] occurrenceData = pids.split(",");
						int arrLength = 0;
						if (occurrenceData.length >= JXFX_NUM) {
							arrLength = JXFX_NUM;
						} else {
							arrLength = occurrenceData.length;
						}
						String[] occurrenceDest = new String[arrLength];

						System.arraycopy(occurrenceData, 0, occurrenceDest, 0, arrLength);
						for (String pid : occurrenceDest) {
							if (!StringUtils.isEmpty(pid)) {
								String[] pidScore = pid.split(":");
								TotalTemplateInfo tempInfo = new TotalTemplateInfo();
								tempInfo.setId(pidScore[0]);
								tempInfo.setScore(Double.valueOf(pidScore[1]));
								totalList.add(tempInfo);
							}
						}
					}
				}

			} else {
				// 有足迹，查询足迹的商品共现
				String myStreetKey = CommonConstants.MOSES_MY_STREET + uuId;
				List<String> lrange = redisUtil.lrange(myStreetKey, 0, -1);
				for (String pid : lrange) {
					TotalTemplateInfo temp = new TotalTemplateInfo();
					temp.setId(pid);
					totalList.add(temp);
				}
			}
			resultMap.put(dataKey, totalList);
		} catch (Exception e) {
			log.error("FGM 异常", e);
		}
		return resultMap;

	}

}
