package com.biyao.search.ui.home.strategy.impl;

import java.util.ArrayList;
import java.util.List;

import com.biyao.search.ui.home.model.HomeTemplate;
import com.biyao.search.ui.home.model.HomeTopic;
import com.biyao.search.ui.home.model.app.AppHomeTemplate;
import com.biyao.search.ui.home.model.app.TemplateDetailInfo;
import com.biyao.search.ui.topic.model.TopicImage;


/**
 * special(3,"special")
 * 单排模板-样式2（专题）（小程序M站本期不实现）
 * @author monkey
 * @date 2018年8月15日
 */
public class SpecialAdapter extends AbstractTemplateAdapter {

	
	/**
	 * "data":[
     *       {
     *           "mainTitle":"沐浴的学问",
     *           "mainTitleColor":"#111",
     *           "subtitle":"舒服的沐浴",
     *           "subtitleColor":"#qqq",
     *           "priceStr":"¥123",
     *           "priceCent":"123.00",
     *           "priceColor":"#fff",
     *           "images":[
     *               "1"
     *           ],
     *           "routerUrl":"value"
     *       }
     *   ]
	 */
	@Override
	AppHomeTemplate buildAppHomeTemplate(AppHomeTemplate appHomeTemplate,
			HomeTemplate homeTemplate) {
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
			
			// 单排special设置价格
			templateDetailInfo.setPriceStr("￥"+homeTopic.getPrice()+"元起");
			templateDetailInfo.setPriceCent(homeTopic.getPrice()+".00");
			templateDetailInfo.setPriceColor(homeTopic.getPriceColor());
			
			
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
