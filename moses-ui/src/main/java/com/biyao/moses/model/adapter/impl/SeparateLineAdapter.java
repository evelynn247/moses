package com.biyao.moses.model.adapter.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import com.biyao.moses.common.enums.TemplateTypeEnum;
import com.biyao.moses.context.ByUser;
import com.biyao.moses.model.SeparateLineTemplateInfo;
import com.biyao.moses.model.adapter.TemplateAdapter;
import com.biyao.moses.model.template.Template;
import com.biyao.moses.model.template.TemplateInfo;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;

/**
 * 分割线模板
 * @Description 
 * @Date 2018年9月27日
 */
@Component("separateLine")
public class SeparateLineAdapter implements TemplateAdapter {


	@Override
	public Template<TemplateInfo> adapte(Integer pageIndex, Template<TotalTemplateInfo> oriTemplate,
			List<TotalTemplateInfo> data, Integer feedIndex, String stp, ByUser user)
			throws Exception {

		Template<TemplateInfo> resultTemplate = new Template<TemplateInfo>();
		BeanUtils.copyProperties(oriTemplate, resultTemplate);
		List<TemplateInfo> arrayList = new ArrayList<TemplateInfo>();
		resultTemplate.setData(arrayList);
		
		SeparateLineTemplateInfo templateInfo = new SeparateLineTemplateInfo();
		arrayList.add(templateInfo);
		return resultTemplate;
	}

	@Override
	public Integer getCurTemplateDataNum(int actualSize) {
		return TemplateTypeEnum.BORDER_BOTTOM.getDataSize();
	}

	@Override
	public Template<TemplateInfo> adapte(
			Template<TotalTemplateInfo> oriTemplate,
			List<TotalTemplateInfo> temData, String stp, ByUser user) throws Exception {
		return this.adapte(0, oriTemplate, temData, 0, stp,user);
	}

}