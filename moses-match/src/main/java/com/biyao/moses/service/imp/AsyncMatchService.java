package com.biyao.moses.service.imp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import com.biyao.moses.common.enums.MatchSourceEnum;
import com.biyao.moses.match2.constants.MatchStrategyConst;
import com.biyao.moses.match2.param.MatchParam;
import com.biyao.moses.match2.service.Match2;
import com.biyao.moses.model.match2.MatchItem2;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import com.biyao.moses.model.exp.MatchDataSourceTypeConf;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.service.RecommendMatch;
import com.biyao.moses.util.ApplicationContextProvider;

@Service
@Slf4j
public class AsyncMatchService {

	@BProfiler(key = "com.biyao.moses.service.imp.AsyncMatchService.executeRecommendMatch", monitorType = {
			MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR })
	@SuppressWarnings("static-access")
	@Async
	public Future<Map<String, List<TotalTemplateInfo>>> executeRecommendMatch(MatchDataSourceTypeConf mdst, String redisKey,
			String uuId) {
		RecommendMatch match = ApplicationContextProvider.getBean(mdst.getName(), RecommendMatch.class);
		Map<String, List<TotalTemplateInfo>> resultMap =match.executeRecommendMatch(redisKey, mdst, uuId);
		return new AsyncResult<Map<String,List<TotalTemplateInfo>>>(resultMap);
	}


	@BProfiler(key = "AsyncMatchService.executeMatch2", monitorType = {
			MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR })
	@SuppressWarnings("static-access")
	@Async
	public Future<List<MatchItem2>> executeMatch2(MatchParam matchParam, String matchBeanName) {
		List<MatchItem2> result = new ArrayList<>();
		try {
			Match2 match;
			if(MatchSourceEnum.isHadExclusiveBean(matchBeanName)){
				match = ApplicationContextProvider.getBean(matchBeanName, Match2.class);
			}else {
				match = ApplicationContextProvider.getBean(MatchStrategyConst.COMMON_MATCH, Match2.class);
			}
			result = match.match(matchParam);
		}catch (Exception e){
			log.error("[严重异常]获取召回源{}的数据时发生异常，", matchBeanName, e);
		}
		return new AsyncResult<>(result);
	}

}
