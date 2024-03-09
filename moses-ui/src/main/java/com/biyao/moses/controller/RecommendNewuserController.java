package com.biyao.moses.controller;

import com.biyao.moses.common.constant.ErrorCode;
import com.biyao.moses.model.template.entity.FirstCategory;
import com.biyao.moses.params.ApiResult;
import com.biyao.moses.params.NewUserCategoryRequest;
import com.biyao.moses.params.RecommendNewuserRequest;
import com.biyao.moses.service.IProductService;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author 邹立强 (zouliqiang@idstaff.com)
 * <p>Copyright (c) Department of Research and Development/Beijing.</p>
 * @version V1.0
 * @Description 新手专享
 * @date 2019年5月6日下午2:56:48
 */
@RestController
@RequestMapping(value = "/recommend/newuser")
@Api("RecommendNewuserController相关的api")
@Slf4j
public class RecommendNewuserController {

    @Autowired
    private IProductService iProductService;

    @BProfiler(key = "com.biyao.moses.controller.RecommendNewuserController.isNewuserProduct", monitorType = {MonitorType.TP,
            MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR})
    @ApiOperation(value = "判断商品是否是新手专享商品")
    @PostMapping("/isNewuserProduct")
    public ApiResult<String> isNewuserProduct(@RequestBody @ApiParam RecommendNewuserRequest recommendNewuserRequest) {
        return iProductService.isNewuserProduct(recommendNewuserRequest);
    }

    @BProfiler(key = "com.biyao.moses.controller.RecommendNewuserController.queryFirstCategory", monitorType = {MonitorType.TP,
            MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR})
    @ApiOperation(value = "获取新手专享前台一级类目")
    @PostMapping("/queryFirstCategory")
    public ApiResult<List<FirstCategory>> queryFirstCategory(@ApiParam NewUserCategoryRequest newUserCategoryRequest,@RequestHeader  String siteId) {
        ApiResult<List<FirstCategory>> apiResult = new ApiResult<>();
        //两参数任一为空，则不认为是新手专享页面请求
        if (StringUtils.isEmpty(newUserCategoryRequest.getPageId()) || StringUtils.isEmpty(newUserCategoryRequest.getTopicId()) ) {
            apiResult.setError("params error");
            apiResult.setSuccess(ErrorCode.PARAM_ERROR_CODE);
            return apiResult;
        } else {
            newUserCategoryRequest.setSiteId(siteId);
            ApiResult<List<FirstCategory>> rtResult = iProductService.getFirstCategoryDuplicate(apiResult, newUserCategoryRequest);
            return rtResult;
        }
    }
}