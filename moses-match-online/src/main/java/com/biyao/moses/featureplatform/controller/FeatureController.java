package com.biyao.moses.featureplatform.controller;


import com.alibaba.fastjson.JSON;
import com.biyao.moses.featureplatform.domain.FeatureRequest;
import com.biyao.moses.featureplatform.domain.FeatureResponse;
import com.biyao.moses.featureplatform.domain.ProductFeatureDTO;
import com.biyao.moses.featureplatform.domain.UserFeatureDTO;
import com.biyao.moses.featureplatform.service.FeatureCenterService;
import com.biyao.moses.match.ApiResult;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @program: moses-parent-online
 * @description:特征平台
 * @author: zhangzhimin
 * @create: 2021-09-22 11:02
 **/
@RestController
@Slf4j
@RequestMapping(value = "/feature")
@Api("特征平台相关的接口")
public class FeatureController {

    @Autowired
    FeatureCenterService featureCenterService;


    /**
     * 根据商品特征召回商品信息集
     * @param request
     * @return
     */
    @BProfiler(key = "FeatureController.getProductInfoListByFeatures",
            monitorType = {MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR})
    @ApiOperation(value = "商品特征召回商品信息集")
    @PostMapping("/getProductInfoListByFeatures")
    public ApiResult<FeatureResponse<List<ProductFeatureDTO>>> getProductInfoListByFeatures(@RequestBody FeatureRequest request) {

        int matchNum = 0;
        log.info("[商品特征召回商品信息集]请求入参{}", JSON.toJSONString(request));
        long startTime = System.currentTimeMillis();
        ApiResult<FeatureResponse<List<ProductFeatureDTO>>> apiResult = featureCenterService.getProductInfoListByFeatures(request);
        if(apiResult!= null && apiResult.getData()!=null && apiResult.getData().getCurrentPageNum()>0){
            matchNum = apiResult.getData().getCurrentPageNum();
        }
        log.info("[商品特征召回商品信息集]-[总耗时], 耗时={}ms],召回数量：{}", System.currentTimeMillis()-startTime,matchNum);
        if(System.currentTimeMillis()-startTime >1000){
            log.error("商品特征召回商品信息集超时，超时时间：{}，request:{}",System.currentTimeMillis()-startTime,JSON.toJSONString(request));
        }
        return apiResult;
    }


    /**
     * 用户特征召回用户信息集合
     * @param request
     * @return
     */
    @BProfiler(key = "FeatureController.getUserInfoListByFeatures",
            monitorType = {MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR})
    @ApiOperation(value = "用户特征召回用户信息集合")
    @PostMapping("/getUserInfoListByFeatures")
    public ApiResult<FeatureResponse<List<UserFeatureDTO>>> getUserInfoListByFeatures(@RequestBody FeatureRequest request) {

        int matchNum = 0;
        log.info("[用户特征召回用户信息集合]请求入参{}", JSON.toJSONString(request));
        long startTime = System.currentTimeMillis();
        ApiResult<FeatureResponse<List<UserFeatureDTO>>> apiResult = featureCenterService.getUserInfoListByFeatures(request);
        if(apiResult!= null && apiResult.getData()!=null && apiResult.getData().getCurrentPageNum()>0){
            matchNum = apiResult.getData().getCurrentPageNum();
        }
        log.info("[用户特征召回用户信息集合]-[总耗时], 耗时={}ms],召回数量：{}", System.currentTimeMillis()-startTime,matchNum);
        if(System.currentTimeMillis()-startTime > 1000){
            log.error("商用户特征召回用户信息集合超时，超时时间：{}，request:{}",System.currentTimeMillis()-startTime,JSON.toJSONString(request));
        }
        return apiResult;
    }

}
