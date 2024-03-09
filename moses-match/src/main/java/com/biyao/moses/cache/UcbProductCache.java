package com.biyao.moses.cache;


import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @ClassName UcbProductCache
 * @Description ucb召回源缓存
 * @Author xiaojiankai
 * @Date 2019/12/24 10:41
 * @Version 1.0
 **/
@Component
public class UcbProductCache extends UcbProductCacheNoCron {

    @PostConstruct
    protected void init() {
        super.init();
    }
}
