package com.biyao.moses.Enum;

import com.alibaba.dubbo.common.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * PlatformEnum
 * 
 * @Description
 * @Date 2018年9月27日
 */
public enum PlatformEnum {

	ANDROID("android", 9,(byte)1),
	IOS("ios", 7,(byte)1),
	PC("pc", 3,(byte)1),
	M("mweb", 1,(byte)1) ,
	MINI("miniapp", 2,(byte)1),
	BYFX("byfx", 4,(byte)2),
	BYMANAGER("byManager", 5,(byte)4);
	private String name;
	private Integer num;
	private byte channelTpye;


	/**
	 * @Des 根据分端id 查询渠道类型
	 * @Param [num]
	 * @return java.lang.String
	 * @Author changxiaowei
	 * @Date  2022/3/28
	 */
	public static byte getChannelTypeBySiteId(Integer siteId){
		if(Objects.isNull(siteId)){
			return ANDROID.channelTpye;
		}
		for (PlatformEnum platformEnum : PlatformEnum.values()) {
			if(platformEnum.num.equals(siteId)){
				return platformEnum.getChannelTpye();
			}
		}
		return ANDROID.channelTpye;
	}

	PlatformEnum(String name, Integer num,Byte channelTpye) {
		this.name = name;
		this.num = num;
		this.channelTpye = channelTpye;
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
	public byte getChannelTpye() {
		return channelTpye;
	}
	public void setChannelTpye(byte channelTpye) {
		this.channelTpye = channelTpye;
	}
}
