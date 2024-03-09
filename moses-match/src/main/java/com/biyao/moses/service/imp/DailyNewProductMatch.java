package com.biyao.moses.service.imp;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.biyao.dclog.service.DCLogger;
import com.biyao.moses.cache.DailyNewExposureCache;
import com.biyao.moses.cache.DailyNewProductCache;
import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.common.constant.CacheRedisKeyConstant;
import com.biyao.moses.common.constant.CommonConstants;
import com.biyao.moses.common.constant.RedisKeyConstant;
import com.biyao.moses.common.enums.MatchStrategyEnum;
import com.biyao.moses.model.exp.MatchDataSourceTypeConf;
import com.biyao.moses.model.match.ProductImage;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.service.RecommendMatch;
import com.biyao.moses.util.CacheRedisUtil;
import com.biyao.moses.util.FilterUtil;
import com.biyao.moses.util.RedisUtil;
import com.biyao.moses.params.ProductInfo;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component("DailyNewProductMatch")
public class DailyNewProductMatch implements RecommendMatch {

	@Autowired
	private RedisUtil redisUtil;

	@Autowired
	private CacheRedisUtil cacheRedisUtil;

	@Autowired
	private DailyNewProductCache dailyNewProductCacheNoCron;

	@Autowired
	private ProductDetailCache productDetailCacheNoCron;

	@Autowired
	private DailyNewExposureCache dailyNewExposureCache;
	@Autowired
	private FilterUtil filterUtil;

	// 三级类目偏好redisKey
	private static final String REDIS_KEY_LEVEL3HOBBY = "moses:level3hobby_";

	private static DCLogger mosesuiDcLogger = DCLogger.getLogger("moses_daily_new_exposure");

	@BProfiler(key = "com.biyao.moses.service.imp.DailyNewProductMatch.executeRecommendMatch", monitorType = {
			MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR })
	@Override
	public Map<String, List<TotalTemplateInfo>> executeRecommendMatch(String dataKey, MatchDataSourceTypeConf mdst,
			String uuId) {
		
		Map<String, List<TotalTemplateInfo>> resultMap = new HashMap<>();// 结果集
		List<TotalTemplateInfo> list = new ArrayList<TotalTemplateInfo>();// list结果集
		Long dailyNewProductId = null;// 每日上新商品Id
		List<ProductImage> dailyNewProductList = new ArrayList<ProductImage>(); // 每日上新商品源集合
		List<Long> userCatePreferProductList = new ArrayList<Long>();// 类目偏好List
		List<Long> userSexPreferProductList = new ArrayList<Long>();// 获取用户性别List
		List<Long> nonSexProductList = new ArrayList<Long>();// 无性别的商品List
		Map<String, String> matchContent = new HashMap<String, String>();//融合召回内容，用map存放，日志格式使用json格式字符串
		String sex = mdst.getSex();
		String userPreferCate3Str = "";
		
		try {
			if(mdst.isPersonalizedRecommendSwitch()) {
				// 三级类目
				// 存储格式 catfegory3Id:count,catfegory3Id:count,catfegory3Id:count
				userPreferCate3Str = redisUtil.getString(REDIS_KEY_LEVEL3HOBBY + uuId);
				List<Long> userPreferCateIdList = new ArrayList<Long>();
				if (StringUtils.isNotBlank(userPreferCate3Str)) {
					userPreferCateIdList = Arrays.stream(userPreferCate3Str.split(",")).map(cateScoreStr -> {
						String cateId = cateScoreStr.split(":")[0];
						return Long.valueOf(cateId);
					}).collect(Collectors.toList());
				}
				sex = StringUtils.isBlank(sex) ? CommonConstants.UNKNOWN_SEX : sex;
				// 获取对用户已曝光的每日上新商品
				Set<String> userViewedProductSet = cacheRedisUtil.smems(CacheRedisKeyConstant.MOSES_DNUVP_PREFIX + uuId);
				if (userViewedProductSet == null) {
					userViewedProductSet = new HashSet<>();
				}
//			log.error("用户已曝光的每日上新商品集合userViewedProductSet：{}", JSONObject.toJSONString(userViewedProductSet));
				// 获取每日上新数据源格式
				dailyNewProductList = dailyNewProductCacheNoCron.getAllProductList();
				if (CollectionUtils.isEmpty(dailyNewProductList)) {
					dailyNewProductList = dailyNewProductCacheNoCron.getAllProductListByRedis();
				}

				if (CollectionUtils.isEmpty(dailyNewProductList))
					return emptyResult(resultMap, list, dataKey);
				Iterator<ProductImage> it = dailyNewProductList.iterator();
				while (it.hasNext()) {
					ProductImage productImage = it.next();

					Long productId = productImage.getProductId();

					ProductInfo productInfo = productDetailCacheNoCron.getProductInfo(productId);
					if (FilterUtil.isCommonFilter(productInfo)) {
						it.remove();
						continue;
					}
					if (filterUtil.isFilteredBySiteId(productId, mdst.getSiteId())) {
						continue;
					}
					if (userViewedProductSet.contains(productId.toString())) {
						continue;
					}

					Long category3Id = productInfo.getThirdCategoryId();
					Long category2Id = productInfo.getSecondCategoryId();
					// 二级类目是眼镜的需要特殊处理
					if (CommonConstants.SPECIAL_CATEGORY2_IDS.contains(category2Id)) {
						category3Id = category2Id;
					}
					// 三级类目偏好
					if (userPreferCateIdList.contains(category3Id)) {
						userCatePreferProductList.add(productId);
						continue;
					}
					// 用户性别相同的商品
					String productGender = productInfo.getProductGender() == null ? CommonConstants.UNKNOWN_SEX : productInfo.getProductGender().toString();
					if (StringUtils.isBlank(sex) || CommonConstants.UNKNOWN_SEX.equals(sex) ||
							(CommonConstants.MALE_SEX.equals(sex) && (CommonConstants.MALE_SEX.equals(productGender) || CommonConstants.COMMON_SEX.equals(productGender))) ||
							(CommonConstants.FEMALE_SEX.equals(sex) && (CommonConstants.FEMALE_SEX.equals(productGender) || CommonConstants.COMMON_SEX.equals(productGender)))) {
						userSexPreferProductList.add(productId);
					} else {
						// 无性别的商品
						nonSexProductList.add(productId);
					}

				}

				// 对3个List进行排序，将曝光次数少于1000次的商品排在前面
				userCatePreferProductList = sortList(userCatePreferProductList);
				userSexPreferProductList = sortList(userSexPreferProductList);
				nonSexProductList = sortList(nonSexProductList);

				// 按照用户类目偏好、性别相同、无性别的优先顺序推荐出第一个商品
				dailyNewProductId = getDailyProduct(matchContent, uuId, dailyNewProductList, userSexPreferProductList, nonSexProductList,
						userCatePreferProductList);
				// 将本次每日上新推荐的商品加入到用户每日上新已曝光的集合中
				cacheRedisUtil.sadd(CacheRedisKeyConstant.MOSES_DNUVP_PREFIX + uuId, dailyNewProductId.toString());
				cacheRedisUtil.expire(CacheRedisKeyConstant.MOSES_DNUVP_PREFIX + uuId, 60 * 60 * 24);
			}else{
				dailyNewProductId = getDailyNewProductByRandom(mdst);
				if(dailyNewProductId == null){
					return emptyResult(resultMap, list, dataKey);
				}
			}
			TotalTemplateInfo totalTemplateInfo = getTotalTemplateList(dailyNewProductId);
			list.add(totalTemplateInfo);
			// 打印本次每日上新曝光的日志
			printMosesMatchLog(mdst, dailyNewProductId, uuId,userPreferCate3Str,matchContent,sex);
			// 计算本次每日上新推荐的商品的曝光次数
			cacheRedisUtil.incr(CacheRedisKeyConstant.MOSES_DN_PRODUCT_COUNT_PREFIX + dailyNewProductId);
		} catch (Exception e) {
			log.error("[严重异常]每日上新DailyNewProductMatch异常,dataKey:{},uuid:{}",dataKey,uuId,e);
		}
		resultMap.put(dataKey, list);
		return resultMap;
	}

	/**
	 * 在过滤掉下架、定制（定制咖啡除外）、分端隐藏后的商品中随机选择一个
	 * @return
	 */
	private Long getDailyNewProductByRandom(MatchDataSourceTypeConf mdst){
		List<ProductImage>  dailyNewProductList = dailyNewProductCacheNoCron.getAllProductList();
		if (CollectionUtils.isEmpty(dailyNewProductList)) {
			dailyNewProductList = dailyNewProductCacheNoCron.getAllProductListByRedis();
		}

		if (CollectionUtils.isEmpty(dailyNewProductList)){
			return null;
		}

		Iterator<ProductImage> it = dailyNewProductList.iterator();
		while (it.hasNext()) {
			ProductImage productImage = it.next();
			if(productImage == null){
				it.remove();
				continue;
			}
			Long productId = productImage.getProductId();

			ProductInfo productInfo = productDetailCacheNoCron.getProductInfo(productId);
			if (FilterUtil.isCommonFilter(productInfo)) {
				it.remove();
				continue;
			}
			if (filterUtil.isFilteredBySiteId(productId, mdst.getSiteId())) {
				it.remove();
				continue;
			}
		}

		if (CollectionUtils.isEmpty(dailyNewProductList)){
			return null;
		}

		Random random = new Random();
		int n = random.nextInt(dailyNewProductList.size());
		return dailyNewProductList.get(n).getProductId();
	}

	/**
	 * @Description 封装totalTemplateInfo
	 * @param dailyNewProductId
	 * @return TotalTemplateInfo
	 * @version V1.0
	 * @auth 邹立强 (zouliqiang@idstaff.com)
	 */
	private TotalTemplateInfo getTotalTemplateList(Long dailyNewProductId) {
		ProductImage dailyNewProduct = dailyNewProductCacheNoCron.getProductImgInfo(dailyNewProductId);
		TotalTemplateInfo totalTemplateInfo = new TotalTemplateInfo();
		totalTemplateInfo.setId(dailyNewProductId.toString());
		List<String> images = new ArrayList<String>();
		List<String> imagesWebp = new ArrayList<String>();
		images.add(dailyNewProduct.getImage());
		imagesWebp.add(dailyNewProduct.getWebpImage());
		totalTemplateInfo.setLongImages(images);
		totalTemplateInfo.setLongImagesWebp(imagesWebp);
		totalTemplateInfo.setImages(images);
		totalTemplateInfo.setImagesWebp(imagesWebp);
		totalTemplateInfo.setFeedLongProducts(Collections.singletonList(dailyNewProductId));
		totalTemplateInfo.setMainTitle("");
		totalTemplateInfo.setSubtitle("");
		return totalTemplateInfo;
	}

	/**
	 * @Description 按照用户类目偏好、性别相同、无性别的优先顺序推荐出第一个商品
	 * @param uuId
	 * @param dailyNewProductList
	 * @param userSexPreferProductList
	 * @param nonSexProductList
	 * @param userPreferCateIdList
	 * @return Long
	 * @version V1.0
	 * @auth 邹立强 (zouliqiang@idstaff.com)
	 */
	private Long getDailyProduct(Map<String, String> matchContent, String uuId, List<ProductImage> dailyNewProductList,
			List<Long> userSexPreferProductList, List<Long> nonSexProductList, List<Long> userPreferCateIdList) {
		Long dailyNewProductId = null;
		if (userPreferCateIdList.size() > 0) {
			dailyNewProductId = userPreferCateIdList.get(0);
			matchContent.put(MatchStrategyEnum.CATE.getName(),dailyNewProductId.toString());
			log.info("推荐理由：从用户偏好中获取 dailyNewProductId：{}", dailyNewProductId);
			return dailyNewProductId;
		}
		if (dailyNewProductId == null && userSexPreferProductList.size() > 0) {
			dailyNewProductId = userSexPreferProductList.get(0);
			matchContent.put(MatchStrategyEnum.SEX.getName(),dailyNewProductId.toString());
			log.info("推荐理由：从性别集合中获取 dailyNewProductId：{}", dailyNewProductId);
			return dailyNewProductId;
		}

		if (dailyNewProductId == null && nonSexProductList.size() > 0) {
			dailyNewProductId = nonSexProductList.get(0);
			matchContent.put(MatchStrategyEnum.NO_SEX.getName(),dailyNewProductId.toString());
			log.info("推荐理由：从无性别集合中获取 dailyNewProductId：{}", dailyNewProductId);
			return dailyNewProductId;
		}
		// 每日上新商品不为空
		Random random = new Random();
		int n = random.nextInt(dailyNewProductList.size());
		dailyNewProductId = dailyNewProductList.get(n).getProductId();
		// 将用户已曝光的商品redis缓存删除
		cacheRedisUtil.del(CacheRedisKeyConstant.MOSES_DNUVP_PREFIX + uuId);
		matchContent.put(MatchStrategyEnum.RAND.getName(),dailyNewProductId.toString());
		log.info("推荐理由：随机获取新品 dailyNewProductId：{}", dailyNewProductId);
		return dailyNewProductId;
	}

	/**
	 * @Description List进行排序，将曝光次数少于1000次的商品排在前面
	 * @param list
	 * @return List<Long>
	 * @version V1.0
	 * @auth 邹立强 (zouliqiang@idstaff.com)
	 */
	private List<Long> sortList(List<Long> list) {
		if (CollectionUtils.isEmpty(list))
			return list;
		List<Long> tmpList = new ArrayList<Long>();
		Iterator<Long> productIterator = list.iterator();
		while (productIterator.hasNext()) {
			long productId = productIterator.next();
			Map<Long, Integer> dailyNewExposureMap = dailyNewExposureCache.getDailyNewExposureMap();
			int exposureCount = 0;
			if (!dailyNewExposureMap.isEmpty()) {
				exposureCount = dailyNewExposureMap.getOrDefault(productId, 0);
			}
			if (exposureCount < 1000) {
				productIterator.remove();
				tmpList.add(productId);
			}
		}
		list.addAll(0, tmpList);
		return list;
	}

	/**
	 * @Description 打印曝光日志
	 * @param mdst
	 * @param productId
	 * @param uuId
	 * @return void
	 * @version V1.0
	 * @auth 邹立强 (zouliqiang@idstaff.com)
	 */
	private void printMosesMatchLog(MatchDataSourceTypeConf mdst, Long productId, String uuId,String ucprefer,Map<String,String> matchContent,String sex) {
		// uuid=xxx\tpids=pid1,pid2\tst=1231\ttopicId=120131\tuid=\tstid=siteId\t
		StringBuffer logStr = new StringBuffer();
		logStr.append("lt=moses_daily_new_exposure").append("\t");
		String uu = StringUtils.isNotBlank(uuId) ? uuId : "";
		logStr.append("uu=").append(uu).append("\t");
		String productIdStr = productId != null ? productId.toString() : "";
		logStr.append("pids=").append(productIdStr).append("\t");
		String topicId = StringUtils.isNotBlank(mdst.getDataSourceType()) ? mdst.getDataSourceType() : "";
		logStr.append("topicId=").append(topicId).append("\t");
		String uid = StringUtils.isNotBlank(mdst.getUid()) ? mdst.getUid() : "";
		logStr.append("u=").append(uid).append("\t");
		String siteId = !StringUtils.isEmpty((mdst.getSiteId())) ? mdst.getSiteId() : "";
		logStr.append("stid=" + siteId + "\t");
	    ucprefer = !StringUtils.isEmpty((ucprefer)) ? ucprefer : "";
		logStr.append("ucprefer=" + ucprefer + "\t");
		sex = !StringUtils.isEmpty((sex)) ? sex : "";
		logStr.append("usex=" + sex + "\t");
		logStr.append("mixmatch=" + JSON.toJSONString(matchContent) + "\t");
		Date d = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		logStr.append("st=" + sdf.format(d));
		String logString = logStr.toString();
		mosesuiDcLogger.printDCLog(logString);
	}

	/**
	 * @Description 空结果集
	 * @param result
	 * @param list
	 * @param dataKey
	 * @return Map<String,List<TotalTemplateInfo>>
	 * @version V1.0
	 * @auth 邹立强 (zouliqiang@idstaff.com)
	 */
	private Map<String, List<TotalTemplateInfo>> emptyResult(Map<String, List<TotalTemplateInfo>> result,
			List<TotalTemplateInfo> list, String dataKey) {
		list.add(new TotalTemplateInfo());
		log.error("[严重异常]推荐每日上新商品结果集totalTemplateInfoList为空");
		result.put(dataKey, list);
		return result;
	}

}
