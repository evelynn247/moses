package com.biyao.moses.common.enums;

public enum RedisKeyTypeEnum {
    STRING(1, "String类型"),
    HASH(2, "Hash类型");

    private Integer id;
    private String des;

    private RedisKeyTypeEnum(Integer id, String des) {
        this.id = id;
        this.des = des;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getDes() {
        return des;
    }

    public void setDes(String des) {
        this.des = des;
    }
}
