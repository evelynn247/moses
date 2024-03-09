package com.biyao.moses.common.enums;

public enum SourceEnum {

	HP("hp", "首页页面"),
	HR("hr", "首页下面推荐入口"),
	S1("s1", "个人中心入口"),
	NHP("nhp", "新用户首页页面"),
	FLBT("flbt","第一张轮播图"),
	XSZX1("xszx1","新手专享数据源2双排"),
	XSZXYS("xszxys","新手专享数据源1双排"),
	XSZXYD("xszxyd","新手专享数据源1单排");

	private String source;
	private String des;

	private SourceEnum(String source, String des) {
		this.source = source;
		this.des = des;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getDes() {
		return des;
	}

	public void setDes(String des) {
		this.des = des;
	}

}