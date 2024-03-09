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
public class CacheRedisConfig {

	@Value("${redis.appId}")
	private int appId;

	@Value("${cache.redis.clusterId}")
	private int clusterId;

	@Value("${cache.redis.sentinelHost}")
	private String sentinelHost;

	@Value("${cache.redis.maxWaitMillis}")
	private int maxWaitMillis;

	@Value("${cache.redis.maxTotal}")
	private int maxTotal;

	@Value("${cache.redis.minIdle}")
	private int minIdle;

	@Value("${cache.redis.maxIdle}")
	private int maxIdle;

	@Value("${cache.redis.timeOut}")
	private int timeOut;

	@Bean(name = "cacheRedisClusterService")
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
