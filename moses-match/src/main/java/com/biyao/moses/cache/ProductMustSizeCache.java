package com.biyao.moses.cache;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@EnableScheduling
public class ProductMustSizeCache extends ProductMustSizeCacheNoCron {

    @PostConstruct
    protected void init() {
        super.init();
    }

    @Scheduled(cron = "0 0/2 * * * ?")
    @Override
    protected void refreshProductMustSizeCache() {
        super.refreshProductMustSizeCache();
    }
}
