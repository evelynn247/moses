package com.biyao.moses.params;

import lombok.Data;

/**
 * @ClassName ProductScoreInfo
 * @Description 商品ID及其分值
 * @Author admin
 * @Date 2019/10/12 16:08
 * @Version 1.0
 **/
@Data
public class ProductScoreInfo {
    private Long productId;
    private Double score = 0.0;
    // 记录由哪个商品召回
    private Long parentPid;
}
