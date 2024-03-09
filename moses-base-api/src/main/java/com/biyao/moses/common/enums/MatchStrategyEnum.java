package com.biyao.moses.common.enums;

public enum MatchStrategyEnum {

    WEA("wea", "天气召回"),
    SEX("sex", "性别召回"),
    CATE("cate", "类目兴趣"),
    GZH_NEW("ghz_new", "高转化上新(轮播图)"),
    GZH_DJC_NEW("ghz_djc_new", "高转化低决策"),
    VIEW1("view1", "足迹1"),
    VIEW3("view3", "足迹3"),
    HS("hs", "热销召回"),
    NEW_CATE("new_cate", "兴趣上新"),
    NEW_LOW("new_low", "基础曝光上新"),
    ZJ2("zj2", "足迹2"),
    LD("ld", "低决策"),
    RAND("rand", "随机召回"),
    NO_SEX("no_sex", "无性别商品召回"),
    EXP("exp", "已曝光"),
    USER_BUY("user_buy", "用户购买行为召回");


    private String name;
    private String des;

    MatchStrategyEnum(String name, String des){
        this.name = name;
        this.des = des;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDes() {
        return des;
    }

    public void setDes(String des) {
        this.des = des;
    }
}
