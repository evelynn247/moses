package com.biyao.moses.service;

import com.biyao.moses.model.drools.BuildBaseFactParam;
import com.biyao.moses.model.drools.RuleBaseFact;
import com.biyao.moses.model.drools.RuleFact;
import com.biyao.moses.params.*;

public interface IFunctionService {

    /**
     * 获取推荐商品的全量pid集合信息
     * @param request
     * @param bodyRequest
     * @return
     */
    RecommendPidsResponse getRecommendPids(RecommendAllRequest request, RecommendAllBodyRequest bodyRequest);

    /**
     * 获取全量推荐商品信息
     * @param request
     * @param bodyRequest
     * @return
     */
    RecommendAllResponse getAllRecommendInfo(RecommendAllRequest request, RecommendAllBodyRequest bodyRequest);


    /**
     * 根据好友uid获取好友已购买的商品信息
     * @param request
     * @return
     */
    RecommendInfoMapResponse getAllRecommendInfoMap(RecommendAllRequest request,RecommendAllBodyRequest bodyRequest);



    RuleFact getRuleFact(RecommendAllRequest request);

}
