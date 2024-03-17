package com.biyao.search.ui.remote.response;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class BlockData implements Serializable{
	/**
	 * 搜索页顶部提示文本（目前是未完全匹配时使用）
	 * 外层的list代表多行文本
	 * 内层的list代表一行文本里的多个文本块
	 */
	private List<SearchTitle> title = new ArrayList<>();
	
	/**
	 * 区块ID
	 */
	private String blockId;
	
	/**
	 * 当前区块的模板数据列表
	 */
	private List<Template> templates = new ArrayList<>();
	
	/**
	 * 当前页码
	 */
	private Integer pageIndex;
	
	/**
	 * 当前区块是否还有更多分页
	 * 1 - 是
	 * 0 - 否
	 */
	private Integer hasMore = 0;
	
	/* 内部处理逻辑 */
	private transient String curQuery = "";


	public List<SearchTitle> getTitle() {
		return title;
	}

	public void setTitle(List<SearchTitle> title) {
		this.title = title;
	}

	public String getBlockId() {
		return blockId;
	}

	public String getCurQuery() {
		return curQuery;
	}

	public void setCurQuery(String curQuery) {
		this.curQuery = curQuery;
	}

	public void setBlockId(String blockId) {
		this.blockId = blockId;
	}

	public List<Template> getTemplates() {
		return templates;
	}

	public void setTemplates(List<Template> templates) {
		this.templates = templates;
	}

	public Integer getPageIndex() {
		return pageIndex;
	}

	public void setPageIndex(Integer pageIndex) {
		this.pageIndex = pageIndex;
	}

	public Integer getHasMore() {
		return hasMore;
	}

	public void setHasMore(Integer hasMore) {
		this.hasMore = hasMore;
	}
	
	
}
