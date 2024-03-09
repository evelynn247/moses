package com.biyao.moses.constant;

/**
 * @program: moses-parent-online
 * @description: redis key 配置常量类
 * @author: changxiaowei
 * @Date: 2022-02-19 16:01
 **/
public class RedisKeyConstant {

    /**
     * 新手专享运营配置的商品信息
     * 类型：String
     * value格式：pid1,pid2,...
     * 数据来源： 新手专享SCM tagId
     */
    public static final String MOSES_NEWUSER_SPECIAL_PRODUCTS = "moses:new_user_special_product";

    /**
     * 商品季节信息，类型为String，value格式为：pid:season1|season2,pid:season1,...,pid:season1:season2
     * season值为以下几个：春、夏、秋、冬、四季
     * 数据刷入：李晓峰
     * 输入时间：半小时一次
     */
    public static final String PRODUCT_SEASON = "moses:product_season";

    /**
     * 商品性别信息
     * 类型：hash  field: productId value:性别
     * 数据刷入： 赵晓峰
     * 刷入时间： 每30分钟
     */
    public static final String MOSES_PRODUCT_SEX = "moses:product_sex_label";
    /**
     * 商品和视频的关系  类型：hash field：product
     * value数据格式：json
     * 数据刷入
     * 刷入时间 ：10分钟一次
     * 过期时间：3天
     * 备注：内容策略V1.0-商品内容视频化项目新增
     */
    public static final String PRODUICT_VEDIO_REDIS ="video_info";
}

