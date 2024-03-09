package com.biyao.moses.rank;

import java.io.Serializable;
import java.util.List;

import lombok.Builder;
import lombok.Data;

import com.biyao.moses.model.template.entity.TotalTemplateInfo;

@Data
@Builder
public class RankRequest implements Serializable{

	private static final long serialVersionUID = 1L;
	
	private List<TotalTemplateInfo> oriData;
	
//	private ByUser user;
	
	private String dataNum;
	
	private String rankName;
	
	private String topicId;
	
	private String uuid;
	
	private String uid;
	
	private String sortType;
	
	private String sortValue;
	/**
	 * updUserType
	 */
	private Integer upcUserType;

	/**
	 * 前台类目ID
	 */
	private String frontendCategoryId;
	/**
	 * 站点 1小程序 2M站 7IOS 9安卓
	 */
	private String siteId;
	/**
	 * 后台三级类目ID
	 */
	private String categoryIds;
}