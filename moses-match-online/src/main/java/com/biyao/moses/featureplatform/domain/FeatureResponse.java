package com.biyao.moses.featureplatform.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;


/**
 * @author zhangzhimin
 * @date 2022-03-25
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FeatureResponse<T>  implements Serializable {

    private static final long serialVersionUID = -2246435765460160009L;

    /**
     * 符合条件的总数量
     */
    private Long totalNum=0L;

    /**
     * 当前页返回数据数量
     */
    private Integer currentPageNum=0;

    /**
     * es召回的数据
     */
    private T data = null;
}
