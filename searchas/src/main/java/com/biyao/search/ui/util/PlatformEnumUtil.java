package com.biyao.search.ui.util;

import com.biyao.search.common.enums.PlatformEnum;

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
