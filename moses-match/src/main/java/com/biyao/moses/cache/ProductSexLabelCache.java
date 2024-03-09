package com.biyao.moses.cache;

import javax.annotation.PostConstruct;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@EnableScheduling
public class ProductSexLabelCache extends ProductSexlabelCacheNoCron{
 
	@PostConstruct
	protected void init() {
		super.init();
	}
	
	@Scheduled(cron = "0 0 0/3 * * ?")
	@Override
	protected void refreshProductSexlabelCache() {
		super.refreshProductSexlabelCache();

	}
	
}