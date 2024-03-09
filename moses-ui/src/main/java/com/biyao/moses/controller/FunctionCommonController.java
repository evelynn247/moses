package com.biyao.moses.controller;

import com.alibaba.fastjson.JSON;
import com.biyao.moses.params.ApiResult;
import com.biyao.moses.params.RecommendAllBodyRequest;
import com.biyao.moses.params.RecommendCommonInfo;
import com.biyao.moses.service.imp.CommonService;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.biyao.moses.common.constant.ErrorCode.PARAM_ERROR_CODE;
import static com.biyao.moses.common.constant.ErrorCode.SYSTEM_ERROR_CODE;

/**
 * @program: moses-parent
 * @description:
 * @author: changxiaowei
 * @Date: 2022-02-21 16:55
 **/

@RestController
@RequestMapping(value = "/recommend/funccommon")
@Api("moses通用处理相关的api")
@Slf4j
public class FunctionCommonController {
    @Autowired
    CommonService commonService;

    /**
     * @return com.biyao.moses.params.ApiResult<java.util.List < com.biyao.moses.params.RecommendCommonInfo>>
     * @Des 根据商品id获取替换的视频id
     * @Param [RecommendAllBodyRequest]
     * @Author changxiaowei
     * @Date 2022/2/14
     */
    @BProfiler(key = "com.biyao.moses.controller.FunctionCommonController.getVideoIdByProductIds", monitorType = {MonitorType.TP,
            MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR})
    @ApiOperation(value = "根据商品id获取替换的视频id")
    @PostMapping("/getVideoIdByProductIds")
    public ApiResult<List<RecommendCommonInfo>> getVideoIdByProductIds(@ApiParam @RequestBody RecommendAllBodyRequest bodyRequest) {
        ApiResult<List<RecommendCommonInfo>> result = new ApiResult<>();
        if (!bodyRequest.isValidForGetVid()) {
            result.setSuccess(PARAM_ERROR_CODE);
            result.setError("请求入参错误");
            return result;
        }
        try {
            List<RecommendCommonInfo> resultList = commonService.getVideoIdByRule(bodyRequest);
            result.setData(resultList);
        }catch (Exception e){
            result.setSuccess(SYSTEM_ERROR_CODE);
            result.setError("系统异常");
            log.error("[严重异常]根据商品id获取替换的视频id时出现异常， request {}", JSON.toJSONString(bodyRequest), e);
        }
        return result;
    }


    @BProfiler(key = "com.biyao.moses.controller.FunctionCommonController.getScoreByVideoId", monitorType = {MonitorType.TP,
            MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR})
    @ApiOperation(value = "根据视频id查询算法视频分")
    @PostMapping("/getScoreByVideoId")
    public ApiResult<List<RecommendCommonInfo>> getVideoScoreByVideoIds(@ApiParam @RequestBody RecommendAllBodyRequest bodyRequest) {
        log.info("getAllRecommendInfoMap bodyRequest:{}", JSON.toJSONString(bodyRequest));
        ApiResult<List<RecommendCommonInfo>> apiResult = new ApiResult<>();
        if (!bodyRequest.isValidForGetVidScore()) {
            apiResult.setSuccess(PARAM_ERROR_CODE);
            apiResult.setError("参数异常");
            return apiResult;
        }
        List<Long> videoIds = bodyRequest.getVideoIds();//获取视频ids
        String caller = bodyRequest.getCaller();//获取服务调用方caller
        List<RecommendCommonInfo> scoreByVideoId = commonService.getVideoScoreByVideoIds(videoIds, caller);
        if (scoreByVideoId == null) {
            apiResult.setSuccess(SYSTEM_ERROR_CODE);
            apiResult.setError("查询算法视频分异常");
            return apiResult;
        }
        apiResult.setData(scoreByVideoId);
        return apiResult;
    }
}
