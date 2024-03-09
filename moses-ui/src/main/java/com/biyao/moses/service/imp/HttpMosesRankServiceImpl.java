package com.biyao.moses.service.imp;

import com.biyao.moses.params.rank2.RankRequest2;
import com.biyao.moses.params.rank2.RankResponse2;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.biyao.moses.common.constant.ErrorCode;
import com.biyao.moses.params.ApiResult;
import com.biyao.moses.params.rank.RankResponse;
import com.biyao.moses.params.rank.RecommendRankRequest;
import com.biyao.moses.util.HttpClientUtil;

/**
 * HttpMosesConfServiceImpl
 * 
 * @Description
 * @Date 2018年9月27日
 */
@Service
@Slf4j
public class HttpMosesRankServiceImpl {
	/**
	 * mosesrank rank接口
	 */
	private static String rankUrl = "http://mosesrank.biyao.com/recommend/rank";
	//private static String rankUrl = "http://localhost:8082/recommend/rank";

	private final static String PRODUCT_RANK_URL = "http://mosesrank.biyao.com/recommend/productrank";
	//private final static String PRODUCT_RANK_URL = "http://localhost:8082/recommend/productrank";

	public ApiResult<RankResponse> rank(RecommendRankRequest recommendRankRequest) {
		ApiResult<RankResponse> parseObject = new ApiResult<>();
		parseObject.setSuccess(ErrorCode.SYSTEM_ERROR_CODE);
		try {
			String jsonString = JSONObject.toJSONString(recommendRankRequest);
			String sendPostJSON = HttpClientUtil.sendPostJSONGZIP(rankUrl, null, jsonString, 2000);
			parseObject = JSONObject.parseObject(sendPostJSON,new TypeReference<ApiResult<RankResponse>>() {});
		} catch (Exception e) {
			log.error("[严重异常][rank]请求rank出错",e);
		}
		return parseObject;
	}

	/**
	 * 商品排序
	 * @param request
	 * @return
	 */
	public ApiResult<RankResponse2> rank(RankRequest2 request) {
		ApiResult<RankResponse2> parseObject = new ApiResult<>();
		parseObject.setSuccess(ErrorCode.SYSTEM_ERROR_CODE);
		try {
			String jsonString = JSONObject.toJSONString(request);
			String sendPostJSON = HttpClientUtil.sendPostJSONGZIP(PRODUCT_RANK_URL, null, jsonString, 2000);
			parseObject = JSONObject.parseObject(sendPostJSON,new TypeReference<ApiResult<RankResponse2>>() {});
		} catch (Exception e) {
			log.error("[严重异常][新rank]请求product rank出错",e);
		}
		return parseObject;
	}

}