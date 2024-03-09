package com.biyao.moses.cache;

import javax.annotation.PostConstruct;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class ProductRetrievalCache extends ProductRetrievalCacheNoCron{
	 
	@PostConstruct
	protected void init() {
		super.init();
	}
	
	@Scheduled(cron = "0 0 0/3 * * ?")
	@Override
	protected void refreshProductRetrievalCache() {
		 
		super.refreshProductRetrievalCache();
	}
}