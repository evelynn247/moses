package com.biyao.moses.service;

import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @ClassName InsertResult
 * @Description 横插的结果
 * @Author xiaojiankai
 * @Date 2019/12/26 17:56
 * @Version 1.0
 **/
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InsertResult {
    /**
     * 插入后的待展示商品集合
     */
    private List<TotalTemplateInfo> allWaitShowProducts;

    /**
     * 本次已插入的商品信息集合
     */
    private List<String> insertedProducts;
}
