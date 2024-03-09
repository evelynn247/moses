package com.biyao.moses.constants;

/**
 * 
 * @Description
 * @Date 2018年9月27日
 */
public enum EXPType {

	BID("BID实验", "1"), TID("TID实验", "2");

	private String name;
	private String type;

	EXPType(String name, String type) {
		this.name = name;
		this.type = type;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

}
