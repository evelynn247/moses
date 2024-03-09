package com.biyao.moses.service.imp;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.biyao.moses.common.constant.ErrorCode;
import com.biyao.moses.common.enums.ExceptionTypeEnum;
import com.biyao.moses.context.ByUser;
import com.biyao.moses.params.ApiResult;
import com.biyao.moses.params.match.MatchRequest;
import com.biyao.moses.params.match.MatchResponse;
import com.biyao.moses.params.match2.MatchRequest2;
import com.biyao.moses.params.match2.MatchResponse2;
import com.biyao.moses.params.matchOnline.MatchOnlineRequest;
import com.biyao.moses.service.MosesMatchService;
import com.biyao.moses.util.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class HttpMosesMatchServiceImpl implements MosesMatchService {

  private final static String matchUrl = "http://mosesmatch.biyao.com/recommend/match";
  //private final static String matchUrl = "http://localhost:8082/recommend/match";

  private final static String PRODUCT_MATCH_URL = "http://mosesmatch.biyao.com/recommend/productmatch";
//  private final static String PRODUCT_MATCH_URL = "http://localhost:8082/recommend/productmatch";

	private final static String PRODUCT_MATCH_ONLINE_URL = "http://mosesmatchonline.biyao.com/recommend/productMatchOnline";
//	private final static String PRODUCT_MATCH_ONLINE_URL = "http://localhost:8082/recommend/productMatchOnline";
	@Override
	public ApiResult<MatchResponse> match(MatchRequest matchRequest, ByUser user) {

		ApiResult<MatchResponse> parseObject = new ApiResult<>();
		parseObject.setSuccess(ErrorCode.SYSTEM_ERROR_CODE);
		try {
			String jsonString = JSONObject.toJSONString(matchRequest);
			String sendPostJSON = HttpClientUtil.sendPostJSONGZIP(matchUrl, null, jsonString, 2000);
			parseObject = JSONObject.parseObject(sendPostJSON, new TypeReference<ApiResult<MatchResponse>>() {
			});
		} catch (Exception e) {
			log.error("[严重异常][match]请求match失败， matchRequest {}，", JSON.toJSONString(matchRequest), e);
			user.getExceptionTypeMap().put(ExceptionTypeEnum.OLD_MATCH_EXCEPTION.getId(), ExceptionTypeEnum.OLD_MATCH_EXCEPTION);
		}
		return parseObject;

	}

	/**
	 * 新版商品match
	 * @param request
	 * @return
	 */
	@Override
	public ApiResult<MatchResponse2> productMatch(MatchRequest2 request, ByUser user) {
		ApiResult<MatchResponse2> parseObject = new ApiResult<>();
		parseObject.setSuccess(ErrorCode.SYSTEM_ERROR_CODE);
		try {
			String jsonString = JSONObject.toJSONString(request);
			String sendPostJSON = HttpClientUtil.sendPostJSONGZIP(PRODUCT_MATCH_URL, null, jsonString, 2000);
			parseObject = JSONObject.parseObject(sendPostJSON, new TypeReference<ApiResult<MatchResponse2>>() {
			});
		} catch (Exception e) {
			log.error("[严重异常][新match]请求productmatch失败， request = {}， ", JSON.toJSONString(request), e);
			user.getExceptionTypeMap().put(ExceptionTypeEnum.NEW_MATCH_EXCEPTION.getId(), ExceptionTypeEnum.NEW_MATCH_EXCEPTION);
		}
		return parseObject;
	}

	@Override
	public ApiResult<MatchResponse2> productMatchOnline(MatchOnlineRequest request, ByUser byUser) {
		ApiResult<MatchResponse2> parseObject = new ApiResult<>();
		parseObject.setSuccess(ErrorCode.SYSTEM_ERROR_CODE);
		long start = System.currentTimeMillis();
		try {
			String jsonString = JSONObject.toJSONString(request);
			String sendPostJSON = HttpClientUtil.sendPostJSONGZIP(PRODUCT_MATCH_ONLINE_URL, null, jsonString, 2000);
			parseObject = JSONObject.parseObject(sendPostJSON, new TypeReference<ApiResult<MatchResponse2>>() {
			});
		} catch (Exception e) {
			log.error("[严重异常][matchOnline]请求在线召回接口失败， request = {}， ", JSON.toJSONString(request), e);
			byUser.getExceptionTypeMap().put(ExceptionTypeEnum.MATCH_ONLINE_EXCEPTION.getId(), ExceptionTypeEnum.MATCH_ONLINE_EXCEPTION);
		}
		if (request.isDebug()){
			log.info("[检查日志]在线召回耗时:{},sid:{}", System.currentTimeMillis()-start, request.getSid());
		}
		return parseObject;
	}
}
