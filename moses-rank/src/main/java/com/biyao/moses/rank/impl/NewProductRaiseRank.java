package com.biyao.moses.rank.impl;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.rank.RankRequest;
import com.biyao.moses.params.ProductInfo;

import lombok.extern.slf4j.Slf4j;

/**
 * 新品提升权重
 * 
 * @Description
 */
@Slf4j
@Component("npr")
public class NewProductRaiseRank extends DefaultRank {

	@Autowired
	ProductDetailCache productDetailCache;

	private static final BigDecimal DAYS = new BigDecimal(3);

	@BProfiler(key = "NewProductRaiseRank.executeRecommend", monitorType = {
			MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR })
	@Override
	public List<TotalTemplateInfo> executeRecommend(RankRequest rankRequest) {

		log.info("NewProductRaiseRank请求参数，uuid={}", rankRequest.getUuid());
		rankRequest.setRankName("dr");
		List<TotalTemplateInfo> executeRecommend = super.executeRecommend(rankRequest);
		raiseRank(executeRecommend);
		return executeRecommend;
	}

	// 新品提升权重
	public void raiseRank(List<TotalTemplateInfo> executeRecommend) {
		executeRecommend.stream().forEach(i -> {
			ProductInfo productInfo = productDetailCache.getProductInfo(Long.valueOf(i.getId()));
			double newProductScore = getNewProductScore(productInfo);
			i.setScore(i.getScore()+newProductScore);
		});
		
		Collections.sort(executeRecommend, new Comparator<TotalTemplateInfo>() {
			@Override
			public int compare(TotalTemplateInfo o1, TotalTemplateInfo o2) {
				Double o2Score = o2.getScore() != null ? o2.getScore() : 0;
				Double o1Score = o1.getScore() != null ? o1.getScore() : 0;
				return o2Score.compareTo(o1Score);
			}
		});
	}

	public double getNewProductScore(ProductInfo productInfo) {
		double resultScore = 0;
		try {
			Date firstOnshelfDate = productInfo.getFirstOnshelfTime();
			BigDecimal timer = new BigDecimal(0);
			if (firstOnshelfDate != null) {
				BigDecimal b1 = new BigDecimal(firstOnshelfDate.getTime());
				BigDecimal b2 = new BigDecimal(System.currentTimeMillis());
				// 已经上线天数=(当前时间戳-上架时间戳)/24小时
				timer = b2.subtract(b1).divide(new BigDecimal(1000 * 3600 * 24), 2, BigDecimal.ROUND_DOWN);
				//判断是否为新品
				int compareTo = DAYS.compareTo(timer);
				if (compareTo > 0) {
					resultScore = Math.tanh(DAYS.subtract(timer).doubleValue());
				}
			}
		} catch (Exception e) {
			log.error("计算新品质量分异常", e);
		}
		return resultScore;
	}

}
