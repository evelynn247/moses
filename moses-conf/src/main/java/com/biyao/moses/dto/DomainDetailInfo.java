package com.biyao.moses.dto;

import lombok.Data;

@Data
public class DomainDetailInfo {

	// 可能为 topicId 或者 pageId
	private String expKey = "";
	
	// 描述
	private String des;
	
	// 实验Id信息
	private String expIds = "";
	
}