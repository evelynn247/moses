package com.biyao.moses.rank;

import java.util.List;

import com.biyao.moses.model.template.entity.TotalTemplateInfo;


/**
 * rank排序接口
 * @author houkun
 * @Date 2018年12月18日
 */
public interface RecommendRank {

	/**
	 * @param oriData 待排序数据集
	 * @param user 当前用户的一些在本地线程中的请求数据
	 * @param scores 当前请求楼层的合并数据分值集
	 * @return List<TotalTemplateInfo> 排序完成的数据
	 */
	List<TotalTemplateInfo> executeRecommend(RankRequest rankRequest);



}