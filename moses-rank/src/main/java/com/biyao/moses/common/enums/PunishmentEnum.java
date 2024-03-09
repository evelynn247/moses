package com.biyao.moses.common.enums;

import org.apache.commons.lang3.StringUtils;

/**
 * @program: moses-parent
 * @description: 惩罚因子枚举类
 * @author: changxiaowei
 * @create: 2021-03-25 17:08
 **/
public enum PunishmentEnum {

    ProductExposurePunishment("1","productExposurePunishmentImpl"),

    ProductRefundRatePunishment("2","productRefundRatePunishmentImpl"),

    ProductSizePunishment("3","productSizePunishmentImpl"),

    ProductPriceFactor("4","productPriceFactorImpl"),
    // 特权金因子 必要造物项目新增
    ProductTQJFactor("5","productTQJFactorImpl"),
    // 视频因子 内容策略项目新增
    ProductVideoFactor("6","productVideoFactorImpl"),
    ;

    private String punishmentId;

    private String beanName;

    PunishmentEnum(String id, String name){
        this.beanName = name;
        this.punishmentId = id;
    }


    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    public String getId() {
        return punishmentId;
    }

    public void setId(String id) {
        this.punishmentId = id;
    }


    public static PunishmentEnum getByPunishmentId(String punishmentId){
        if(StringUtils.isBlank(punishmentId)){
            return null;
        }
        PunishmentEnum[] enums = values();
        for(PunishmentEnum punishmentEnum : enums){
            if(punishmentId.equals(punishmentEnum.getId())){
                return punishmentEnum;
            }
        }
        return null;
    }
}
