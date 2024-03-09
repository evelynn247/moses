package com.biyao.moses.Enum;

import lombok.Getter;

/**
 * @program: moses-parent-online
 * @description: 索引类型枚举类
 * @author: changxiaowei
 * @create: 2021-09-18 17:40
 **/
@Getter
public enum IndexTypeEnum {

    PRODUCT("product","普通商品");
    /**
     * 类型
     */
    String type;
    /**
     * 描述
     */
    String des;

    public String getType() {
        return type;
    }

    IndexTypeEnum(String type, String des) {
        this.type = type;
        this.des = des;
    }
}
