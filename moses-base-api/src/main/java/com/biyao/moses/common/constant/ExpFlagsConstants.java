package com.biyao.moses.common.constant;

public interface ExpFlagsConstants {
    /**
     * FLAG参数定义
     */
    /**
     * 应用此实验的前端页面ID
     * 格式： frontPageId,...,frontPageId 或  !frontPageId,...,frontPageId
     * 说明：前面不带“!”时，表示应用该实验的前台页面ID集合信息；
     *      前面带"!"时，表示不应用该实验的前台页面ID集合信息
     */
    String SFLAG_APPLY_FRONT_PAGEID = "sflag_apply_front_pageid";
    /**
     * 召回源source获取数据的策略
     * 格式： source,strategy|...|source,strategy
     * 具体策略如下：
     * 1：只使用uid获取数据；
     * 2：只使用uuid获取数据；
     * 3: 优先使用uid获取数据，如果没有获取到数据，则使用uuid获取数据；
     * 4：如果有uid且uid不为0则使用uid获取数据，如果没有uid或uid为0则使用uuid
     * 注：如果新增的召回源不配此项，则默认使用策略4获取数据
     */
    String SFLAG_SOURCE_DATA_STRATEGY = "sflag_source_data_strategy";
    /**
     * 各召回源数据存放的redis信息
     * 格式：source,redis|...|source,redis
     * redis信息具体含义如下：
     * 1：表示1007算法redis集群，2：表示1005 match redis集群
     * 注：如果新增的召回源不配此项，则默认使用1007算法redis集群
     */
    String SFLAG_SOURCE_REDIS = "sflag_source_redis";

    /**
     * ucb数据号，格式dataNum1,dataNum2
     * 用于构造ucb source名称，以及构造redis key，
     * 其中dataNum1
     *     1）为个性化redis key的数据号，即个性化redis key为moses:mab_ucb{dataNum1}_${uid}
     *     2) 召回源source名称：ucb${dataNum1};
     *     dataNum2 为兜底redis key的数据号，
     *     即兜底redis key为：moses:mab_ucb${dataNum2}_common、
     *     moses:mab_ucb${dataNum2}_male、moses:mab_ucb${dataNum2}_female。
     * 如果未配置dataNum2，即只配置dataNum1,此时则dataNum2默认为dataNum1。
     * 如果该参数未配置，则默认都为空字符串。
     */
    String SFLAG_UCB_DATA_NUM = "sflag_ucb_data_num";

    /**
     * 分类页rank对应的bean名称参数
     * car  -- 算法排序规则
     * clr  -- 类目页分层排序规则
     */
    String FLAG_CATEGORY_RANK_NAME = "sflag_category_rank_name";

    /**
     * 算法数据号参数，格式dataNum1,dataNum2
     * 其中dataNum1为个性化redis key的数据号，
     *     即个性化redis key为：moses:cr_up_${dataNum1}_${uid}，moses:cr_up_${dataNum1}_${uuid},
     *     当uid大于0时，使用uid级别的个性化key。其他情况使用uuid级别个性化key。
     * dataNum2 为兜底redis key的数据号，
     *     即兜底redis key为：moses:rs_p_dr_${dataNum2}
     * 如果未配置dataNum2，即只配置dataNum1,此时则dataNum2默认为dataNum1。
     * 如果该参数未配置，则默认都为空，则使用实验layer中的默认配置。
     */
    String FALG_ALGORITHM_DATA_NUM = "sflag_algorithm_data_num";

    /**
     * 基础召回源及其权重，
     * 其值格式为ibcf:0.4,tag:0.5,hots:0.1;
     * 其中ibcf、tag、hots分别为召回源bean名称，0.4、0.5、0.1分别为其权重
     */
    String FLAG_SOURCE_AND_WEIGHT = "sflag_source_and_weight";

    /**
     * 期望返回的商品数量上限
     */
    String FLAG_EXPECT_NUM_MAX = "sflag_expect_num_max";

    /**
     * 前x的概率 格式为：1,0-10|2,10-20|3,20-40|4,40-100
     */
    String FLAG_BEFOREX_PROBABILITY = "sflag_beforeX_probability";

    /**
     * 后y的概率 格式为：1,0-10|2,10-20|3,20-40|4,40-100
     */
    String FLAG_AFTERY_PROBABILITY = "sflag_afterY_probability";
    /**
     * FLAG值定义
     */
    /**
     * 默认值
     */
    String VALUE_DEFAULT = "DEFAULT";
    /**
     * 空字符串
     */
    String VALUE_EMPTY_STRING = "";
}
