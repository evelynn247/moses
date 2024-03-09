package com.biyao.moses;

import com.alibaba.fastjson.JSONObject;
import com.biyao.moses.config.AlgorithmRedisConfig;
import com.biyao.moses.config.JedisClusterPipeline;
import com.biyao.moses.config.JedisSlotAdvancedConnectionHandler;
import com.biyao.moses.po.Video;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import redis.clients.jedis.*;
import redis.clients.util.JedisClusterCRC16;

import javax.annotation.Resource;
import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * @program: moses-parent-online
 * @description:
 * @author: changxiaowei
 * @Date: 2022-02-15 09:06
 **/
@SpringBootTest
public class PipelineTest{

    public static void main(String[] args) throws UnsupportedEncodingException {

    }

//    //普通JedisCluster 批量写入测试
//    public void jedisCluster(Set<HostAndPort> nodes,String redisPassword) throws UnsupportedEncodingException {
//
//        List<String> setKyes = new ArrayList<>();
//        for (int i = 0; i < 10000; i++) {
//            setKyes.add("single"+i);
//        }
//        long start = System.currentTimeMillis();
//        for(int j = 0;j < setKyes.size();j++){
//            jedisCluster.setex(setKyes.get(j),100,"value"+j);
//        }
//        System.out.println("JedisCluster total time:"+(System.currentTimeMillis() - start));
//    }
    //JedisCluster Pipeline 批量写入测试
    public void clusterPipeline() {

    }
}

