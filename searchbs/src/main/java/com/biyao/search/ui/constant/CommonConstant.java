package com.biyao.search.ui.constant;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * 搜索返回结果类型
 *
 * @author guochong
 * @date 2017-02-22
 */
public interface CommonConstant {

    /**
     *  周综合排行
     */
    public static final HashSet<String> platforms = new HashSet<String>(
            Arrays.asList("ios", "android", "mweb", "pc", "miniapp"));

    // APP版本号
    /**
     * ios拼团的APP版本号 17.09.28
     */
    public static final int IOS_GROUP_VERSION = 68;
    /**
     * android拼团的APP版本号 17.09.28
     */
    public static final int ANDROID_GROUP_VERSION = 211;
    /**
     * iOS同事一起拼版本号  18.05.04
     */
    public static final int IOS_TOGETHER_VERSION = 85;
    /**
     * Android同事一起拼版本号18.05.04
     */
    public static final int ANDROID_TOGETHER_VERSION = 225;
    /**
     * iOS中间页优化版本号  18.07.09
     */
    public static final int IOS_SEARCHUI_VERSION = 91;
    /**
     * Android中间页优化版本号18.07.09
     */
    public static final int ANDROID_SEARCHUI_VERSION = 231;
    /**
     * iOS一起拼1.2.1版本 18.07.23
     */
    public static final int IOS_YIQIPIN121_VERSION = 93;
    /**
     * Android一起拼1.2.1版本 18.07.23
     */
    public static final int ANDROID_YIQIPIN121_VERSION = 232;
    /**
     * 推荐中间页展示IOS版本号
     */
    public static final int IOS_ZHULI_VERSION = 102;
    //	public static final int IOS_ZHULI_VERSION = 110; // 推荐中间页展示IOS版本号扩大到不显示，ios提审通过后使用102
    /**
     * 推荐中间页展示andriod版本号
     */
    public static final int ANDROID_ZHULI_VERSION = 242;

    /**
     * 标准品咖啡展示版本号,小于此版本不展示
     */
    public static final int IOS_COFFEE_VERSION = 114;
    public static final int ANDROID_COFFEE_VERSION = 251;

    /**
     * 一起拼支持阶梯团老版本兼容 4.9版本
     */
    public static final int IOS_YQPGROUP_VERSION = 112;//
    public static final int ANDROID_YQPGROUP_VERSION = 251;
    /**
     * 推荐中间页展示IOS版本号
     */
    public static final int IOS_HOMECACHESEARCH_VERSION = 101;

    /**
     * iOS一起拼1.7版本
     */
    public static final int IOS_YIQIPIN17_VERSION = 109;
    /**
     * Android一起拼1.7版本
     */
    public static final int ANDROID_YIQIPIN17_VERSION = 248;
    /**
     * 1-同事一起拼
     */
    public static final String YIQIPIN_ACTIVITY = "1";
    /**
     * 1-搜索站点名称
     */
    public static final String SYSTEM_NAME = "search";
    /**
     * 新手专享
     */
    public static final String NEW_USER_TITLE = "新手专享";
    /**
     * 新手专享热词颜色
     */
    public static final String NEW_USER_COLOR = "#FE1211";
    /**
     * 低模眼镜支持裂变IOS版本号
     */
    public static final int IOS_GLASSES_VERSION = 123;
    /**
     * 低模眼镜支持裂变ANDROID版本号
     */
    public static final int ANDROID_GLASSES_VERSION = 262;

    public static final int IOS_PLANTFORM_VERSION = 127; //平台优化920IOS版本号

    public static final int ANDROID_PLANTFORM_VERSION = 267; //平台优化920ANDROID版本号

    /**
     * 大运河V1.1版本兼容版本号
     */
    public static final int ANDROID_DAYUNHE_VERSION = 274;

    public static final int IOS_DAYUNHE_VERSION = 134;

    public static final int MINIAPP_DAYUNHE_VERSION = 207;

    /**
     * 大运河V1.2版本号，该版本号在2020年4月22日紧急上线需求中要求app版本号升级，只对当前最新版本露出大V和企业定制用户
     */
    public static final int ANDROID_DAYUNHE_VERSION_DAYUNHE1_2 = 286;

    public static final int IOS_DAYUNHE_VERSION_DAYUNHE1_2 = 147;

    public static final int MINIAPP_DAYUNHE_VERSION_DAYUNHE1_2 = 210;

    /**
     * APP 商品编辑器路由
     */
    public static class APPRoute {
        // 高模商品
        public static final String H_PRODUCT_URL_PREFIX = "biyao://product/browse/productDetail?goodsID=";
        // 无模型或者低模商品
        public static final String L_PRODUCT_URL_PREFIX = "biyao://product/browse/goodsDetail?goodsID=";
        // 拼团商品
        public static final String GROUP_PRODUCT_URL_PREFIX = "biyao://product/group/goodsDetail?goodsId=";
        // 同事一起拼商品
        public static final String TOGETHER_PRODUCT_URL_PREFIX = "biyao://product/togetherGroup/goodsDetail?suId=";

        // 参团落地页
        public static final String JOINGROUP_PRODUCT_URL_PREFIX = "biyao://product/group/goodsDetail/joinGroup?joinGroupType=1&suId=";
        //阶梯团商品
        public static final String LAGGER_GROUP_URL_PREFIX = "biyao://product/ladderGroup/goodsDetail?suId=";

        //咖啡biyao://product/browse/coffeeDetail?suId=%s&customCoffeeId=0
        public static final String COFFEE_PRODUCT_URL_PREFIX = "biyao://product/browse/coffeeDetail?suId=";
        /**
         * 一次定制商品路由，
         *  biyao://product/browse/mWeb?mUrl=xx
         *  mUrl：参数为appsup站点的url编码后的参数
         *  appsup站点的路由地址为：https://appsup.biyao.com/product/detail/suId?editorType=xx&joinGroupType=xx&groupType=xx&fromType=xx
         *  路由链接suId为商品Id，必填
         *  路由参数：
         *  editorType， // 编辑器类型，必填， 0 为普通编辑器， 1为一起拼编辑器
         *  joinGroupType, // 1参团
         *  groupType, // 1阶梯团
         *  fromType, // 1: 新手专享时使用
         *  备注说明：editorType为appsup新增参数，joinGroupType、groupType、fromType为原来app端路由参数
         *
         *  示例如下：
         *  一起拼参团的商品ID为1300476627010100001，appsup路由地址如下：
         *  https://appsup.biyao.com/product/detail/1300476627010100001?joinGroupType=1&editorType=1
         *
         *  appsup路由编码的地址为：
         *  https%3A%2F%2Fappsup.biyao.com%2Fproduct%2Fdetail%2F1300476627010100001%3FjoinGroupType%3D1%26editorType%3D1
         *
         *  app端拼接的路由地址为：
         *  biyao://product/browse/mWeb?mUrl=https%3A%2F%2Fappsup.biyao.com%2Fproduct%2Fdetail%2F1300476627010100001%3FjoinGroupType%3D1%26editorType%3D1
         */
        public static final String DERIVE_PRODUCT_URL_PREFIX = "biyao://product/browse/mWeb?mUrl=";

        /**
         * appsup 站点路由前缀
         */
        public static final String APP_SUP_URL_PREFIX = "https://appsup.biyao.com/product/detail/";
    }

    /**
     * M站商品编辑器路由
     */
    public final static String M_PRODUCT_URL_PREFIX = "https://m.biyao.com/products/";

    /**
     *
     */
    public final static String MINI_PRODUCT_URL_PREFIX = "pages/products/products?suId=";

    /**
     * 前端模板类型
     *
     * @author luozhuo
     */
    public static class TemplateType {
        public static final String TEXT_BUTTON = "textButton";
        public static final String IMAGE = "image";
        public static final String STORE = "store";
        public static final String SINGLE_PRODCUT = "singleProduct";
        public static final String DOUBLE_PRODUCT = "doubleProduct";
        public static final String GROUP_BUY_SINGLE_PRODCUT = "groupBuySingleProduct";
        public static final String LADDER_GROUP_PRODUCT = "ladderGroupProduct";
    }

    public static class FacetType {
        public static final String MUILT_SELECT = "muilt";
        public static final String SINGLE_SELECT = "single";
    }

    // guava缓存的配置文件定时刷新时间
    public final static long GUAVA_REFRESH_DELAY = 10 * 60 * 1000;
    public final static long GUAVA_REFRESH_PERIOD = 10 * 60 * 1000;

    // 搜索中间页路由
    public static final String ROUTE_TYPE_SEARCH_PAGE = "searchPage";
    public static final String ROUTE_TYPE_NEW_PAGE = "newPage";

    // 搜索中间页路由
    public static final String ROUTE_2SEARCHPAGE_MINIAPP = "/pages/search/search";
    public static final String ROUTE_2SEARCHPAGE_MWEB = "http://m.biyao.com/search/searchResult";
    public static final String ROUTE_2SEARCHPAGE_APP = "biyao://product/browse/searchResult";
    public static final String HOME_ROUTE_2SEARCHPAGE_MWEB = "/search/searchResult";

    // 每日上新路由
    public static final String ROUTE_2NEW_APP = "biyao://product/browse/dailyPage";
    public static final String ROUTE_2NEW_MINIAPP = "/pages/middle/new/new";
    public static final String ROUTE_2NEW_MWEB = "/classify/newproductList";

    // 专题列表路由
    public static final String ROUTE_2TOPICLIST_APP = "biyao://product/browse/topicList";
    public static final String ROUTE_2TOPICLIST_MWEB = "/classify/topicList";

    // 专题详情路由
    public static final String ROUTE_2TOPICDETAIL_APP = "biyao://product/browse/topicDetail";
    public static final String ROUTE_2TOPICDETAIL_MINIAPP = "/pages/middle/special/special";
    public static final String ROUTE_2TOPICDETAIL_MWEB = "/classify/topicDetail";

    // 商家店铺路由
    public static final String ROUTE_2SUPPLIERPAGE_APP = "biyao://product/browse/supplierPage";
    public static final String ROUTE_2SUPPLIERPAG_MINIAPP = "/pages/supplier/supplierIndex/supplierIndex";
    public static final String ROUTE_2SUPPLIERPAG_MWEB = "/classify/supplierHome";

    // 推荐中间页路由
    public static final String ROUTE_2RECOMMEND_APP = "biyao://product/browse/recommendMiddlePage";
    public static final String ROUTE_2RECOMMEND_MWEB = "/classify/pagesRecommend";
    public static final String ROUTE_2RECOMMEND_MINIAPP = "/pages/middle/recommend/recommend";

    /**
     * 新手专享商品列表中间页路由
     **/
    public static final String NEW_GUY_BENEFIT_PRODUCT_MIDDLE_PAGE_URL = "biyao://product/browse/newUserDiscountList";

    /**
     * nova conf 地址
     */
    String NOVA_CONF_URL = "http://conf.nova.biyao.com/nova/";
    /**
     * 本地配置文件目录
     */
    String LOCAL_CONF_PATH = Thread.currentThread().getContextClassLoader().getResource("").getPath() + "/conf/";
    /**
     * suggestion remote url
     */
    String QUERY_SUGGESTION_URL = NOVA_CONF_URL + "query_suggestion.txt";
    /**
     * suggestion local path
     */
    String QUERY_SUGGESTION_PATH = LOCAL_CONF_PATH + "query_suggestion.txt";
    /**
     * 从mosesmatch获取商品feed流
     */
    String MOSES_PRODUCT_MATCH_URL = "http://mosesmatch.biyao.com/recommend/productfeed";
    /**
     * 从moses获取个性化商品feed流
     */
    String MOSES_PRODUCT_FEED_URL = "http://moses.biyao.com/recommend/function/getRecommendPids?uuid=%s&uid=%s&biz=%s&caller=%s&expNum=%s";
    /**
     * 请求日志名称
     */
    String REQUEST_LOG_NAME = "search_request";
    /**
     * 结果日志名称
     */
    String RESPONSE_LOG_NAME = "search_show";

    /**
     * 小程序跳转到大V主页的URL
     */
    public static final String MINIAPP_BYFRIEND_HOMEPAGE_URL = "pages/byFriend/homePage/homePage";
    public static final String M_BYFRIEND_HOMEPAGE_URL = "https://m.biyao.com/biyaoFriends/friendIndex";
    public static final String APP_BYFRIEND_HOMEPAGE_URL = "biyao://friend/moment/otherProfile";


    /**
     * 一起拼标签的content
     */
    public static final String LABEL_CONTENT_YQP = "一起拼";
    /**
     * 津贴标签content前缀
     */
    public static final String LABEL_CONTENT_ALLOWANCE_PREFIX="津贴可抵";
    /**
     * Byte类型 true
     */
    public static final Byte BYTE_TRUE = new Byte("1");
    /**
     * Byte类型 false
     */
    public static final Byte BYTE_FALSE = new Byte("0");

    String SIMILAR_PRODUCT_URL = "http://hdfsfile.biyao.com/download/tmp/online_running/similar_product/similar_product.txt?caller=search_ui";


    /**
     * 津贴标签素材ID
     */
    public final static long ALLOWANCE_LABEL_ID = 10830249L;

    /**
     * 标签类型：2：标识津贴
     */
    public final static Integer LABEL_TYPE_ALLOWANCE = 2;


    /**
     * 视频标识展示开关:开
     */
    public final static String IS_SHOW_VIDEO_FLAG_YES = "1";


    /**
     * 查询用户资产类型-津贴  1通用特权金、2新手特权金、3津贴、4立减金、5参团卡 、6新手专享优惠凭证
     */
    public final static Integer USER_COUPON_TYPE_ALLOWANCE_CONS = 3;

    /**
     * CMS获取津贴文案，支持两个变量
     *变量一、当前商品的购物津贴可用金额
     */
    public final static String ALLOWANCE_LABEL_ZUIGAOKEDI_CONS = "{jintiezuigaokedi}";

    /**
     *变量2、取当前用户的总津贴数和当前商品的购物津贴可用金额两者的最小值
     */
    public final static String ALLOWANCE_LABEL_YONGHUKEDI_CONS = "{yonghujintiekedi}";


    /**
     *从CMS获取津贴标签文案为null或者没有配置变量时，托底文案
     */
    public final static String ALLOWANCE_LABEL_TUODI_CONS = "可用购物津贴";


    /**
     * @description 下面参数为支持必要造物商品新增
     * @project 【鸿源商品底盘V1.0.0-新增支持必要造物商品】
     * @author 张志敏
     * @date 2022-02-16
     */

    /**
     * 造物角标素材id
     */
    public static final Long CREATION_SHOW_ICON_ID = 11560606L;

    /**
     * 造物价文案素材id
     */
    public static final Long CREATION_PAPERWORK_ICON_ID = 11560207L;

    /**
     * 造物价文案  托底
     */
    public static final String CREATION_PAPERWORK_EXCHANGE = "优惠价";


    /**
     * 造物标签素材id
     */
    public static final Long CREATION_LABEL_ID = 11560205L;

    /**
     * 支持必要造物商品标识—版本和活动页都支持必要造物商品
     */
    public static final int IS_SUPPORT_CREATOR_YES = 1;

    /**
     * 支持必要造物商品标识—仅版本支持必要造物商品
     */
    public static final int IS_SUPPORT_CREATOR_FLAG_2 = 2;

    /**
     * 必要造物商品标识—YES
     */
    public static final int IS_CREATOR_YES = 1;



    /**
     * 展示造物角标：是否显示图标,0:无 1:新品 2:团购 3:同事一起拼  8 造物角标
     * （Android共用的模板，4：一起拼正常的标签图片  5：一起拼比较小的标签图片，6：直播入口  7：复购 ）
     */
    public static final int SHOW_CREATOR_ICON = 8;

    /**
     * 是否支持webp图片—YES
     */
    public static final int IS_SUPPORT_WEBP_YES = 1;


    /**
     * label标签文案类型-造物
     */
    public static final int LABEL_TYPE_CREATOR = 1;

}