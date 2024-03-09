package com.biyao.moses.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:prop/rocketmq.properties")
public class RocketMqConfig {

}
