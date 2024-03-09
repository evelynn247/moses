package com.biyao.moses.conf;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.by.profiler.annotation.Annotation;

@Configuration
public class UMPConfig {

	@Bean
	public Annotation umpAnnotation() {
		Annotation annotation = new Annotation();
		annotation.setAppId(10109);
		return annotation;
	}

}