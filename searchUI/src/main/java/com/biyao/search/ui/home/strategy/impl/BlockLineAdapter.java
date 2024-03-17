package com.biyao.search.ui.home.strategy.impl;

import java.util.ArrayList;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.biyao.search.common.enums.PlatformEnum;
import com.biyao.search.common.model.TopicItem;
import com.biyao.search.ui.home.constant.HomeTemplateEnum;
import com.biyao.search.ui.home.model.HomeTemplate;
import com.biyao.search.ui.home.model.app.AppHomeTemplate;
import com.biyao.search.ui.home.model.app.TemplateDetailInfo;
import com.biyao.search.ui.home.strategy.TemplateAdapter;

/**
 * 空白模板转换 block_line(11,"blockline")
 * @author monkey
 * @date 2018年8月15日
 */
//@Component 加入spring管理
public class BlockLineAdapter extends AbstractTemplateAdapter {


	/**
	 * 空白模板只需要设置高度
	 */
	@Override
	AppHomeTemplate buildAppHomeTemplate(AppHomeTemplate appHomeTemplate,
			HomeTemplate homeTemplate) {

		//填充模板数据
		ArrayList<TemplateDetailInfo> data = new ArrayList<TemplateDetailInfo>(); //模板数据list
		TemplateDetailInfo templateDetailInfo = new TemplateDetailInfo(); // every topic 详细数据
		
		templateDetailInfo.setHeight(String.valueOf(homeTemplate.getHeight()));
		
		data.add(templateDetailInfo);
		appHomeTemplate.setData(data);
		
		return appHomeTemplate;
	}

}