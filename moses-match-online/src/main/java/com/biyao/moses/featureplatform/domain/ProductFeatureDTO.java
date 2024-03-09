package com.biyao.moses.featureplatform.domain;


import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

/**
 * @author zhangzhimin
 * @date 2022-03-25
 */
@Data
public class ProductFeatureDTO {


    /**
     *商品唯一标识
     */
    @JsonAlias(value = {"productId","product_id"})
    private Integer productId;
}
