package com.biyao.moses.cache;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @ClassName SimilarCategory3IdCache
 * @Description 相似三级类目缓存
 * @Author xiaojiankai
 * @Date 2020/1/17 17:32
 * @Version 1.0
 **/
@Component
public class SimilarCategory3IdCache extends SimilarCategory3IdNoCache {
    @PostConstruct
    protected void init(){
        super.init();
    }
}
