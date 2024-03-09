package com.biyao.moses.cache;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Ctvr数据缓存
 */
@Component
public class CtvrProductCache extends CtvrCacheNoCron {

    @PostConstruct
    protected void init() {
        super.init();
    }

}
