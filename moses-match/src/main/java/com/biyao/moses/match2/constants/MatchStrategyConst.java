package com.biyao.moses.match2.constants;

/**
 * @author zhaiweixi@idstaff.com
 * @date 2019/10/8
 **/
public class MatchStrategyConst {

    /**
     * 新手专享召回源
     */
    public final static String NCHS = "nchs";

    /**
     * 热销召回源
     */
    public final static String HOTS = "hots";

    /**
     * 基于商品内容的协同过滤召回源
     */
    public final static String IBCF = "ibcf";

    /**
     * 基于商品内容的协同过滤召回源
     */
    public final static String IBCF2 = "ibcf2";

    /**
     * 基于标签的召回源
     */
    public final static String TAG = "tag";
    /**
     * 基于实时浏览商品的召回源
     */
    public final static String RTCBR = "rtcbr";

    /**
     * 基于实时浏览商品的召回源2
     */
    public final static String RTBR2 = "rtbr2";

    /**
     * 实时兴趣探索召回源
     */
    public final static String RTEE = "rtee";
    /**
     * 双向编码推荐召回源
     */
    public final static String BERT = "bert";
    /**
     * bert2召回源
     */
    public final static String BERT2 = "bert2";
    /**
     * 多臂赌博机召回源
     */
    public final static String UCB = "ucb";

    /**
     * slot1实验槽source名称
     */
    public final static String SLOT1 = "slot1";

    /**
     * slot2实验槽source名称
     */
    public final static String SLOT2 = "slot2";

    /**
     * 多臂赌博机召回源优化后的召回源
     */
    public final static String UCB2 = "ucb2";

    /**
     * action based recommendation（基于操作的推荐）
     */
    public final static String ACREC = "acrec";

    /**
     * 通用match召回
     */
    public final static String COMMON_MATCH = "commonMatch";
    /**
     * 感兴趣商品集工程规则召回
     */
    public final static String GXQSP_MATCH = "gxqsp";
    /**
     * 天气召回源
     */
    public final static String WEATHER = "wea";

    /**
     * 基础流量召回源
     */
    public final static String BASE = "base";

    /**
     * 基础流量召回源2
     */
    public final static String BASE2 = "base2";

    /**
     * 梦工厂聚合页店铺个性化召回源
     */
    public final static String DWS = "dws";

    /**
     * 梦工厂聚合页店铺通用召回源
     */
    public final static String DWSCOMM = "dwscomm";

    /**
     * 梦工厂聚合页商品个性化召回源
     */
    public final static String DWP = "dwp";

    /**
     * 梦工厂聚合页商品性别通用召回源
     */
    public final static String DWPSEX = "dwpsex";

    /**
     * 首页轮播图-小蜜蜂新品召回源
     * 推荐V2.24.0-轮播图及分类页优化项目新增
     * 用于首页轮播图老客召回
     */
    public final static String CATEXMF = "catexmf";

    /**
     * 首页轮播图-普通新品召回源
     * 推荐V2.24.0-轮播图及分类页优化项目新增
     * 用于首页轮播图老客召回
     */
    public final static String CATEXP = "catexp";

    /**
     * 首页轮播图-热销召回源
     * 推荐V2.24.0-轮播图及分类页优化项目新增
     * 用于首页轮播图兜底召回源
     */
    public final static String LBTHOT = "lbthot";

    /**
     * 首页轮播落地页-新品召回源
     * 推荐V2.24.0-轮播图及分类页优化项目新增
     */
    public final static String LBTLDY_NEW_PRODUCT = "newpro";

    /**
     * 首页轮播落地页-相似商品召回源
     * 推荐V2.24.0-轮播图及分类页优化项目新增
     */
    public final static String LBTLDY_LIKE = "like";

    /**
     * 首页轮播落地页-后台三级类目热销召回源
     * 推荐V2.24.0-轮播图及分类页优化项目新增
     */
    public final static String LBTLDY_CATE3_HOT = "cate3hot";

    /**
     * 首页轮播落地页-后台二级类目热销召回源
     * 推荐V2.24.0-轮播图及分类页优化项目新增
     */
    public final static String LBTLDY_CATE2_HOT = "cate2hot";

    /**
     * 必要朋友V2.0 好友已购业务
     */
    public final static String UCB3YG = "ucb3_yg";

    /**
     * 必要朋友V2.0  精选内容 个性化评论
     */
    public final static String COM = "com";

    /**
     * 必要朋友V2.0 精选内容  热度商品评论
     */
    public final static String COMHOTS = "comhots";

    /**
     * 必要朋友V2.0 精选内容  个性化主动发布内容
     */
    public final static String VIEWS = "views";

    /**
     * 必要朋友V2.0 精选内容  热度主动发布内容
     */
    public final static String VIEWHOTS = "viewhots";


    /**
     * 到站老客转化升级V1.0-精准活动推荐 个性化推荐活动召回源
     */
    public final static String DZLKACT = "act";
}
