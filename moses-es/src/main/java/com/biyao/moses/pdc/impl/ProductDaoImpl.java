package com.biyao.moses.pdc.impl;

import com.alibaba.fastjson.JSONObject;
import com.biyao.moses.pdc.IProductDao;
import com.biyao.moses.pdc.domain.ProductDomain;
import com.biyao.moses.pdc.mapper.ProductMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * @program: moses-parent-online
 * @description: 商品缓存
 * @author: changxiaowei
 * @create: 2021-09-14 14:29
 **/

@Slf4j
@Repository
public class ProductDaoImpl implements IProductDao {

    @Autowired
    private ProductMapper productMapper;

    @Override
    public List<ProductDomain> getProductsByLastId(Long lastId, int pageSize) {
        List<ProductDomain> productList = new ArrayList<>();
        try {
            productList = productMapper.getProductsByLastId(lastId, pageSize);
        }catch (Exception e){
            log.error("[严重异常]根据lastId从数据库中查询商品异常，lastId:{}，pageSize:{}异常信息：{}，",lastId,pageSize,e);
        }
        return productList;
    }

    @Override
    public List<ProductDomain> getUpdateProductsByLimitTime(String time) {
        List<ProductDomain> productList = new ArrayList<>();
        try {
            productList = productMapper.getUpdateProductsByLimitTime(time);
        }catch (Exception e){
            log.error("[严重异常]同步{}后更新的商品异常，异常信息:",JSONObject.toJSONString(time),e);
        }
        return productList;
    }


    /**
     * 根据商品id 查询数据库中商品基本信息
     * @param pids 商品spuId 列表
     * @return
     */
    @Override
    public List<ProductDomain> getProductInfoByPidList(List<Long> pids) {
        List<ProductDomain> productDomainList = new ArrayList<>();
        try {
           productDomainList = productMapper.getProductInfoByPidList(pids);
        }catch (Exception e){
            log.error("[严重异常]根据pidList查询商品基本信息异常，参数：{}，异常信息:", JSONObject.toJSONString(pids),e);
        }
        return productDomainList;
    }


}
