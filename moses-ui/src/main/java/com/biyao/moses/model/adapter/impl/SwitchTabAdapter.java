package com.biyao.moses.model.adapter.impl;

import org.springframework.stereotype.Component;

import com.biyao.moses.context.ByUser;
import com.biyao.moses.model.SwitchTabTemplateInfo;
import com.biyao.moses.model.template.TemplateInfo;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;

/**
 * 顶部导航模板
 * 
 * @Description
 * @Date 2018年9月27日
 */
@Component("switchTab")
public class SwitchTabAdapter extends BaseTemplateAdapter {

	
	@Override
	public TemplateInfo newTemplateInfoInstance() {
		return new SwitchTabTemplateInfo();
	}

	@Override
	public TemplateInfo specialDataHandle(TemplateInfo templateInfo, TotalTemplateInfo totalData,
			Integer curDataIndex, ByUser user) {
		return templateInfo;
	}
	
	@Override
	public Integer getCurTemplateDataNum(int actualSize) {
		return actualSize;
	}
}