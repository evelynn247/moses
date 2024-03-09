package com.biyao.moses.service.imp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.params.ProductInfo;
import com.biyao.moses.util.FilterUtil;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.biyao.moses.common.constant.CommonConstants;
import com.biyao.moses.model.exp.MatchDataSourceTypeConf;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.params.UIBaseBody;
import com.biyao.moses.service.RecommendMatch;
import com.biyao.moses.util.RedisUtil;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;

@Slf4j
@Component("productMatch")
public class ProductMatch implements RecommendMatch {
	
	@Autowired
	RedisUtil redisUtil;

	@Autowired
	private ProductDetailCache productDetailCache;
	
	
	@BProfiler(key = "com.biyao.moses.service.imp.ProductMatch.executeRecommendMatch",
			monitorType = {MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR})
	@Override
	public Map<String, List<TotalTemplateInfo>> executeRecommendMatch(String dataKey, MatchDataSourceTypeConf mdst, String uuId) {
		UIBaseBody uiBaseBody = mdst.getUiBaseBody();
		List<String> cpIds = null;
		Map<String, List<TotalTemplateInfo>> resultMap = new HashMap<>();
		List<TotalTemplateInfo> list = new ArrayList<TotalTemplateInfo>();
		//商品set，去重使用
		HashSet<String> pidSet = new HashSet<>();
		//组合cpIds与结果为map
		HashMap<String,String> mergeMap = new HashMap<>();
		resultMap.put(dataKey, list);
		if(mdst.isPersonalizedRecommendActSwitch()){
			if (uiBaseBody!=null) {
				cpIds = uiBaseBody.getCpIds();
			}
			List<String> hmget = null;
			if (cpIds!=null&&cpIds.size()>0) {
				//商品数>0
				String[] array = new String[cpIds.size()];
				array = cpIds.toArray(array);
				if(array.length != 0){
					hmget = redisUtil.hmget(CommonConstants.MOSES_GOD_OCCURRENCE, array);
				}
			}
			if (hmget!=null&&hmget.size()>0) {
				for (int i = 0; i < cpIds.size(); i++) {
					String pid = cpIds.get(i);
					pidSet.add(pid);
					String occPids = hmget.get(i);
					if (StringUtils.isNotBlank(occPids)) {
						mergeMap.put(pid, occPids);
					}
				}
			}
			//保存共现商品到结果集,过滤掉订单商品
			if (hmget!=null&&hmget.size()>0) {
				for (String pid : pidSet) {
					//共现商品
					String string = mergeMap.get(pid);
					if (StringUtils.isBlank(string)) {
						continue;
					}
					String[] split = string.split(",");
					if (split==null||split.length==0) {
						continue;
					}
					for (int j = 0; j < split.length; j++) {
						String checkPid = split[j];
						if (!pidSet.contains(checkPid)) {
							ProductInfo productInfo = productDetailCache.getProductInfo(Long.valueOf(checkPid));
							if(FilterUtil.isCommonFilter(productInfo)){
								continue;
							}
							TotalTemplateInfo totalTemplateInfo = new TotalTemplateInfo();
							totalTemplateInfo.setId(checkPid);
							totalTemplateInfo.setScore(100d);
							list.add(totalTemplateInfo);
						}
					}
				}
			}
		}
		if (list.size()<50) {
			// 获取兜底数据，放到结果集
			String matchDataStr = redisUtil.getString(CommonConstants.MOSES_TOTAL_HOT_SALE);
			if (StringUtils.isNotBlank(matchDataStr)) {
				
				String[] hotProductIds = matchDataStr.split(",");
				if (hotProductIds!=null&&hotProductIds.length>0) {
					for (String pid : hotProductIds) {
						if (pidSet.contains(pid)) {
							continue;
						}
						ProductInfo productInfo = productDetailCache.getProductInfo(Long.valueOf(pid));
						if(FilterUtil.isCommonFilter(productInfo)){
							continue;
						}
						TotalTemplateInfo totalTemplateInfo = new TotalTemplateInfo();
						totalTemplateInfo.setId(pid);
						totalTemplateInfo.setScore(1d);
						list.add(totalTemplateInfo);
					}
				}
			}
		}
		return resultMap;
	}
}