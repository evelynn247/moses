package com.biyao.moses.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @program: moses-parent
 * @description: 人工召回源缓存
 * @author: changxiaowei
 * @create: 2021-04-08 11:27
 **/
@Slf4j
@Component
@EnableScheduling
public class RecommendManualSourceConfigCache extends RecommendManualSourceConfigCacheCron {
    @PostConstruct
    protected void init() {
        log.info("[启动日志]同步rsm系统推荐人工配置召回源数据源缓存开始");
        super.init();
        log.info("[启动日志]同步rsm系统推荐人工配置召回源数据源缓存结束");
    }
    @Scheduled(cron = "0 0/5 * * * ? ")
    protected void refreshAllManualSourceConfigCache() {
        log.info("[定时任务]同步rsm系统推荐人工配置召回源数据源缓存任务开始");
        super.refreshAllManualSourceConfigCache();
        log.info("[定时任务]同步rsm系统推荐人工配置召回源数据源缓存任务结束");
    }
}
