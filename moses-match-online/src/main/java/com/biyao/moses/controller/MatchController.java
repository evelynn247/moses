package com.biyao.moses.controller;

import com.alibaba.fastjson.JSON;
import com.biyao.moses.match.ApiResult;
import com.biyao.moses.match.MatchOnlineRequest;
import com.biyao.moses.match.MatchResponse2;
import com.biyao.moses.service.RecommendMatchContext;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @program: moses-parent-online
 * @description:
 * @author: changxiaowei
 * @create: 2021-09-22 11:02
 **/
@RestController
@Slf4j
@RequestMapping(value = "/recommend")
@Api("matchonline相关的api")
public class MatchController {

    @Autowired
    RecommendMatchContext recommendMatchContext;
    /**
     * 推荐商品在线召回接口
     * @param request
     * @return
     */
    @BProfiler(key = "MatchController.productMatchOnline",
            monitorType = {MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR})
    @ApiOperation(value = "推荐在线召回")
    @PostMapping("/productMatchOnline")
    public ApiResult<MatchResponse2> productMatchOnline(@RequestBody MatchOnlineRequest request) {
        int matchNum = 0;
        log.info("[推荐matchOnline]请求入参{}", JSON.toJSONString(request));
        long startTime = System.currentTimeMillis();
        ApiResult<MatchResponse2> apiResult = recommendMatchContext.productMatch(request);
        if(apiResult!= null && apiResult.getData()!=null && !CollectionUtils.isEmpty(apiResult.getData().getMatchItemList())){
            matchNum = apiResult.getData().getMatchItemList().size();
        }
        log.info("[推荐matchOnline]-[总耗时]-sid={}, 耗时={}ms],召回数量：{}", request.getSid(), System.currentTimeMillis()-startTime,matchNum);
        if(System.currentTimeMillis()-startTime >500){
            log.error("match召回超时，超时时间：{}，request:{}",System.currentTimeMillis()-startTime,JSON.toJSONString(request));
        }
        return apiResult;
    }
}