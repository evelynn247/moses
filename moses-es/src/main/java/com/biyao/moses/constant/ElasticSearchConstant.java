package com.biyao.moses.constant;

/**
 * @program: moses-parent-online
 * @description:
 * @author: changxiaowei
 * @create: 2021-09-15 15:50
 **/
public class ElasticSearchConstant {
    /**
     * 普通商品索引
     */
   public static final String PRODUCT_INDEX_PREFIX = "by_moses_product_";
    /**
     * 普通商品索引别名前缀
     */
    public static final String PRODUCT_INDEX_ALIAS = "by_moses_product_alias_";
   /**
     *分片数
     */
   public static final Integer SHARDS_NUM = 5;
    /**
     * 副本数
     */
   public static final Integer REPLICAS_NUM =1;
    /**
     * 索引的过期时间  3天
     */
   public static final long INDEX_EXPIRE_MILLS = 259200000;
}