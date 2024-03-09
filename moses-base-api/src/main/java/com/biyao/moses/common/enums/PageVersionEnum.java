package com.biyao.moses.common.enums;

/**
 * 页面版本枚举类
 */
public enum PageVersionEnum {
    PAGE_VERSION_ADVERT("1.0","新访转化专项V1.0-x元购物返现活动项目新增双排商品feed流+支持活动广告模板");

    private String version;
    private String desc;

    PageVersionEnum(String version, String desc){
        this.version = version;
        this.desc = desc;
    }

    public String getVersion() {
        return version;
    }

    public String getDesc() {
        return desc;
    }
}
