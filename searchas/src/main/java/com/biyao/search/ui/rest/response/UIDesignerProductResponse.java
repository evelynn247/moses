package com.biyao.search.ui.rest.response;

import java.util.ArrayList;
import java.util.List;

import com.biyao.search.common.model.UIProduct;
import com.biyao.search.ui.constant.UIResultType;
import com.biyao.search.ui.model.UIDesignerProduct;

/**
 * 搜索返回对象
 */
public class UIDesignerProductResponse {
	
	/** 搜索词 */
	private String query = "";   
	
	/** 搜索ID */
	private String sid = "";
	
	/** 搜索结果类型：正常数据/周排行数据 */
	private Integer resultType = UIResultType.WEEK_HOT;
	
	/** 分页，从1开始计数 */
	private Integer pageIndex = 1;
	
	/** 分页总数 */
	private Integer pageCount = 1;
	
	/** 搜索出的商品 */
	List<UIDesignerProduct> products = new ArrayList<UIDesignerProduct>();
	
	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public String getSid() {
		return sid;
	}

	public void setSid(String sid) {
		this.sid = sid;
	}
	
	public Integer getResultType() {
		return resultType;
	}

	public void setResultType(Integer resultType) {
		this.resultType = resultType;
	}

	public Integer getPageIndex() {
		return pageIndex;
	}

	public void setPageIndex(Integer pageIndex) {
		this.pageIndex = pageIndex;
	}

	public Integer getPageCount() {
		return pageCount;
	}

	public void setPageCount(Integer pageCount) {
		this.pageCount = pageCount;
	}

	public List<UIDesignerProduct> getProducts() {
		return products;
	}

	public void setProducts(List<UIDesignerProduct> products) {
		this.products = products;
	}
	
}
