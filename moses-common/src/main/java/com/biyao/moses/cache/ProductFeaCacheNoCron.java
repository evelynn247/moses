package com.biyao.moses.cache;

import com.alibaba.fastjson.JSONObject;
import com.biyao.moses.common.constant.RedisKeyConstant;
import com.biyao.moses.util.RedisUtil;
import com.google.common.base.Splitter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * LR算法商品特征缓存
 */
@Slf4j
public class ProductFeaCacheNoCron {

    @Autowired
    private RedisUtil redisUtil;
    //商品特征Json
    private JSONObject productFeaJson;

    /**
     * 初始化
     */
    protected void init() {
        refreshFeaCache();
    }

    /**
     * 定时刷新缓存
     */
    protected void refreshFeaCache() {

        try {
            //刷新商品特征缓存
            String productFeaStr = redisUtil.getString(RedisKeyConstant.MOSES_FEA_PRODUCT_FEATURE);
            if(StringUtils.isNotBlank(productFeaStr)){
                productFeaJson = JSONObject.parseObject(productFeaStr);
            }
        } catch (Exception e) {
            log.error("LR商品特征缓存刷新失败  ",e);
        }

    }

    /**
     * 获取商品特征
     * @return
     */
    public JSONObject getProductFeaJson(){
        return productFeaJson;
    }

}
