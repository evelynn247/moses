package com.biyao.moses.cache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;


@Slf4j
@Component
@EnableScheduling
public class SwitchConfigCache extends SwitchConfigCacheCron{

    //3分钟刷新一次
    @Scheduled(cron = "0 0/3 * * * ?")
    @PostConstruct
    protected void refresh() {
        log.info("[检查日志]同步rsm系统推荐配置缓存任务开始");
        super.init();
        log.info("[检查日志]同步rsm系统推荐配置缓存任务结束");
    }
}
