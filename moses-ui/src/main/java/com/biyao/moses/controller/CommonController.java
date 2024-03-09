package com.biyao.moses.controller;

import com.alibaba.fastjson.JSON;
import com.biyao.moses.common.constant.ErrorCode;
import com.biyao.moses.context.ByUser;
import com.biyao.moses.context.UserContext;
import com.biyao.moses.params.ApiResult;
import com.biyao.moses.params.CommonResponse;
import com.biyao.moses.params.UICommonRequest;
import com.biyao.moses.service.imp.CommonService;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @ClassName CommonController
 * @Description 通用Controller
 * @Author xiaojiankai
 * @Date 2019/8/5 20:28
 * @Version 1.0
 **/
@RestController
@RequestMapping(value = "/recommend/common")
@Api("moses通用处理相关的api")
@Slf4j
public class CommonController {

    private static int pidMaxCount = 600;

    @Autowired
    CommonService commonService;

    @BProfiler(key = "com.biyao.moses.controller.CommonController.rank", monitorType = {MonitorType.TP,
            MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR})
    @ApiOperation(value = "通用rank方法")
    @PostMapping("/rank")
    public ApiResult<CommonResponse> rank(@ApiParam UICommonRequest uiCommonRequest){

        ByUser user = UserContext.getUser();
        BeanUtils.copyProperties(user,uiCommonRequest);
//        log.error("通用rank方法入参：head {}, request {}", JSON.toJSONString(user), JSON.toJSONString(uiCommonRequest));
        long start = System.currentTimeMillis();
        ApiResult<CommonResponse> result = new ApiResult<>();
        //校验参数
        if(!uiCommonRequest.validate()){
            result.setSuccess(ErrorCode.PARAM_ERROR_CODE);
            result.setError("入参校验失败！");
            log.error("[严重异常]入参校验失败！, head {}, request {}", JSON.toJSONString(user), JSON.toJSONString(uiCommonRequest));
            return result;
        }
        //解析入参pids：格式为pid,pid,pid...
        String pids = uiCommonRequest.getPids();
        List<Long> pidList = new ArrayList<>();
        if(StringUtils.isNotBlank(pids)) {
            try {
                pidList = Arrays.stream(uiCommonRequest.getPids().split(","))
                        .filter(p -> StringUtils.isNotBlank(p))
                        .map(p -> Long.valueOf(p))
                        .collect(Collectors.toList());
            } catch (Exception e) {
                log.error("[严重异常]pids入参校验失败！，e {}, head {}, request {}", JSON.toJSONString(e), JSON.toJSONString(user), JSON.toJSONString(uiCommonRequest));
                result.setSuccess(ErrorCode.PARAM_ERROR_CODE);
                result.setError("pids入参校验失败！");
                return result;
            }
            //当传入的pid总数超过600个时，只取前600个进行排序
            if(pidList.size() > pidMaxCount){
                pidList = pidList.subList(0, pidMaxCount);
            }
        }

        try {
            CommonResponse commonResponse = commonService.rank(uiCommonRequest, pidList);
            result.setData(commonResponse);
            result.setSuccess(ErrorCode.SUCCESS_CODE);
        }catch(Exception e){
            result.setSuccess(ErrorCode.SYSTEM_ERROR_CODE);
            result.setError("系统未知错误！");
            log.error("[严重异常]common rank排序异常,{}", JSON.toJSONString(e));
        }finally {
            UserContext.manulClose();
        }
//        log.error("最终返回的结果 {}, uuid={}", JSON.toJSONString(result), uiCommonRequest.getUuid());
//        log.info("[commonrank]-[总耗时]-[tagId={},耗时={}, uuid={}]", uiCommonRequest.getTagId(), System.currentTimeMillis() - start, uiCommonRequest.getUuid());
        return result;
    }
}
