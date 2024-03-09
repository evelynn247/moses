package com.biyao.moses;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ImportResource;
import org.springframework.scheduling.annotation.EnableAsync;

import com.biyao.moses.cache.HomeCacheListener;

/**
 * 启动类
 * 
 * @Description
 * @Date 2018年9月27日
 */
@SpringBootApplication
@EnableFeignClients
@EnableHystrix
@EnableAsync
@ImportResource(value = { "classpath:dubbo-consumer.xml" })
public class StartApplication extends SpringBootServletInitializer {

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(StartApplication.class);
	}

	public static void main(String[] args) {
		SpringApplication.run(StartApplication.class, args);
	}
}