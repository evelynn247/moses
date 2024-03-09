package com.biyao.moses.service;

import com.biyao.moses.context.ByUser;
import com.biyao.moses.params.ApiResult;
import com.biyao.moses.params.match.MatchRequest;
import com.biyao.moses.params.match.MatchResponse;
import com.biyao.moses.params.match2.MatchRequest2;
import com.biyao.moses.params.match2.MatchResponse2;
import com.biyao.moses.params.matchOnline.MatchOnlineRequest;

/**
 * 
 * @Description
 * @Date 2018年9月27日
 */
public interface MosesMatchService {

	ApiResult<MatchResponse> match(MatchRequest matchRequest, ByUser byUser);


	ApiResult<MatchResponse2> productMatch(MatchRequest2 request, ByUser byUser);



	ApiResult<MatchResponse2> productMatchOnline(MatchOnlineRequest request, ByUser byUser);
}