package com.biyao.moses.common;

/**
 * @program: moses-parent-online
 * @description: 子模块通用的redis key 配置常量类
 * @author: changxiaowei
 * @Date: 2022-02-19 16:04
 **/
public class RediskeyCommonConstant {
    /**
     * es中商品属性rediskey  +spuId
     * 类型：hash
     * filed：fmVector ；icfVector ； hotscore ：热门分  ； hashcode （用来记录es中的商品信息是否变化）
     * 算法刷入
     */
    public static final String RECOMMOND_ES_REDISKEY_PREFIX= "rs_es_";
}
