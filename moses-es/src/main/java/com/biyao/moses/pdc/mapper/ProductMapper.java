package com.biyao.moses.pdc.mapper;

import com.biyao.moses.pdc.domain.ProductDomain;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @program: moses-parent-online
 * @description:
 * @author: changxiaowei
 * @create: 2021-09-13 21:44
 **/
public interface ProductMapper {
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


    List<ProductDomain>  getProductInfoByPidList(List<Long> pids);
}
