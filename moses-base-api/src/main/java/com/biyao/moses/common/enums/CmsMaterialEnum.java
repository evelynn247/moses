package com.biyao.moses.common.enums;

/**
 * @ClassName CmsMaterialEnum
 * @Description CMS素材ID枚举
 * @Author xiaojiankai
 * @Date 2020/3/5 20:13
 * @Version 1.0
 **/
public enum CmsMaterialEnum {
    ACTIVITY_ID(10810236L,"2","活动ID配置"),
    ACTIVITY_SHOW_POSITION(10810437L,"4","活动展示位置配置"),
    ACTIVITY_DOUBLE_ROW_PICTURE1(10810603L,"6","活动入口图1配置"),
    ACTIVITY_DOUBLE_ROW_PICTURE2(10810604L,"6","活动入口图2配置"),
    ACTIVITY_DOUBLE_ROW_PICTURE3(10810605L,"6","活动入口图3配置"),
    ACTIVITY_DOUBLE_ROW_PICTURE4(10810606L,"6","活动入口图4配置"),
    ACTIVITY_DOUBLE_ROW_PICTURE5(10810607L,"6","活动入口图5配置");

    private Long id;
    private String type;
    private String desc;

    CmsMaterialEnum(Long id, String type, String desc){
        this.id = id;
        this.type = type;
        this.desc = desc;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
