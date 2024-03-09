package com.biyao.moses.featureplatform.service;


import com.biyao.moses.featureplatform.domain.FeatureRequest;
import com.biyao.moses.featureplatform.domain.FeatureResponse;
import com.biyao.moses.featureplatform.domain.ProductFeatureDTO;
import com.biyao.moses.featureplatform.domain.UserFeatureDTO;
import com.biyao.moses.match.ApiResult;

import java.util.List;

/**
 * @program: moses-parent-online
 * @description:
 * @author: zzm
 * @create: 2022-03-22 15:00
 **/
public interface FeatureCenterService {


    /**
     * 根据用户特征查询用户群包
     * @param request
     * @return
     */
    ApiResult<FeatureResponse<List<UserFeatureDTO>>> getUserInfoListByFeatures(FeatureRequest request);


    /**
     * 根据商品特征查询商品ID集合
     * @param request
     * @return
     */
    ApiResult<FeatureResponse<List<ProductFeatureDTO>>> getProductInfoListByFeatures(FeatureRequest request);


}
