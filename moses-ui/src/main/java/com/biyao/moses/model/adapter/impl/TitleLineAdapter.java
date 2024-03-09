package com.biyao.moses.model.adapter.impl;

import org.springframework.stereotype.Component;

import com.biyao.moses.common.enums.TemplateTypeEnum;
import com.biyao.moses.context.ByUser;
import com.biyao.moses.model.TitleLineTemplateInfo;
import com.biyao.moses.model.template.TemplateInfo;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;

/**
 * 楼层标题(有副标题显示副标题,没有不显示)
 * 
 * @Description
 * @Date 2018年9月27日
 */
@Component("titleline")
public class TitleLineAdapter extends BaseTemplateAdapter {

	@Override
	public TemplateInfo newTemplateInfoInstance() {
		return new TitleLineTemplateInfo();
	}

	@Override
	public TemplateInfo specialDataHandle(TemplateInfo templateInfo, TotalTemplateInfo totalData,
			Integer curDataIndex, ByUser user) {
		return templateInfo;
	}

	@Override
	public Integer getCurTemplateDataNum(int actualSize) {
		return TemplateTypeEnum.TITLE_LINE.getDataSize();
	}

}
