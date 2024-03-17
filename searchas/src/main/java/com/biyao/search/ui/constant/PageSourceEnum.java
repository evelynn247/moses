package com.biyao.search.ui.constant;

public enum PageSourceEnum {

	HOME_PAGE("10001","1","首页"),
	TYPE_PAGE("10002","1","分类页"),
	CUSTOM_PAGE("10003","2/3","定制频道"),
	TOGETHER_GROUP_CHANNEL("10004","1","一起拼频道页"),
	JOINGROUP("10005","1","一起拼参团落地页"),
	JTT_JOINGROUP("10006","1","阶梯团参团落地页"),
	TQJ_SEND_SUCCESS("10007","1","特权金下发成功页"),
	ALLTOGETHER_GROUP_CHANNEL("10008","1","全民拼频道页");

	private String sourceId;
	private String typeId;
	private String name;
	private PageSourceEnum(String sourceId, String typeId, String name) {
		this.sourceId = sourceId;
		this.typeId = typeId;
		this.name = name;
	}
	public String getSourceId() {
		return sourceId;
	}
	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}
	public String getTypeId() {
		return typeId;
	}
	public void setTypeId(String typeId) {
		this.typeId = typeId;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
}