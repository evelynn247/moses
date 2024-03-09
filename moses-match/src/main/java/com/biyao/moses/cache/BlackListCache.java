package com.biyao.moses.cache;

import javax.annotation.PostConstruct;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class BlackListCache extends BlackListCacheNoCron {

	@PostConstruct
	public void init() {
		log.info("初始化黑名单过滤缓存...");
		super.refreshBlackCache();
	}

	@Scheduled(cron = "0 0/1 * * * ?")
	public void refreshBlackCache() {
		super.refreshBlackCache();
	}

}