package com.biyao.search.ui.task;

import com.biyao.search.ui.cache.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;


/**
 * @author zj
 * @version 1.0
 * @date 2019/10/23 15:55
 * @description 定时任务管理类
 */
@Component
@Slf4j
@EnableScheduling
public class TaskManager {
    @Autowired
    FacetCache facetCache;

    @Resource
    ProductCache productCache;

    @Resource
    RedisDataCache redisDataCache;

    @Resource
    SyncVDataCache syncVDataCache;

    @Resource
    SyncVStoreCache syncVStoreCache;

    @Autowired
    DeriveProductDetailCache deriveProductDetailCache;
    
    @Autowired
    CoffeePrivateCache coffeePrivateCache;

    @Autowired
    SimilarProductCache similarProductCache;

    @Autowired
    AlgorithmRedisDataCache algorithmRedisDataCache;

    @Scheduled(cron = "40 0/5 * * * ?")
    public void refreshFacet(){
    	log.info("[任务报告]定时5分钟初始化商品facet数据到本地缓存--》start");
        facetCache.refreshFacet();
        log.info("[任务报告]定时5分钟初始化商品facet数据到本地缓存--》end");
    }

    @Scheduled(cron = "0 0/4 * * * ?")
    public void refreshProduct(){
    	log.info("[任务报告]定时4分钟同步商品数据到本地缓存--》start");
        productCache.refresh();
        log.info("[任务报告]定时4分钟同步商品数据到本地缓存--》start");
    }

    @Scheduled(cron = "30 0/5 * * * ?")
    public void refreshRedisData(){
    	log.info("[任务报告]定时5分钟同步Redis数据到本地缓存--》start");
        redisDataCache.refresh();
    	log.info("[任务报告]定时5分钟同步Redis数据到本地缓存--》end");

    }

    @Scheduled(cron = "20 0/5 * * * ? ")
    private void refreshVData() {
    	log.info("[任务报告]定时5分钟同步大V/企业定制用户/平台号数据到本地缓存--》start");
        syncVDataCache.refreshCache();
        log.info("[任务报告]定时5分钟同步大V/企业定制用户/平台号数据到本地缓存--》end");
    }

    @Scheduled(cron = "25 0/5 * * * ? ")
    private void refreshVStore() {
        log.info("[任务报告]定时5分钟同步梦工厂店铺数据到本地缓存--》start");
        syncVStoreCache.refreshCache();
        log.info("[任务报告]定时5分钟同步梦工厂店铺数据到本地缓存--》end");
    }

    @Scheduled(cron = "10 0/4 * * * ?")
    private void refreshDeriveProductCache(){
        log.info("[任务报告]定时4分钟同步衍生商品数据到本地缓存--》start");
        deriveProductDetailCache.refreshProductDetailCache();
        log.info("[任务报告]定时4分钟同步衍生商品数据到本地缓存--》end");
    }
    
    @Scheduled(cron = "0 0/2 * * * ?")
    private void refreshCoffeeConf() {
    	log.info("[任务报告]定时2分钟同步咖啡商品数据到本地缓存--》start");
    	coffeePrivateCache.refreshCoffeeConf();
    	log.info("[任务报告]定时2分钟同步咖啡商品数据到本地缓存--》end");
    }

    @Scheduled(cron = "0 0 6 * * ? ")
    private void refreshSimilarProduct(){
        log.info("[任务报告]定时凌晨6点同步query相似商品数据到本地缓存--》start");
        similarProductCache.refresh();
        log.info("[任务报告]定时凌晨6点同步query相似商品数据到本地缓存--》end");
    }

    @Scheduled(cron = "0 0/10 * * * ?")
    private void refreshProductLunVideoFlag(){
        log.info("[任务报告]定时10分钟同步商品对应是否含有轮播图视频标识映射数据到本地缓存--》start");
        algorithmRedisDataCache.refresh();
        log.info("[任务报告]定时10分钟同步商品对应是否含有轮播图视频标识映射数据到本地缓存--》end");
    }
}
