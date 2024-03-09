package com.biyao.moses.config;

import com.by.bimdb.service.RedisClusterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Tuple;

import javax.annotation.Resource;
import java.util.*;

/**
 * @author monkey
 * @date 2018年9月18日
 */
@Service
@Slf4j
public class AlgorithmRedisUtil {
	private final static String LOCKED = "LOCKED";

	@Resource(name = "algorithmRedisClusterService")
	private RedisClusterService redisClusterService;

	public String getString(String key) {
		String sRet = "";
		try {
			sRet = redisClusterService.get(key);
		} catch (Exception e) {
			log.error("redis getString error, key is {},error message is {}", key, e);
		}
		return sRet;
	}

	/**
	 *
	 * @param key
	 * @param isExcepion  是否发生异常
	 * @return
	 */
	public String getString(String key, Boolean[] isExcepion) {
		String sRet = "";
		isExcepion[0] = false;
		try {
			sRet = redisClusterService.get(key);
		} catch (Exception e) {
			log.error("redis getString error, key is {},error message is {}", key, e);
			isExcepion[0] = true;
		}
		return sRet;
	}

	public boolean setString(String key, String value, int seconds) {
		boolean bRet = false;
		try {
			if (seconds > 0) {
				redisClusterService.setex(key, seconds, value);
			} else {
				redisClusterService.set(key, value);
			}
			bRet = true;
		} catch (Exception e) {
			log.error("redis setString error, key is {},value is {},error message is {}", key, value, e);
		}
		return bRet;
	}

	public Long incr(String key) {
		Long incr = 0L;
		try {
			incr = redisClusterService.incr(key);
			if (incr == 0) {
				throw new RuntimeException("get incr error");
			}
		} catch (Exception e) {
			log.error("redis incr error, key is {},error message is {}", key, e);
		}
		return incr;
	}

	public String hgetStr(String key, String field) {
		String result = "";
		try {
			result = redisClusterService.hget(key, field);
		} catch (Exception e) {
			log.error("redis hgetAll error, key is {},error message is {}", key, e);
		}
		return result;
	}

	public boolean hmset(String key, Map<String, String> hash) {
		boolean bRet = false;
		try {
			redisClusterService.hmset(key, hash);
			bRet = true;
		} catch (Exception e) {
			log.error("redis hmsetString error, key is {},value is {},error message is{}", key, hash, e);
		}
		return bRet;
	}

	public boolean hset(String key, String field, String value) {
		boolean bRet = false;
		try {
			redisClusterService.hset(key, field, value);
			bRet = true;
		} catch (Exception e) {
			log.error("redis hmsetString error, key is {},value is {},error message is{}", key, field, e);
		}
		return bRet;
	}


	public boolean del(String key) {
		try {
			return redisClusterService.del(key) > 0;
		} catch (Exception e) {
			log.error("redis del error, key is {},error message is {}", key, e);
			return false;
		}
	}

	public Map<String, String> hgetAll(String key) {
		Map<String, String> map = new HashMap<>();
		try {
			map = redisClusterService.hgetAll(key);
			if (map.isEmpty()) {
				return map;
			}
		} catch (Exception e) {
			log.error("redis hgetAll error, key is {},error message is {}", key, e);
		}
		return map;
	}

	public Map<String, String> hgetAll(String key, Boolean[] isException) {
		Map<String, String> map = new HashMap<>();
		isException[0] = false;
		try {
			map = redisClusterService.hgetAll(key);
			if (map.isEmpty()) {
				return null;
			}
		} catch (Exception e) {
			log.error("redis hgetAll error, key is {},error message is {}", key, e);
			isException[0] = true;
		}
		return map;
	}

	public List<String> hmget(String key, String[] fields) {
		List<String> hmget = new ArrayList<String>();
		try {
			hmget = redisClusterService.hmget(key, fields);
			if (hmget.isEmpty()) {
				return null;
			}
		} catch (Exception e) {
			log.error("redis hmget error, key is {},error message is {}", key, e);
		}
		return hmget;
	}

	public String hget(String key, String field) {
		String result = null;
		try {
			result = redisClusterService.hget(key, field);
		} catch (Exception e) {
			log.error("redis hmget error, key is {},error message is {}", key, e);
		}
		return result;
	}

	/**
	 * lpush
	 *
	 * @param key
	 * @param item
	 * @return
	 */
	public Long lpush(String key, String... item) {
		try {
			return redisClusterService.lpush(key, item);
		} catch (Exception e) {
			log.error("redis lpush error, key is {}, value is {}.", key, item, e);
		}
		return null;
	}

	/**
	 * lrange
	 *
	 * @param key
	 * @param start
	 * @param end
	 * @return
	 */
	public List<String> lrange(String key, int start, int end) {
		try {
			return redisClusterService.lrange(key, start, end);
		} catch (Exception e) {
			log.error("redis lrange error, key is {}, start is {}, end is {}.", key, start, end, e);
		}
		return new ArrayList<>();
	}

	/**
	 * expire
	 *
	 * @param key
	 * @param seconds
	 * @return
	 */
	public Long expire(String key, int seconds) {
		try {
			return redisClusterService.expire(key, seconds);
		} catch (Exception e) {
			log.error("redis expire error, key is {}, seconds is {}.", key, seconds, e);
		}
		return null;
	}

	/**
	 * ltrim
	 *
	 * @param key
	 * @param start
	 * @param end
	 * @return
	 */
	public String ltrim(String key, int start, int end) {
		try {
			return redisClusterService.ltrim(key, start, end);
		} catch (Exception e) {
			log.error("redis ltrim error, key is {}, start is {}, end is {}.", key, start, end, e);
		}
		return null;
	}

	public Set<String> smems(String key) {
		try {
			return redisClusterService.smembers(key);
		} catch (Exception e) {
			log.error("redis smems error, key is {}, start is {}, end is {}.", key, e);
		}
		return null;
	}

	/**
	 * 加锁
	 *
	 * @param key
	 *            获取锁的时间，即业务等待时间
	 * @param lockTime
	 *            锁多长时间释放(不unlock的情况下)
	 */
	public boolean lock(String key, int lockTime) {
		Boolean locked = false;
		if(lockTime <= 0){
			lockTime = 60;
		}
		try {
			String result = redisClusterService.set(key, LOCKED, "NX", "PX", lockTime);
			if ("OK".equals(result)) {
				locked = true;
			}
		} catch (Exception e) {
			log.error("", e);
		}
		return locked;
	}

	/**
	 * 加锁
	 * @param key 锁的key
	 * @param lockTime  锁多长时间释放(不unlock的情况下) 秒
	 * @param timeout  获取锁的时间，即业务等待时间,微秒
	 * @updateDate
	 */
	public boolean lock(String key,int lockTime, long timeout) {
		Boolean locked = false;
		long nano = System.nanoTime();
		timeout *= 1000000;
		final Random r = new Random();
		if(lockTime <= 0){
			lockTime = 60;
		}
		try {
			while ((System.nanoTime() - nano) < timeout) {
				String result = redisClusterService.set(key, LOCKED, "NX", "PX", lockTime);
				if ("OK".equals(result)) {
					locked = true;
					break;
				}
				Thread.sleep(50, r.nextInt(500));
			}
		} catch (Exception e) {
			log.error("获取锁失败",e);
		}
		return locked;
	}


	/**
	 * 释放资源
	 */
	public void unlock(String key) {
		try {
			redisClusterService.del(key);
		} catch (Exception e) {
			log.error("redis unlock error, key is {}", key, e);
		}
	}

	public Long zadd(String key, double score, String member) {
		try {
			return redisClusterService.zadd(key, score, member);
		} catch (Exception e) {
			log.error("redis zadd error, key is {}", key, e);
		}
		return null;
	}

	public Set<String> zrange(String key, long start, long end) {
		try {
			return redisClusterService.zrange(key, start, end);
		} catch (Exception e) {
			log.error("redis zrange error, key is {}", key, e);
		}
		return null;
	}

	public Long zcard(String key) {
		try {
			return redisClusterService.zcard(key);
		} catch (Exception e) {
			log.error("redis zcard error, key is {}", key, e);
		}
		return 0L;
	}

	public Set<String> zrevrange(final String key, final long start, final long end) {
		try {
			return redisClusterService.zrevrange(key, start, end);
		} catch (Exception e) {
			log.error("redis zrevrange error, key is {}", key, e);
		}
		return new HashSet<>();
	}

	public Set<Tuple> zrevrangeWithScores(final String key, final long start, final long end) {
		try {
			return redisClusterService.zrevrangeWithScores(key, start, end);
		} catch (Exception e) {
			log.error("redis zrevrangeWithScores error, key is {}", key, e);
		}
		return new HashSet<>();
	}

	public Double zscore(final String key, final String member) {
		try {
			return redisClusterService.zscore(key, member);
		} catch (Exception e) {
			log.error("redis zscore error, key is {}", key, e);
		}
		return null;
	}

	public Long zcount(String key, double min, double max) {
		try {
			return redisClusterService.zcount(key, min, max);
		} catch (Exception e) {
			log.error("redis zcount error, key is {}", key, e);
		}
		return null;
	}

	public Long zremrangeByRank(String key, long start, long end) {
		try {
			return redisClusterService.zremrangeByRank(key, start, end);
		} catch (Exception e) {
			log.error("redis zremrangeByRank error, key is {}", key, e);
		}
		return null;
	}

	public boolean sismember( String key, final String member) {
		try {
			return redisClusterService.sismember(key, member);
		} catch (Exception e) {
			log.error("redis sismember error, key is {}", key, e);
		}
		return false;
	}

	public Long sadd(String key, String member) {
		try {
			return redisClusterService.sadd(key, member);
		} catch (Exception e) {
			log.error("redis sadd error, key is {}", key, e);
		}
		return 0l;
	}

	public Long batchSadd(String key, String... member) {
		try {
			return redisClusterService.sadd(key, member);
		} catch (Exception e) {
			log.error("redis batchsadd error, key is {}", key, e);
		}
		return 0l;
	}

	public String lindex(String key, long index) {
		try {
			return redisClusterService.lindex(key, index);
		} catch (Exception e) {
			log.error("redis lindex error, key is {}", key, e);
		}
		return null;
	}

	public Boolean exist(String key, Boolean[] isException){
		isException[0] = false;
		try{
			return redisClusterService.exists(key);
		}catch(Exception e){
			log.error("redis exist error, key is {}", key, e);
			isException[0] = true;
		}
		return false;
	}
}
