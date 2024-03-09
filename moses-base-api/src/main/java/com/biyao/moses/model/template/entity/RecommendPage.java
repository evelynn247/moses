package com.biyao.moses.model.template.entity;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RecommendPage implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 8348809415476768229L;
	//开关设置，true页面内容展示，false 页面内容不展示
	private boolean onoff;
	//要请求的页面pageId
	private String pid;
	//簇id 
	private String topicId;
	
}
