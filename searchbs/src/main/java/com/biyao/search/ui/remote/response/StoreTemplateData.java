package com.biyao.search.ui.remote.response;

import java.util.ArrayList;
import java.util.List;

public class StoreTemplateData extends TemplateData{
	/**
	 * 店铺名称
	 */
	private String storeName;
	
	/**
	 * 店铺描述
	 */
	private String storeDesc;
	
	/**
	 * 店铺主图
	 */
	private String imageUrl;
	
	/**
	 * 跳转信息
	 */
	private TemplateRoute route;
	
	/**
	 * 店铺推荐商品列表。四个
	 */
	private List<SearchProduct> products = new ArrayList<>();

	public String getStoreName() {
		return storeName;
	}

	public void setStoreName(String storeName) {
		this.storeName = storeName;
	}

	public String getStoreDesc() {
		return storeDesc;
	}

	public void setStoreDesc(String storeDesc) {
		this.storeDesc = storeDesc;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public TemplateRoute getRoute() {
		return route;
	}

	public void setRoute(TemplateRoute route) {
		this.route = route;
	}

	public List<SearchProduct> getProducts() {
		return products;
	}

	public void setProducts(List<SearchProduct> products) {
		this.products = products;
	}
	
	
	
}
