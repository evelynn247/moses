package com.biyao.search.ui.enums;

/**
 * @ClassName: EnumSymbolType
 * @Description: 比较符号枚举类
 * @author yangy
 * @date 18:16 2018/3/21
 */
public enum EnumSymbolType {

    BIG(1,">"),
    BIG_OR_EQUAL(2,">="),
    EQUAL(3,"="),
    LESS(4,"<"),
    LESS_OR_EQUAL(5,"<=")
    ;

    EnumSymbolType(Integer code, String name){
        this.code = code;
        this.name = name;
    }

    private Integer code;
    private String name;

    public Integer getCode() {
        return code;
    }
    public void setCode(Integer code) {
        this.code = code;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public static EnumSymbolType getEnumByCode(Integer code) {
        for (EnumSymbolType enu : EnumSymbolType.values()) {
            if (enu.getCode().intValue() == code.intValue()) {
                return enu;
            }
        }
        return null;
    }

    public static EnumSymbolType getEnumByName(String name) {
        for (EnumSymbolType enu : EnumSymbolType.values()) {
            Integer c = enu.getCode();
            if(name.equals(EnumSymbolType.getEnumByCode(c).getName())){
                return enu;
            }
        }
        return null;
    }

}
