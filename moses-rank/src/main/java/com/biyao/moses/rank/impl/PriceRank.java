package com.biyao.moses.rank.impl;

import java.util.Collections;
import java.util.List;

import com.biyao.moses.common.enums.SortEnum;
import com.biyao.moses.compare.AscComparator;
import com.biyao.moses.compare.DesComparator;
import com.biyao.moses.params.ProductInfo;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.rank.RankRequest;
import com.biyao.moses.rank.RecommendRank;

/**
 * 按价格排序rank
 */
@Slf4j
@Component("priceRank")
public class PriceRank implements RecommendRank {
	
	@Autowired
	ProductDetailCache productDetailCache;

	@BProfiler(key = "PriceRank.executeRecommend", monitorType = {
			MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR })
	@Override
	public List<TotalTemplateInfo> executeRecommend(RankRequest rankRequest) {
		
		List<TotalTemplateInfo> oriData = rankRequest.getOriData();
		try {
			String uuid = rankRequest.getUuid();
			String dataNum = rankRequest.getDataNum();
			String rankName = rankRequest.getRankName();
			String topicId = rankRequest.getTopicId();
			String sortType = rankRequest.getSortType();
			String sortValue = rankRequest.getSortValue();

			log.info("priceRank请求参数，uuid={},dataNum={},rankName={},topicId={},sortType={},sortValue={}",uuid,dataNum,rankName,topicId,sortType,sortValue);

			for (TotalTemplateInfo totalTemplateInfo : oriData) {
				if (StringUtils.isNotBlank(totalTemplateInfo.getId())) {
					if(StringUtils.isNotBlank(totalTemplateInfo.getSkuPrice())){
						totalTemplateInfo.setScore(Double.valueOf(totalTemplateInfo.getSkuPrice()));
						continue;
					}
					ProductInfo productInfo = productDetailCache.getProductInfo(Long.valueOf(totalTemplateInfo.getId()));
					if (productInfo!=null) {
						totalTemplateInfo.setScore(Double.valueOf(productInfo.getPrice()));
					}else{
						totalTemplateInfo.setScore(0d);
					}
				}
			}

			if (SortEnum.DES.getType().equals(sortValue)) {
				//降序
				Collections.sort(oriData,new DesComparator());
			}else{
				//价格升序
				Collections.sort(oriData,new AscComparator());
			}

		} catch (Exception e) {
			log.error("rank未知错误",e);
		}
		return oriData;
	}
	
	public static void main(String[] args) {
		
		
		
		
		
	}
}