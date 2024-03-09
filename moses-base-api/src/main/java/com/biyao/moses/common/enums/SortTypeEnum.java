package com.biyao.moses.common.enums;

public enum SortTypeEnum {

	ALL("0", "综合","sssr"), SALE("1", "销量","saleRank"), PRICE("2", "价格","priceRank"),PRIVILEGE("3","特权金","privilegeRank"),NOVICEPRICERANK("4","新手专享价格","NovicePriceRank"),ONSHELFTIMERANK("5","上新时间","OnShelfTimeRank");

	private String type;
	private String des;
	private String rankName;

	private SortTypeEnum(String type, String des,String rankName) {
		this.type = type;
		this.des = des;
		this.rankName = rankName;
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

	public String getRankName() {
		return rankName;
	}

	public void setRankName(String rankName) {
		this.rankName = rankName;
	}
	
	public static SortTypeEnum getSortTypeEnumByType(String type) {
		for (SortTypeEnum sortTypeEnum : values()) {
			if (sortTypeEnum.getType().equals(type)) {
				return sortTypeEnum;
			}
		}
		return null;
	}
	
}