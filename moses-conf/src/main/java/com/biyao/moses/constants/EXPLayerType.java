package com.biyao.moses.constants;

/**
 * 
 * @Description
 * @Date 2018年9月27日
 */
public enum EXPLayerType {

	MATCH("match", "1"), RANK("rank", "2");

	private String name;
	private String num;

	EXPLayerType(String name, String num) {
		this.name = name;
		this.num = num;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getNum() {
		return this.num;
	}

	public void setNum(String num) {
		this.num = num;
	}

	public static String getValueByName(String name) {
		EXPLayerType[] var1 = values();
		int var2 = var1.length;

		for (int var3 = 0; var3 < var2; ++var3) {
			EXPLayerType platform = var1[var3];
			if (platform.getName().equals(name)) {
				return platform.getNum();
			}
		}

		return null;
	}
}
