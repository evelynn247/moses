package com.biyao.search.ui.enums;

/**
 * @Description: 新品标签枚举类
 * @Date: 2020/6/30
 * @Author: xiafang
 */
public enum NewShowIconEnum {
    NEW_ICON_SHOW(1,"展示新品标签"),
    NEW_ICON_NOT_SHOW(0,"不展示新品标签");

    private Integer code;
    private String desc;

    NewShowIconEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
    public Integer getCode() {
        return code;
    }
}
