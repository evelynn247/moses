package com.biyao.search.ui.config;

import com.by.bimdb.service.RedisClusterService;
import com.by.bimdb.service.impl.RedisClusterServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * redis
 * 
 * @author ap
 *
 */
@Configuration
public class AlgorithmRedisConfig {

	@Value("${algorithm.redis.appId}")
	private int appId;

	@Value("${algorithm.redis.clusterId}")
	private int clusterId;

	@Value("${algorithm.redis.clusterHost}")
	private String sentinelHost;

	@Value("${algorithm.redis.maxWaitMillis}")
	private int maxWaitMillis;

	@Value("${algorithm.redis.maxTotal}")
	private int maxTotal;

	@Value("${algorithm.redis.minIdle}")
	private int minIdle;

	@Value("${algorithm.redis.maxIdle}")
	private int maxIdle;

	@Value("${algorithm.redis.timeOut}")
	private int timeOut;

	@Bean(name = "algorithmRedisClusterService")
	public RedisClusterService redisClusterService() {
		RedisClusterService rsi = null;
		try {
			rsi = new RedisClusterServiceImpl(appId, clusterId, sentinelHost, maxWaitMillis, maxTotal,
					minIdle, maxIdle, timeOut);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return rsi;

	}

}
