package com.biyao.moses.controller;

import com.alibaba.fastjson.JSON;
import com.biyao.mac.client.redbag.shop.privilegebag.dto.ShowPrivilegeLogoResultDto;
import com.biyao.moses.cache.RedisCache;
import com.biyao.moses.cache.SwitchConfigCache;
import com.biyao.moses.common.constant.ErrorCode;
import com.biyao.moses.constants.CommonConstants;
import com.biyao.moses.constants.MosesBizConfigEnum;
import com.biyao.moses.params.*;
import com.biyao.moses.service.IFunctionService;
import com.biyao.moses.util.IdCalculateUtil;
import com.biyao.moses.util.PlatformEnumUtil;
import com.biyao.moses.util.ProductDetailUtil;
import com.biyao.moses.util.RedisUtil;
import com.biyao.search.common.enums.PlatformEnum;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * 
 * @Description
 * @Date 2018年9月27日
 */
@RestController
@RequestMapping(value = "/recommend/function")
@Api("FunctionUiController相关的api")
@Slf4j
public class FunctionController {

	@Autowired
	ProductDetailUtil productDetailUtil;

	@Autowired
	RedisUtil redisUtil;

	@Autowired
	private IFunctionService functionServiceImpl;

	@Autowired
	SwitchConfigCache switchConfigCache;
	@Autowired
	RedisCache redisCache;

	@BProfiler(key = "FunctionController.getNewUserByUuid", monitorType = { MonitorType.TP,
			MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR })
	@ApiOperation(value = "未登录时，通过UUID判断新老客状态")
	@PostMapping("/getNewUserByUuid")
	public ApiResult<NotLoginUuidResponse> getNewUserByUuid(@ApiParam NotLoginUuidRequest notLoginUuidRequest) {
		ApiResult<NotLoginUuidResponse> apiResult = new ApiResult<NotLoginUuidResponse>();
		NotLoginUuidResponse response = new NotLoginUuidResponse();
		apiResult.setSuccess(ErrorCode.SUCCESS_CODE);
		// 用户新老客状态 true为新客，false为老客
		boolean isNew = true;
		try {
			String uuid = notLoginUuidRequest.getUuid();
			String siteId = notLoginUuidRequest.getSiteId();
			//参数校验
			if (StringUtils.isBlank(uuid)||StringUtils.isBlank(siteId) || !com.biyao.moses.common.constant.CommonConstants.SITEID.contains(siteId.trim())){
				apiResult.setSuccess(ErrorCode.PARAM_ERROR_CODE);
				response.setNew(isNew);
				apiResult.setData(response);
				return apiResult;
			}
			PlatformEnum platformEnum = PlatformEnumUtil.getPlatformEnumBySiteId(Integer.valueOf(siteId));
			//通过uuid查询uid，存在uid，通过mac判断新老客，不存在uid，为新客
			String isExistUid = redisUtil.getString(CommonConstants.UUID_TWO_UID_PREFIX+uuid);
			//如果查询出来的uid不为空，则通过特权金逻辑判断新老客
			if(StringUtils.isNotBlank(isExistUid)){
				ShowPrivilegeLogoResultDto userHasPrivilege = productDetailUtil.isUserHasPrivilege(isExistUid,platformEnum.getName());
				if (userHasPrivilege!=null) {
					Integer userType = userHasPrivilege.getUserType();
					if (userType!=null&&userType==2) {
						isNew = false;
					}
				}
			}
		} catch (Exception e) {
			log.error("[严重异常]未登录时，通过UUID判断新老客状态异常，异常信息:", e);
			apiResult.setSuccess(ErrorCode.SYSTEM_ERROR_CODE);
		}
		response.setNew(isNew);
		apiResult.setData(response);
		return apiResult;
	}

	@BProfiler(key = "FunctionController.getRecommendPids", monitorType = { MonitorType.TP,
			MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR })
	@ApiOperation(value = "获取全量推荐商品的pid信息")
	@PostMapping("/getRecommendPids")
	public ApiResult<RecommendPidsResponse> getRecommendPids(@ApiParam RecommendAllRequest request, @ApiParam @RequestBody(required = false) RecommendAllBodyRequest bodyRequest) {
		log.info("getRecommendPids request {}, bodyRequest {}", JSON.toJSONString(request), JSON.toJSONString(bodyRequest));
		ApiResult<RecommendPidsResponse> apiResult = new ApiResult<>();
		apiResult.setSuccess(ErrorCode.SUCCESS_CODE);
		if(!request.isValid()){
			apiResult.setSuccess(ErrorCode.PARAM_ERROR_CODE);
			apiResult.setError("入参错误，请检查入参");
			log.error("[严重异常]获取全量推荐商品的pid信息时入参格式不对， request {}", JSON.toJSONString(request));
			return apiResult;
		}
		MosesBizConfigEnum bizConfigEnum = MosesBizConfigEnum.getByBizName(request.getBiz());
		if(Objects.isNull(bizConfigEnum) && StringUtils.isEmpty(request.getSceneId())){
			apiResult.setSuccess(ErrorCode.PARAM_ERROR_CODE);
			apiResult.setError("入参错误，请检查入参");
			log.error("[严重异常]获取全量推荐信息时找不到对应的业务，request {}",  JSON.toJSONString(request));
			return apiResult;
		}
		// 如果个性化活动开关为关且该活动无非个性化数据且没有命中规则时 则无数据返回
		if(Objects.nonNull(bizConfigEnum) && Objects.isNull(functionServiceImpl.getRuleFact(request))){
			if(!switchConfigCache.getRecommendSwitchByConfigId(CommonConstants.PERSONALIZE_RECOMMEND_OFF_CONFIG_ID) && Objects.isNull(bizConfigEnum.getImpersonalSourceWeight())){
				apiResult.setSuccess(ErrorCode.SUCCESS_CODE);
				apiResult.setError("无非个性化数据");
				return apiResult;
			}
		}
		try {
			RecommendPidsResponse result = functionServiceImpl.getRecommendPids(request, bodyRequest);
			apiResult.setData(result);
		}catch (Exception e){
			log.error("[严重异常]获取全量推荐商品的pid信息时出现异常， request {}，", JSON.toJSONString(request), e);
			apiResult.setSuccess(ErrorCode.SYSTEM_ERROR_CODE);
			apiResult.setError("系统异常");
		}
		return apiResult;
	}

	@BProfiler(key = "FunctionController.getAllRecommendInfo", monitorType = { MonitorType.TP,
			MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR })
	@ApiOperation(value = "获取全量推荐信息")
	@PostMapping("/getAllRecommend")
	public ApiResult<RecommendAllResponse> getAllRecommendInfo(@ApiParam RecommendAllRequest request, @ApiParam @RequestBody(required = false) RecommendAllBodyRequest bodyRequest) {
		if(StringUtils.isBlank(request.getSid())){  request.setSid(IdCalculateUtil.createUniqueId()); }
		request.setDebug(redisCache.isHomeFeedWhite(request.getUuid()));
		log.info("getAllRecommendInfo request {}, bodyRequest {}", JSON.toJSONString(request), JSON.toJSONString(bodyRequest));
		ApiResult<RecommendAllResponse> apiResult = new ApiResult<>();
		apiResult.setSuccess(ErrorCode.SUCCESS_CODE);
		if(!request.isValid()){
			apiResult.setSuccess(ErrorCode.PARAM_ERROR_CODE);
			apiResult.setError("入参错误，请检查入参");
			log.error("[严重异常]获取全量推荐信息时入参格式不对，request {}",  JSON.toJSONString(request));
			return apiResult;
		}
		MosesBizConfigEnum bizConfigEnum=MosesBizConfigEnum.getByBizName(request.getBiz());
		if(Objects.isNull(bizConfigEnum) && StringUtils.isEmpty(request.getSceneId())){
			apiResult.setSuccess(ErrorCode.PARAM_ERROR_CODE);
			apiResult.setError("入参错误，请检查入参");
			log.error("[严重异常]获取推荐信息时找不到对应的业务，request {}",  JSON.toJSONString(request));
			return apiResult;
		}
		// 如果个性化活动开关为关且该活动无非个性化数据 则无数据返回
		if(Objects.nonNull(bizConfigEnum) && Objects.isNull(functionServiceImpl.getRuleFact(request))){
			if(!switchConfigCache.getRecommendSwitchByConfigId(CommonConstants.PERSONALIZE_RECOMMEND_OFF_CONFIG_ID) && Objects.isNull(bizConfigEnum.getImpersonalSourceWeight())){
				apiResult.setSuccess(ErrorCode.SUCCESS_CODE);
				apiResult.setError("无非个性化数据");
				return apiResult;
			}
		}
		try {
			RecommendAllResponse result = functionServiceImpl.getAllRecommendInfo(request, bodyRequest);
			apiResult.setData(result);
		}catch (Exception e){
			log.error("[严重异常]获取全量推荐信息时出现异常，request {}，",  JSON.toJSONString(request), e);
			apiResult.setSuccess(ErrorCode.SYSTEM_ERROR_CODE);
			apiResult.setError("系统异常");
		}
		return apiResult;
	}


	@BProfiler(key = "FunctionController.getCategoryPids", monitorType = { MonitorType.TP,
			MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR })
	@ApiOperation(value = "获取类目下商品信息")
	@PostMapping("/getCategoryPids")
	public ApiResult<RecommendAllResponse> getCategoryPids(@ApiParam RecommendAllRequest request, @ApiParam @RequestBody(required = false) RecommendAllBodyRequest bodyRequest) {
		ApiResult<RecommendAllResponse> apiResult = new ApiResult<>();
		apiResult.setSuccess(ErrorCode.SUCCESS_CODE);
		if(!request.isValidForCateGory()){
			apiResult.setSuccess(ErrorCode.PARAM_ERROR_CODE);
			apiResult.setError("入参错误，请检查入参");
			log.error("[严重异常]获取类目下商品时参数错误，request {}",  JSON.toJSONString(request));
			return apiResult;
		}
		return getAllRecommendInfo(request,bodyRequest);
	}

	@BProfiler(key = "FunctionController.getAllRecommendInfoMap", monitorType = {MonitorType.TP,
			MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR})
	@ApiOperation(value = "获取全量推荐信息（map）")
	@PostMapping("/getAllRecommendInfoMap")
	public ApiResult<RecommendInfoMapResponse> getAllRecommendInfoMap(@ApiParam RecommendAllRequest request, @ApiParam @RequestBody(required = false) RecommendAllBodyRequest bodyRequest) {
		log.info("getAllRecommendInfoMap request {},bodyRequest:{}", JSON.toJSONString(request), JSON.toJSONString(bodyRequest));
		ApiResult<RecommendInfoMapResponse> apiResult = new ApiResult<>();
		apiResult.setSuccess(ErrorCode.SUCCESS_CODE);
		//获取responseMapKeys 若url中获取不到，则从body中获取
		String responseMapKeys = "";
		if (StringUtils.isNotEmpty(request.getResponseMapKeys())) {
			responseMapKeys = request.getResponseMapKeys();
		} else if (!Objects.isNull(bodyRequest)) {
			responseMapKeys = bodyRequest.getResponseMapKeys();
		}
		request.setResponseMapKeys(responseMapKeys);

		//  参数校验
		if (!(request.isValid() && StringUtils.isNotEmpty(responseMapKeys))) {
			apiResult.setSuccess(ErrorCode.PARAM_ERROR_CODE);
			apiResult.setError("入参错误，请检查入参");
			log.error("[严重异常]获取全量推荐信息（map）时入参格式不对，request {},bodyRequest:{}", JSON.toJSONString(request), JSON.toJSONString(bodyRequest));
			return apiResult;
		}
		// responseMapKeys 大小不能超过50
		String[] split = responseMapKeys.split(",");
		if (split.length > 50) {
			apiResult.setSuccess(ErrorCode.PARAM_ERROR_CODE);
			apiResult.setError("responseMapKeys长度超过上限,请检查入参");
			log.error("[严重异常]获取全量推荐信息（map）时入不对，request {},bodyRequest:{},responseMapKeys长度{}", JSON.toJSONString(request), JSON.toJSONString(bodyRequest), split.length);
			return apiResult;
		}
		MosesBizConfigEnum bizConfigEnum=MosesBizConfigEnum.getByBizName(request.getBiz());
		if(Objects.isNull(bizConfigEnum) && StringUtils.isEmpty(request.getSceneId())){
			apiResult.setSuccess(ErrorCode.PARAM_ERROR_CODE);
			apiResult.setError("biz入参错误，请检查入参");
			log.error("[严重异常]获取全量推荐信息（map）时找不到biz对应的业务，request {},bodyRequest:{}", JSON.toJSONString(request), JSON.toJSONString(bodyRequest));
			return apiResult;
		}

		// 如果个性化活动开关为关且该活动无非个性化数据 则无数据返回
		if(Objects.nonNull(bizConfigEnum) && Objects.isNull(functionServiceImpl.getRuleFact(request))){
			if(!switchConfigCache.getRecommendSwitchByConfigId(CommonConstants.PERSONALIZE_RECOMMEND_OFF_CONFIG_ID) && Objects.isNull(bizConfigEnum.getImpersonalSourceWeight())){
				apiResult.setSuccess(ErrorCode.SUCCESS_CODE);
				apiResult.setError("无非个性化数据");
				return apiResult;
			}
		}
		try {
			RecommendInfoMapResponse result = functionServiceImpl.getAllRecommendInfoMap(request, bodyRequest);
			apiResult.setData(result);
		} catch (Exception e) {
			log.error("[严重异常]获取全量推荐信息时出现异常，request {}，", JSON.toJSONString(request), e);
			apiResult.setSuccess(ErrorCode.SYSTEM_ERROR_CODE);
			apiResult.setError("系统异常");
		}
		return apiResult;
	}
}