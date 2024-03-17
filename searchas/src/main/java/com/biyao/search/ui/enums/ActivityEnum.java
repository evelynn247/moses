package com.biyao.search.ui.enums;

/**
 * @Description: 活动类型
 * @Date: 2020/3/13
 * @Author: xiafang
 */
public enum ActivityEnum {
    NO_ACTIVITY(0,"无活动"),
    GROUP_BUY(1,"一起拼"),
    LADDER_GROUP(2,"阶梯团"),
    BUY2_RETURN_ALLOWANCE(3,"买二返一"),
    ALLOWANCE_DEDUCTION(4,"津贴抵扣"),
    RECOMMEND_SEARCH(5,"轮播图落地页搜索"),
    SUPPLIER_SEARCH(6,"商家店铺页搜本店"),
    SUPPLIER_ALL_SEARCH(7,"商家店铺页搜全站"),
    FRESH_CUST_PRIVILEGE_SEARCH(8,"新手特权金活动"),
    COMMON_CUST_PRIVILEGE_SEARCH(9,"通用特权金活动");

    private Integer code;
    private String desc;

    ActivityEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
    public Integer getCode() {
        return code;
    }

    /**
     * 该方法根据前端传递参数toActivity取值判断请求参加的活动类型
     *
     * @param toActivity 活动标识  newSearch接口入参的属性
     * @return
     */
    public static ActivityEnum judgeActivityType(Integer toActivity) {
        if (ActivityEnum.GROUP_BUY.getCode().equals(toActivity)) {
            return ActivityEnum.GROUP_BUY;
        }
        if (ActivityEnum.LADDER_GROUP.getCode().equals(toActivity)) {
            return ActivityEnum.LADDER_GROUP;
        }
        if (ActivityEnum.BUY2_RETURN_ALLOWANCE.getCode().equals(toActivity)) {
            return ActivityEnum.BUY2_RETURN_ALLOWANCE;
        }
        if(ActivityEnum.ALLOWANCE_DEDUCTION.getCode().equals(toActivity)){
            return ActivityEnum.ALLOWANCE_DEDUCTION;
        }
        if(ActivityEnum.RECOMMEND_SEARCH.getCode().equals(toActivity)){
            return ActivityEnum.RECOMMEND_SEARCH;
        }
        if(ActivityEnum.SUPPLIER_SEARCH.getCode().equals(toActivity)){
            return ActivityEnum.SUPPLIER_SEARCH;
        }
        if(ActivityEnum.SUPPLIER_ALL_SEARCH.getCode().equals(toActivity)){
            return ActivityEnum.SUPPLIER_ALL_SEARCH;
        }
        return ActivityEnum.NO_ACTIVITY;
    }
}
