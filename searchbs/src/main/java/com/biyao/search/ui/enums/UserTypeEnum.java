package com.biyao.search.ui.enums;


import java.util.Arrays;


public enum UserTypeEnum {

    NEW_VISITOR(1, "新访客"), OLD_VISITOR(2, "老访客"), ALL_NEW_USER(3, "全部新用户");

    private Integer code;
    private String desc;

    UserTypeEnum (Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static UserTypeEnum getByCode(Integer code) {
        return Arrays.stream(values()).filter(v -> v.code.equals(code)).findFirst().orElse(null);
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
