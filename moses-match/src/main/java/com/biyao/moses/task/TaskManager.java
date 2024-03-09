package com.biyao.moses.task;

import com.biyao.moses.cache.*;
import com.biyao.moses.match2.exp.MatchExperimentSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author zhaiweixi@idstaff.com
 * @date 2019/10/12
 **/
@Component
@EnableScheduling
public class TaskManager {

    @Autowired
    private HotSaleProductCache hotSaleProductCache;

    @Autowired
    private ProductSeasonCache productSeasonCache;

    @Autowired
    private MatchExperimentSpace matchExperimentSpace;

    @Autowired
    private CandidateCate3ProductCache candidateCate3ProductCache;

    @Autowired
    private UcbProductCache ucbProductCache;

    @Autowired
    private SimilarCategory3IdCache similarCategory3IdCache;

    @Autowired
    private BaseSourceProductCache baseSourceProductCache;
    /**
     * 每3小时刷新一次热销商品信息
     */
    @Scheduled(cron = "12 1 0/3 * * ?")
    private void refreshHotSaleProduct(){
        hotSaleProductCache.refreshHotSaleProductCache();
    }

    /**
     * 每半小时刷新一次季节商品信息
     */
    @Scheduled(cron = "12 0/30 * * * ?")
    private void refreshProductSeason(){
        productSeasonCache.refreshProductSeasonCache();
    }

    /**
     * 每10分钟刷新一次实验
     */
    @Scheduled(cron = "30 0/10 * * * ?")
    private void refreshMatchExperiment(){
        matchExperimentSpace.refresh();
    }

    /**
     * 每3小时刷新一次候选商品集合和候选类目集合信息
     */
    @Scheduled(cron = "30 2 0/3 * * ?")
    private void refreshCandidateCate3Product(){
        candidateCate3ProductCache.refreshCache();
    }


    /**
     * 每10分钟刷新一次ucb召回源商品信息
     */
    @Scheduled(cron = "10 5/10 * * * ?")
    private void refreshUcbProduct(){
        ucbProductCache.refresh();
    }

    /**
     * 每10分钟刷新相似三级类目信息
     */
    @Scheduled(cron = "20 0/10 * * * ?")
    private void refreshSimilarCate3Id(){
        similarCategory3IdCache.refresh();
    }


    /**
     * 每10分钟刷新一次基础流量召回源候选商品
     */
    @Scheduled(cron = "10 6/10 * * * ?")
    private void refreshBaseSourceProduct(){
        baseSourceProductCache.refresh();
    }
}
