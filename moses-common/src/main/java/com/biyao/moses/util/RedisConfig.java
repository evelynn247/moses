package com.biyao.moses.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.by.bimdb.service.RedisClusterService;
import com.by.bimdb.service.impl.RedisClusterServiceImpl;

/**
 * redis
 * 
 * @author ap
 *
 */
@Configuration
@EnableCaching
public class RedisConfig {

	@Value("${redis.appId}")
	private int appId;

	@Value("${redis.clusterId}")
	private int clusterId;

	@Value("${redis.sentinelHost}")
	private String sentinelHost;

	@Value("${redis.maxWaitMillis}")
	private int maxWaitMillis;

	@Value("${redis.maxTotal}")
	private int maxTotal;

	@Value("${redis.minIdle}")
	private int minIdle;

	@Value("${redis.maxIdle}")
	private int maxIdle;

	@Value("${redis.timeOut}")
	private int timeOut;

	@Bean(name = "redisClusterService")
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
