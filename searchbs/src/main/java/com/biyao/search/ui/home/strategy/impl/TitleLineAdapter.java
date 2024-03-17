package com.biyao.search.ui.home.strategy.impl;

import java.util.ArrayList;

import com.biyao.search.ui.home.model.HomeTemplate;
import com.biyao.search.ui.home.model.HomeTopic;
import com.biyao.search.ui.home.model.app.AppHomeTemplate;
import com.biyao.search.ui.home.model.app.TemplateDetailInfo;

/**
 * 标题行模板处理 title_line(1,"titleline")
 * 有副标题显示副标题,没有不显示
 * @author monkey
 * @date 2018年8月15日
 */
public class TitleLineAdapter extends AbstractTemplateAdapter {

	
	/**
	 *  "mainTitle":"摩登时尚",
     *  "mainTitleColor":"#fff",
     *  "subtitle":" CK、LAPERLA等制造商直供",
     *  "subtitleColor":"#bbb",
     *  "routerUrl":"biyao://sjj"
	 */
	@Override
	AppHomeTemplate buildAppHomeTemplate(AppHomeTemplate appHomeTemplate,
			HomeTemplate homeTemplate) {

		ArrayList<TemplateDetailInfo> data = new ArrayList<TemplateDetailInfo>(); //模板数据list
		TemplateDetailInfo templateDetailInfo = new TemplateDetailInfo(); // every topic 详细数据
		
		//填充模板数据
		HomeTopic homeTopic = homeTemplate.getData().get(0);
		
		templateDetailInfo.setMainTitle(homeTopic.getTitle().getContent());
		templateDetailInfo.setMainTitleColor(homeTopic.getTitle().getColor());
		// 老接口要加上子标题
		templateDetailInfo.setSubtitle(homeTopic.getSubTitle().getContent());
		templateDetailInfo.setSubtitleColor(homeTopic.getSubTitle().getColor());
		
		if (homeTopic.getMore()!=null) {
			templateDetailInfo.setRouterUrl(homeTopic.getMore().getUrl());
		}
		
		data.add(templateDetailInfo);
		appHomeTemplate.setData(data);
		return appHomeTemplate;
	}

}
