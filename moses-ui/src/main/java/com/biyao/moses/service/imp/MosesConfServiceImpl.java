package com.biyao.moses.service.imp;

import java.util.List;

import org.springframework.stereotype.Component;

import com.biyao.moses.model.template.Page;
import com.biyao.moses.model.template.Template;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.params.ApiResult;
import com.biyao.moses.service.MosesConfService;

/**
 * MosesConfServiceImpl
 * 
 * @Description
 * @Date 2018年9月27日
 */
@Component
public class MosesConfServiceImpl implements MosesConfService {

	@Override
	public ApiResult<List<Template<TotalTemplateInfo>>> queryBlockByExpId(String bid, String expId) {
		ApiResult<List<Template<TotalTemplateInfo>>> apiResult = new ApiResult<List<Template<TotalTemplateInfo>>>();
		apiResult.setSuccess(2000);
		apiResult.setError("系统繁忙，查询失败!");
		return apiResult;
	}

	@Override
	public ApiResult<Page<TotalTemplateInfo>> queryPageById(String pid) {
		ApiResult<Page<TotalTemplateInfo>> apiResult = new ApiResult<Page<TotalTemplateInfo>>();
		apiResult.setSuccess(2000);
		apiResult.setError("系统繁忙，查询失败!");
		return apiResult;
	}

}
