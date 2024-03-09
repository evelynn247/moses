package com.biyao.moses.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.biyao.moses.params.ApiResult;
import com.biyao.moses.params.match.MatchRequest;
import com.biyao.moses.params.match.MatchResponse;
import com.biyao.moses.service.imp.RecommendMatchImpl;

@FeignClient(value = "moses-match", fallback = RecommendMatchImpl.class)
public interface RecommendMatchApi {
	
	//match
	@PostMapping(value = "/moses/match/executeMatch")
	public ApiResult<MatchResponse> match(@RequestBody MatchRequest matchRequest,@RequestParam("sessionId") String sessionId);
	
}
