package com.biyao.moses.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.biyao.moses.filter.GzipFilter; 

/**
 * 
 * @author biyao
 *
 */
@Configuration
public class GzipFilterConfig {
	
    @Bean
    public FilterRegistrationBean<GzipFilter> authenticatorFilter() {
    	GzipFilter gzipFilter = new GzipFilter();
        FilterRegistrationBean<GzipFilter> registrationBean = new FilterRegistrationBean<GzipFilter>(gzipFilter);
        registrationBean.setEnabled(true);
        registrationBean.setOrder(1);
        registrationBean.addUrlPatterns("/recommend/*");
        return registrationBean;
    }
}
