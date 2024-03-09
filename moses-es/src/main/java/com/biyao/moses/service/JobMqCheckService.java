package com.biyao.moses.service;

import com.biyao.moses.mq.MqComparisonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @program: moses-parent-online
 * @description: Mq对账任务
 * @author: changxiaowei
 * @Date: 2021-12-06 15:07
 **/
@Component
public class JobMqCheckService {

    @Autowired
    MqComparisonService mqComparisonService;

    /**
     * 商品基本信息对账
     */
    public void productBaseInfoMqDiff() {
   // mqComparisonService.comparison();
    }
}
