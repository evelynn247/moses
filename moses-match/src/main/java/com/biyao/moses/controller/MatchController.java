package com.biyao.moses.controller;

import com.alibaba.fastjson.JSON;
import com.biyao.moses.common.constant.ErrorCode;
import com.biyao.moses.model.match.MatchItem;
import com.biyao.moses.params.ApiResult;
import com.biyao.moses.params.match.MatchRequest;
import com.biyao.moses.params.match.MatchResponse;
import com.biyao.moses.params.match.ProductMatchRequest;
import com.biyao.moses.params.match2.MatchResponse2;
import com.biyao.moses.params.match2.MatchRequest2;
import com.biyao.moses.rpc.UcRpcService;
import com.biyao.moses.service.RecommendMatchContext;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@Slf4j
@RequestMapping(value = "/recommend")
@Api("match相关的api")
public class MatchController {
	@Autowired
	RecommendMatchContext recommendMatchContext;
	@Autowired
	UcRpcService ucRpcService;

	@BProfiler(key = "com.biyao.moses.controller.MatchController.match",
			monitorType = {MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR})
	@ApiOperation(value = "召回")
	@PostMapping("/match")
	public ApiResult<MatchResponse> match(@RequestBody MatchRequest matchRequest) {
		
		long start = System.currentTimeMillis();

		ApiResult<MatchResponse> match = recommendMatchContext.match(matchRequest);
		long end = System.currentTimeMillis();

		log.info("[推荐match]-[总耗时]-[dataSource={},耗时={}]", matchRequest.getDataSourceType(), end - start);

		return match;
	}

	@BProfiler(key = "com.biyao.moses.controller.MatchController.productFeed",
			monitorType = {MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR})
	@ApiOperation(value = "商品召回")
	@PostMapping("/productfeed")
	public ApiResult<List<MatchItem>> productFeed(@RequestBody ProductMatchRequest request) {
		log.info("商品召回productFeed请求：request={}", JSON.toJSONString(request));
		ApiResult<List<MatchItem>> apiResult = new ApiResult<>();
		if (!request.validate()){
			apiResult.setSuccess(ErrorCode.PARAM_ERROR_CODE);
			apiResult.setError("参数错误");
			return apiResult;
		}
		request.preHandler();
		List<MatchItem> result = new ArrayList<>();
		try {
			long start = System.currentTimeMillis();
			result = recommendMatchContext.productFeedMatch(request);
			apiResult.setData(result);
			long end = System.currentTimeMillis();
			log.info("[推荐productFeed]-[总耗时]-[dataSource={},耗时={}ms]", request.getDataSourceType(), end - start);
		}catch (Exception e){
			log.error("[严重异常]productFeed商品召回失败：request={}", JSON.toJSONString(request), e);
		}

		return apiResult;
	}

	/**
	 * 推荐商品召回接口
	 * @param request
	 * @return
	 */
	@BProfiler(key = "com.biyao.moses.controller.MatchController.productFeedMatch",
			monitorType = {MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR})
	@ApiOperation(value = "新版商品召回")
	@PostMapping("/productmatch")
	public ApiResult<MatchResponse2> productMatch(@RequestBody MatchRequest2 request) {
		ApiResult<MatchResponse2> apiResult = new ApiResult<>();
		long startTime = System.currentTimeMillis();
		boolean valid = request.valid();
		if(!valid){
			apiResult.setSuccess(ErrorCode.PARAM_ERROR_CODE);
			apiResult.setError("入参错误，uuid =  " + request.getUuid()+" sid = "+request.getSid());
			return apiResult;
		}

		apiResult = recommendMatchContext.productMatch(request);

		log.info("[推荐productmatch]-[总耗时]-[uuid={}, sid={}, 耗时={}ms]", request.getUuid(), request.getSid(), System.currentTimeMillis()-startTime);
		return apiResult;
	}
}
