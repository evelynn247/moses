package com.biyao.moses.params;

import lombok.Data;

import java.util.List;

@Data
public class UIBaseBody {

	private List<String> cpIds;
	private List<String> csIds;
	/**
	 * 买二返一频道页热门置顶的商品pid
	 */
	private List<String> tpids;
	/**
	 * 可以插入的活动信息集合
     * 格式为：List<AdvertParam>
	 */
	private String advertInfo;
    /**
     * 筛选的属性信息（平台核心转化V2.2新增），格式如下：
     * {
     * "color": ["红色", "黑色", "白色"],
     * "size": ["27", "28", "29"]
     * }
     */
    private String selectedScreenAttrs;
	/**
	 * 配置的首页轮播图信息
	 */
	private String swiperPicConfInfo;


}