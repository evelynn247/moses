package com.biyao.moses.util;

import com.biyao.moses.cache.BlackListCacheNoCron;
import com.biyao.moses.cache.ProductDetailCacheNoCron;
import com.biyao.moses.common.constant.CommonConstants;
import com.biyao.moses.common.constant.RedisKeyConstant;
import com.biyao.moses.common.enums.PlatformEnum;
import com.biyao.moses.model.filter.FilterCond;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.params.ProductInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

@Slf4j
@Component
public class FilterUtil {

	@Autowired
	RedisUtil redisUtil;

	@Autowired
	ProductDetailCacheNoCron productDetailCacheNoCron;

	private static final String ENTER_PIC_PREFIX = "moses:enterPic_";

	private static final String ENTER_PIC_DEFAULT = "default";

	/**
	 * 
	 * @param sourceList
	 *            未过滤数据
	 * @param productInfoList
	 *            查询detail得到的数据详情
	 * @param siteId
	 * @return
	 */
	public List<TotalTemplateInfo> filterNotRecommend(List<TotalTemplateInfo> sourceList,
			List<ProductInfo> productInfoList, String siteId, String uuid, Set<String> topicIdList, String pageId) {
		FilterCond filterCond = new FilterCond();
		filterCond.filterSoldOut = true;
		List<TotalTemplateInfo> result = baseFilter(sourceList, productInfoList, filterCond, siteId, uuid, topicIdList,
				pageId);
		return result;
	}

	/**
	 * 基础过滤，过滤下架和对应平台
	 * 
	 * @param sourceList
	 * @param productInfoList
	 * @param filterCond
	 * @return
	 */
	private List<TotalTemplateInfo> baseFilter(List<TotalTemplateInfo> sourceList, List<ProductInfo> productInfoList,
			FilterCond filterCond, String siteId, String uuid, Set<String> topicIdList, String pageId) {
		List<TotalTemplateInfo> resultTotalInfo = new ArrayList<>();
		Map<Long, ProductInfo> productsMap = productInfoList.stream()
				.collect(Collectors.toMap(ProductInfo::getProductId, p -> p));

		// uuid2topicid过滤key
		String prefix = RedisKeyConstant.MOSES_BLACK_UUID_TOPICID_KEY_PREFIX;
		Map<String, Map<String, String>> uuid2topicMap = new HashMap<>();
		for (String topicId : topicIdList) {
			String uuid2topicidKey = prefix + "_" + uuid + "_" + topicId;
			String redisResult = redisUtil.getString(uuid2topicidKey);
			if (StringUtils.isNotEmpty(redisResult)) {
				Map<String, String> map = new HashMap<>();
				String[] split = redisResult.split(",");
				for (int i = 0; i < split.length; i++) {
					map.put(split[i], split[i]);
				}
				uuid2topicMap.put(topicId, map);
			} else {
				uuid2topicidKey = prefix + "_default_" + topicId;
				String defaultResult = redisUtil.getString(uuid2topicidKey);
				Map<String, String> map = new HashMap<>();
				if (StringUtils.isNotEmpty(defaultResult)) {
					String[] split = defaultResult.split(",");
					for (int i = 0; i < split.length; i++) {
						map.put(split[i], split[i]);
					}
					uuid2topicMap.put(topicId, map);
				}
			}
		}

		for (int j = 0; j < sourceList.size(); j++) {
			TotalTemplateInfo totalTemplate = sourceList.get(j);
			ProductInfo productInfo = productsMap.get(Long.valueOf(totalTemplate.getId()));
			if (productInfo == null) {
				continue;
			}
			// 过滤下架商品
			if (!productInfo.getShelfStatus().toString().equals("1")) {
				continue;
			}

			// 通过用户的平台 判断当前商品是否展示 siteId为空字符串是不进行过滤
			if (StringUtils.isEmpty(productInfo.getSupportPlatform())
					|| (!CommonConstants.NO_SITE_FILTER.contains(siteId) &&!productInfo.getSupportPlatform().contains(siteId))) {
				// 如果当前商品支持平台为空，或者支持平台中不包含当前用户使用的平台
				continue;
			}
			if(CollectionUtils.isEmpty(productInfo.getSupportChannel()) || !productInfo.getSupportChannel().contains(PlatformEnum.getChannelTypeBySiteId(siteId))){
				continue;
			}

			// 黑名单过滤
			if (filterBlackList(productInfo, uuid, topicIdList, pageId, uuid2topicMap)) {
				//log.info("黑名单过滤掉的商品id：" + productInfo.getProductId());
				continue;
			}

			resultTotalInfo.add(totalTemplate);

		}

		//log.info("[baseFilter过滤后,num={}]", resultTotalInfo.size());
		return resultTotalInfo;
	}

	/**
	 * 判断商品是否应该被过滤，通用过滤逻辑
	 * @param productInfo
	 * @return
	 */
	public static boolean isCommonFilter(ProductInfo productInfo){
		if(productInfo == null){
			return true;
		}

		//过滤下架商品
		if(productInfo.getShelfStatus() == null || !productInfo.getShelfStatus().toString().equals("1")){
			return true;
		}

		//过滤定制商品
		if(productInfo.getSupportTexture() != null && productInfo.getSupportTexture().toString().equals("1")){
			return true;
		}

		return false;
	}

	/**
	 * 新手专享基础条件过滤
	 * @param productInfo
	 * @return true 需要被过滤，false 不需要被过滤
	 */
	public static boolean isFilteredByBaseXszxCond(ProductInfo productInfo){
		if(isCommonFilter(productInfo)){
			return true;
		}
		//过滤非一起拼商品
		if (productInfo.getIsToggroupProduct() == null ||
				!productInfo.getIsToggroupProduct().toString().equals("1")){
			return true;
		}
		//过滤定制咖啡
		if ((productInfo.getSupportTexture() != null &&
				productInfo.getSupportTexture().toString().equals("2"))){
			return true;
		}
		//过滤低模眼镜
		if ((productInfo.getRasterType() != null &&
				productInfo.getRasterType().toString().equals("1"))){
			return true;
		}
		//过滤眼镜
		if (productInfo.getSecondCategoryId() == null ||
				CommonConstants.GLASSES_CATEGORY2_IDS.contains(productInfo.getSecondCategoryId())) {
			return true;
		}
		return false;
	}

	/**
	 * 不满足新手专享的条件需要被过滤
	 * @param productInfo
	 * @return true 需要过滤，false 不需要过滤
	 */
	public static boolean isFilteredByAllXszxCond(ProductInfo productInfo){

		if(isFilteredByBaseXszxCond(productInfo)){
			return true;
		}

//		//过滤价格大于500元的商品，pdc库中的价格精确到分，因此此处价格为大于50000分
//		if(productInfo.getPrice() != null && productInfo.getPrice().intValue() > 50000){
//			return true;
//		}
//
//		//过滤好评数小于10的商品
//		if(productInfo.getPositiveComment() == null || productInfo.getPositiveComment() < 10){
//			return true;
//		}

		return false;
	}

	/**
	 * 黑名单过滤
	 * 
	 * @param productInfo
	 * @param uuid
	 * @param topicIdList
	 * @param pageId
	 * @return
	 */
	private static boolean filterBlackList(ProductInfo productInfo, String uuid, Set<String> topicIdList, String pageId,
			Map<String, Map<String, String>> uuid2topicMap) {
		boolean flag = false;
		String spuId = String.valueOf(productInfo.getProductId());
		try {
			// 全局过滤
			if (BlackListCacheNoCron.getBlackListAllProduct(spuId)) {
				flag = true;// 是黑名单
			}
			// 页面过滤
			if (BlackListCacheNoCron.getBlackListPageProduct(pageId, spuId)) {
				flag = true;
			}
			// topic过滤
			for (String topicId : topicIdList) {
				if (BlackListCacheNoCron.getBlackListTopicProduct(topicId, spuId)) {
					flag = true;
				}
				if (!uuid2topicMap.isEmpty() && !uuid2topicMap.get(topicId).isEmpty()
						&& uuid2topicMap.get(topicId).containsKey(spuId)) {
					flag = true;
				}
			}
			// 用户黑名单过滤
			if (BlackListCacheNoCron.getBlackListUserProduct(uuid, spuId)) {
				flag = true;
			}

		} catch (Exception e) {
			log.error("[严重异常]黑名单过滤异常！", e);
		}
		return flag;
	}

	/**
	 * 将redis中保存的入口商品插入到排序后的结果集前
	 * 
	 * @return
	 */
	public Map<String, List<TotalTemplateInfo>> insertEnterPics(Map<String, List<TotalTemplateInfo>> data,
			String tabtopicId, String uuid) {

		// 判空
		try {
			if (data != null && !data.isEmpty()) {

				for (Entry<String, List<TotalTemplateInfo>> entry : data.entrySet()) {

					List<TotalTemplateInfo> value = entry.getValue();

					if (value != null && value.size() > 0) {

						if (StringUtils.isNotEmpty(value.get(0).getId())) {

							//先getString类型获取入口图数据，没有再hget获取
							String enterPicStr = redisUtil.getString(ENTER_PIC_PREFIX + uuid + "_" + tabtopicId);
							if (StringUtils.isEmpty(enterPicStr)) {
								enterPicStr = redisUtil
										.getString(ENTER_PIC_PREFIX + ENTER_PIC_DEFAULT + "_" + tabtopicId);
							}

							String hget = "";
							if (StringUtils.isEmpty(enterPicStr)) {
								hget = redisUtil.hget(ENTER_PIC_PREFIX + uuid, tabtopicId);

								if (StringUtils.isEmpty(hget)) {
									hget = redisUtil.hget(ENTER_PIC_PREFIX + ENTER_PIC_DEFAULT, tabtopicId);
								}
							} else {
								hget = enterPicStr;
							}

							if (StringUtils.isNotEmpty(hget)) {

								// 查找当前数据集中已经包含了要插入商品的位置
								ArrayList<TotalTemplateInfo> deleteElements = new ArrayList<TotalTemplateInfo>();
								for (int i = 0; i < value.size(); i++) {
									if (hget.contains(value.get(i).getId())) {
										deleteElements.add(value.get(i));
									}
								}
								// 将重复商品从当前数据集中删除
								for (int i = 0; i < deleteElements.size(); i++) {
									value.remove(deleteElements.get(i));
								}
								// 将入口图商品添加到数据集前面
								String[] split = hget.split(",");

								if (split != null && split.length > 0) {
									List<String> asList = Arrays.asList(split);

									if (CollectionUtils.isNotEmpty(asList)) {
										Collections.shuffle(asList);

										for (String pid : asList) {
											if (StringUtils.isNotBlank(pid)) {

												com.biyao.moses.params.ProductInfo productInfo = productDetailCacheNoCron
														.getProductInfo(Long.valueOf(pid));
												if (productInfo != null
														&& !productInfo.getShelfStatus().toString().equals("1")) {// 当前商品下架
													continue;
												}
												TotalTemplateInfo totalTemplateInfo = new TotalTemplateInfo();
												totalTemplateInfo.setId(pid);
												value.add(0, totalTemplateInfo);
											}
										}

									}
								}
								break;
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return data;
	}

	/**
	 * feed流插入优先展示的商品
	 * @param data 原始数据
	 * @param priorityProductId 需要置顶的商品ID
	 * @return
	 */
	public Map<String, List<TotalTemplateInfo>> insertPriorityProduct(Map<String, List<TotalTemplateInfo>> data, String priorityProductId,String siteId){
		try {
			if (data != null && !data.isEmpty()) {
				ProductInfo productInfo = productDetailCacheNoCron.getProductInfo(Long.valueOf(priorityProductId));
				// 当前商品不存在或者已下架或者置顶商品为定制商品，则直接返回
				if (FilterUtil.isCommonFilter(productInfo)) {
					return data;
				}
				// 当前商品不支持用户所持有的端，则直接返回
				if(isFilteredBySiteId(Long.valueOf(priorityProductId),siteId)){
					return data;
				}
				//当置顶的商品 data中存在，且其位置在活动入口位置前时，需要把活动入口的位置前移一位。
				//目前只有类目页、轮播图落地页有置顶商品要求，不需要考虑隔断。
				//判断是否已插入到feed流中
				boolean insertFlag = false;
				boolean priorityProductFlag = false; //集合中是否存在置顶商品
				TotalTemplateInfo priorityProductInfo = null; //集合中存在置顶商品时，信息放到这
				boolean advertFlag = false;//是否存在广告活动
				List<Integer> advertPositionList = new ArrayList<>();//广告活动的索引位置集合
//				int advertPosition = 0;//广告活动的索引位置
				for (Entry<String, List<TotalTemplateInfo>> entry : data.entrySet()) {
					List<TotalTemplateInfo> value = entry.getValue();
					// 如果value为null或者为空，则跳过
					if (value == null || value.size() == 0){
						continue;
					}
					int i = 0;
					// 删除当前数据集中已经包含了要插入商品，则删除。
					Iterator<TotalTemplateInfo> iterable = value.iterator();
					while (iterable.hasNext()){
						TotalTemplateInfo info = iterable.next();
						//最多只有1个活动广告
						if(CommonConstants.SHOW_TYPE_ADVERT.equals(info.getShowType())){
							advertFlag = true;
							advertPositionList.add(i);
						}
						if (priorityProductId.equals(info.getId())){
							priorityProductFlag = true;
							priorityProductInfo = info;
							iterable.remove();
							break;
						}
						i++;
					}

					if (!insertFlag){
						if(priorityProductInfo == null) {
							priorityProductInfo = new TotalTemplateInfo();
							priorityProductInfo.setId(priorityProductId);
						}
						value.add(0, priorityProductInfo);
						insertFlag = true;
						//如果在置顶商品前存在广告，则该广告位置前移一位
						if(priorityProductFlag && advertFlag){
							for(Integer advertPosition : advertPositionList) {
								TotalTemplateInfo advertInfo = value.get(advertPosition + 1);
								value.remove(advertPosition + 1);
								value.add(advertPosition, advertInfo);
							}
						}
					}
				}
			}
		}catch (Exception e){
			log.error("[严重异常]插入优先展示商品失败：{}", priorityProductId, e);
		}

		return data;
	}

	/**
	 * 判断商品是否应该被过滤
	 * 过滤条件：不支持请求端的普通商品
	 * @param productId 普通商品id
	 * @param siteId 请求端id
	 * @return true 应该被过滤   false 不应该被过滤
	 */
	public boolean isFilteredBySiteId(Long productId,String siteId){
		//  满足任一一个则过滤 ：1） 商品信息为空 2） 商品信息支持平台为空 3） 用户siteid不为空,且不在不过滤的set中 且商品支持平台中不包含当前用户使用的平台
		ProductInfo productInfo=productDetailCacheNoCron.getProductInfo(productId);
		if(productInfo == null || productInfo.getSupportPlatform() == null || CollectionUtils.isEmpty(productInfo.getSupportChannel()) ){
			return true;
		}
		if(CollectionUtils.isEmpty(productInfo.getSupportChannel()) || !productInfo.getSupportChannel().contains(PlatformEnum.getChannelTypeBySiteId(siteId))){
			return true;
		}
		if(!StringUtil.isBlank(siteId) && !CommonConstants.NO_SITE_FILTER.contains(siteId.trim()) && !productInfo.getSupportPlatform().contains(siteId.trim())){
			return true;
		}

		return false;
	}
}