package com.biyao.moses.cache;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * LR算法商品特征缓存 10分钟刷新一次
 */
@Component
@EnableScheduling
public class ProductFeaCache extends ProductFeaCacheNoCron{

    @PostConstruct
    @Override
    protected void init() {
        super.init();
    }

    @Scheduled(cron = "0 0/10 * * * ?")
    @Override
    protected void refreshFeaCache() {
        super.refreshFeaCache();
    }
}
