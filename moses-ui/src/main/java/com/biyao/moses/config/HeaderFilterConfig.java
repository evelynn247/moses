package com.biyao.moses.config;

import com.biyao.moses.filter.GzipFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.biyao.moses.filter.HeaderFilter;
import org.springframework.core.annotation.Order;

/**
 * 
 * @Description 
 * @Date 2018年9月27日
 */
@Configuration
public class HeaderFilterConfig {
	
    @Bean
    @Order(1)
    public FilterRegistrationBean authenticatorFilter() {
    	HeaderFilter headerFilter = new HeaderFilter();
        FilterRegistrationBean registrationBean = new FilterRegistrationBean(headerFilter);
        registrationBean.setEnabled(true);
        registrationBean.addUrlPatterns("/recommend/ui/*", "/recommend/common/*");
        return registrationBean;
    }
    @Bean
    @Order(2)
    public FilterRegistrationBean<GzipFilter> gzipFilter() {
        GzipFilter gzipFilter = new GzipFilter();
        FilterRegistrationBean<GzipFilter> registrationBean = new FilterRegistrationBean<GzipFilter>(gzipFilter);
        registrationBean.setEnabled(true);
        registrationBean.addUrlPatterns("/recommend/*");
        return registrationBean;
    }
}
