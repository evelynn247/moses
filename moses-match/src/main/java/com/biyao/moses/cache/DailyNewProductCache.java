package com.biyao.moses.cache;

import javax.annotation.PostConstruct;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class DailyNewProductCache extends DailyNewProductCacheNoCron{
	
	@PostConstruct
	protected void init() {
		super.init();
	}

	@Scheduled(cron = "0 0/2 * * * ?")
	@Override
	protected void refreshDailyNewProductCache() {
		super.refreshDailyNewProductCache();
	}
}
