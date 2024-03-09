package com.biyao.moses.model.adapter.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.biyao.moses.common.enums.TemplateTypeEnum;
import com.biyao.moses.context.ByUser;
import com.biyao.moses.model.SpecialTemplateInfo;
import com.biyao.moses.model.template.TemplateInfo;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.util.RouteParamUtil;

/**
 * 单排模板-样式2（专题）
 * 
 * @Description
 * @Date 2018年9月27日
 */
@Component("special")
public class SpecialAdapter extends BaseTemplateAdapter {

	@Autowired
	RouteParamUtil routeParamUtil;

	
	@Override
	public Integer getCurTemplateDataNum(int actualSize) {
		return TemplateTypeEnum.SPECIAL.getDataSize();
	}


	@Override
	public TemplateInfo newTemplateInfoInstance() {
		return new SpecialTemplateInfo();
	}


	@Override
	public TemplateInfo specialDataHandle(TemplateInfo templateInfo,
			TotalTemplateInfo totalData, Integer curDataIndex, ByUser user) {
		
		SpecialTemplateInfo curTemplateInfo = (SpecialTemplateInfo) templateInfo;
		curTemplateInfo.setPriceStr("￥"+totalData.getPriceStr()+"元起");
		curTemplateInfo.setPriceCent(totalData.getPriceCent()+".00");
		
		return curTemplateInfo;
	}

}