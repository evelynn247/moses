package com.biyao.search.ui.model;

import lombok.*;

/**
 * @author zj
 * @version 1.0
 * @date 2019/10/21 14:38
 * @description
 */
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchSuProductInfo {

    private java.lang.Long suId;
    private java.lang.Long productId;
    private java.lang.String squarePortalImg;
    private java.lang.String squarePortalImgWebp;
    private java.lang.Long price;
    private java.lang.Long groupPrice;
    private java.lang.Long supplierDiscountPrice;
    private java.lang.Long platformDiscountPrice;
    private java.math.BigDecimal novicePrice;
    private java.math.BigDecimal newPrivilegeLimit;
    private java.math.BigDecimal oldRivilegeLimit;
    private java.lang.Integer duration;
    private java.lang.String tags;
    private java.lang.Integer onSale;
    private java.lang.Integer isGoldenSize;
    private java.lang.Integer saleVolume7;
    /**
     * 尺寸
     */
    private String size;

    /**
     * 颜色
     */
    private String color;


    /**
     * SKU造物价
     */
    private String creationPriceStr;


}
