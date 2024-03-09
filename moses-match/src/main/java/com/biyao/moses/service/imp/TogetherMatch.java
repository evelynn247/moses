package com.biyao.moses.service.imp;

import java.util.*;

import com.biyao.moses.common.constant.CommonConstants;
import com.biyao.moses.util.FilterUtil;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.model.exp.MatchDataSourceTypeConf;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.service.RecommendMatch;
import com.biyao.moses.util.ProductWeightUtil;
import com.biyao.moses.util.RedisUtil;
import com.biyao.moses.params.ProductInfo;

import lombok.extern.slf4j.Slf4j;

/**
 * 一起拼match
 * 
 * @Description
 * @author zyj
 * @Date 2018年12月21日
 */
@Slf4j
@Component("TGM")
public class TogetherMatch implements RecommendMatch {

    @Autowired
    private RedisUtil redisUtil;

	@Autowired
	private ProductDetailCache productDetailCache;

	@BProfiler(key = "com.biyao.moses.service.imp.TogetherMatch.executeRecommendMatch",
			monitorType = {MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR})
	@Override
	public Map<String, List<TotalTemplateInfo>> executeRecommendMatch(String dataKey, MatchDataSourceTypeConf mdst,
			String uuId) {

		Map<String, List<TotalTemplateInfo>> resultMap = new HashMap<>();
		try {

			// redis的key=dataSourceType+matchName+expNum
			Object matchData = redisUtil.getString(dataKey);
			if (StringUtils.isEmpty(matchData)) {
				log.error("[严重异常][TGM]查询TGM无数据 uuid={}, datakey={}", uuId, dataKey);
				return resultMap;
			}

			Set<Long> discountNPidSet = new HashSet<>(productDetailCache.getProductByScmTagId(CommonConstants.DISCOUNT_N_SCM_TAGID));

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
				// 一起拼商品
				Long pid = Long.valueOf(tti.getId());
				ProductInfo productInfo = productDetailCache.getProductInfo(pid);
				if(FilterUtil.isCommonFilter(productInfo)){
					continue;
				}
				if (productInfo.getIsToggroupProduct() == 1) {

					// 新手专享V1.4新增逻辑
					if (!StringUtils.isEmpty(mdst.getNovicefrontcategoryOneId())) {
						//传入前台一级类目时，默认是新手专享页按前台一级类目筛选商品
						//如果商品对应前台一级类目为空 或 传入前台一级类目不在该商品对应前台一级类目内，则该商品不展示
						if (CollectionUtils.isEmpty(productInfo.getFCategory1Ids()) || !productInfo.getFCategory1Ids().contains(mdst.getNovicefrontcategoryOneId())) {
							continue;
						}
					}
					//过滤N折活动池中的商品
					if(discountNPidSet.contains(pid)){
						continue;
					}
					//tti.setNovicePrice(填充新手专享价格)，价格无效则认为是无效数据，不添加到结果中
					if (productInfo.getNovicePrice() != null && productInfo.getNovicePrice().signum() == 1) {
						tti.setNovicePrice(String.valueOf(productInfo.getNovicePrice()));
						totalList.add(tti);
					}

				}
			}
			//计算权重
			ProductWeightUtil.calculationWeight(mdst.getWeight(), totalList);
			resultMap.put(dataKey, totalList);
		} catch (Exception e) {
			log.error("[严重异常][TGM]一起拼match异常！,uuid={},dataKey={}", uuId, dataKey, e);
		}
		return resultMap;
	}

}
