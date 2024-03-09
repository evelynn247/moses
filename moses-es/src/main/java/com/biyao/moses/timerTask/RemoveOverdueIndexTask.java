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
 * @description:
 * @author: changxiaowei
 * @create: 2021-09-17 13:27
 **/
@Slf4j
public class RemoveOverdueIndexTask implements SimpleJob {
    @Autowired
    ProductEsServiceImpl productEsTaskService;
    @BProfiler(key = "RemoveIndexTask.execute", monitorType = {MonitorType.TP, MonitorType.HEARTBEAT})
    @Override
    public void execute(ShardingContext shardingContext) {
        Long startTime = System.currentTimeMillis();
        log.info("[检查报告]定时删除索引任务启动，当前时间：" + new Date());
        productEsTaskService.removeIndexTimer();
        Long endTime = System.currentTimeMillis();
        log.info("[检查报告]定时删除索引任务结束，当前时间：{},耗时：{}ms", new Date(), (endTime - startTime));
    }
}
