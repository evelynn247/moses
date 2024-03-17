package com.biyao.search.ui.model;

import lombok.Data;

import java.util.Date;

/**
 * @author zj
 * @version 1.0
 * @date 2020/1/21 14:18
 * @description
 */
@Data
public class DeriveProductInfo {

    /**
     * 商品ID
     */
    private String productId;
    /**
     * 商家ID
     */
    private Long supplierId;
    /**
     * 一级类目ID
     */
    private Long firstCategoryId;
    /**
     * 一级类目名称
     */
    private String firstCategoryName;
    /**
     * 二级类目ID
     */
    private Long secondCategoryId;
    /**
     * 二级类目名称
     */
    private String secondCategoryName;
    /**
     * 三级类目ID
     */
    private Long thirdCategoryId;
    /**
     * 三级类目名称
     */
    private String thirdCategoryName;
    /**
     * 首次上架时间
     */
    private Date firstOnShelfTime;
    /**
     * 方形入口图
     */
    private String squarePortalImg;
    /**
     * 方形入口图webp格式
     */
    private String squarePortalImgWebp;
    /**
     * 卖点
     */
    private String salePoint;
    /**
     * 短标题
     */
    private String shortTitle;
    /**
     * 标题
     */
    private String title;
    /**
     * 在售状态
     */
    private Byte shelfStatus;
    /**
     * 制造商背景
     */
    private String supplierBackground;
    /**
     * 商家名称
     */
    private String supplierName;
    /**
     * 店铺名称
     */
    private String storeName;
    /**
     * 入口suID
     */
    private String suId;
    /**
     * 最短销售周期
     */
    private Integer minDuration;
    /**
     * SPU价格
     */
    private Long price;
    /**
     * 增值服务费
     */
    private Long deriveAddvalTotalPrice;
    /**
     * 是否一起拼商品
     */
    private Byte isToggroupProduct;
    /**
     * 前台一级类目名称集合
     */
    private String fCategory1Names;
    /**
     * 前台一级类目ID集合
     */
    private String fCategory1Ids;
    /**
     * 前台二级类目名称集合
     */
    private String fCategory2Names;
    /**
     * 前台二级类目ID集合
     */
    private String fCategory2Ids;
    /**
     * 前台三级类目名称集合
     */
    private String fCategory3Names;
    /**
     * 前台三级级类目ID集合
     */
    private String fCategory3Ids;
    /**
     * 前台三级类目卖点
     */
    private String fCategory3SalePoint;
    /**
     * 商品7日销量
     */
    private Long salesVolume7;
    /**
     * 商品总销量
     */
    private Long salesVolume;
    /**
     * 支持售卖平台
     */
    private String supportPlatform;
    /**
     * 最低商品拼团价格
     */
    private Long groupPrice;
    /**
     * 衍生商品所属者uid
     */
    private Long createUid;
    /**
     *  身份信息
     */
    private SaleAgent saleAgent;
}
