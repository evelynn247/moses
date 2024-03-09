package com.biyao.moses.common.enums;

/**
* @Description source和spm模块对应关系
* @date 2019年7月23日下午4:08:18
* @version V1.0 
* @author 邹立强 (zouliqiang@idstaff.com)
 */
public enum SourceSpmEnum {

	HP("HP", "feeds"), S1("614S1", "feeds");

	private String source;
	private String spmModle;

	private SourceSpmEnum(String source, String spmModle) {
		this.source = source;
		this.spmModle = spmModle;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getSpmModle() {
		return spmModle;
	}

	public void setSpmModle(String spmModle) {
		this.spmModle = spmModle;
	}
	
	/**
	* @Description 通过source获取 spmModle
	* @param source
	* @return String 
	* @version V1.0
	* @auth 邹立强 (zouliqiang@idstaff.com)
	 */
	public static String getSourceSpmModle(String source) {
		for (SourceSpmEnum sourceSpmEnum : SourceSpmEnum.values()) {
			if (sourceSpmEnum.getSource().equalsIgnoreCase(source)) {
				return sourceSpmEnum.getSpmModle();
			}
		}
		return null;
	}
}