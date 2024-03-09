package com.biyao.moses.service;

import java.util.List;

import com.alibaba.fastjson.JSONObject;
import com.biyao.moses.model.template.Page;
import com.biyao.moses.model.template.Template;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.params.ApiResult;

/**
 * 配置接口
 * 
 * @Description
 * @author zyj
 * @Date 2018年9月27日
 */
public interface MosesConfService {

	// 根据区块id和实验id，查询模板列表
	ApiResult<List<Template<TotalTemplateInfo>>> queryBlockByExpId(String blockId, String expId);

	// 查询page信息
	@SuppressWarnings("rawtypes")
	ApiResult<Page> queryPageById(String pid);

	// 添加模板
	ApiResult<String> templateAdd(JSONObject json);

	// 添加区块
	ApiResult<String> blockAdd(JSONObject json);

	// 添加页面
	ApiResult<String> pageAdd(JSONObject json, String... expId);

	// 修改页面
	ApiResult<String> updatePage(JSONObject json);

	// 修改区块
	ApiResult<String> updateBlock(JSONObject json);

	// 添加match初始数据，测试使用
	ApiResult<String> matchAdd(JSONObject json);

	// 根据页面名称生成feed页面
	ApiResult<String> buildPageByPageName(String pageName, Integer templateType, String expId);
}
