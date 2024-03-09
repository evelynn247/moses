package com.biyao.moses.params;

import lombok.Data;

/**
 * @ClassName MatchSourceData
 * @Description 类中字段对应于算法刷入到redis中的召回源数据的各个字段
 * @Author xiaojiankai
 * @Date 2020/11/20 11:34
 * @Version 1.0
 **/
@Data
public class MatchSourceData {
    /**
     * 当普通商品召回时，则为商品spuId
     * 当店铺召回时，则为店铺id
     */
    private String id;

    /**
     * 分数
     */
    private Double score;

    /**
     * 真实的召回源名称
     */
    private String realSourceName;
}
