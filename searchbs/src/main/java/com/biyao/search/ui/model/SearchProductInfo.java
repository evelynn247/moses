package com.biyao.search.ui.model;

import com.biyao.client.model.SuProduct;
import lombok.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * @author zhaiweixi@idstaff.com
 * @date 2019/9/5
 **/
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchProductInfo {
    /**
     * 商品ID
     */
    private Long productId;
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
     * 长方形入口图
     */
    private String rectPortalImg;
    /**
     * 长方形入口图webp格式
     */
    private String rectPortalImgWebp;
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
    private Long suId;
    /**
     * 最短销售周期
     */
    private Integer minDuration;
    /**
     * SPU价格
     */
    private Long price;
    /**
     * 是否一起拼商品
     */
    private Byte isToggroupProduct;
    /**
     * 评论数
     */
    private Integer commentNum;
    /**
     * 前台一级类目名称集合
     */
    private List<String> fCategory1Names;
    /**
     * 前台一级类目ID集合
     */
    private List<String> fCategory1Ids;
    /**
     * 前台二级类目名称集合
     */
    private List<String> fCategory2Names;
    /**
     * 前台二级类目ID集合
     */
    private List<String> fCategory2Ids;
    /**
     * 前台三级类目名称集合
     */
    private List<String> fCategory3Names;
    /**
     * 前台三级级类目ID集合
     */
    private List<String> fCategory3Ids;
    /**
     * 前台三级类目卖点
     */
    private List<String> fCategory3SalePoint;
    /**
     * 搜索标签
     */
    private List<String> searchLabels;
    /**
     * 商品7日销量
     */
    private Long salesVolume7;
    /**
     * 商品总销量
     */
    private Long salesVolume;

    /**
     * 是否支持贴图
     */
    private Byte supportTexture;

    /**
     * 是否支持全民拼(0 不支持 1 支持)
     */
    private Byte allTogether = 0;

    /**
     * 支持售卖平台
     */
    private String supportPlatform;

    /**
     * 是否支持签名(0 不支持 1 支持)
     */
    private Byte supportCarve = 0;

    /**
     * 低模商品类型：0-普通低模商品；1-眼镜低模商品
     */
    private Byte rasterType = 0;

    /**
     * 是否支持阶梯团(0 不支持 1 支持)
     */
    private Integer isLaddergroupProduct;

    /**
     * 最低商品拼团价格
     */
    private Long groupPrice;

    /**
     * 好评
     */
    private Integer positiveComment;

    /**
     * 新用户特权金限额
     */
    private BigDecimal newPrivilateLimit;

    /**
     * 老客特权金限额
     */
    private BigDecimal oldPrivilateLimit;

    /**
     * 是否支持老客特权金 0:不支持，1:支持
     */
    private Integer oldUserPrivilege;

    /**
     * 是否支持新客特权金 0:不支持，1:支持
     */
    private Integer newUserPrivilege;

    /**
     * 所有人可见好评（包含默认好评）
     */
    private Integer goodCommentToAll;

    /**
     * 是否设置黄金尺码
     */
    private Integer isSetGoldenSize;

    /**
     * 黄金尺码su集合
     */
    private List<SuProduct> goldenSizeSu;

    /**
     * su集合
     */
    private List<SuProduct> suProductList;

    /**
     * 黄金尺码
     */
    private Set<String> goldenSizeSet;

    /**
     * Facet
     */
    private String productFacet;

    /**
     *  新增一个字段表示商品是否支持津贴抵扣，1：支持，0：不支持
     */
    private Integer isAllowance = 0;
    /**
     *  津贴抵扣金额，默认值为0
     */
    private BigDecimal allowancePrice = BigDecimal.ZERO;



    /**
     *  造物标识 1：是必要造物商品  0：不是必要造物商品
     */
    private Byte isCreation;

    /**
     *  造物价
     */
    private String creationPriceStr;

}
