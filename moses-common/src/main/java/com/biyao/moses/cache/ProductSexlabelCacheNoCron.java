package com.biyao.moses.cache;

import com.biyao.moses.common.constant.CommonConstants;
import com.biyao.moses.common.constant.MatchRedisKeyConstant;
import com.biyao.moses.util.MatchRedisUtil;
import com.biyao.moses.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * 定时刷新商品性别缓存
 * 商品性别属性值定义：中性 = 2， 女性=1，男性=0，未知=-1
 * @author biyao
 *
 */
@Slf4j
public class ProductSexlabelCacheNoCron {

    @Autowired
    private MatchRedisUtil matchRedisUtil;

    private Map<String, String> productSexMap = new HashMap<>();
	 
    protected void init() {                         
    	refreshProductSexlabelCache();
    }


    /**
     * 	定时刷新商品性别缓存
     */
    protected void refreshProductSexlabelCache() {

    	log.info("[任务进度][商品性别]获取商品性别信息开始");
		long start = System.currentTimeMillis();
		try {
        	Map<String, String> redisProductSexMap = matchRedisUtil.hscan(MatchRedisKeyConstant.MOSES_PRODUCT_SEX);
        	
        	if(CollectionUtils.isEmpty(redisProductSexMap)) {
        		log.error("[严重异常][商品性别]获取商品性别为空");
        		return;
        	}
        	productSexMap = redisProductSexMap;
			log.info("[任务进度][商品性别]获取商品性别信息结束，耗时{}ms，商品个数 {}", System.currentTimeMillis()-start, redisProductSexMap.size());
        } catch (Exception e) {
            log.error("[严重异常][商品性别]获取商品性别出现异常 ",e);
        }
    } 
    /**
	 * 根据商品ID获取商品性别
     * @param productId
     * @return
     */
    public String getProductSexLabel(String productId) {
    	
    	if (StringUtils.isBlank(productId) || CollectionUtils.isEmpty(productSexMap) ){
			return CommonConstants.UNKNOWN_SEX;
		}
    	return productSexMap.getOrDefault(productId, CommonConstants.UNKNOWN_SEX);	
    }
}
