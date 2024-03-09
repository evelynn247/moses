package com.biyao.moses.Enum;

public enum SeasonEnum {
    SPRING(1,"春"),SUMMER(2,"夏"),AUTUMN(3,"秋"),WINTER(4,"冬"),COMMON(0,"四季");

    private Integer id;
    private String name;

    SeasonEnum(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    public static int getSeasonIdByName(String name){
        for (SeasonEnum value : SeasonEnum.values()) {
            if(value.name.equals(name)){
                return value.id;
            }
        }
        return 0;
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
