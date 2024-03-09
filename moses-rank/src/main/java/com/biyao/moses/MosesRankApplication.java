package com.biyao.moses;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ImportResource;
import org.springframework.scheduling.annotation.EnableAsync;


//@EnableFeignClients
//@EnableHystrix
@EnableAsync
@SpringBootApplication
@ImportResource(value = { "classpath:dubbo-consumer.xml" })
public class MosesRankApplication extends SpringBootServletInitializer {

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(MosesRankApplication.class);
	}

	public static void main(String[] args) {
		SpringApplication.run(MosesRankApplication.class, args);
	}

}
