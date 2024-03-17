package com.biyao.search.ui.home.strategy.impl;

import java.util.Map;

import com.alibaba.fastjson.JSONObject;
import com.biyao.search.common.enums.PlatformEnum;
import com.biyao.search.common.model.TopicItem;
import com.biyao.search.ui.home.constant.HomeTemplateEnum;
import com.biyao.search.ui.home.model.HomeTemplate;
import com.biyao.search.ui.home.model.app.AppHomeTemplate;
import com.biyao.search.ui.home.strategy.TemplateAdapter;

/**
 * 模板适配抽象父类，把模板类型id抽取出来
 * @author monkey
 * @date 2018年8月15日
 */
public abstract class AbstractTemplateAdapter implements TemplateAdapter {

	@Override
	public AppHomeTemplate adapte(JSONObject modelJson,
			Map<Integer, TopicItem> topicItemMap, String searchPageUrl,
			PlatformEnum platformEnum, String pointPrefix,
			StringBuffer showContent, Map<String, String> stp) {
		return null;
	}

	@Override
	public AppHomeTemplate adapte(HomeTemplate homeTemplate) throws Exception {
		AppHomeTemplate blockLineTem = new AppHomeTemplate();
		
		Integer templateId = HomeTemplateEnum.getTemplateIdByValue(homeTemplate.getModelType());
		if (templateId == null) {
			throw new Exception("无对应的模板类型,modelType="+homeTemplate.getModelType());
		}
		
		blockLineTem.setTemplateType(templateId);
		blockLineTem = buildAppHomeTemplate(blockLineTem,homeTemplate);
		
		return blockLineTem;
	}
	
	
	abstract AppHomeTemplate buildAppHomeTemplate(AppHomeTemplate appHomeTemplate,HomeTemplate homeTemplate);
	
}
