package com.biyao.moses.service;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.biyao.moses.model.template.Page;
import com.biyao.moses.model.template.Template;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.params.ApiResult;
import com.biyao.moses.service.imp.MosesConfServiceImpl;

/**
 * fegin
 * 
 * @Description
 * @Date 2018年9月27日
 */
@FeignClient(value = "moses-conf", fallback = MosesConfServiceImpl.class)
public interface MosesConfService {

	// 根据bid和expId查询实验
	@PostMapping(value = "/moses/conf/queryBlockByExpId")
	ApiResult<List<Template<TotalTemplateInfo>>> queryBlockByExpId(@RequestParam("bid") String bid,
			@RequestParam("expId") String expId);

	// 查询首页
	@PostMapping(value = "/moses/conf//queryPageById")
	ApiResult<Page<TotalTemplateInfo>> queryPageById(@RequestParam("pid") String pid);
	
	
	
}
