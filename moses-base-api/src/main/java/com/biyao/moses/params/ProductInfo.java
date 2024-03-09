package com.biyao.moses.params;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 商品信息
 *
 * @author wangbo
 * @version 1.0 2018/6/4
 */
@Data
@NoArgsConstructor
public class ProductInfo implements Serializable {

    private static final long serialVersionUID = 2101417280079964962L;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 商家ID
     */
    private Long supplierId;
    /**
     * 商品支持的渠道
     */
    private Set<String> supportChannel;

    /**
     * 后台一级类目id
     */
    private Long firstCategoryId;

    /**
     * 后台一级类目名称
     */
    private String firstCategoryName;

    /**
     * 后台二级类目id
     */
    private Long secondCategoryId;

    /**
     * 后台二级类目名称
     */
    private String secondCategoryName;

    /**
     * 后台三级类目id
     */
    private Long thirdCategoryId;

    /**
     * 后台三级类目名称
     */
    private String thirdCategoryName;

    /**
     * 首次上架时间
     */
    private Date firstOnshelfTime;

    /**
     * 正方形入口图片
     */
    private String squarePortalImg;

    /**
     * 正方形入口图片webp格式
     */
    private String squarePortalImgWebp;

    /**
     * 长方形入口图片
     */
    private String rectPortalImg;

    /**
     * 长方形入口图片webp格式
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
     * 长标题
     */
    private String title;

    /**
     * 上下架状态1-上架，0-未上架
     */
    private Byte shelfStatus;

    /**
     * 商品制造商背景
     */
    private String supplierBackground;

    /**
     * 商品模型0-无模 1-低模
     */
    private Byte rasterProduct;
    /**
     * 商品支持的活动类型
     */
    private String productPool="";

    /**
     * 商品基本属性
     */
//    private Map<String, List<String>> baseAttribute;

    /**
     * 商品筛选属性
     */
//    private Map<String, List<String>> filterAttribute;

    /**
     * 商品规格
     */
//    private Map<String, List<String>> productSpec;

    /**
     * 商家名称
     */
//    private String supplierName;

    /**
     * 店铺名称
     */
//    private String storeName;

    /**
     * 最低价格suid
     */
    private Long suId;

    /**
     * sku最小生产周期
     */
    private Integer minDuration;

    /**
     * sku最低价格(分)
     */
    private Long price;

    /**
     * 是否正在拼团商品1-是 0-不是
     */
//    private Byte isGroupProduct;

    /**
     * 最低商品拼团价格
     */
    private Long groupPrice;

    /**
     * 是否一起拼 1-是 0-不是
     */
    private Byte isToggroupProduct;

    /**
     * 商品评论数
     */
    private Integer commentNum;

    /**
     * 有图评论数
     */
    private Integer imgCount;

    /**
     * 差评
     */
    private Integer negativeComment;

    /**
     * 中评
     */
    private Integer neutralComment;

    /**
     * 好评
     */
    private Integer positiveComment;

    /**
     * 商品打分
     */
    private Double productScore;

    /**
     * 前台一级类目名称列表
     */
    private List<String> fCategory1Names;

    /**
     * 前台一级类目id列表
     */
    private List<String> fCategory1Ids;

    /**
     * 前台二级类目名称列表
     */
    private List<String> fCategory2Names;

    /**
     * 前台二级类目id列表
     */
    private List<String> fCategory2Ids;

    /**
     * 前台三级类目名称列表
     */
    private List<String> fCategory3Names;

    /**
     * 前台三级类目id列表
     */
    private List<String> fCategory3Ids;

    /**
     * 前台三级类目卖点
     */
//    private List<String> fCategory3SalePoint;

    /**
     * 搜索标签
     */
    private List<String> searchLabels;

    /**
     * 商品适用性别 0-未知1-男性2-女性3-两性
     */
    private Byte productGender;

    /**
     * 该商品计算得到hash值
     */
//    private Integer hashValue;

    /**
     * 7日销量
     */
    private Long salesVolume7;

    /**
     * 是否支持特权金
     */
//    private Integer supportPrivilegeAmount;


    /**
     * 是否支持老客特权金 0:不支持，1:支持
     */
    private Integer oldUserPrivilege;
    /**
     * 是否支持新客特权金 0:不支持，1:支持
     */
    private Integer newUserPrivilege;

    private String supportPlatform;


    //新用户特权金限额
    private BigDecimal newPrivilateLimit;
    //老客特权金限额
    private BigDecimal oldPrivilateLimit;

    private Integer isLaddergroupProduct;

//    private String scmIds;
    //spu下sku，及sku的标准销售属性
//    private Map<String, Map<String, String>> skuStdSaleAttrs;
    //spu的标准销售属性聚合
    private Map<String, Set<String>> spuStdSaleAttrs;
    /**
     * 平台核心转化提升V1.7新增新手特权金抵扣金额
     */
    private BigDecimal newPrivilateDeduct;

    private BigDecimal novicePrice = BigDecimal.ZERO;

    /**
     * 是否支持签名(0 不支持 1 支持)
     */
    private Byte supportCarve = 0;

    /**
     * 低模商品类型：0-普通低模商品；1-眼镜低模商品
     */
    private Byte rasterType = 0;

    /**
     * 对所有人可见的好评数(包含默认好评数)
     */
    private Integer goodCommentAll;

    /**
     * 该spu是否设置了黄金尺码 0:无 1:有
     */
    private Integer isSetGoldenSize;
    /**
     * 可售黄金尺码su
     */
    private List<SuProductInfo> goldenSizeSu;
    /**
     * su集合
     */
    private List<SuProductInfo> suProductList;

    /**
     * product.facet   {
     * “活动"：["一起拼", “特权金”]，
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
    private String productFacet;

    /**
     * 可售黄金尺码size集合
     */
    private Set<String> goldenSizeSet;

    /**
     * 总销量
     */
    private Long salesVolume;


    /**
     * 是否是小蜜蜂商家商品 0 否 1 是
     */
    private Integer isBee = 0;

    /**
     * spu标签id，多个以逗号分隔
     * 例如：222,333,444
     */
    private String tagsId = "";

    //一起拼平台优惠金额
//    private Long platformDiscountPrice;

    //一起拼商家优惠金额
//    private Long supplierDiscountPrice;

    // 增加是否支持贴图 zhaiweixi 20190418
    private Byte supportTexture;

    //是否支持全民拼
//    private Byte allTogether = 0;
    /**
     * 退货退款率 精确到0.01
     */
    private Double returnRefundRate;
    /**
     * 是否为必要造物商品 0 =false 1 =true
     */
    private Byte isCreator;
}