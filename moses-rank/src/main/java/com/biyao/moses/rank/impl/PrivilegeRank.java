package com.biyao.moses.rank.impl;

import com.alibaba.fastjson.JSON;
import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.compare.DesComparator;
import com.biyao.moses.compare.DesPrivilegeComparator;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.rank.RankRequest;
import com.biyao.moses.rank.RecommendRank;
import com.biyao.moses.params.ProductInfo;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 按特权金抵扣金额降序
 */
@Slf4j
@Component("privilegeRank")
public class PrivilegeRank implements RecommendRank {
	
	@Autowired
	ProductDetailCache productDetailCache;

	@BProfiler(key = "PrivilegeRank.executeRecommend", monitorType = {
			MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR })
	@Override
	public List<TotalTemplateInfo> executeRecommend(RankRequest rankRequest) {
		
		List<TotalTemplateInfo> oriData = rankRequest.getOriData();
		try {
			Collections.sort(oriData,new DesPrivilegeComparator());

		} catch (Exception e) {
			log.error("privilegeRank未知错误",e);
		}
		return oriData;
	}
}