package com.biyao.moses.common.constant;

public class CacheRedisKeyConstant {
	/**
	 * 新首页feeds流曝光
	 */
	public static final String NEW_HOME_FEED_EXPOSURE= "moses:new_home_feed_exposure";

	/**
	 * 新首页feeds流曝光缓冲区
	 */
	public static final String NEW_HOME_FEED_EXPOSURE_CACHE= "moses:new_home_feed_exposure_cache";

	/**
	 * 新类目页feeds流曝光缓冲区
	 */
	public static final String NEW_CATEGORY_FEED_EXPOSURE_CACHE_PREFIX= "moses:new_cate_imp_cache_";

	/**
	 * 用户在上新标签点击时间戳缓存
	 */
	public static final String NEW_PRODUCT_TAG_TIME="moses:new_products_business_flag";

	/**
	 * feeds流插入的商品信息，value格式为pid:time
	 * 过期时间3天
	 * 类型：list
	 * 大小：100
	 */
	public static final String FEED_INSERT_PID_PREFIX = "moses:feed_insert_pid_";

	/**
	 * 每个每日上新商品已曝光的次数
	 * 过期时间:无
	 * 类型：String
	 */
	public static final String MOSES_DN_PRODUCT_COUNT_PREFIX = "moses:DN_product_count_";

	/**
	 * 用户已曝光的每日上新商品信息
	 * 过期时间:1天
	 * 类型：set
	 */
	public static final String MOSES_DNUVP_PREFIX = "moses:DNUVP_";

	/**
	 * 用户已曝光的首页轮播图商品信息
	 * 过期时间:7天
	 * 类型：String
	 */
	public static final String MOSES_EXPOSURE_TO_UUID_PREFIX = "moses:exposureToUuid_";

	/**
	 * 新手专享运营配置的商品中对用户曝光的商品集合信息
	 * 过期时间:3天
	 * 类型：String
	 * value格式：productId:count,productId:count,...  其中：productId为商品ID，count为该商品对用户的曝光次数
	 */
	public static final String MOSES_NEWUSER_EXPOSURE_PRODUCT_PREFIX = "moses:newuser_exposure_product_";


	/**
	 * 新类目页假曝光redis key前缀
	 */
	public static final String CATEGORY_FAKE_EXPOSURE_PREFIX= "moses:cate_exp_";

	/**
	 * 新类目页假曝光第一页未统计到假曝光库中的商品缓存信息redis key前缀
	 */
	public static final String CATEGOTY_FAKE_EXPOSURE_FIRST_PAGE_CACHE_PREFIX= "moses:cate_exp_cache_";

	/**
	 * 视频流落地页分页信息 的商品缓存信息redis key前缀
	 */
	public static final String VIDEO_PAGE_CACHE_PREFIX= "moses:vid:cache:";

	/**
	 * 视频流落地页曝光的商品缓存信息redis key前缀
	 */
	public static final String VIDEO_EXPO_CACHE_PREFIX = "moses:vid:expo:";
}