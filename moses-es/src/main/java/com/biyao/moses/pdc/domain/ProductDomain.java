package com.biyao.moses.pdc.domain;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @program: moses-parent-online
 * @description:
 * @author: changxiaowei
 * @Date: 2021-12-02 16:46
 **/
@Data
public class ProductDomain {
    /**
     * ===================== 来自redis =============================
     */
    /**
     * 商品性别 中性 = 2， 女性=1，男性=0，未知=-1
     */
    private Byte sex;
    /**
     * 商品季节 SPRING(1,"春"),SUMMER(2,"夏"),AUTUMN(3,"秋"),WINTER(4,"冬"),COMMON(0,"四季");
     */
    private Byte season;
    /**
     * 热门分
     */
    private double hotScore;
    /**
     * 是否支持新房客新手专享
     */
    private Byte isNewUser;
    /**
     * 是否有视频
     */
    private Byte isVideo;

    /**
     * ===================== 来自数据库 =============================
     */
    /**
     * 前台一级类目id
     */
    private String fcategory1Ids;
    /**
     * 前台三级类目id
     */
    private String fcategory3Ids;
    /**
     * 商品id
     */
    private Long productId;
    /**
     * 上下架状态 0 下架 1上架
     */
    private Byte shelfStatus ;
    /**
     * 是否可售 0 下架 1上架
     */
    private Byte showStatus ;
    /**
     * 后台三级类目id
     */
    private Long thirdCategoryId;
    /**
     * 后台二级类目id
     */
    private Long secondCategoryId;
    /**
     * 首次上架时间
     */
    private Date firstOnshelfTime;
    /**
     * 支持的平台
     */
    private String supportPlatform;
    /**
     * 支持的渠道
     */
    private String supportChannel;
    /**
     * 支持的活动
     */
    private String supportAct;
    /**
     * 短标题
     */
    private String shortTitle;
    /**
     * 是否为必要造物商品
     */
    private Byte isCreator;
    /**
     * 是否支持新客特权金 0:不支持，1:支持
     */
    private Byte newUserPrivilege;
    /**
     * 平台核心转化提升V1.7新增新手特权金抵扣金额
     */
    private BigDecimal newPrivilateDeduct = BigDecimal.ZERO;
    /**
     * 是否为定制商品 0 不支持 1 支持 2 定制咖啡
     */
    private Byte supportTexture;
    /**
     * 标签
     */
    private String tagsId;
    /**
     * 是否支持一起拼
     */
    private Byte isToggroupProduct;
}
