package com.biyao.moses.Enum;

/**
 * @program: moses-parent-online
 * @description: 消息类型枚举类
 * @author: changxiaowei
 * @create: 2021-12-03 14:58
 **/
public enum EMqLogType {
    /**
     * 商品
     */
    PRODUCT(0),
    ALLOWANCE(110);
    private int type;

     EMqLogType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    /**
     * 使用type获取枚举
     *
     * @param typeValue
     * @return
     */
    public EMqLogType valueOf(int typeValue) {
        for (EMqLogType type : EMqLogType.values()) {
            if(type.getType() == typeValue) {
                return type;
            }
        }
        return null;
    }
}
