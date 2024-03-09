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
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSONArray;
import com.biyao.moses.common.constant.CommonConstants;
import com.biyao.moses.model.exp.MatchDataSourceTypeConf;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.service.RecommendMatch;
import com.biyao.moses.util.RedisUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * 足迹图片match
 * 
 * @Description
 * @author zyj
 * @Date 2018年10月10日
 */
@Slf4j
@Component("FootPrintPictureMatch")
public class FootPrintPictureMatch implements RecommendMatch {

	@Autowired
	RedisUtil redisUtil;
	// 没有图片，默认试验号0000
	public static final String DEFAULT_MATCH = "_0000";
	// 包含足迹图片，试验号1000
	public static final String PIC_MATCH = "_1000";

	@BProfiler(key = "com.biyao.moses.service.imp.FootPrintPictureMatch.executeRecommendMatch",
			monitorType = {MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR})
	@Override
	public Map<String, List<TotalTemplateInfo>> executeRecommendMatch(String dataKey, MatchDataSourceTypeConf mdst,
			String uuId) {
		Map<String, List<TotalTemplateInfo>> resultMap = new HashMap<>();
		List<TotalTemplateInfo> totalList = new ArrayList<>();
		try {
			// 查询浏览记录商品
			// 检查横滑模板数量，大于2个，显示足迹图片。否则显示兜底图片
			String footPrintSplitKey = CommonConstants.MOSES_FOOT_PRINT + uuId;
			Set<String> listSize = redisUtil.zrange(footPrintSplitKey, 0, -1);
			// 查询浏览记录商品
			String matchKey = "";
			if (listSize != null && listSize.size() >= CommonConstants.PRODUCT_SPLIT_NUM) {
				int indexOf = dataKey.indexOf(DEFAULT_MATCH);
				matchKey = dataKey.substring(0, indexOf) + PIC_MATCH;
			} else {
				matchKey = dataKey;
			}
			// 查询图片
			String matchDataStr = redisUtil.getString(matchKey);
			if (StringUtils.isEmpty(matchDataStr)) {
				log.error("查询足迹图片match无数据! ");
				return resultMap;
			}
			totalList = JSONArray.parseArray(matchDataStr, TotalTemplateInfo.class);
			resultMap.put(dataKey, totalList);
		} catch (Exception e) {
			log.error("FootPrintPictureMatch 异常！", e);
		}
		return resultMap;
	}

}
