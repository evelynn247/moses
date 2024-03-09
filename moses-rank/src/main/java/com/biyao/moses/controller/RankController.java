package com.biyao.moses.controller;

import com.biyao.moses.common.constant.ErrorCode;
import com.biyao.moses.model.rank2.RankItem2;
import com.biyao.moses.params.rank.CommonRankRequest;
import com.biyao.moses.params.rank2.RankRequest2;
import com.biyao.moses.params.rank2.RankResponse2;
import com.biyao.moses.rank.impl.CommonRank;
import com.biyao.moses.rank2.service.Rank2;
import com.biyao.moses.rank2.service.impl.DefaultRankImpl;
import com.biyao.moses.util.ApplicationContextProvider;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.biyao.moses.params.ApiResult;
import com.biyao.moses.params.rank.RankResponse;
import com.biyao.moses.params.rank.RecommendRankRequest;
import com.biyao.moses.rank.RecommendRankContext;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;

/**
 *
 * @Description
 * @Date 2018年9月27日
 */

@RestController
@RequestMapping(value = "/recommend")
@Api("rank相关的api")
@Slf4j
public class RankController {

	@Autowired
	RecommendRankContext recommendRankContext;

	@Autowired
	CommonRank commonRank;

	@Autowired
	DefaultRankImpl defaultRank2;

	@BProfiler(key = "com.biyao.moses.controller.RankController.rank",
			monitorType = {MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR})
	@ApiOperation(value = "排序")
	@PostMapping("/rank")
	public ApiResult<RankResponse> rank(@RequestBody RecommendRankRequest recommendRankRequest) {
		long start = System.currentTimeMillis();
		ApiResult<RankResponse> rank = recommendRankContext.rank(recommendRankRequest);
		long end = System.currentTimeMillis();
		log.info("[推荐rank]-[总耗时]-[dataId={},耗时={}]", recommendRankRequest.getRandDataId(), end - start);

		return rank;
	}

	@BProfiler(key = "com.biyao.moses.controller.RankController.commonRank",
			monitorType = {MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR})
	@ApiOperation(value = "通用排序")
	@PostMapping("/commonrank")
	public ApiResult<List<Long>> commonRank(@RequestBody CommonRankRequest commonRankRequest){
		long start = System.currentTimeMillis();
		ApiResult<List<Long>> rankResponse = new ApiResult<>();
		List<Long> rankPids = commonRank.recommendPids(commonRankRequest);
		rankResponse.setSuccess(ErrorCode.SUCCESS_CODE);
		rankResponse.setData(rankPids);
		long end = System.currentTimeMillis();
		log.info("[推荐commonRank]-[总耗时]-[耗时={}]", end - start);

		return rankResponse;
	}

	@BProfiler(key = "com.biyao.moses.controller.RankController.productRank",
			monitorType = {MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR})
	@ApiOperation(value = "商品排序")
	@PostMapping("/productrank")
	public ApiResult<RankResponse2> productRank(@RequestBody RankRequest2 request){
		ApiResult<RankResponse2> rankResponse = new ApiResult<>();
		RankResponse2 response = new RankResponse2();
		//参数校验
		boolean checkResult = request.parameterValidate(request);
		if(!checkResult){
			rankResponse.setSuccess(ErrorCode.PARAM_ERROR_CODE);
			rankResponse.setError("productrank 入参错误 uuid:"+request.getUuid()
					+",uid:"+request.getUid()+",rankName:"+request.getRankName());
			return rankResponse;
		}

		rankResponse = recommendRankContext.productRank(request);
		return rankResponse;
	}

}