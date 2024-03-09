package com.biyao.moses.params;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class SuProductInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long suId;// sku id
    private Long productId;//spu id
    private String squarePortalImg;    //正方形入口图
    private String squarePortalImgWebp;    //正方形入口图webp格式
    private Long price = 0L;    //售价
    private Long groupPrice = 0L;//一起拼拼团价格
//    private Long supplierDiscountPrice = 0L;//一起拼商家优惠价格
//    private Long platformDiscountPrice = 0L;//一起拼平台优惠价格
    private BigDecimal novicePrice = BigDecimal.ZERO; //新手专享价格
    private BigDecimal newPrivilegeLimit = BigDecimal.ZERO; //新用户特权金限额
    private BigDecimal oldRivilegeLimit = BigDecimal.ZERO;//老客特权金限额
    private Integer duration = 0;    //生产周期
    private String facet;//JSON格式，包括标准颜色销售属性信息、标准尺码销售属性
    private String tags;    //一起拼、全民拼、特权金标签信息，多个以逗号分隔
    private Integer onSale = 0;    //是否在售，0：不在售，1：在售
    private Integer isGoldenSize = 0;//是否是黄金尺码 0：不是 1：是
    private Integer saleVolume7 = 0;   //7天销量，不足7天则按时间天数

    /**
     * su.facet   {
     * “尺码”:["35", "36",“M”, "L"],
     * "颜色"：["黑色"、“粉丝”、“枣红色”]，
     * “眼镜度数”：[“远视725度”，“远视775度”，“近视700度”] ，
     * "瞳距":["45","46","47","48"]，
     * “左眼散光”：["散光100度，“左眼散光无"]，
     * “右眼散光”：["散光100度，“左眼散光无"]，
     * "镜片功能"：["抗蓝防紫"，"感光变色镜片"],
     * "折射率"：["1.76","1.67"]
     * }
     */
    private String suFacet ;

}
