package com.biyao.moses;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.ParameterBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.schema.ModelRef;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Parameter;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * Swagger2
 * 
 * @Description
 * @Date 2018年9月27日
 */
@Configuration
@EnableSwagger2
public class Swagger2 {
	@Bean
	public Docket createRestApi() {
		List<Parameter> parameters = new ArrayList<Parameter>();

		ParameterBuilder parameterBuilder = new ParameterBuilder();
		parameterBuilder.modelRef(new ModelRef("string")).parameterType("header").required(true).build();

		parameters.add(parameterBuilder.name("uuid").description("设备uuid,必传").build());
		parameters.add(parameterBuilder.name("uid").description("用户ID，如果没有，则传空字符串").build());
		parameters.add(parameterBuilder.name("siteId").description("网站编号,必传").build());
		parameters.add(parameterBuilder.name("device").description("设备,必传没有则传空字符串").build());
		parameters.add(parameterBuilder.name("avn").description("数字版本号，必传，没有则传空字符串").build());
		parameters.add(parameterBuilder.name("pvid").description("页面唯一标识,必传").build());
		parameters.add(parameterBuilder.name("did").description("设备号，必传，没有则传空字符串").build());
		parameters.add(parameterBuilder.name("ctp").description("ctp").required(false).build());
		parameters.add(parameterBuilder.name("stp").description("stp").required(false).build());

		return new Docket(DocumentationType.SWAGGER_2).useDefaultResponseMessages(false).apiInfo(apiInfo()).select()
				.apis(RequestHandlerSelectors.basePackage("com.biyao.moses")).paths(PathSelectors.any()).build()
				.globalOperationParameters(parameters);
	}

	private ApiInfo apiInfo() {
		return new ApiInfoBuilder().title("mosesrank文档").description("post请求").termsOfServiceUrl("http://www.baidu.com")
				.version("1.0").build();
	}
}
