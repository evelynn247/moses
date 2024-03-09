package com.biyao.moses;

import com.alibaba.fastjson.JSONObject;
import com.biyao.moses.config.MatchRedisUtil;
import com.biyao.moses.service.ProductEsServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @program: moses-parent-online
 * @description: 处理异步任务
 * @author: changxiaowei
 * @Date: 2022-02-19 17:09
 **/
@Slf4j

public class SyncTask {

    private final  static int DAY7_SECONDS =7*24*60*60;


//    @Autowired
//    ProductEsServiceImpl productEsService;
//    @Async
//    public void syncTaskUpdateEs(List<Long> pids){
//        productEsService.updateIndexByPids(pids);
//    }


}

