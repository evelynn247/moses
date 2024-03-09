package com.biyao.moses.service;

import com.biyao.moses.params.rank2.RankRequest2;
import com.biyao.moses.params.rank2.RankResponse2;
import org.springframework.web.bind.annotation.RequestBody;

import com.biyao.moses.params.ApiResult;
import com.biyao.moses.params.rank.RankResponse;
import com.biyao.moses.params.rank.RecommendRankRequest;

/**
 * fegin
 * 
 * @Description
 * @Date 2018年9月27日
 */
//@FeignClient(value = "moses-rank")
@Deprecated
public interface MosesRankService {

	/**
	 * 调mosesrank服务的 rank接口
	 * @param recommendRankRequest
	 * @return
	 */
	ApiResult<RankResponse> rank(@RequestBody RecommendRankRequest recommendRankRequest);

	/**
	 * 商品排序
	 * @param request
	 * @return
	 */
	ApiResult<RankResponse2> productRank(RankRequest2 request);
}