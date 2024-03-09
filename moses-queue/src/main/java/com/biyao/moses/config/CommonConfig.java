package com.biyao.moses.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;

/**
* @Description UMP监控配置
* @date 2019年6月4日下午6:25:47
* @version V1.0 
* @author 邹立强 (zouliqiang@idstaff.com)
* <p>Copyright (c) Department of Research and Development/Beijing.</p>
 */
@Configuration
@ImportResource({ "classpath:conf/spring-ump.xml" })
@PropertySource(value ={"classpath:prop/common.properties"} ,
        encoding = "utf-8")
public class CommonConfig {

}
