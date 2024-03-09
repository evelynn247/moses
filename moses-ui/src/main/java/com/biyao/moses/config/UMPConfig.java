package com.biyao.moses.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.by.profiler.annotation.Annotation;

@Configuration
public class UMPConfig {

	@Bean
	public Annotation umpAnnotation() {
		Annotation annotation = new Annotation();
		annotation.setAppId(10108);
		return annotation;
	}

}
