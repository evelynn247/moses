package com.biyao.search.ui.rest.response;

public class SearchOrderBy {
	/**
	 * 排序类型  oneWay-单向  twoWay-双向
	 */
	private String type;
	
	/**
	 * 排序类型描述
	 */
	private String desc;
	
	/**
	 * 升序code
	 */
	private String ascCode;
	
	/**
	 * 降序code
	 */
	private String descCode;
	
	public SearchOrderBy() {
	}
	
	public SearchOrderBy(String type, String desc, String ascCode,
			String descCode) {
		super();
		this.type = type;
		this.desc = desc;
		this.ascCode = ascCode;
		this.descCode = descCode;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public String getAscCode() {
		return ascCode;
	}

	public void setAscCode(String ascCode) {
		this.ascCode = ascCode;
	}

	public String getDescCode() {
		return descCode;
	}

	public void setDescCode(String descCode) {
		this.descCode = descCode;
	}
	
}
