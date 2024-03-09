package com.biyao.moses.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 通用特权金单排feed流模版
 */
@Setter
@Getter
public class SingleCommonPrivilegeTemplageInfo extends ProductTemplateInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer goodAppraise; //好评数

    private Integer priCouponAmount; //特权金抵扣金额

    private String imageWebp ; //webp格式正方形入口图
}
