package com.biyao.moses.controller;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSON;
import com.biyao.moses.common.constant.ErrorCode;
import com.biyao.moses.common.constant.RedisKeyConstant;
import com.biyao.moses.params.ApiResult;
import com.biyao.moses.util.RedisUtil;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

/**
 * filter配置管理
 * 
 * @Description
 * @author zyj
 * @Date 2019年3月25日
 */
@RestController
@Api("filter配置")
@RequestMapping(value = "/moses/filterConf")
@Slf4j
public class MosesFilterController {

	@Autowired
	private RedisUtil redisUtil;

	/**
	 * 添加全局黑名单过滤
	 * 
	 * @param params
	 * @param bindingResult
	 * @return
	 */
	@ApiOperation(value = "添加全局黑名单过滤")
	@PostMapping(value = "/blackListAllAdd")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "spuIds", value = "spuId集合，多个supId使用逗号隔开", dataType = "String", paramType = "query", required = true) })
	public ApiResult<String> blackListAllAdd(HttpServletRequest request) {
		ApiResult<String> apiResult = new ApiResult<String>();
		try {
			String redisSpuIds = redisUtil.getString(RedisKeyConstant.MOSES_BLACK_lIST_ALL_KEY);
			String redisValue = combinationSpuIds(request.getParameter("spuIds"), redisSpuIds);
			boolean setString = redisUtil.setString(RedisKeyConstant.MOSES_BLACK_lIST_ALL_KEY, redisValue, -1);
			apiResult.setData("添加成功!");
			if (!setString) {
				apiResult.setData("添加全局黑名单redis错误!");
				apiResult.setError("添加全局黑名单redis错误!");
				apiResult.setSuccess(ErrorCode.SYSTEM_ERROR_CODE);
			}
		} catch (Exception e) {
			log.error("添加全局黑名单失败", e);
			apiResult.setError("数据格式错误!");
			apiResult.setSuccess(ErrorCode.PARAM_ERROR_CODE);
			return apiResult;
		}

		return apiResult;
	}

	/**
	 * 移除全局黑名单
	 * 
	 * @param request
	 * @return
	 */
	@ApiOperation(value = "移除全局黑名单过滤")
	@PostMapping(value = "/blackListAllRemove")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "spuIds", value = "spuId集合，多个supId使用逗号隔开", dataType = "String", paramType = "query", required = true) })
	public ApiResult<String> blackListAllRemove(HttpServletRequest request) {
		ApiResult<String> apiResult = new ApiResult<String>();
		try {
			String redisSpuIds = redisUtil.getString(RedisKeyConstant.MOSES_BLACK_lIST_ALL_KEY);
			String redisValue = removeSpuIds(request.getParameter("spuIds"), redisSpuIds);
			boolean setString = redisUtil.setString(RedisKeyConstant.MOSES_BLACK_lIST_ALL_KEY, redisValue, -1);
			apiResult.setData("移除成功!");
			if (!setString) {
				apiResult.setData("移除全局黑名单redis错误!");
				apiResult.setError("移除全局黑名单redis错误!");
				apiResult.setSuccess(ErrorCode.SYSTEM_ERROR_CODE);
			}
		} catch (Exception e) {
			log.error("移除全局黑名单失败", e);
			apiResult.setError("数据格式错误!");
			apiResult.setSuccess(ErrorCode.PARAM_ERROR_CODE);
			return apiResult;
		}

		return apiResult;
	}

	@ApiOperation(value = "添加页面黑名单过滤")
	@PostMapping(value = "/blackListPageAdd")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "pageId", value = "页面id", dataType = "String", paramType = "query", required = true),
			@ApiImplicitParam(name = "spuIds", value = "spuId集合，多个supId使用逗号隔开", dataType = "String", paramType = "query", required = true) })
	public ApiResult<String> blackListPageAdd(HttpServletRequest request) {
		ApiResult<String> apiResult = new ApiResult<String>();
		try {
			String pageId = request.getParameter("pageId");
			String paramSpuIds = request.getParameter("spuIds");
			String redisSpuIds = redisUtil.hgetStr(RedisKeyConstant.MOSES_BLACK_lIST_PAGE_KEY, pageId);
			String redisValue = combinationSpuIds(paramSpuIds, redisSpuIds);
			Long hset = redisUtil.hset(RedisKeyConstant.MOSES_BLACK_lIST_PAGE_KEY, pageId, redisValue);
			if (hset >= 0) {
				apiResult.setData("添加成功!");
			} else {
				apiResult.setData("添加失败!");
				apiResult.setError("redis返回结果：" + String.valueOf(hset));
			}
		} catch (Exception e) {
			log.error("添加页面黑名单失败", e);
			apiResult.setError("数据格式错误!");
			apiResult.setSuccess(ErrorCode.PARAM_ERROR_CODE);
			return apiResult;
		}

		return apiResult;
	}

	@ApiOperation(value = "移除页面黑名单过滤")
	@PostMapping(value = "/blackListPageRemove")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "pageId", value = "页面id", dataType = "String", paramType = "query", required = true),
			@ApiImplicitParam(name = "spuIds", value = "spuId集合，多个supId使用逗号隔开", dataType = "String", paramType = "query", required = true) })
	public ApiResult<String> blackListPageRemove(HttpServletRequest request) {
		ApiResult<String> apiResult = new ApiResult<String>();
		try {
			String pageId = request.getParameter("pageId");
			String paramSpuIds = request.getParameter("spuIds");
			String redisSpuIds = redisUtil.hgetStr(RedisKeyConstant.MOSES_BLACK_lIST_PAGE_KEY, pageId);
			String redisValue = removeSpuIds(paramSpuIds, redisSpuIds);
			Long hset = redisUtil.hset(RedisKeyConstant.MOSES_BLACK_lIST_PAGE_KEY, pageId, redisValue);
			if (hset >= 0) {
				apiResult.setData("移除成功!");
			} else {
				apiResult.setData("移除失败!");
				apiResult.setError("redis返回结果：" + String.valueOf(hset));
			}
		} catch (Exception e) {
			log.error("移除页面黑名单失败", e);
			apiResult.setError("数据格式错误!");
			apiResult.setSuccess(ErrorCode.PARAM_ERROR_CODE);
			return apiResult;
		}

		return apiResult;
	}

	@ApiOperation(value = "添加topic黑名单过滤")
	@PostMapping(value = "/blackListTopicAdd")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "topicId", value = "主题id", dataType = "String", paramType = "query", required = true),
			@ApiImplicitParam(name = "spuIds", value = "spuId集合，多个supId使用逗号隔开", dataType = "String", paramType = "query", required = true) })
	public ApiResult<String> blackListTopicAdd(HttpServletRequest request) {
		ApiResult<String> apiResult = new ApiResult<String>();
		try {
			String topicId = request.getParameter("topicId");
			String paramSpuIds = request.getParameter("spuIds");
			String redisSpuIds = redisUtil.hgetStr(RedisKeyConstant.MOSES_BLACK_lIST_TOPIC_KEY, topicId);
			String redisValue = combinationSpuIds(paramSpuIds, redisSpuIds);
			Long hset = redisUtil.hset(RedisKeyConstant.MOSES_BLACK_lIST_TOPIC_KEY, topicId, redisValue);
			if (hset >= 0) {
				apiResult.setData("添加成功!");
			} else {
				apiResult.setData("添加失败!");
				apiResult.setError("redis返回结果：" + String.valueOf(hset));
			}
		} catch (Exception e) {
			log.error("添加topic黑名单失败", e);
			apiResult.setError("数据格式错误!");
			apiResult.setSuccess(ErrorCode.PARAM_ERROR_CODE);
			return apiResult;
		}

		return apiResult;
	}

	@ApiOperation(value = "移除topic黑名单过滤")
	@PostMapping(value = "/blackListTopicRemove")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "topicId", value = "主题id", dataType = "String", paramType = "query", required = true),
			@ApiImplicitParam(name = "spuIds", value = "spuId集合，多个supId使用逗号隔开", dataType = "String", paramType = "query", required = true) })
	public ApiResult<String> blackListTopicRemove(HttpServletRequest request) {
		ApiResult<String> apiResult = new ApiResult<String>();
		try {
			String topicId = request.getParameter("topicId");
			String paramSpuIds = request.getParameter("spuIds");
			String redisSpuIds = redisUtil.hgetStr(RedisKeyConstant.MOSES_BLACK_lIST_TOPIC_KEY, topicId);
			String redisValue = removeSpuIds(paramSpuIds, redisSpuIds);
			Long hset = redisUtil.hset(RedisKeyConstant.MOSES_BLACK_lIST_TOPIC_KEY, topicId, redisValue);
			if (hset >= 0) {
				apiResult.setData("移除成功!");
			} else {
				apiResult.setData("移除失败!");
				apiResult.setError("redis返回结果：" + String.valueOf(hset));
			}
		} catch (Exception e) {
			log.error("移除topic黑名单失败", e);
			apiResult.setError("数据格式错误!");
			apiResult.setSuccess(ErrorCode.PARAM_ERROR_CODE);
			return apiResult;
		}

		return apiResult;
	}

	@ApiOperation(value = "添加user黑名单过滤")
	@PostMapping(value = "/blackListUserAdd")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "uuid", value = "uuid", dataType = "String", paramType = "query", required = true),
			@ApiImplicitParam(name = "spuIds", value = "spuId集合，多个supId使用逗号隔开", dataType = "String", paramType = "query", required = true) })
	public ApiResult<String> blackListUserAdd(HttpServletRequest request) {
		ApiResult<String> apiResult = new ApiResult<String>();
		try {
			String uuid = request.getParameter("uuid");
			String paramSpuIds = request.getParameter("spuIds");
			String redisSpuIds = redisUtil.hgetStr(RedisKeyConstant.MOSES_BLACK_lIST_USER_KEY, uuid);
			String redisValue = combinationSpuIds(paramSpuIds, redisSpuIds);
			Long hset = redisUtil.hset(RedisKeyConstant.MOSES_BLACK_lIST_USER_KEY, uuid, redisValue);
			if (hset >= 0) {
				apiResult.setData("添加成功!");
			} else {
				apiResult.setData("添加失败!");
				apiResult.setError("redis返回结果：" + String.valueOf(hset));
			}
		} catch (Exception e) {
			log.error("添加user黑名单失败", e);
			apiResult.setError("数据格式错误!");
			apiResult.setSuccess(ErrorCode.PARAM_ERROR_CODE);
			return apiResult;
		}

		return apiResult;
	}

	@ApiOperation(value = "移除user黑名单过滤")
	@PostMapping(value = "/blackListUserRemove")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "uuid", value = "uuid", dataType = "String", paramType = "query", required = true),
			@ApiImplicitParam(name = "spuIds", value = "spuId集合，多个supId使用逗号隔开", dataType = "String", paramType = "query", required = true) })
	public ApiResult<String> blackListUserRemove(HttpServletRequest request) {
		ApiResult<String> apiResult = new ApiResult<String>();
		try {
			String uuid = request.getParameter("uuid");
			String paramSpuIds = request.getParameter("spuIds");
			String redisSpuIds = redisUtil.hgetStr(RedisKeyConstant.MOSES_BLACK_lIST_USER_KEY, uuid);
			String redisValue = removeSpuIds(paramSpuIds, redisSpuIds);
			Long hset = redisUtil.hset(RedisKeyConstant.MOSES_BLACK_lIST_USER_KEY, uuid, redisValue);
			if (hset >= 0) {
				apiResult.setData("移除成功!");
			} else {
				apiResult.setData("移除失败!");
				apiResult.setError("redis返回结果：" + String.valueOf(hset));
			}
		} catch (Exception e) {
			log.error("移除user黑名单失败", e);
			apiResult.setError("数据格式错误!");
			apiResult.setSuccess(ErrorCode.PARAM_ERROR_CODE);
			return apiResult;
		}
		return apiResult;
	}

	@ApiOperation(value = "查询黑名单内容")
	@PostMapping(value = "/queryBalckList")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "blackType", value = "黑名单类型", dataType = "String", paramType = "query", allowableValues = "all,page,topic,user", required = true),
			@ApiImplicitParam(name = "field", value = "redis二级key,不填则查询全量", dataType = "String", paramType = "query") })
	public ApiResult<String> queryBalckList(HttpServletRequest request) {
		ApiResult<String> apiResult = new ApiResult<String>();
		String redisKey = "";
		try {
			String blackType = request.getParameter("blackType");
			String field = request.getParameter("field");
			if (blackType.equals("all")) {
				redisKey = RedisKeyConstant.MOSES_BLACK_lIST_ALL_KEY;
				String redisSpuIds = redisUtil.getString(redisKey);
				apiResult.setData(redisSpuIds);
				apiResult.setError("查询成功!");
				return apiResult;
			} else if (blackType.equals("page")) {
				redisKey = RedisKeyConstant.MOSES_BLACK_lIST_PAGE_KEY;
			} else if (blackType.equals("topic")) {
				redisKey = RedisKeyConstant.MOSES_BLACK_lIST_TOPIC_KEY;
			} else if (blackType.equals("user")) {
				redisKey = RedisKeyConstant.MOSES_BLACK_lIST_USER_KEY;
			}
			if (StringUtils.isEmpty(field)) {
				Map<String, String> hgetAll = redisUtil.hgetAll(redisKey);
				apiResult.setData(JSON.toJSONString(hgetAll));
				apiResult.setError("查询全量成功!");
			} else {
				String redisSpuIds = redisUtil.hgetStr(redisKey, field);
				apiResult.setData(redisSpuIds);
				apiResult.setError("查询成功!");
			}

			return apiResult;
		} catch (Exception e) {
			log.error("查询黑名单失败", e);
			apiResult.setError("查询黑名单失败错误!" + redisKey);
			apiResult.setSuccess(ErrorCode.PARAM_ERROR_CODE);
			return apiResult;
		}

	}

	/**
	 * 合并且过滤重复的spuIds
	 * 
	 * @param spuIds
	 * @param resultSpuIds
	 * @return
	 */
	private static String combinationSpuIds(String paramSpuIds, String redisSpuIds) {

		String resultSpuIds = "";
		if (!StringUtils.isEmpty(redisSpuIds)) {
			resultSpuIds = redisSpuIds + "," + paramSpuIds;
		} else {
			resultSpuIds = paramSpuIds;
		}
		String[] spuIds = resultSpuIds.split(",");
		List<String> asList = Arrays.asList(spuIds);
		Set<String> spuSet = new HashSet<>(asList);

		String redisValue = org.apache.commons.lang.StringUtils.join(spuSet.toArray(), ",");

		return redisValue;
	}

	/**
	 * 去除redisSpuIds中存在paramSpuIds的spuIds
	 * 
	 * @param paramSpuIds
	 * @param redisSpuIds
	 * @return
	 */
	private static String removeSpuIds(String paramSpuIds, String redisSpuIds) {

		String[] spuIds = redisSpuIds.split(",");
		Set<String> spuSet = new HashSet<>();
		String[] parameter = paramSpuIds.split(",");
		String redisValue = "";
		List<String> asList = Arrays.asList(parameter);
		Map<String, String> maps = asList.stream()
				.collect(Collectors.toMap(i -> i, Function.identity(), (key1, key2) -> key2));
		for (int j = 0; j < spuIds.length; j++) {
			String spuId = spuIds[j];
			if (maps.containsKey(spuId)) {
				maps.remove(spuId);
			} else if (!StringUtils.isEmpty(spuId)) {
				spuSet.add(spuId);
			}
		}
		if (spuSet.size() > 0) {
			redisValue = org.apache.commons.lang.StringUtils.join(spuSet.toArray(), ",");
		}
		return redisValue;

	}
}
