package com.biyao.moses.common.enums;

public enum SeasonEnum {
    SPRING(1,"春"),SUMMER(2,"夏"),AUTUMN(4,"秋"),WINTER(8,"冬"),COMMON(15,"四季");

    private Integer id;
    private String name;

    SeasonEnum(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
