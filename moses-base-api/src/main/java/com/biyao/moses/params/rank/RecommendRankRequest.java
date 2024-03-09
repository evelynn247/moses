package com.biyao.moses.params.rank;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import com.biyao.moses.common.enums.PlatformEnum;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RecommendRankRequest implements Serializable{

	private static final long serialVersionUID = 1L;
	
	private String sessionId;
	
	private String uuid;
	
	private String randDataId;
	/**
	 * key = dataSource-expId
	 */
	private Map<String, List<TotalTemplateInfo>> matchData;
	
	private String uid;
	
	private String sortType;
	
	private String sortValue;
	/**
	 * upc用户类型
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
	 * 后台三级类目
	 */
	private String categoryIds;
	
}