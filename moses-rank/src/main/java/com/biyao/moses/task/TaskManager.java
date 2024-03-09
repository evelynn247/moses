package com.biyao.moses.task;

import com.biyao.moses.cache.CtvrProductCache;
import com.biyao.moses.rank2.exp.RankExperimentSpace;
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
    CtvrProductCache ctvrProductCache;

    @Autowired
    RankExperimentSpace rankExperimentSpace;

    /**
     * 每天早晨8点刷新
     */
    @Scheduled(cron = "0 0 8 * * ?")
    private void refreshCtvr(){
        ctvrProductCache.refresh();
    }


    /**
     * 每10分钟刷新一次实验
     */
    @Scheduled(cron = "30 0/10 * * * ?")
    private void refreshRankExperiment(){
        rankExperimentSpace.refresh();
    }
}
