package com.biyao.moses.cache;


import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @ClassName HotSaleProductCache
 * @Description 热销召回源缓存
 * @Author admin
 * @Date 2019/10/16 14:41
 * @Version 1.0
 **/
@Component
public class HotSaleProductCache extends HotSaleProductCacheNoCron {

    @PostConstruct
    protected void init() {
        super.init();
    }
}
