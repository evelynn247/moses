package com.biyao.moses.config;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import java.util.Set;

/**
 * @program: moses-parent-online
 * @description:
 * @author: changxiaowei
 * @Date: 2022-02-15 09:01
 **/

public class JedisClusterPipeline extends JedisCluster {

    public JedisClusterPipeline(Set<HostAndPort> jedisClusterNode, int connectionTimeout, final GenericObjectPoolConfig poolConfig) {
        super(jedisClusterNode,connectionTimeout, poolConfig);
        super.connectionHandler = new JedisSlotAdvancedConnectionHandler(jedisClusterNode, poolConfig,connectionTimeout);
    }

    public JedisSlotAdvancedConnectionHandler getConnectionHandler() {
        return (JedisSlotAdvancedConnectionHandler)this.connectionHandler;
    }

    /**
     * 刷新集群信息，当集群信息发生变更时调用
     * @param
     * @return
     */
    public void refreshCluster() {
        connectionHandler.renewSlotCache();
    }
}
