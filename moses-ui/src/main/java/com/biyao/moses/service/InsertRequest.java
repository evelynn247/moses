package com.biyao.moses.service;

import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.params.AdvertInfo;
import com.biyao.moses.params.BaseRequest2;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * @ClassName InsertRequest
 * @Description 横插入参
 * @Author xiaojiankai
 * @Date 2019/12/26 17:56
 * @Version 1.0
 **/
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InsertRequest {
    /**
     * 待展示商品集合
     */
    private List<TotalTemplateInfo> allWaitShowProducts;
    /**
     * 待插入商品集合
     */
    private List<TotalTemplateInfo> waitInsertProducts;
    /**
     * 插入的位置集合
     */
    private List<Integer> insertPositionList;
    /**
     * 当前页索引，从1开始
     */
    private int pageIndex;
    /**
     * 每页大小
     */
    private int pageSize;
    /**
     * 用户相似三级类目相同商品上限
     */
    private Map<Long, Integer> userCategroyNum;

    /**
     * 待展示的活动入口集合
     */
    protected List<AdvertInfo> advertInfoList;
    /**
     * 实验信息
     */
    private BaseRequest2 baseRequest2;
}
