package com.biyao.search.ui.home.strategy.impl;


/**
 * 单排模板 single_line(2,"singleline")
 * 样式1(标题+单张横屏大图)
 * @author monkey
 * @date 2018年8月15日
 */
public class SingleLineAdapter extends BaseTemplateAdapter{
	
	
	/**
	 *   "mainTitle":"甄选家",
     *   "mainTitleColor":"#aaa",
     *   "subtitle":"治愈系家具",
     *   "subtitleColor":"#bbb",
     *   "images":[
     *       "1"
     *   ],
     *   "routerUrl":"value"
	 */
	/*@Override
	AppHomeTemplate buildAppHomeTemplate(AppHomeTemplate appHomeTemplate, HomeTemplate homeTemplate) {
		
		ArrayList<TemplateDetailInfo> data = new ArrayList<TemplateDetailInfo>(); //模板数据list
		TemplateDetailInfo templateDetailInfo = new TemplateDetailInfo(); // every topic 详细数据
		
		//填充模板数据
		HomeTopic homeTopic = homeTemplate.getData().get(0);
		//主标题 子标题
		templateDetailInfo.setMainTitle(homeTopic.getTitle().getContent());
		templateDetailInfo.setMainTitleColor(homeTopic.getTitle().getColor());
		templateDetailInfo.setSubtitle(homeTopic.getSubTitle().getContent());
		templateDetailInfo.setSubtitleColor(homeTopic.getSubTitle().getColor());
		// 单张横屏大图
		TopicImage topicImage = homeTopic.getImgUrl().get(0);
		ArrayList<String> images = new ArrayList<String>();
		images.add(topicImage.getImageUrl());
		templateDetailInfo.setImages(images);
		// 跳转url
		templateDetailInfo.setRouterUrl(topicImage.getUrl());
		
		data.add(templateDetailInfo);
		appHomeTemplate.setData(data);
		return appHomeTemplate;
	}*/

}
