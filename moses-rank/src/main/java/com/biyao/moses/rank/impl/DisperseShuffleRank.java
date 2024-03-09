package com.biyao.moses.rank.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.rank.RankRequest;
import com.biyao.moses.params.ProductInfo;

import lombok.extern.slf4j.Slf4j;

/**
 * 乱序，打散rank
 * @Description 
 */
@Slf4j
@Component("dsr")
public class DisperseShuffleRank extends ShuffleRank {

	@Autowired
	ProductDetailCache productDetailCache;

	// 相同类目的商品间隔距离
	public static final int PRODUCT_SPACING = 4;
	// 无法打散的商品类目间隔距离
	public static final int LAST_ARRAY_LENGTH = 1;

	@BProfiler(key = "DisperseShuffleRank.executeRecommend", monitorType = {
			MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR })
	@Override
	public List<TotalTemplateInfo> executeRecommend(RankRequest rankRequest) {
		log.info("DisperseShuffleRank请求参数，uuid={}", rankRequest.getUuid());
		List<TotalTemplateInfo> executeRecommend = super.executeRecommend(rankRequest);
		categoryShuffleRank(executeRecommend);
		return executeRecommend;
	}

	public void categoryShuffleRank(List<TotalTemplateInfo> executeRecommend) {
		try {
			executeRecommend.stream().forEach(i -> {
				ProductInfo productInfo = productDetailCache.getProductInfo(Long.valueOf(i.getId()));
				if (productInfo != null) {
					i.setLevel2Category(String.valueOf(productInfo.getSecondCategoryId()));
				}
			});
		} catch (Exception e) {
			log.error("获取后台二级类目id错误", e);
		}
		// 准备数据结束---------------------------
		List<TotalTemplateInfo> temProductList = new ArrayList<>();

		List<TotalTemplateInfo> resultList = new ArrayList<>();
		// 分类目打散开始--------------------------------------

		// 二级类目id
		String[] level2Category = new String[PRODUCT_SPACING];
		// 当前数组中的数据用map结构存储，
		Map<String, String> categoryMap = new HashMap<>();
		temProductList = composeData(executeRecommend, temProductList, resultList, PRODUCT_SPACING, level2Category,
				categoryMap, false, false);
		// resultList.addAll(temProductList);
		executeRecommend.clear();
		executeRecommend.addAll(resultList);

	}

	/**
	 * 按类目打散数重新组合数据
	 * 
	 * @param list
	 *            源数据
	 * @param newList
	 *            未能分类目处理的数据
	 * @param resultList
	 *            按类目打散返回的数据
	 * @param productSpacing
	 *            类目打算间隔
	 * @param level2Category
	 *            2级类目数组
	 * @param categoryMap
	 *            当前2级类目map缓存
	 * @param isRecursion
	 *            是否为递归数组，指未能分类目处理的数据（newlist）
	 * @param isBreak
	 *            是否停止，当最后一次递归的时候为true
	 * @return
	 */
	private static List<TotalTemplateInfo> composeData(List<TotalTemplateInfo> list, List<TotalTemplateInfo> newList,
			List<TotalTemplateInfo> resultList, int productSpacing, String[] level2Category,
			Map<String, String> categoryMap, boolean isRecursion, boolean isBreak) {
		int leftNum = 0;
		try {
			Iterator<TotalTemplateInfo> it = list.iterator();
			while (it.hasNext()) {
				TotalTemplateInfo nextTemplateInfo = it.next();
				// 当前商品的二级类目id
				String categoryId = nextTemplateInfo.getLevel2Category();
				// 如果当前类目id与数组中类目id存在相同的，则存储到newlist中
				if (categoryMap.containsKey(categoryId)) {
					newList.add(nextTemplateInfo);
				} else {
					if (leftNum >= productSpacing && !isRecursion) {
						leftNum = 1;
					} else if (!isRecursion) {
						++leftNum;
					}
					// 数组依次向右移动一位，首位为新的类目id
					moveArray(categoryId, level2Category, categoryMap, productSpacing);
					resultList.add(nextTemplateInfo);
					if (isRecursion) {
						it.remove();
					}
					// 当前数组循环完成一轮，则遍历newList添加元素
					if (leftNum == productSpacing && !isRecursion) {
						composeData(newList, new ArrayList<>(), resultList, productSpacing, level2Category, categoryMap,
								 true, false);// , totalMap
					}
				}
				if (!it.hasNext() && !isRecursion && !isBreak) {
					String[] levelLastCategory = new String[LAST_ARRAY_LENGTH];
					categoryMap.clear();
					for (int i = 0; i < levelLastCategory.length; i++) {
						levelLastCategory[i] = level2Category[i];
						categoryMap.put(level2Category[i], level2Category[i]);
					}

					composeData(newList, new ArrayList<>(), resultList, productSpacing, levelLastCategory, categoryMap,
							 true, true);// , totalMap
				}
			}
		} catch (Exception e) {
			log.error("按类目打散异常!", e);
		}

		return newList;
	}

	/**
	 * 向右偏移类目数组
	 * 
	 * @param firstStr
	 * @param level3Category
	 * @param categoryMap
	 * @param productSpacing
	 */
	public static void moveArray(String firstStr, String[] level3Category, Map<String, String> categoryMap,
			int productSpacing) {
		categoryMap.clear();
		for (int i = level3Category.length - 1; i >= 1; i--) {
			level3Category[i] = level3Category[i - 1];
			categoryMap.put(level3Category[i], level3Category[i]);
		}
		level3Category[0] = firstStr;
		categoryMap.put(firstStr, firstStr);
	}

}
