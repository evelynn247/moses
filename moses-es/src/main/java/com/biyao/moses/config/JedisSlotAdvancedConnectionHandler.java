package com.biyao.moses.config;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisSlotBasedConnectionHandler;
import redis.clients.jedis.exceptions.JedisNoReachableClusterNodeException;

import java.util.Set;

/**
 * @program: moses-parent-online
 * @description: 获取redis 资源池的门面类  即获取到JedisClusterInfoCache  （
 *  JedisClusterInfoCache 是 JedisClusterConnectionHandler 的属性 因此需要写一个获取资源池的类  继承 JedisClusterConnectionHandler）
 * @author: changxiaowei
 * @Date: 2022-02-15 09:02
 **/
public class JedisSlotAdvancedConnectionHandler extends JedisSlotBasedConnectionHandler {

    public JedisSlotAdvancedConnectionHandler(Set<HostAndPort> nodes, GenericObjectPoolConfig poolConfig, int connectionTimeout) {
        super(nodes, poolConfig,connectionTimeout);
    }

    public JedisPool getJedisPoolFromSlot(int slot) {
        JedisPool connectionPool = cache.getSlotPool(slot);
        if (connectionPool != null) {
            return connectionPool;
        } else {
            // 查询不到就刷新节点信息
            renewSlotCache();
            connectionPool = cache.getSlotPool(slot);
            if (connectionPool != null) {
                return connectionPool;
            } else {
                throw new JedisNoReachableClusterNodeException("No reachable node in cluster for slot " + slot);
            }
        }
    }
}