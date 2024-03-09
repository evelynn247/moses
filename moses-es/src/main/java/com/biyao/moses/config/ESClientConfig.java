package com.biyao.moses.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * @program: moses-parent-online
 * @description:
 * @author: changxiaowei
 * @create: 2021-09-15 15:28
 **/
@Configuration
@Slf4j
public class ESClientConfig {
    @Value("${es.cluster.master.hostlist}")
    private String esHostList ;
    @PostConstruct
    @Bean(name = "restHighLevelClient")
    public RestHighLevelClient initRestHighLevelClient() {
        RestHighLevelClient restHighLevelClient = null ;
        //解析配置参数
        List<HttpHost> httpHosts = buildHost();
        // 创建RestClientBuilder
        RestClientBuilder builder = RestClient.builder(httpHosts.toArray(new HttpHost[0]));
        try {
            // 创建RestHighLevelClient
            restHighLevelClient = new RestHighLevelClient(builder);
            log.info("[检查日志]RestHighLevelClient 初始化成功");
        }catch (Exception e){
            log.error("[严重异常][邮件告警]RestHighLevelClient 初始化失败,异常信息:",e);
        }
        return restHighLevelClient;
    }

    private List<HttpHost> buildHost(){
        List<HttpHost> httpHosts =new ArrayList<>();
        String[] esHosts = esHostList.split(";");
        for (String esHost :esHosts){
            try {
                String[] hostAndPort = esHost.split(":");
                HttpHost host = new HttpHost(hostAndPort[0],Integer.valueOf(hostAndPort[1]));
                httpHosts.add(host);
            }catch (Exception e){
                log.error("[严重异常]解析eshost配置时出错，原因：",e);
            }
        }
       return httpHosts;
    }
}
