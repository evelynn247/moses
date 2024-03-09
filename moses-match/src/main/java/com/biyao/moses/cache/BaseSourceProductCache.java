package com.biyao.moses.cache;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @ClassName BaseSourceProductCache
 * @Description 基础召回源候选商品缓存
 * @Author xiaojiankai
 * @Date 2020/7/28 14:05
 * @Version 1.0
 **/
@Component
public class BaseSourceProductCache extends BaseSourceProductNoCache {

    @PostConstruct
    protected void init(){
        super.init();
    }
}
