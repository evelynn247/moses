package com.biyao.moses.cache;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * LR算法特征缓存 10分钟刷新一次
 */
@Component("lrFeatureCache")
@EnableScheduling
public class LRFeatureCache extends LRFeatureCacheNoCron{

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
