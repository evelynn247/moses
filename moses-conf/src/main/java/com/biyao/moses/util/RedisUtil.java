package com.biyao.moses.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.by.bimdb.service.RedisClusterService;

/**
 * @description: redis工具类
 * @author: luozhuo
 * @date: 2017年2月22日 上午10:19:39
 * @version: V1.0.0
 */
@Service
public class RedisUtil {
	private Logger logger = LoggerFactory.getLogger(RedisUtil.class);
	@Resource
	private RedisClusterService redisClusterService;

	public String getString(String key) {
		String sRet = "";
		try {
			sRet = redisClusterService.get(key);
		} catch (Exception e) {
			logger.error("redis getString error, key is {},error message is {}", key, e);
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
			logger.error("redis setString error, key is {},value is {},error message is {}", key, value, e);
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
			logger.error("redis incr error, key is {},error message is {}", key, e);
		}
		return incr;
	}

	public String hgetStr(String key, String field) {
		String result = "";
		try {
			result = redisClusterService.hget(key, field);
		} catch (Exception e) {
			logger.error("redis hgetStr error, key is {},error message is {}", key, e);
		}
		return result;
	}

	public boolean hmset(String key, Map<String, String> hash) {
		boolean bRet = false;
		try {
			redisClusterService.hmset(key, hash);
			bRet = true;
		} catch (Exception e) {
			logger.error("redis hmsetString error, key is {},value is {},error message is{}", key, hash, e);
		}
		return bRet;
	}

	public boolean del(String key) {
		try {
			return redisClusterService.del(key) > 0;
		} catch (Exception e) {
			logger.error("redis del error, key is {},error message is {}", key, e);
			return false;
		}
	}

	public Map<String, String> hgetAll(String key) {
		Map<String, String> map = new HashMap<>();
		try {
			map = redisClusterService.hgetAll(key);
			if (map.isEmpty()) {
				return null;
			}
		} catch (Exception e) {
			logger.error("redis hgetAll error, key is {},error message is {}", key, e);
		}
		return map;
	}

	public Long hset(String key, String field, String value) {
		Long result = 0L;
		try {
			result = redisClusterService.hset(key, field, value);

		} catch (Exception e) {
			logger.error("redis hset error, key is {},error message is {}", key, e);
		}
		return result;
	}

	public Long hdel(String key, String field) {
		Long result = 0L;
		try {
			result = redisClusterService.hdel(key, field);

		} catch (Exception e) {
			logger.error("redis hset error, key is {},error message is {}", key, e);
		}
		return result;
	}

	public Boolean existString(String key) {
		Boolean sRet = false;
		try {
			sRet = redisClusterService.exists(key);
		} catch (Exception e) {
			logger.error("redis exists error, key is {},error message is {}", key, e);
		}
		return sRet;
	}

	public Boolean sismember(String key, String member) {
		Boolean sRet = false;
		try {
			sRet = redisClusterService.sismember(key, member);
		} catch (Exception e) {
			logger.error("redis sismember error, key is {},error message is {}", key, e);
		}
		return sRet;
	}

	public Long srem(String key, String member) {
		Long sRet = null;
		try {
			sRet = redisClusterService.srem(key, member);
		} catch (Exception e) {
			logger.error("redis srem error, key is {},error message is {}", key, e);
		}
		return sRet;
	}

	public Long sadd(String key, String member) {
		Long sRet = null;
		try {
			sRet = redisClusterService.sadd(key, member);
		} catch (Exception e) {
			logger.error("redis srem error, key is {},error message is {}", key, e);
		}
		return sRet;
	}

	/**
	 * 加锁
	 * 
	 * @param timeout
	 *            获取锁的时间，即业务等待时间
	 * @param lockTime
	 *            锁多长时间释放(不unlock的情况下)
	 */
	public boolean lock(String key, int lockTime) {
		Boolean locked = false;
		try {
			if (redisClusterService.setnx(key, "LOCKED") > 0) {
				if (lockTime > 0) {
					redisClusterService.expire(key, lockTime);
				} else {
					redisClusterService.expire(key, 60);
				}
				locked = true;
			}
		} catch (Exception e) {
			logger.error("", e);
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
			logger.error("redis unlock error, key is {}", key, e);
		}
	}

	public Set<String> smember(String key) {
		Set<String> smembers = null;
		try {
			smembers = redisClusterService.smembers(key);
		} catch (Exception e) {
			logger.error("redis unlock error, key is {}", key, e);
		}
		return smembers;
	}
	
	public Long zadd(String key, double score, String member) {
		try {
			return redisClusterService.zadd(key, score, member);
		} catch (Exception e) {
			logger.error("redis zadd error, key is {}", key, e);
		}
		return null;
	}
	
	public Set<String> zrevrange(final String key, final long start, final long end) {
		try {
			return redisClusterService.zrevrange(key, start, end);
		} catch (Exception e) {
			logger.error("redis zrevrange error, key is {}", key, e);
		}
		return null;
	}
	
	
	public Long renamenx(String oldkey, String newkey) {
		Long sRet = null;
		try {
			sRet = redisClusterService.renamenx(oldkey, newkey);
		} catch (Exception e) {
			logger.error("redis renamenx error, oldkey is {},newkey is {},error message is {}", oldkey,newkey,e);
		}
		return sRet;
	}

}
