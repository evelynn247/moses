package com.biyao.moses.common.enums;

public enum OpenSourceEnum {

	HP("hp", "首页页面"), HR("hr", "首页下面推荐入口"),HPTOGETHER("hptogether","首页新客一起拼");

	private String source;
	private String des;

	private OpenSourceEnum(String source, String des) {
		this.source = source;
		this.des = des;
	}

	public static boolean getOpenPage(String source) {
		boolean flag=false;
		for (OpenSourceEnum semum : values()) {
			if(semum.getSource().equals(source.toLowerCase())) {
				flag= true;
				break;
			} 
		}
		return flag;
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