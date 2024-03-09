package com.biyao.moses.service.imp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.biyao.moses.model.template.Page;
import com.biyao.moses.model.template.Template;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.params.ApiResult;
import com.biyao.moses.service.MosesConfService;
import com.biyao.moses.util.HttpClientUtil;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;

/**
 * HttpMosesConfServiceImpl
 * 
 * @Description
 * @Date 2018年9月27日
 */
@Service
public class HttpMosesConfServiceImpl implements MosesConfService {

	private static String mosesConfPageUrl = "http://mosesconf.biyao.com/moses/conf/queryPageById";

	private static String mosesConfDataUrl = "http://mosesconf.biyao.com/moses/conf/queryBlockByExpId";
	
	
	@BProfiler(key = "com.biyao.moses.service.imp.HttpMosesConfServiceImpl.queryBlockByExpId",
			monitorType = {MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR})
	@Override
	public ApiResult<List<Template<TotalTemplateInfo>>> queryBlockByExpId(String bid, String expId) {

		// String reqURL = mosesConfDataUrl+"?bid="+bid+"&expId="+expId;
		ApiResult<List<Template<TotalTemplateInfo>>> parseObject = null;
		try {
			Map<String, String> params = new HashMap<String, String>();
			params.put("bid", bid);
			params.put("expId", expId);
			String result = HttpClientUtil.sendPostRequest(mosesConfDataUrl, null, params);
			parseObject = JSONObject.parseObject(result,
					new TypeReference<ApiResult<List<Template<TotalTemplateInfo>>>>() {
					});
		} catch (Exception e) {
			e.printStackTrace();
		}

		return parseObject;
	}

	@Override
	@BProfiler(key = "com.biyao.moses.service.imp.HttpMosesConfServiceImpl.queryPageById",
	monitorType = {MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR})
	public ApiResult<Page<TotalTemplateInfo>> queryPageById(String pid) {
		// String reqURL = mosesConfPageUrl+"?pid="+pid;
		ApiResult<Page<TotalTemplateInfo>> parseObject = null;
		try {
			Map<String, String> params = new HashMap<String, String>();
			params.put("pid", pid);
			String result = HttpClientUtil.sendPostRequest(mosesConfPageUrl, null, params);
			parseObject = JSONObject.parseObject(result, new TypeReference<ApiResult<Page<TotalTemplateInfo>>>() {
			});
		} catch (Exception e) {
			e.printStackTrace();
		}

		return parseObject;
	}

}
