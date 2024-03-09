package com.biyao.moses.service.imp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.params.ProductInfo;
import com.biyao.moses.util.FilterUtil;
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
/**
 * datakey=moses:{topicId}_{matchName}_{数据号}
 * @author Administrator
 */
@Component("shopMatch")
public class ShopMatch implements RecommendMatch{
	
	@Autowired
	RedisUtil redisUtil;

	@Autowired
	private ProductDetailCache productDetailCache;
	
	@BProfiler(key = "com.biyao.moses.service.imp.ShopMatch.executeRecommendMatch",
			monitorType = {MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR})
	@Override
	public Map<String, List<TotalTemplateInfo>> executeRecommendMatch(String dataKey, MatchDataSourceTypeConf mdst, String uuId) {
		
		UIBaseBody uiBaseBody = mdst.getUiBaseBody();
		
		List<String> csIds = null;
		Map<String, List<TotalTemplateInfo>> resultMap = new HashMap<>();
		List<TotalTemplateInfo> list = new ArrayList<TotalTemplateInfo>();
		resultMap.put(dataKey, list);
		if(mdst.isPersonalizedRecommendActSwitch()){
			if (uiBaseBody!=null) {
				csIds = uiBaseBody.getCsIds();
			}
			List<String> hmget = null;
			if (csIds!=null&&csIds.size()>0) {
				//店铺数>0
				String[] array = new String[csIds.size()];
				array = csIds.toArray(array);
				hmget = redisUtil.hmget(dataKey, array);
			}
			//保存店铺商品到结果集
			if (hmget!=null&&hmget.size()>0) {
				for (String pidsAndScores : hmget) {
					if (StringUtils.isEmpty(pidsAndScores)) {
						continue;
					}
					String[] pidInfos = pidsAndScores.split(",");
					if (pidInfos==null||pidInfos.length==0) {
						continue;
					}

					for (String pidInfo : pidInfos) {

						String[] pidAndScore = pidInfo.split(":");
						if (pidAndScore==null||pidAndScore.length!=2) {
							continue;
						}
						String pid = pidAndScore[0];
						String score = pidAndScore[1];
						ProductInfo productInfo = productDetailCache.getProductInfo(Long.valueOf(pid));
						//通用过滤
						if (FilterUtil.isCommonFilter(productInfo)) {
							continue;
						}
						TotalTemplateInfo totalTemplateInfo = new TotalTemplateInfo();
						totalTemplateInfo.setId(pid);
						totalTemplateInfo.setScore(Double.valueOf(score));
						list.add(totalTemplateInfo);
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
						ProductInfo productInfo = productDetailCache.getProductInfo(Long.valueOf(pid));
						//通用过滤
						if (FilterUtil.isCommonFilter(productInfo)) {
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