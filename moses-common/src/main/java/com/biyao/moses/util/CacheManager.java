package com.biyao.moses.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

public class CacheManager {
    private static final Logger logger = LoggerFactory.getLogger(CacheManager.class);
    // 键值对集合
    private final static Map<String, Entity> map = new ConcurrentHashMap<>();
    // 定时器线程池，用于清除过期缓存
    private final static ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();


    /**
     * 清空所有缓存
     *
     */
    public static void removeAll() {
        map.clear();
    }
    /**
     * 添加缓存
     *
     * @param key  键
     * @param data 值
     */
    public synchronized static void put(String key, Object data) {
        CacheManager.put(key, data, 0);
    }
    /**
     * 判断key是否存在
     *
     * @param key  键
     */
    public static boolean exists(String key) {
        return map.keySet().contains(key);
    }

    /**
     * 添加缓存
     *
     * @param key    键
     * @param data   值
     * @param expire 过期时间，单位：毫秒， 0表示无限长
     */
    public synchronized static void put(String key, Object data, long expire) {
        // 清除原键值对
        CacheManager.remove(key);
        // 设置过期时间
        if (expire > 0) {
            Future future = executor.schedule(new Runnable() {
                @Override
                public void run() {
                    // 过期后清除该键值对
                    synchronized (CacheManager.class) {
                        map.remove(key);
                        //System.out.println("remove key :" + key+ " date:" + (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).format(new Date()));
                    }
                }
            }, expire, TimeUnit.MILLISECONDS);
            map.put(key, new Entity(data, future));
        } else {
            // 不设置过期时间
            map.put(key, new Entity(data, null));
        }
    }

    /**
     * 读取缓存
     *
     * @param key 键
     * @return
     */
    public  static Object get(String key) {
        Entity entity = map.get(key);
        return entity == null ? null : entity.getValue();
    }

    /**
     * 读取缓存
     *
     * @param key 键 * @param clazz 值类型
     * @return
     */
    public  static <T> T get(String key, Class<T> clazz) {
        return clazz.cast(CacheManager.get(key));
    }

    /**
     * 清除缓存
     *
     * @param key
     * @return
     */
    public synchronized static boolean remove(String key) {
        boolean flag = true;
        try {
            // 清除原缓存数据
            Entity entity = map.remove(key);
            if (entity == null)
                return flag;
            // 清除原键值对定时器
            Future future = entity.getFuture();
            if (future != null)
                future.cancel(true);
        } catch (Exception e) {
            logger.error("缓存移除异常", e);
            flag = false;
        }
        return flag;
    }
    /**
     * 清除缓存  根据key的包含关系，进行删除，慎用，可能会造成删除别的key，
     * 如果逻辑上key包含字段唯一可以使用
     * 效率低
     * @param key
     * @return
     */
    public synchronized static boolean removeLike(String key) {
        boolean flag = true;
        try {
            Set<String> set = map.keySet();
            for(String k : set){
                if(!k.contains(key)){
                    continue;
                }
                // 清除原缓存数据
                Entity entity = map.remove(k);
                if (entity == null) {
                    return flag;
                }
                // 清除原键值对定时器
                Future future = entity.getFuture();
                if (future != null) {
                    future.cancel(true);
                }
            }
        } catch (Exception e) {
            logger.error("缓存移除异常", e);
            flag = false;
        }
        return flag;
    }

    /**
     * 查询当前缓存的键值对数量
     *
     * @return
     */
    public synchronized static int size() {
        return map.size();
    }

    /**
     * 缓存实体类
     */
    private static class Entity {
        // 键值对的value
        private Object value;
        // 定时器Future
        private Future future;

        public Entity(Object value, Future future) {
            this.value = value;
            this.future = future;
        }

        /**
         * 获取值
         *
         * @return
         */
        public Object getValue() {
            return value;
        }

        /**
         * 获取Future对象
         *
         * @return
         */
        public Future getFuture() {
            return future;
        }
    }
}
