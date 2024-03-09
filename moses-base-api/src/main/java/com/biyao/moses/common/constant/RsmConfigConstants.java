package com.biyao.moses.common.constant;

/**
 * @program: moses-parent
 * @description: rsm 系统中配置的配置id常量类 （2022-02-19 后的配置都放到此常量类下  后续可以将在此之前的配置迁移过来）
 * @author: changxiaowei
 * @Date: 2022-02-19 10:32
 **/
public class RsmConfigConstants {
    //视频在落地页的初始曝光量  rsm 配置id 和默认值
    public static final String VIDEO_BASE_NUM = "video-basenum";
    public static final int VIDEO_BASE_NUM_DEFAULT = 1000;

    //视频因子  rsm 配置id 和默认值
    public static final String VIDEO_V = "Videoexciter";
    public static final float VIDEO_V_DEFAULT = 1.1f;

    //视频奖励周期  rsm 配置id 和默认值
    public static final String VIDEO_C= "cycle";
    public static final Long VIDEO_C_DEFAULT = 2592000000L;

    //距视频首次上传时间  rsm 配置id 和默认值
    public static final String VIDEO_N = "onlinetime-N";
    public static final int VIDEO_N_DEFAULT = 3;

    //在线召回深度浏览商品  rsm 配置id 和默认值
    public static final String TF_DEEP_VIEW_LIMIT = "tfDeepViewLimit";
    public static final int TF_DEEP_VIEW_LIMIT_DEFAULT = 10;

    //在线召回加购商品 rsm 配置id 和默认值
    public static final String TF_CART_LIMIT = "tfCartLimit";
    public static final int TF_CART_LIMIT_DEFAULT = 8;

    //在线召回购买商品  rsm 配置id 和默认值
    public static final String TF_ORFER_LIMIT = "tfOrderLimit";
    public static final int TF_ORFER_LIMIT_DEFAULT = 2;
}
