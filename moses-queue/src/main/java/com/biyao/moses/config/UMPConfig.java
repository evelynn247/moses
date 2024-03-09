package com.biyao.moses.config;

import com.by.profiler.annotation.Annotation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UMPConfig {

    @Bean
    public Annotation umpAnnotation() {
        Annotation annotation = new Annotation();
        annotation.setAppId(10174);
        return annotation;
    }

}
