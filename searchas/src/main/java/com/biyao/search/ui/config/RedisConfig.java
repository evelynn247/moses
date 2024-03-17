package com.biyao.search.ui.config;

import java.util.List;

/**
 * redis配置类
 * @author: luozhuo
 * @date: 2017-2-22
 */
public class RedisConfig {
	/**
	 * redis机器IP地址
	 */
	private List<String> ipAddressList;
	
	/**
	 * redis机器映射列表
	 */
	private List<Integer> indexMappingList;

	public List<String> getIpAddressList() {
		return ipAddressList;
	}

	public void setIpAddressList(List<String> ipAddressList) {
		this.ipAddressList = ipAddressList;
	}

	public List<Integer> getIndexMappingList() {
		return indexMappingList;
	}

	public void setIndexMappingList(List<Integer> indexMappingList) {
		this.indexMappingList = indexMappingList;
	}
	
}
