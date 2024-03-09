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

import com.biyao.moses.common.constant.CommonConstants;
import com.biyao.moses.model.exp.MatchDataSourceTypeConf;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.service.RecommendMatch;
import com.biyao.moses.util.RedisUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * 发现好货横滑match
 * 
 * @Description
 * @author zyj
 * @Date 2019年1月3日
 */
@Slf4j
@Component("FGSM")
public class FindGoodsSlideMatch implements RecommendMatch {

	private static final int SPLIT_NUM = 15;// 发现好货页面横滑模板数量上限
	@Autowired
	RedisUtil redisUtil;

	@BProfiler(key = "com.biyao.moses.service.imp.FindGoodsSlideMatch.executeRecommendMatch",
			monitorType = {MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR})
	@Override
	public Map<String, List<TotalTemplateInfo>> executeRecommendMatch(String dataKey, MatchDataSourceTypeConf mdst,
			String uuId) {
		// 展示用户浏览足迹，如无足迹则展示周热销商品（TOP15）
		Map<String, List<TotalTemplateInfo>> resultMap = new HashMap<>();

		List<TotalTemplateInfo> totalList = new ArrayList<>();
		try {

			String matchKey = CommonConstants.MOSES_FOOT_PRINT + uuId;
			Set<String> zrevrange = redisUtil.zrevrange(matchKey, 0, -1);

			if (zrevrange.size() < CommonConstants.PRODUCT_SPLIT_NUM) {
				// 查询热销商品
				log.info("无足迹数据，查询热销：" + dataKey);
				String matchDataStr = redisUtil.getString(CommonConstants.MOSES_TOTAL_HOT_SALE);
				if (StringUtils.isEmpty(matchDataStr)) {
					log.error("查询热销商品无数据！");
					resultMap.put(dataKey, totalList);
					return resultMap;
				}
				String[] matchData = matchDataStr.split(",");
				int temp = 0;
				for (String productInfo : matchData) {
					if (temp >= SPLIT_NUM) {
						break;
					}
					String productId = productInfo.split(":")[0];
					TotalTemplateInfo templateInfo = new TotalTemplateInfo();
					templateInfo.setId(productId);
					totalList.add(templateInfo);
					temp++;
				}
				resultMap.put(dataKey, totalList);
			} else {
				int score = zrevrange.size();
				for (String productId : zrevrange) {
					TotalTemplateInfo templateInfo = new TotalTemplateInfo();
					templateInfo.setId(productId);
					templateInfo.setScore(Double.valueOf(--score));
					totalList.add(templateInfo);
				}
				resultMap.put(dataKey, totalList);
			}
		} catch (Exception e) {
			log.error("FGSM 异常！", e);
		}
		return resultMap;
	}

}
