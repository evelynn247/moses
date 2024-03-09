package com.biyao.moses.model.adapter.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import com.biyao.moses.common.enums.TemplateTypeEnum;
import com.biyao.moses.context.ByUser;
import com.biyao.moses.model.BlocklineTemplateInfo;
import com.biyao.moses.model.adapter.TemplateAdapter;
import com.biyao.moses.model.template.Template;
import com.biyao.moses.model.template.TemplateInfo;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;

/**
 * 空白模板
 * @Description
 * @Date 2018年9月27日
 */
@Component("blockline")
public class BlockLineAdapter implements TemplateAdapter {

	/**
	 * 空白模板，数据只有一个高度height
	 */
	@Override
	public Template<TemplateInfo> adapte(Integer pageIndex, Template<TotalTemplateInfo> oriTemplate, List<TotalTemplateInfo> data,
			Integer feedIndex, String stp,ByUser user) throws Exception {

		Template<TemplateInfo> resultTemplate = new Template<TemplateInfo>();
		BeanUtils.copyProperties(oriTemplate, resultTemplate);
		List<TemplateInfo> arrayList = new ArrayList<TemplateInfo>();
		resultTemplate.setData(arrayList);

		BlocklineTemplateInfo templateInfo = new BlocklineTemplateInfo();
		if (oriTemplate.getData() != null && oriTemplate.getData().size() > 0) {
			if (StringUtils.isNotBlank(oriTemplate.getData().get(0).getHeight())) {
				templateInfo.setHeight(oriTemplate.getData().get(0).getHeight());
				templateInfo.setColor(oriTemplate.getData().get(0).getColor());
			} else {
				templateInfo.setHeight("20");
			}
		} else {
			templateInfo.setHeight("20");
		}
		arrayList.add(templateInfo);

		return resultTemplate;
	}

	@Override
	public Integer getCurTemplateDataNum(int actualSize) {
		return TemplateTypeEnum.BLOCK_LINE.getDataSize();
	}

	/**
	 * 正常模板的适配实现，不是feed流
	 */
	@Override
	public Template<TemplateInfo> adapte(
			Template<TotalTemplateInfo> oriTemplate,
			List<TotalTemplateInfo> temData, String stp,ByUser user) throws Exception {
		return this.adapte(0, oriTemplate, temData, 0, stp,user);
	}

}
