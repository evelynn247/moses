package com.biyao.moses.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @program: moses-parent
 * @description: 推荐页面活动配置规则 缓存
 * @author: changxiaowei
 * @create: 2021-02-18 14:44
 **/
@Slf4j
@Component
@EnableScheduling
public class RecommendOperationConfigCache extends  RecommendOperationalConfigCacheCron {

    @PostConstruct
    protected void init() {
        log.info("[启动日志]同步rsm系统推荐运营位配置规则缓存开始");
        super.init();
        log.info("[启动日志]同步rsm系统推荐运营位配置规则缓存结束");
    }
    @Scheduled(cron = "30 0/5 * * * ?")
    protected void refreshProductSexlabelCache() {
        log.info("[定时任务]同步rsm系统推荐运营位配置规则缓存任务开始");
        super.refreshRecommendOperationalConfigCache();
        log.info("[定时任务]同步rsm系统推荐运营位配置规则缓存任务结束");
    }

}
