package com.biyao.moses.cache;

import com.biyao.moses.common.constant.CacheRedisKeyConstant;
import com.biyao.moses.model.match.ProductImage;
import com.biyao.moses.util.CacheRedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class DailyNewExposureCacheNoCron {

	private Map<Long, Integer> dailyNewExposureMap = new HashMap<Long, Integer>();
	
	
     @Autowired
	 private CacheRedisUtil cacheRedisUtil;
     
     @Autowired
 	 private DailyNewProductCacheNoCron dailyNewProductCacheNoCron;
	
	/**
	 * 初始化
	 */
	protected void init() {
		// todo 之后可以离线计算曝光次数，定时更新到缓存中。
		refreshDailyNewExposureCache();
	}

	/**
	 * 每2分钟刷新一次
	 */
	protected void refreshDailyNewExposureCache() {
		log.info("[任务进度][每日上新曝光]计算每日上新商品曝光开始");
		List<Long> productImageList = null;
		try {
			productImageList = getAllProductList();
		} catch (Exception e) {
			log.error("[严重异常][每日上新曝光]获取每日上新商品信息失败", e);
			return;
		}
		Map<Long, Integer> tmpMap = new HashMap<Long, Integer>();
		for (Long productId : productImageList) {
			String exposureCount = cacheRedisUtil.getString(CacheRedisKeyConstant.MOSES_DN_PRODUCT_COUNT_PREFIX + productId);
			if (StringUtils.isNotBlank(exposureCount)) {
				tmpMap.put(productId, Integer.parseInt(exposureCount));
			}
		}
		dailyNewExposureMap = tmpMap ;
		log.info("[任务进度][每日上新曝光]计算每日上新商品曝光结束，曝光商品个数{}", tmpMap.size());
	}
    
	/**
	* @Description 从redis获取所有的每日上新商品 
	* @return List<ProductImage> 
	* @version V1.0
	* @auth 邹立强 (zouliqiang@idstaff.com)
	 */
	public List<Long> getAllProductList() {
		long start = System.currentTimeMillis();
		List<ProductImage> allProductList = dailyNewProductCacheNoCron.getAllProductList();
		if (CollectionUtils.isEmpty(allProductList)) {
			allProductList = dailyNewProductCacheNoCron.getAllProductListByRedis();
		}
		List<Long> list = new ArrayList<Long>();
		for (ProductImage productImage : allProductList) {
			list.add(productImage.getProductId());
		}
		long end = System.currentTimeMillis();
		log.info("[任务进度][每日上新曝光]获取所有商品曝光信息缓存耗时={}，商品个数{}", end - start, list.size());
		return list;
	}
     
	
	public Map<Long, Integer> getDailyNewExposureMap() {
		return this.dailyNewExposureMap;
	}
	
}