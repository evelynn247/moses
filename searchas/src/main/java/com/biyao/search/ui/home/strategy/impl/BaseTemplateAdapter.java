package com.biyao.search.ui.home.strategy.impl;

import java.util.ArrayList;
import java.util.List;

import com.biyao.search.ui.home.model.HomeTemplate;
import com.biyao.search.ui.home.model.HomeTopic;
import com.biyao.search.ui.home.model.app.AppHomeTemplate;
import com.biyao.search.ui.home.model.app.TemplateDetailInfo;
import com.biyao.search.ui.topic.model.TopicImage;

/**
 * 基本实现，除了标题 空白 和单排特殊都适用
 * @author monkey
 * @date 2018年8月15日
 */
public class BaseTemplateAdapter extends AbstractTemplateAdapter {

	@Override
	AppHomeTemplate buildAppHomeTemplate(AppHomeTemplate appHomeTemplate, HomeTemplate homeTemplate) {

		ArrayList<TemplateDetailInfo> data = new ArrayList<TemplateDetailInfo>(); //模板数据list
		
		//填充模板数据
		
		List<HomeTopic> homeTopicList = homeTemplate.getData();
		for (HomeTopic homeTopic : homeTopicList) {
			TemplateDetailInfo templateDetailInfo = new TemplateDetailInfo(); // every topic 详细数据
			//主标题 子标题
			templateDetailInfo.setMainTitle(homeTopic.getTitle().getContent());
			templateDetailInfo.setMainTitleColor(homeTopic.getTitle().getColor());
			templateDetailInfo.setSubtitle(homeTopic.getSubTitle().getContent());
			templateDetailInfo.setSubtitleColor(homeTopic.getSubTitle().getColor());
			
			List<TopicImage> imgUrlList = homeTopic.getImgUrl();
			ArrayList<String> images = new ArrayList<String>();
			for (TopicImage topicImage : imgUrlList) {
				images.add(topicImage.getImageUrl());
				templateDetailInfo.setImages(images);
				// 跳转url
				templateDetailInfo.setRouterUrl(topicImage.getUrl());
			}
			
			data.add(templateDetailInfo);
		}
		
		appHomeTemplate.setData(data);
		
		return appHomeTemplate;
	}

}
