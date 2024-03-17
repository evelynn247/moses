package com.biyao.search.ui.enums;

/**
 * Author:doujiale
 * Date: 2018/8/18-15:55
 * Description: PlatForm标识
 */
public enum EnumPlatFormType {

    ANDRIOD("android",9),
    IOS("iOS",7),
    IOS_LOGGED_ON("ios",7),
    PC("pc",3),
    M("mweb",1),
    MINI("mini",2);

    EnumPlatFormType(String description,int code) {
        this.code = code;
        this.name = description;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private Integer code;
    private String name;


    public static int getCodeByName(String name) {
        for (EnumPlatFormType enu : EnumPlatFormType.values()) {
            int c = enu.getCode();
            if (name.equals(enu.getName())) {
                return c;
            }
        }
        return 0;
    }

}
