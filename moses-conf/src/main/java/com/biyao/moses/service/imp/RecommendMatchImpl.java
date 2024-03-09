package com.biyao.moses.service.imp;

import org.springframework.stereotype.Component;

import com.biyao.moses.common.constant.ErrorCode;
import com.biyao.moses.params.ApiResult;
import com.biyao.moses.params.match.MatchRequest;
import com.biyao.moses.params.match.MatchResponse;
import com.biyao.moses.service.RecommendMatchApi;

@Component
public class RecommendMatchImpl implements RecommendMatchApi {

	@Override
	public ApiResult<MatchResponse> match(MatchRequest matchRequest, String sessionId) {
		ApiResult<MatchResponse> apiResult = new ApiResult<MatchResponse>();
		apiResult.setSuccess(ErrorCode.SYSTEM_ERROR_CODE);
		apiResult.setError("match系统繁忙，调用失败!");
		return apiResult;
	}

}
