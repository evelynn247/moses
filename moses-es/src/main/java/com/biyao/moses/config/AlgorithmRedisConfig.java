package com.biyao.moses.config;

import com.by.bimdb.model.RedisConfig;
import com.by.bimdb.service.RedisClusterService;
import com.by.bimdb.service.impl.RedisClusterServiceImpl;
import com.by.bimdb.utils.JedisFactory;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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

	@Value("${algorithm.redis.sentinelHost}")
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


	@Bean(name = "algJedisClusterPipeline")
	public JedisClusterPipeline getJedisClusterPipeline(){
		GenericObjectPoolConfig config = new GenericObjectPoolConfig();
		config.setMaxWaitMillis((long)maxWaitMillis);
		config.setMaxTotal(maxTotal);
		config.setMinIdle(minIdle);
		config.setMaxIdle(maxIdle);
		return new JedisClusterPipeline(getHost(sentinelHost), timeOut, config);
	}


	public Set<HostAndPort> getHost(String hosts) {
		Set<HostAndPort> res = new HashSet<>();
		Set<String> host = new HashSet<>();
		Collections.addAll(host, hosts.split(";"));
		for (String h : host)
			res.add(HostAndPort.parseString(h));
		return res;
	}
}
