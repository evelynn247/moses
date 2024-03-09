package com.biyao.moses.timerTask;

import com.biyao.moses.service.JobMqCheckService;
import com.dangdang.ddframe.job.api.ShardingContext;
import com.dangdang.ddframe.job.api.simple.SimpleJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @program: moses-parent-online
 * @description: 商品基本信息mq对账服务
 * @author: changxiaowei
 * @Date: 2021-12-06 15:09
 **/
@Slf4j
public class ProductBaseInfoMqCheckJob implements SimpleJob {

    @Autowired
    JobMqCheckService jobMqCheckService;
    @Override
    public void execute(ShardingContext shardingContext) {
        jobMqCheckService.productBaseInfoMqDiff();
    }
}
