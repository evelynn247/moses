package com.biyao.moses.service;

import java.util.List;
import java.util.Map;

import com.biyao.moses.model.exp.MatchDataSourceTypeConf;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;

/**
 * 实验接口
 * 
 * @Description
 * @author zyj
 * @Date 2018年9月27日
 */
public interface RecommendMatch {

	// key：dataSourceType+dataType+matchName+expNum
	Map<String, List<TotalTemplateInfo>> executeRecommendMatch(String dataKey, MatchDataSourceTypeConf mdst,
			String uuId);
}
