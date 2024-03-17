package com.biyao.search.ui.cache;
import com.biyao.search.ui.manager.FacetManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * author: xiafang
 * date: 2019/12/4
 */
@Component
@Slf4j
public class FacetCache {
    @Autowired
    FacetManager facetManager;

    @PostConstruct
    public void init(){
        log.info("[任务报告]初始化商品facet数据，系统启动时初始化到本地缓存--》start");
        refreshFacet();
        log.info("[任务报告]初始化商品facet数据，系统启动时初始化到本地缓存--》end");
    }

    public void refreshFacet(){
        try{
            facetManager.refreshCache();
        }catch (Exception e){
            log.error("[严重异常]初始化商品facet数据异常", e);
        }
    }
}