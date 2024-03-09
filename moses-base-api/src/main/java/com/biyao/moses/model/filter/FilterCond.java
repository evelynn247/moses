package com.biyao.moses.model.filter;

public class FilterCond {
	// 过滤下架商品
	public boolean filterSoldOut = true;
	// 白名单过滤
	public boolean whiteFilter = false;
	// 黑名单过滤
	public boolean blackFilter = false;
	// uuid过滤
	public boolean uuidFilter = false;
	// pageId过滤
	public boolean pageIdFilter = false;
	// block过滤
	public boolean blockFilter = false;
	// topicId过滤
	public boolean topicIdFilter = false;
	// 来源过滤
	public boolean sourceFilter = false;
	// 过滤列表中已经存在的商品
	public boolean containFilter = false;
	// 指定商品列表排前
	public boolean frontFilter = false;
}
