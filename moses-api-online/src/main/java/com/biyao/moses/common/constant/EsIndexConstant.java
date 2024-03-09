package com.biyao.moses.common.constant;

/**
 * @program: moses-parent-online
 * @description:
 * @author: changxiaowei
 * @create: 2021-09-28 19:50
 **/
public class EsIndexConstant {
    /**
     * 商品spuId
     */
    public static final String PRODUCT_ID = "productId";
    /**
     * 首次上架时间
     */
    public static final String FIRST_SHELLF_TIME = "firstOnshelfTime";
    /**
     * 商品性别  中性 2，女性1，男性0，未知 -1
     */
    public static final String SEX = "sex";
    /**
     * 商品季节 春1 夏2 秋3 冬4 四季0
     */
    public static final String SEASON = "season";
    /**
     * 商品短标题
     */
    public static final String SHORT_TITLE = "shortTitle";
    /**
     * 商品热门分 算法刷入redis
     */
    public static final String HOT_SCORE = "hotscore";
    /**
     * itemcf （相似度）商品向量
     */
    public static final String ICF_VECTOR = "icfVector";
    /**
     * FM算法 商品向量
     */
    public static final String FM_VECTOR = "fmVector";
    /**
     * 上下架状态 1 上架  0下架
     */
    public static final String SHELF_STATUS = "shelfStatus";
    /**
     * 是否为造物商品 0 =false 1= true
     */
    public static final String IS_CREATOR = "isCreator";
    /**
     * 是否有视频 0 =false 1= true
     */
    public static final String IS_VIDEO = "isVideo";
    /**
     * 是否支持新客特权金 0 =false 1= true
     */
    public static final String NEW_PRIVILEGE = "newPrivilege";

    /**
     * 新客特权金抵扣金额
     */
    public static final String NEW_PRIVILATE_DEDUCT = "newPrivilateDeduct";

    /**
     * 是否为定制商品 0 不支持  1支持 2 定制咖啡
     */
    public static final String SUPPORT_TEXTURE = "supportTexture";
    /**
     * 商品支持的视频 支持的端 （1 必要商城 2 必要分销  3 鸿源分销 ）
     */
    public static final String VID_SUPPORT_PALTFORM = "vidSupportPlatform";
    /**
     * 商品可售渠道（1 必要商城 2 必要分销  3 鸿源分销 ）
     */
    public static final String SUPPORT_CHANNEL = "supportChannelType";
    /**
     * 展示状态 0 展示  1 不展示
     */
    public static final String SHOW_STATUS = "showStatus";
    /**
     * 商品支持的平台
     */
    public static final String SUPPORT_PLATFORM = "supportPlatform";
    /**
     * 商品支持的活动 1 为支持的分销
     */
    public static final String SUPPORT_ACT = "supportAct";
    /**
     * 是否为一起拼商品
     */
    public static final String IS_TOGGROUP = "isToggroupProduct";

    /**
     * 是否为新访客新手专享
     */
    public static final String NEWV_PRODUCT = "isNewVProduct";
    /**
     * 支持的满减券id
     */
    public static final String SUPPORT_MJQ = "supportFullReduce";
    /**
     * 支持的返现id
     */
    public static final String SUPPORT_FX= "supportRebate";
    /**
     * 后台三级类目
     */
    public static final String CATEGORY3ID ="category3Id";
    /**
     * 后台二级类目
     */
    public static final String CATEGORY2ID ="category2Id";
    /**
     * tag
     */
    public static final String TAGSID ="tagsId";

    /**
     * 前台三级类目
     */
    public static final String F_CATEGORY3ID ="fcategory3Id";
    /**
     * 前台一级类目
     */
    public static final String F_CATEGORY1ID ="fcategory1Id";

}
