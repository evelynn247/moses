package com.biyao.moses.service;

import com.biyao.moses.pdc.domain.ProductDomain;

import java.util.List;

/**
 * @program: moses-parent-online
 * @description:
 * @author: changxiaowei
 * @create: 2021-09-18 17:24
 **/
public interface IEsService {
    /**
     * 重建索引
     */
    void rebulidIndex();

    /**
     * 更新索引
     * @param productDomains 待更新的商品列表
     */
    void updateIndex(List<ProductDomain> productDomains);

    /**
     * 更新索引
     * @param minute 更新当前时间 minute时间之内更新的商品
     */
    void updateIndexByTime(int minute);
    /**
     * 删除过期索引
     */
    void  removeIndexTimer();

    /**
     * 根据商品id 更新es中商品信息
     * @param pids
     */
    void updateIndexByPids(List<Long> pids);

}
