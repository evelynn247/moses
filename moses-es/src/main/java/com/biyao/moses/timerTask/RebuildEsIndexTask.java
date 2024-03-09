package com.biyao.moses.timerTask;

import com.biyao.moses.service.ProductEsServiceImpl;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import com.dangdang.ddframe.job.api.ShardingContext;
import com.dangdang.ddframe.job.api.simple.SimpleJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

/**
 * @program: moses-parent-online
 * @description: 重建索引任务
 * @author: changxiaowei
 * @create: 2021-09-17 10:35
 **/
@Slf4j
public class RebuildEsIndexTask implements SimpleJob {

    @Autowired
    ProductEsServiceImpl productEsService;
    @Override
    @BProfiler(key = "RebuildEsIndexTask.execute", monitorType = {MonitorType.TP, MonitorType.HEARTBEAT})
    public void execute(ShardingContext shardingContext) {
        Long startTime = System.currentTimeMillis();
        log.info("[检查报告]重建es索引任务启动，当前时间：" + new Date());
        productEsService.rebulidIndex();
        Long endTime = System.currentTimeMillis();
        log.info("[检查报告]重建es索引任务结束，当前时间：{},耗时：{}ms", new Date(), (endTime - startTime));
    }
}
