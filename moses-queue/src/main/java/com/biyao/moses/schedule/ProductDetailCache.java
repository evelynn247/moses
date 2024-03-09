package com.biyao.moses.schedule;

import com.biyao.moses.cache.ProductDetailCacheNoCron;
import javax.annotation.PostConstruct;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class ProductDetailCache extends ProductDetailCacheNoCron{
	
	@PostConstruct
	protected void init() {
		super.init();
	}
	
	@Scheduled(cron = "0 1 0/1 * * ?")
	@Override
	protected void refreshProductDetailCache() {
		super.refreshProductDetailCache();
	}
	
}