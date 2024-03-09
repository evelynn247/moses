package com.biyao.moses.Task;

import com.biyao.moses.cache.SceneRuleRelationCache;
import com.biyao.moses.tensorflowModel.TensorflowModelCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @program: moses-parent-online
 * @description: 管理定时任务
 * @author: changxiaowei
 * @create: 2021-09-22 10:37
 **/
@Component
@EnableScheduling
public class TaskManager {

    @Autowired
    TensorflowModelCache tensorflowModelCache;
    @Autowired
    SceneRuleRelationCache sceneRuleRelationCache;
    /**
     * 加载模型文件 每天一次
     */
    @Scheduled(cron = "0 0 6,18 * * ?")
    public void loadTensorflowMode() {
        tensorflowModelCache.loadTensorflowModel();
    }

    /**
     * 加载场景规则关系 10分钟
     */
    @Scheduled(cron = "30 0/10 * * * ?")
    public void refreshSceneRuleRealtion() {
        sceneRuleRelationCache.refreshRuleQueryBuilderCache();
    }
}
