package com.biyao.moses.util;

import com.biyao.search.common.enums.PlatformEnum;

/**
 * PlatformEnumUtil
 * 
 * @Description 
 * @Date 2018年9月27日
 */
public class PlatformEnumUtil {

	public static PlatformEnum getPlatformEnumBySiteId(Integer siteId) {
		PlatformEnum[] platformEnums = PlatformEnum.values();
		for (PlatformEnum platformEnum : platformEnums) {
			if (platformEnum.getNum().equals(siteId)) {
				return platformEnum;
			}
		}
		return PlatformEnum.M;
	}

}
