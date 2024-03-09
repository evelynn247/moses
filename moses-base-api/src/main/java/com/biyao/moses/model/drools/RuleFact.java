package com.biyao.moses.model.drools;

import lombok.Data;

import java.util.List;

/**
 * @program: moses-parent
 * @description: 规则类实体对象
 * @author: changxiaowei
 * @create: 2021-03-23 13:34
 **/
@Data
public class RuleFact extends RuleBaseFact{

    private String ruleId;
    /**
     * 召回源及其权重
     * 格式：{召回源id,权重|召回源id,权重；.....|召回源id,权重}
     */
    private String sourceAndWeight;

    /**
     * 召回源及其策略
     * 格式：{召回源id,策略|召回源id,策略；.....|召回源id,策略}
     */
    private String sourceDataStrategy;

    /**
     * 最大召回数量
     * 整数，如100。范围1~1000
     */
    private Integer expectNumMax;
    /**
     * 性别过滤
     * 0：不使用
     * 1：使用
     */
    private String sexFilter;
    /**
     * 季节过滤
     * 0：不使用
     * 1：使用
     */
    private String  seasonFilter;
    /**
     * 是否使用召回份
     * 0：不使用
     * 1：使用
     */
    private String  recallPoints;
    /** 惩罚因子
     * 0：排序中不使用惩罚因子
     * 1：排序中使用曝光惩罚
     * 2：排序中使用退货退款率惩罚
     * 3：断码惩罚
     * 注：1、2、3可同时配置多项。当有0时，不管是否有1、2、3，结果都为不使用惩罚因子
     */
    private String  punishFactor;
    /**
     * 价格因素
     * 0：排序中不使用价格因素
     * 1：排序中使用价格因素
     */
    private String  priceFactor;
    /**
     * 复购过滤
     * 0：不使用已购商品过滤
     * 1：过滤掉用户30天内购买的商品
     * 2：使用算法复购周期已购商品过滤
     */
    private String  repurchaseFilter;
    /**
     * 类目隔断
     * 0：不使用类目隔断
     * 1：非个性化隔断
     * 2：个性化隔断
     */
    private String categoryPartition;
    /**
     * 记录召回逻辑参数的顺序
     */
    private List<String> matchParamList;
    /**
     * 记录rank逻辑中参数的顺序
     */
    private List<String> rankParamList;
    /**
     * 记录rule逻辑中参数的顺序
     */
    private List<String> ruleParamList;
    /**
     * 走在线召回开始离线召回 0 离线 1 在线
     */
    private Byte matchType;

}
