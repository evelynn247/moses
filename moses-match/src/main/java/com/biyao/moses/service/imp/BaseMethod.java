package com.biyao.moses.service.imp;

import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.biyao.moses.model.exp.MatchDataSourceTypeConf;
import com.biyao.moses.util.RedisUtil;

/**
 * 兜底方法
 * 
 * @Description
 * @author zyj
 * @Date 2018年12月21日
 */
@Component("BaseMethod")
public class BaseMethod  {

	private static final String DEFAULT_EXPNUM = "0000";
	@Autowired
	RedisUtil redisUtil;

	@BProfiler(key = "com.biyao.moses.service.imp.BaseMethod.executeRecommendMatch", monitorType = {
			MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR })
	public Object executeRecommendMatch(String dataKey, MatchDataSourceTypeConf mdst,
			String uuId) {

		int lastIndexOf = dataKey.lastIndexOf("_");
		dataKey = dataKey.substring(0, lastIndexOf + 1) + DEFAULT_EXPNUM;
		mdst.setDefalutData(true);
		// redis的key=dataSourceType+matchName+0000
		Object matchData = redisUtil.getString(dataKey);
		return matchData;
	}

}
