package com.biyao.moses.common.enums;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * PlatformEnum
 * 
 * @Description
 * @Date 2018年9月27日
 */
public enum PlatformEnum {


	ANDROID("android", 9), IOS("ios", 7), PC("pc", 3), M("mweb", 1), MINI("miniapp", 2),BYFX("byfx", 4),BYMANAGER("bymanager", 5);
	private String name;
	private Integer num;

	private final static  String BYFXID ="2";
	private final static  String BYMANAGERID ="4";
	private final static  String BYMAINID ="1";
	/**
	 * @Des 根据分端id 查询渠道类型
	 * @Param [num]
	 * @return java.lang.String
	 * @Author changxiaowei
	 * @Date  2022/3/28
	 */
	public static String getChannelTypeBySiteId(String siteId){
		if(StringUtils.isEmpty(siteId)){
			return BYMAINID;
		}
		siteId = siteId.trim();

		switch (siteId){
			case "4":
				return BYFXID;
			case "5":
				return BYMANAGERID;
			default:
				return BYMAINID;
		}
	}



	PlatformEnum(String name, Integer num) {
		this.name = name;
		this.num = num;
	}
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getNum() {
		return num;
	}

	public void setNum(Integer num) {
		this.num = num;
	}

	public static boolean containName(String name) {
		for (PlatformEnum platform : PlatformEnum.values()) {
			if (platform.getName().equals(name)) {
				return true;
			}
		}
		return false;
	}

	public static Integer getValueByName(String name) {
		for (PlatformEnum platform : PlatformEnum.values()) {
			if (platform.getName().equals(name)) {
				return platform.getNum();
			}
		}
		return null;
	}
	
	public static List<String> getPlatformName(){
		List<String> list=new ArrayList<String>();
		for (PlatformEnum platform : PlatformEnum.values()) {
			list.add(platform.getName());
		}
		return list;
	}
}
