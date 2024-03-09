package com.biyao.moses.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @ClassName SearchProductLabelNoCron
 * @Description 缓存商品便签配置
 * @Author xiaojiankai
 * @Date 2019/8/16 15:07
 * @Version 1.0
 **/
@Slf4j
@Component
@EnableScheduling
public class SearchProductLabelCache extends SearchProductLabelNoCron{
    /**
     * 初始化缓存
     */
    @PostConstruct
    protected void init(){
        super.init();
    }

    /**
     * 每隔一小时刷新一次
     */
    @Scheduled(cron = "0 0 0/1 * * ?")
    @Override
    protected void refreshSearchProductLabel(){
        super.refreshSearchProductLabel();
    }

}
