package com.biyao.moses.controller;

import java.util.List;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.biyao.moses.common.constant.ErrorCode;
import com.biyao.moses.common.constant.MosesTypeContans;
import com.biyao.moses.model.template.Page;
import com.biyao.moses.model.template.Template;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.params.ApiResult;
import com.biyao.moses.params.BlockParam;
import com.biyao.moses.params.MatchParam;
import com.biyao.moses.params.PageParam;
import com.biyao.moses.params.TemplateParam;
import com.biyao.moses.service.MosesConfService;
import com.biyao.moses.service.RecommendMatchApi;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * @Description 模板配置管理
 * @author zyj
 * @Date 2018年8月29日
 */
@RestController
@Api("MosesConfController相关的api")
@RequestMapping(value = "/moses/conf")
@SuppressWarnings("unchecked")
public class MosesConfController {

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private MosesConfService mosesConfService;
	
	@Autowired
	private RecommendMatchApi recommendMatchApi;

	@ApiOperation(value = "添加template")
	@PostMapping(value = "/templateAdd")
	public ApiResult<String> templateAdd(@Valid @RequestBody TemplateParam params, BindingResult bindingResult) {
		ApiResult<String> apiResult = new ApiResult<String>();
		if (bindingResult.hasErrors()) {
			return (ApiResult<String>) checkParams(bindingResult, apiResult);
		}
		try {
			JSONObject jsonObject = (JSONObject) JSONObject.toJSON(params);

			apiResult = mosesConfService.templateAdd(jsonObject);
		} catch (Exception e) {
			logger.error("添加template失败", e);
			apiResult.setError("数据格式错误!");
			apiResult.setSuccess(ErrorCode.PARAM_ERROR_CODE);
			return apiResult;
		}
		return apiResult;
	}

	@ApiOperation(value = "添加block")
	@PostMapping(value = "/blockAdd")
	public ApiResult<String> blockAdd(@Valid @RequestBody BlockParam params, BindingResult bindingResult) {
		ApiResult<String> apiResult = new ApiResult<String>();
		if (bindingResult.hasErrors()) {
			return (ApiResult<String>) checkParams(bindingResult, apiResult);
		}
		try {
			JSONObject jsonObject = (JSONObject) JSONObject.toJSON(params);
			apiResult = mosesConfService.blockAdd(jsonObject);
		} catch (Exception e) {
			logger.error("添加block失败", e);
			apiResult.setError("数据格式错误!");
			apiResult.setSuccess(ErrorCode.PARAM_ERROR_CODE);
			return apiResult;
		}

		return apiResult;
	}

	@ApiOperation(value = "添加page")
	@PostMapping(value = "/pageAdd")
	public ApiResult<String> pageAdd(@Valid @RequestBody PageParam params, BindingResult bindingResult) {
		ApiResult<String> apiResult = new ApiResult<String>();
		if (bindingResult.hasErrors()) {
			return (ApiResult<String>) checkParams(bindingResult, apiResult);
		}
		try {
			JSONObject jsonObject = (JSONObject) JSONObject.toJSON(params);
			apiResult = mosesConfService.pageAdd(jsonObject);
		} catch (Exception e) {
			logger.error("添加page失败", e);
			apiResult.setError("数据格式错误!");
			apiResult.setSuccess(ErrorCode.PARAM_ERROR_CODE);
			return apiResult;
		}
		return apiResult;
	}

	@ApiOperation(value = "修改block")
	@PostMapping(value = "/updateBlock")
	public ApiResult<String> updateBlock(@Valid @RequestBody BlockParam params, BindingResult bindingResult) {
		ApiResult<String> apiResult = new ApiResult<String>();
		if (bindingResult.hasErrors()) {
			return (ApiResult<String>) checkParams(bindingResult, apiResult);
		}
		try {
			JSONObject jsonObject = (JSONObject) JSONObject.toJSON(params);
			if (StringUtils.isEmpty(jsonObject.getString(MosesTypeContans.BLOCKE_ID))) {
				apiResult.setError("blockId不能为空");
				apiResult.setSuccess(ErrorCode.PARAM_ERROR_CODE);
			} else {
				apiResult = mosesConfService.updateBlock(jsonObject);
			}

		} catch (Exception e) {
			logger.error("修改block失败", e);
			apiResult.setError("数据格式错误!");
			apiResult.setSuccess(ErrorCode.PARAM_ERROR_CODE);
			return apiResult;
		}

		return apiResult;
	}

	@ApiOperation(value = "修改page页面")
	@PostMapping(value = "/updatePage")
	public ApiResult<String> updatePage(@Valid @RequestBody PageParam params, BindingResult bindingResult) {
		ApiResult<String> apiResult = new ApiResult<String>();
		try {
			JSONObject jsonObject = (JSONObject) JSONObject.toJSON(params);
			if (StringUtils.isEmpty(jsonObject.getString(MosesTypeContans.PAGE_ID))) {
				apiResult.setError("pid不能为空");
				apiResult.setSuccess(ErrorCode.PARAM_ERROR_CODE);
				return apiResult;
			}
			apiResult = mosesConfService.updatePage(jsonObject);
		} catch (Exception e) {
			logger.error("修改page失败", e);
			apiResult.setError("数据格式错误!");
			apiResult.setSuccess(ErrorCode.PARAM_ERROR_CODE);
			return apiResult;
		}
		return apiResult;

	}

	@SuppressWarnings("rawtypes")
	@ApiOperation(value = "查询page页面信息")
	@PostMapping(value = "/queryPageById")
	public ApiResult<Page> queryPageById(@RequestParam("pid") String pid) {
		ApiResult<Page> apiResult = new ApiResult<Page>();
		if (StringUtils.isEmpty(pid)) {
			apiResult.setError("pid不能为空");
			apiResult.setSuccess(ErrorCode.PARAM_ERROR_CODE);
			return apiResult;
		}
		try {
			apiResult = mosesConfService.queryPageById(pid);
		} catch (Exception e) {
			logger.error("查询失败", e);
			apiResult.setError("查询失败!");
			apiResult.setSuccess(ErrorCode.SYSTEM_ERROR_CODE);
			return apiResult;
		}
		return apiResult;
	}

	@ApiOperation(value = "查询实验信息")
	@PostMapping(value = "/queryBlockByExpId")
	public ApiResult<List<Template<TotalTemplateInfo>>> queryBlockByExpId(@RequestParam("bid") String bid,
			@RequestParam("expId") String expId) {
		ApiResult<List<Template<TotalTemplateInfo>>> apiResult = new ApiResult<List<Template<TotalTemplateInfo>>>();
		if (StringUtils.isEmpty(bid) || StringUtils.isEmpty(expId)) {
			apiResult.setError("参数不能为空");
			apiResult.setSuccess(ErrorCode.PARAM_ERROR_CODE);
			return apiResult;
		}
		try {
			apiResult = mosesConfService.queryBlockByExpId(bid, expId);
		} catch (Exception e) {
			logger.error("查询失败", e);
			apiResult.setError("查询失败!");
			apiResult.setSuccess(ErrorCode.SYSTEM_ERROR_CODE);
			return apiResult;
		}
		return apiResult;
	}

	@ApiOperation(value = "添加match")
	@PostMapping(value = "/matchAdd")
	public ApiResult<String> matchAdd(@Valid @RequestBody MatchParam params, BindingResult bindingResult) {
		ApiResult<String> apiResult = new ApiResult<String>();
		if (bindingResult.hasErrors()) {
			return (ApiResult<String>) checkParams(bindingResult, apiResult);
		}
		try {

			JSONObject jsonObject = (JSONObject) JSONObject.toJSON(params);
			apiResult = mosesConfService.matchAdd(jsonObject);
		} catch (Exception e) {
			logger.error("添加match失败", e);
			apiResult.setError("数据格式错误!");
			apiResult.setSuccess(ErrorCode.PARAM_ERROR_CODE);
			return apiResult;
		}

		return apiResult;
	}

	/**
	 * 参数校验
	 * 
	 * @param bindingResult
	 * @param apiResult
	 * @return
	 */
	private ApiResult<?> checkParams(BindingResult bindingResult, ApiResult<?> apiResult) {
		StringBuffer message = new StringBuffer();
		for (int i = 0; i < bindingResult.getAllErrors().size(); i++) {
			message.append(bindingResult.getAllErrors().get(i).getDefaultMessage() + " ");
		}
		apiResult.setError(message.toString());
		apiResult.setSuccess(ErrorCode.PARAM_ERROR_CODE);
		return apiResult;
	}

	
	
}
