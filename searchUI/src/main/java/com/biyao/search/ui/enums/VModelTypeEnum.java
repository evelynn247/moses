package com.biyao.search.ui.enums;

/**
 * date: 2019/12/2
 * @author xiafang
 */
public enum VModelTypeEnum {
    DAV("2","大V用户"),
    ENTERPRISE("3","企业定制用户"),
    PLATFORM("5","平台号");

    private String code;
    private String desc;

    VModelTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
    public String getCode() {
        return code;
    }
}
