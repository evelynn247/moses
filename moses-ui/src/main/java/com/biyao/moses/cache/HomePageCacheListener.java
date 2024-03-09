package com.biyao.moses.cache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.biyao.moses.service.imp.HomePageCacheServiceImpl;

import lombok.extern.slf4j.Slf4j;


@Component
@Slf4j
public class HomePageCacheListener implements ApplicationRunner {

	@Autowired
	HomePageCacheServiceImpl homePageCacheService;

	@Override
	public void run(ApplicationArguments args) throws Exception {
		log.info("初始化获取page页面缓存");
		homePageCacheService.initpageIdCache();
	}

	
}
