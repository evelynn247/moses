package com.biyao.search.ui.enums;


import java.util.Arrays;

/**
 * @Auther: sunbaokui
 * @Date: 2019/5/22 14:25
 * @Description: 访客类型
 */
public enum VisitorTypeEnum {

    NEW_VISITOR(0, "新访客"), OLD_VISITOR(1, "老访客"), OLD_USER(2, "老客");

    private Integer code;
    private String desc;

    VisitorTypeEnum (Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static VisitorTypeEnum getByCode(Integer code) {
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
