package com.biyao.moses.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;

/**
* @Description dubbo配置
* @date 2018年11月28日下午5:39:40
* @version V1.0 
* @author 邹立强 (zouliqiang@idstaff.com)
* <p>Copyright (c) Department of Research and Development/Beijing.</p>
 */
@Configuration
@PropertySource("classpath:prop/dubbo.properties")
@ImportResource({ "classpath:conf/dubbo-consumer.xml" })
public class DubboConfig {  
  
}  