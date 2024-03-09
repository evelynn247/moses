package com.biyao.moses.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * @Description: 加载redis配置
 * @Author: zouliqiang@idstaff.com
 * @Date: 2019/3/27 20:05
 * @Versions:1.0.0
 **/
@Configuration
@PropertySource("classpath:prop/redis.properties")
public class RedisConfiguration {

}
