package com.biyao.moses.common.constant;

/**
 * 实验条件常量
 */
public interface ExpConditionConstants {
    /**
     * 老客且最近有来访
     */
    String COND_OLD_BUYER_LAST_VISIT = "oblv";
    /**
     * 老访客
     */
    String COND_OLD_VISITER = "oldv";
    /**
     * 老客条件
     */
    String COND_CUSTOMER = "customer";
    /**
     * 新访客条件
     */
    String COND_NEW_VISITER = "newv";
    /**
     * 是否满足前端页面ID条件
     * 此条件支持前端页面级别的实验
     */
    String COND_FRONT_PAGEID = "fpageId";
}
