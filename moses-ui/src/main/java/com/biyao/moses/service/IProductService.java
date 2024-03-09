package com.biyao.moses.service;

import com.biyao.moses.context.ByUser;
import com.biyao.moses.model.SearchProduct;
import com.biyao.moses.model.template.entity.FirstCategory;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.params.ApiResult;
import com.biyao.moses.params.NewUserCategoryRequest;
import com.biyao.moses.params.RecommendNewuserRequest;
import com.biyao.moses.params.UIBaseRequest;

import java.util.List;

/**
 * IProductService
 * 
 * @Description
 * @Date 2018年9月27日
 */
public interface IProductService {

	SearchProduct selectByPrimaryKey(Integer productId);
    
	/**
    * @Description 判断是否是新手专享商品 
    * @param recommendNewuserRequest
    * @return ApiResult<String> 
    * @version V1.0
    * @auth 邹立强 (zouliqiang@idstaff.com)
     */
	ApiResult<String> isNewuserProduct(RecommendNewuserRequest recommendNewuserRequest);

	/**
	 * 获取新手专享前台一级类目集合
	 * @param apiResult
	 * @param newUserCategoryRequest
	 * @return
	 */
	ApiResult<List<FirstCategory>> getFirstCategoryDuplicate(ApiResult<List<FirstCategory>> apiResult, NewUserCategoryRequest newUserCategoryRequest);

	/**
	 * 将商品转化为商品组
	 * @param totalTemplateInfoList
	 * @param uiBaseRequest
	 * @param user
	 * @param feedIndex
	 */
	 void converProductToProductGroup(List<TotalTemplateInfo> totalTemplateInfoList, UIBaseRequest uiBaseRequest, ByUser user, Integer feedIndex);
	/**
	 * 将商品转化为视频
	 * @param totalTemplateInfoList
	 * @param uiBaseRequest
	 * @param user
	 * @param feedIndex
	 */
	void converProductToVideo(List<TotalTemplateInfo> totalTemplateInfoList, UIBaseRequest uiBaseRequest, ByUser user, Integer feedIndex);
}
