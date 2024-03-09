package com.biyao.moses.common.enums;

public enum SortEnum {

	DES("1", "降序"), ASC("0", "升序");

	private String type;
	private String des;

	private SortEnum(String type, String des) {
		this.type = type;
		this.des = des;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getDes() {
		return des;
	}

	public void setDes(String des) {
		this.des = des;
	}
	
}