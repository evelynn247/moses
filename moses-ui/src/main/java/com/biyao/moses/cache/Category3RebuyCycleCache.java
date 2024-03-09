package com.biyao.moses.cache;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @ClassName Category3RebuyCycleCache
 * @Description 后台三级类目对应的复购周期区间的最小值缓存
 * @Author xiaojiankai
 * @Date 2020/1/17 17:30
 * @Version 1.0
 **/
@Component
public class Category3RebuyCycleCache extends Category3RebuyCycleNoCache {

    @PostConstruct
    protected void init(){
        super.init();
    }
}
