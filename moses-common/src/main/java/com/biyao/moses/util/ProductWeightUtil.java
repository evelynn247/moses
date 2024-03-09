package com.biyao.moses.util;

import java.util.List;

import org.springframework.util.StringUtils;

import com.biyao.moses.model.template.entity.TotalTemplateInfo;

/**
 * 商品权重计算
 * @Description 
 */
public class ProductWeightUtil {

	
	public static void calculationWeight(String initialWeight, List<TotalTemplateInfo> totalList) {
		
		Double weight = 1.0;
		if (!StringUtils.isEmpty(initialWeight)) {
			weight = Double.valueOf(initialWeight);
		}
		final Double fweight = weight;
		
		
		for (TotalTemplateInfo totalTemplateInfo : totalList) {
			totalTemplateInfo.setScore(totalTemplateInfo.getScore() != null ? totalTemplateInfo.getScore() * fweight : 0);
		}
		
	}
	
}
