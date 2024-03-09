package com.biyao.moses.controller;

import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSON;
import com.biyao.moses.params.ApiResult;
import com.biyao.moses.util.RedisUtil;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * 推荐redis相关controller
 * 
 * @Description
 * @Date 2018年9月27日
 */
@RestController
@Api("RecommendController相关的api")
public class RecommendController {

	@Value("${server.port}")
	private String port;

	@Autowired
	private RedisUtil redisUtil;

	/**
	 * redis查询
	 * 
	 * @param value
	 * @return
	 */
	@PostMapping("/redis/get")
	@ApiOperation(value = "根据redis的key值", notes = "查询value信息")
	public ApiResult<String> redisGet(@RequestParam("value") String value) {
		ApiResult<String> apiResult = new ApiResult<String>();
		String name = redisUtil.getString(value);
		apiResult.setData(name);
		return apiResult;
	}

	@PostMapping("/redis/hget")
	@ApiOperation(value = "根据redis的key值", notes = "查询value信息")
	public ApiResult<String> redisHget(@RequestParam("key") String key, @RequestParam("value") String value) {
		ApiResult<String> apiResult = new ApiResult<String>();
		String hgetStr = redisUtil.hgetStr(key, value);
		apiResult.setData(hgetStr);
		return apiResult;
	}

	@PostMapping("/redis/hgetAll")
	@ApiOperation(value = "根据redis的key值", notes = "查询value信息")
	public ApiResult<String> redisHgetAll(@RequestParam("value") String value) {
		ApiResult<String> apiResult = new ApiResult<String>();
		Map<String, String> hgetAll = redisUtil.hgetAll(value);
		apiResult.setData(JSON.toJSONString(hgetAll));
		return apiResult;
	}
	
	@PostMapping("/redis/renamenx")
	@ApiOperation(value = "修改rediskey值，仅供修改首页UI实验使用", notes = "修改rediskey值")
	public ApiResult<Long> redisRenamenx(@RequestParam("oldkey") String oldkey,@RequestParam("new") String newkey) {
		ApiResult<Long> apiResult = new ApiResult<Long>();
		Long renamenx = redisUtil.renamenx(oldkey, newkey);
		apiResult.setData(renamenx);
		apiResult.setError("修改成功时，返回 1,如果 NEW_KEY已经存在，返回 0 ");
		return apiResult;
	}
	
	

//	@PostMapping("/redis/set")
//	@ApiOperation(value = "添加redis内容")
//	public ApiResult<Boolean> redisSet(@RequestParam("key") String key, @RequestParam("value") String value) {
//		ApiResult<Boolean> apiResult = new ApiResult<Boolean>();
//		Boolean bol = redisUtil.setString(key, value, -1);
//		apiResult.setData(bol);
//		return apiResult;
//	}

//	@PostMapping("/redis/hset")
//	@ApiOperation(value = "添加redis hset内容")
//	public ApiResult<Long> redisHset(@RequestParam("key") String key, @RequestParam("field") String field,
//			@RequestParam("value") String value) {
//		ApiResult<Long> apiResult = new ApiResult<Long>();
//		Long bol = redisUtil.hset(key, field, value);
//		apiResult.setData(bol);
//		return apiResult;
//	}

//	@PostMapping("/redis/hdel")
//	@ApiOperation(value = "删除redis hset内容")
//	public ApiResult<Long> redisHset(@RequestParam("key") String key, @RequestParam("field") String field) {
//		ApiResult<Long> apiResult = new ApiResult<Long>();
//		Long bol = redisUtil.hdel(key, field);
//		apiResult.setData(bol);
//		return apiResult;
//	}

//	@PostMapping("/redis/zadd")
//	@ApiOperation(value = "添加redis zadd内容")
//	public ApiResult<Long> rediszadd(@RequestParam("key") String key, @RequestParam("pid") String pid) {
//		ApiResult<Long> apiResult = new ApiResult<Long>();
//		Long bol = redisUtil.zadd(key, 1, pid);
//		apiResult.setData(bol);
//		return apiResult;
//	}
//
//	@PostMapping("/redis/zrevrange")
//	@ApiOperation(value = "添加redis zrevrange内容")
//	public ApiResult<String> rediszrevrange(@RequestParam("key") String key) {
//		ApiResult<String> apiResult = new ApiResult<String>();
//		Set<String> zrevrange = redisUtil.zrevrange(key, 0, -1);
//		apiResult.setData(JSON.toJSONString(zrevrange));
//		return apiResult;
//	}
}
