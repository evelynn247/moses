package com.biyao.moses.pdc;

import com.biyao.moses.pdc.domain.ProductDomain;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @program: moses-parent-online
 * @description: 普通商品dao
 * @author: changxiaowei
 * @create: 2021-12-02 20:19
 **/
public interface IProductDao {

    /**
     * 根据lateId分页返回商品
     * @param lastId
     * @param pageSize
     * @return
     */
    List<ProductDomain> getProductsByLastId(@Param("lastId") Long lastId, @Param("pageSize") int pageSize);

    /**
     * 获取指定时间到当前时间段内更新的商品
     * @param time
     * @return
     */
    List<ProductDomain>  getUpdateProductsByLimitTime(@Param("time") String time);

    /**
     * 根据pid查询商品信息
     * @param pids
     * @return
     */
    List<ProductDomain>  getProductInfoByPidList(List<Long> pids);
}