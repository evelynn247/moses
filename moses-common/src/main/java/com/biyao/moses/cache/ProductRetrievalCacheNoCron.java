package com.biyao.moses.cache;

import com.biyao.moses.common.constant.RedisKeyConstant;
import com.biyao.moses.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 商品召回定时任务 <br>
 * 热销召回源</br>
 * <br>
 * 低决策召回源</br>
 * <br>
 * 新品基础流量召回源</br>
 * <br>每日上新新品召回源</br>
 * <br>天气商品召回源</br>
 * 
 * @author biyao
 *
 */
@Slf4j
public class ProductRetrievalCacheNoCron {

	@Autowired
	private RedisUtil redisUtil;

	private List<Long> feedRdkLst = new ArrayList<Long>();

	private List<Long> lowDecisionRdkLst = new ArrayList<Long>();

	private List<Long> homeFeedExposureLessLst = new ArrayList<Long>();
	
	private List<Long> newShelfRdkLst = new ArrayList<Long>();
	
	private Map<String, String> weaMatchProductMap = new HashMap<String, String>();

	protected void init() {
	 
		refreshProductRetrievalCache();
	}
	/**
	 * 商品召回定时任务
	 */
	protected void refreshProductRetrievalCache() {

		log.info("[任务进度][商品召回]获取商品召回信息开始");
		try {
			long start = System.currentTimeMillis();
			// 刷新热销商品
			feedRdkLst = commRefreshProductRetrievalCache(feedRdkLst, RedisKeyConstant.FEED_RDK);
			// 刷新低决策商品
			lowDecisionRdkLst = commRefreshProductRetrievalCache(lowDecisionRdkLst, RedisKeyConstant.LOW_DECISION_RDK);
			// 刷新新品基础曝光流量商品
			homeFeedExposureLessLst = commRefreshProductRetrievalCache(homeFeedExposureLessLst, RedisKeyConstant.HOME_FEED_EXPOSURE_LESS_KEY);
			//刷新每日上新新品推荐
			newShelfRdkLst = commRefreshProductRetrievalCache(newShelfRdkLst, RedisKeyConstant.NEW_SHELF_RDK);
			//刷新天气商品
		    refreshWeaProductCache(RedisKeyConstant.WEA_MATCH_DATA_KEY);
		    log.info("[任务进度][商品召回]获取商品召回信息结束，耗时{}ms", System.currentTimeMillis()-start);
		} catch (Exception e) {
			log.error("[严重异常][商品召回]获取商品召回信息出现异常 ", e);
		}
	}
	/**
	 * 刷新天气商品
	 */
	private void refreshWeaProductCache(String redisKey) {
		
		try {
			//获取天气商品集合，values数据格式 pid1,pid2...
			Map<String, String> redisWeaProductMap = redisUtil.hgetAll(redisKey);
			if(CollectionUtils.isEmpty(redisWeaProductMap)) {
				log.error("[严重异常][商品召回]获取天气商品召回信息为空，内存缓存不做更新，redis key={}", redisKey);
				return;
			}
			weaMatchProductMap = redisWeaProductMap;
			log.info("[任务进度][商品召回]获取天气商品个数 {}，redis key={}",redisWeaProductMap.size(), redisKey);
		} catch (Exception e) {
			log.error("[严重异常][商品召回]获取天气商品召回出现异常redis key={}", redisKey, e);
		}
	}
	
	/**
	 * 通用刷新商品召回缓存
	 */
	private List<Long> commRefreshProductRetrievalCache(List<Long> productRetrievalLst, String redisKey) {

		try {
			// redis数据格式：productId:score,productId:score,productId:score
			String redisResult = redisUtil.getString(redisKey);
			List<Long> productLst = getRcdProductList(redisResult);
			if (CollectionUtils.isEmpty(productLst)) {
				log.error("[严重异常][商品召回]获取商品召回信息为空，内存缓存不做更新，redis key={}", redisKey);
				return productRetrievalLst;
			}
			if(productLst.size() > 1000) {
				productLst = productLst.subList(0, 1000);
			}
			log.info("[任务进度][商品召回]获取商品个数 {}，redis key={}",productLst.size(), redisKey);
			return productLst;
		} catch (Exception e) {
			 log.error("[严重异常][商品召回]通用刷新商品召回缓存异常redis key={}", redisKey, e);
		}
		return productRetrievalLst;
	} 
	/**
	 * 解析商品ID
	 * 
	 * @param productScoreStr
	 * @return
	 */
	public List<Long> getRcdProductList(String productScoreStr) {
		if (StringUtils.isBlank(productScoreStr)) {
			return new ArrayList<>();
		}
		List<Long> rcdProductList = Arrays.stream(productScoreStr.split(",")).map(productScore -> {
			return Long.valueOf(productScore.split(":")[0]);
		}).collect(Collectors.toList());
		return rcdProductList;
	}
	/**
	 * 热销商品
	 * 
	 * @return
	 */
	public List<Long> getFeedRdkLst() {

		if (CollectionUtils.isEmpty(feedRdkLst)) {
			commRefreshProductRetrievalCache(feedRdkLst, RedisKeyConstant.FEED_RDK);
		}
		return feedRdkLst;
	}
	/**
	 * 低决策商品
	 * 
	 * @return
	 */
	public List<Long> getLowDecisionRdkLst() {

		if (CollectionUtils.isEmpty(lowDecisionRdkLst)) {
			commRefreshProductRetrievalCache(lowDecisionRdkLst, RedisKeyConstant.LOW_DECISION_RDK);
		}
		return lowDecisionRdkLst;
	}
	/**
	 * 新品基础曝光流量商品
	 * 
	 * @return
	 */
	public List<Long> getHomeFeedExposureLessLst() {

		if (CollectionUtils.isEmpty(homeFeedExposureLessLst)) {
			commRefreshProductRetrievalCache(homeFeedExposureLessLst, RedisKeyConstant.HOME_FEED_EXPOSURE_LESS_KEY);
		}
		return homeFeedExposureLessLst;
	}
	/**
	 * 每日上新新品推荐
	 * 
	 * @return
	 */
	public List<Long> getNewShelfRdkLst() {

		if (CollectionUtils.isEmpty(newShelfRdkLst)) {
			commRefreshProductRetrievalCache(newShelfRdkLst, RedisKeyConstant.NEW_SHELF_RDK);
		}
		return newShelfRdkLst;
	}
	/**
	 * 获取天气商品集合
	 * @param wea
	 * @return
	 */
	public List<Long> getWeatherProductLst(String wea){
		
		if (CollectionUtils.isEmpty(weaMatchProductMap) || StringUtils.isEmpty(wea)) {
			return new ArrayList<Long>();
		}
		//获取天气商品集合，values数据格式 pid1,pid2...
		 return getRcdProductList(weaMatchProductMap.get(wea));
	}
	
}
