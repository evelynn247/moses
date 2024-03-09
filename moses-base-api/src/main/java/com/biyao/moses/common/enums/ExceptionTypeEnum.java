package com.biyao.moses.common.enums;

/**
 * 异常类型枚举，定义异常信息
 */
public enum ExceptionTypeEnum {
    OLD_MATCH_EXCEPTION("1", "mosesui调用老系统match-/match发生异常"),
    NEW_MATCH_EXCEPTION("2", "mosesui调用新系统match-/productMatch发生异常"),
    MATCH_ONLINE_EXCEPTION("3", "mosesui调用在线召回match-/productMatch发生异常"),
    FEED_EXCEPTION("4", "feed流召回异常");

    private String id;

    public String getId() {
        return id;
    }

    public String getDesc() {
        return desc;
    }

    private String desc;

    ExceptionTypeEnum(String id, String desc){
        this.id = id;
        this.desc = desc;
    }
}
