package com.biyao.moses.common.enums;

/**
 * @author zhaiweixi@idstaff.com
 * @date 2019/7/23
 **/
public enum ProductFeedEnum {
    /**
     * 首页feed流match
     */
    HOME("home", "首页feed流");

    /**
     * match名称
     */
    private String name;
    /**
     * match描述
     */
    private String desc;

    ProductFeedEnum(String name, String desc) {
        this.name = name;
        this.desc = desc;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
