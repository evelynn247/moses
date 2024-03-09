package com.biyao.moses.util;

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
@EnableCaching
public class MatchRedisConfig {

	@Value("${redis.appId}")
	private int appId;

	@Value("${match.redis.clusterId}")
	private int clusterId;

	@Value("${match.redis.sentinelHost}")
	private String sentinelHost;

	@Value("${match.redis.maxWaitMillis}")
	private int maxWaitMillis;

	@Value("${match.redis.maxTotal}")
	private int maxTotal;

	@Value("${match.redis.minIdle}")
	private int minIdle;

	@Value("${match.redis.maxIdle}")
	private int maxIdle;

	@Value("${match.redis.timeOut}")
	private int timeOut;

	@Bean(name = "matchRedisClusterService")
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
