package com.biyao.moses.task;

import com.biyao.moses.cache.Category3RebuyCycleCache;
import com.biyao.moses.cache.ProductSeasonCache;
import com.biyao.moses.cache.SimilarCategory3IdCache;
import com.biyao.moses.cache.drools.RuleConfigCache;
import com.biyao.moses.exp.UIExperimentSpace;
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
    UIExperimentSpace uiExperimentSpace;

    @Autowired
    SimilarCategory3IdCache similarCategory3IdCache;

    @Autowired
    Category3RebuyCycleCache category3RebuyCycleCache;

    @Autowired
    ProductSeasonCache productSeasonCache;
    @Autowired
    RuleConfigCache ruleConfigCache;

    /**
     * 每10分钟刷新一次实验配置
     */
    @Scheduled(cron = "12 0/10 * * * ?")
    private void refreshUIExperiment(){
        uiExperimentSpace.refresh();
    }

    /**
     * 每10分钟刷新一次规则配置
     */
    @Scheduled(cron = "30 0/10 * * * ?")
    private void ruleConfigCache(){
        ruleConfigCache.loadRule();
    }

    /**
     * 每10分钟刷新一次相似后台三级类目
     */
    @Scheduled(cron = "20 0/10 * * * ?")
    private void refreshSimilarCate3Id(){
        similarCategory3IdCache.refresh();
    }

    /**
     * 每3小时刷新一次后台三级类目复购周期
     */
    @Scheduled(cron = "5 1 0/3 * * ?")
    private void refreshCate3RebuyCycle(){
        category3RebuyCycleCache.refresh();
    }

    /**
     * 每半小时刷新一次商品季节信息
     */
    @Scheduled(cron = "12 0/30 * * * ?")
    private void refreshProductSeason(){
        productSeasonCache.refreshProductSeasonCache();
    }

}
