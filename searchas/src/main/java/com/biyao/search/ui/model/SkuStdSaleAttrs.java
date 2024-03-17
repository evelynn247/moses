package com.biyao.search.ui.model;

import lombok.*;

/**
 * @author zj
 * @version 1.0
 * @date 2019/10/14 21:04
 * @description
 */
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkuStdSaleAttrs {

    /**
     * 尺寸
     */
    private String size = "";

    /**
     * 颜色
     */
    private String color = "";

    /**
     * 价格
     */
    private Long price = 0L;

}
