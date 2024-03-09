package com.biyao.moses.cache;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @ClassName CandidateCate3ProductCache
 * @Description 不同用户类型候选三级类目组及其候选商品缓存
 * @Author xiaojiankai
 * @Date 2019/11/29 20:17
 * @Version 1.0
 **/
@Component
public class CandidateCate3ProductCache extends CandidateCate3ProductCacheNoCron {

    @PostConstruct
    protected void init(){
        super.init();
    }
}
