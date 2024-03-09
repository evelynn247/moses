package com.biyao.moses;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ImportResource;
import org.springframework.scheduling.annotation.EnableAsync;


@SpringBootApplication
@ImportResource({"classpath:spring.xml"})
@EnableAsync
public class MosesEsApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(MosesEsApplication.class);
    }
    public static void main(String[] args) {
        SpringApplication.run(MosesEsApplication.class, args);
    }

}
