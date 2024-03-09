package com.biyao.moses.cache;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @ClassName ProductSeasonCache
 * @Description 商品季节信息缓存
 * @Author xiaojiankai
 * @Date 2019/10/23 16:17
 * @Version 1.0
 **/
@Component
public class ProductSeasonCache extends ProductSeasonCacheNoCron {

    @PostConstruct
    protected void init(){
        super.init();
    }
}