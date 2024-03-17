package com.biyao.search.ui.util;

import com.alibaba.dubbo.common.utils.CollectionUtils;
import com.by.bimdb.service.RedisSentinelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;
import redis.clients.jedis.exceptions.JedisException;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
    private RedisSentinelService redisSentinelService;
	/**
	 *
	 * @param key
	 * @param object
	 * @param seconds
	 *            时间，< 0 表示永不过期，单位 s
	 * @return
	 */
	public boolean set(String key, Object object, int seconds) {
		try {
			if (seconds > 0) {
                redisSentinelService.setex((key).getBytes(), seconds, SerializeUtil.serialize(object));
			} else {
                redisSentinelService.set((key).getBytes(), SerializeUtil.serialize(object));
			}
		} catch (JedisException e) {
            logger.error("[严重异常][redis异常]redis set error, key is {},value is {},error message is {}",key,object,e);
            return false;
        }
        return true;
    }

	/**
	 * @param key
	 * @return 不存在返回null
	 */
	public Object get(String key) throws JedisException {
        byte[] value = null;
		try {
			value = redisSentinelService.get((key).getBytes());
			if (value == null)
				return null;
		} catch (JedisException e) {
            logger.error("[严重异常][redis异常]redis get object error, key is {},error message is {}",key,e);
		}
        return SerializeUtil.unserialize(value);
	}

	/**
	 * 剩余有效期
	 *
	 * @param key
	 * @return 有过期时间：过期时间
	 * @return 无过期时间：-1
	 * @return 不存在的key：-1
	 */
	public Long ttl(String key) {
		try {
			return redisSentinelService.ttl(key);
		} catch (JedisException e) {
            logger.error("[严重异常][redis异常]redis get ttl error, key is {},error message is {}",key,e);
            return null;
		}
	}

	/**
	 * 删除
	 *
	 * @param key
	 * @return
	 */
	public boolean del(String key) {
		try {
			return redisSentinelService.del(key) > 0;
		} catch (JedisException e) {
            logger.error("[严重异常][redis异常]redis del error, key is {},error message is {}",key,e);
            return false;
		}
	}

	/********************************* Redis 哈希 ******************************/
	/**
	 * 根据一级key，获取所有的数据
	 *
	 * @param key
	 *            一级key
	 * @return 不存在返回null
	 */
	public Map<String, Object> hget(String key) {
        Map<String, Object> returnMap = new HashMap<String, Object>();
		try {
			Map<byte[], byte[]> values = redisSentinelService.hgetAll(key.getBytes());
			if (values.size() == 0) {
				return null;
			} else {
				for (Entry<byte[], byte[]> value : values.entrySet()) {
					returnMap.put(new String(value.getKey()), SerializeUtil.unserialize(value.getValue()));
				}

			}
		} catch (JedisException e) {
            logger.error("[严重异常][redis异常]redis hget error, key is {},error message is {}",key,e);
		}
        return returnMap;
	}

	/**
	 * 根据一级key，二级key获取数据
	 *
	 * @param key
	 *            一级key
	 * @param field
	 *            二级key
	 * @return 不存在返回null
	 */
	public Object hget(String key, String field) {
	    Object object = null;
		try {
			byte[] bytes = redisSentinelService.hget(key.getBytes(), field.getBytes());
			if (bytes == null)
				return null;
            object = SerializeUtil.unserialize(bytes);
		} catch (JedisException e) {
            logger.error("[严重异常][redis异常]redis hget error, key is {},field is {},error message is {}",key,field,e);
		}
        return object;
	}

	public String hgetString(String key, String field) {
	    String sRet = "";
		try {
			byte[] obj = redisSentinelService.hget(key.getBytes(), field.getBytes());
			if (obj == null)
				return sRet;
            sRet = new String(obj);
		} catch (JedisException e) {
            logger.error("[严重异常][redis异常]redis hgetString error, key is {},field is {},error message is {}",key,field,e);
		}
		return sRet;
	}

	/**
	 * 设置值
	 *
	 * @param key
	 *            一级key
	 * @param field
	 *            二级key
	 * @param value
	 * @param seconds
	 *            <0 表示永不过期，单位s，过期时间作用于一级key
	 * @return
	 */
	public boolean hset(String key, String field, Object value, int seconds) {
	    boolean bRet = false;
        try {
            redisSentinelService.hset(key.getBytes(), field.getBytes(), SerializeUtil.serialize(value));
            if (seconds > 0) {
                redisSentinelService.expire(key, seconds);
            }
            bRet = true;
        } catch (JedisException e) {
            logger.error("[严重异常][redis异常]redis hset error, key is {},field is {},value is {} ,seconds is {} ,error message is {}", key, field, value ,seconds ,e);
        }
        return bRet;
    }
	/**
	 * 根据一级key，二级key删除数据
	 *
	 * @param key
	 * @param field
	 * @return
	 */
	public boolean hdel(String key, String field) {
        boolean bRet = false;
		try {
            redisSentinelService.hdel(key, field);
            bRet =  true;
		} catch (JedisException e) {
            logger.error("[严重异常][redis异常]redis hdel error, key is {},field is {},error message is {}", key, field ,e);
        }
        return bRet;
	}

	/**
	 * 根据一级key删除所有
	 *
	 * @param key
	 * @return
	 */
	public boolean hdelAll(String key) {
        boolean bRet = false;
		try {
            redisSentinelService.hdel(key);
            bRet = true;
		} catch (JedisException e) {
            logger.error("[严重异常][redis异常]redis hdelAll error, key is {},error message is {}", key, e);
        }
        return bRet;
	}


	public boolean exists(String key) {
        boolean bRet = false;
		try {
            bRet = redisSentinelService.exists(key);
		} catch (JedisException e) {
            logger.error("[严重异常][redis异常]redis exists error, key is {},error message is {}", key, e);
        }
        return bRet;
	}
	public boolean hexist(String key, String field) {
        boolean bRet = false;
		try {
            bRet = redisSentinelService.hexists(key, field);
		} catch (JedisException e) {
            logger.error("[严重异常][redis异常]redis hexist error, key is {},field is {} ,error message is {}", key,field, e);
		}
		return bRet;
	}


	/**
	 * @param key
	 * @return 不存在返回null
	 */
	public byte[] gettobyte(String key) {
	    byte[] byteRet = null;
		try {
            byteRet = redisSentinelService.get(key.getBytes());
		} catch (JedisException e) {
            logger.error("[严重异常][redis异常]redis gettobyte error, key is {},error message is {}", key , e);
		}
		return byteRet;
	}


	/**
	 * 根据一级key，二级key获取数据
	 *
	 * @param key
	 *            一级key
	 * @param field
	 *            二级key
	 * @return 不存在返回null
	 */
	public byte[] hgettobyte(String key, String field) {
	    byte[]  byteRet = null;
		try {
            byteRet = redisSentinelService.hget(key.getBytes(), field.getBytes());
		} catch (JedisException e) {
            logger.error("[严重异常][redis异常]redis hgettobyte error, key is {},field is {} ,error message is {}", key,field, e);
        }
        return byteRet;
	}



	/**
	 *
	 * @param key
	 * @param value
	 * @param seconds
	 *            时间，< 0 表示永不过期，单位 s
	 * @return
	 */
	public boolean settobyte(String key, byte[] value, int seconds) {
	    boolean bRet = false;
		try {
			if (seconds > 0)
				redisSentinelService.setex(key.getBytes(), seconds, value);
			else
                redisSentinelService.set(key.getBytes(), value);
            bRet = true;
		} catch (JedisException e) {
            logger.error("[严重异常][redis异常]redis settobyte error, key is {},value is {},error message is {}",key,new String(value),e);
        }
        return bRet;
	}

	/**
	 * 设置值
	 *
	 * @param key
	 *            一级key
	 * @param field
	 *            二级key
	 * @param value
	 * @param seconds
	 *            <0 表示永不过期，单位s，过期时间作用于一级key
	 * @return
	 */
	public boolean hsettobyte(String key, String field, byte[] value, int seconds) {
	    boolean bRet = false;
		try {
			redisSentinelService.hset(key.getBytes(), field.getBytes(), value);
			if (seconds > 0) {
				redisSentinelService.expire(key, seconds);
			}
            bRet = true;
		} catch (JedisException e) {
            logger.error("[严重异常][redis异常]redis hsettobyte error, key is {},field is {} ,value is {},error message is {}",key,field,new String(value),e);
		}
		return bRet;
	}


	/**
     * @description: getString
     * @param key
     * @return
     * @author: luozhuo
     * @date: 2017年2月23日 下午8:35:57
     */
    public String getString(String key) {
        String sRet = "";
        try {
            sRet =  redisSentinelService.get(key);
        } catch (JedisException e) {
            logger.error("[严重异常][redis异常]redis getString error, key is {},error message is {}",key,e);
        }
        return sRet;
    }

    /**
     * @description: set
     * @param key
     * @param value
     * @param seconds 时间，< 0 表示永不过期，单位 s
     * @return
     * @author: luozhuo
     * @date: 2017年2月23日 下午8:36:26
     */
    public boolean setString(String key, String value, int seconds) {
        boolean bRet = false;
        try {
            if (seconds > 0)
                redisSentinelService.setex(key, seconds, value);
            else
                redisSentinelService.set(key, value);
            bRet = true;
        } catch (JedisException e) {
            logger.error("[严重异常][redis异常]redis setString error, key is {},value is {},error message is {}",key,value,e);
        }
        return bRet;
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
			return redisSentinelService.lrange(key, start, end);
		} catch (Exception e) {
			logger.error("[严重异常][redis异常]redis lrange error, key is {}, start is {}, end is {}.", key, start, end, e);
		}
		return null;
	}

	/**
	 * redis hscan
	 * @param key
	 * @return
	 */
	public Map<String, String> hscan(String key){
		Map<String, String> result = new HashMap<>();
		try{
			String cursor = "0";
			ScanParams scanParams = new ScanParams();
			scanParams.count(500);
			ScanResult<Entry<String, String>> tempResult = redisSentinelService.hscan(key, cursor, scanParams);
			while (tempResult != null && !"0".equals(tempResult.getStringCursor())){
				cursor = tempResult.getStringCursor();
				if (!CollectionUtils.isEmpty(tempResult.getResult())){
					tempResult.getResult().forEach(item -> {result.put(item.getKey(), item.getValue());});
				}
				tempResult = redisSentinelService.hscan(key, cursor, scanParams);
			}
			if (tempResult != null && !CollectionUtils.isEmpty(tempResult.getResult())){
				tempResult.getResult().stream().forEach(item -> {result.put(item.getKey(), item.getValue());});
			}
		}catch (Exception e){
			logger.error("redis hscan error, key is {},error message is {}", key, e);
		}
		return result;
	}

}
