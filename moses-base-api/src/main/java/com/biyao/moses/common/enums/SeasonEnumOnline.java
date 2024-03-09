package com.biyao.moses.common.enums;

/**
 * @program: moses-parent
 * @description: 用户季节枚举
 * @author: changxiaowei
 * @Date: 2021-12-24 12:26
 **/
public enum  SeasonEnumOnline {

    SPRING((byte)1,"春"),SUMMER((byte)2,"夏"),AUTUMN((byte)3,"秋"),WINTER((byte)4,"冬"),UNKONW((byte)-1,"未知");

    private Byte id;
    private String name;


    public static Byte getSeasonIdByName(String name){
        for (SeasonEnumOnline value : SeasonEnumOnline.values()) {
            if (value.name.equals(name)){
                return value.getId();
            }
        }
        return UNKONW.getId();
    }


    SeasonEnumOnline(Byte id, String name) {
        this.id = id;
        this.name = name;
    }

    public Byte getId() {
        return id;
    }

    public void setId(Byte id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}