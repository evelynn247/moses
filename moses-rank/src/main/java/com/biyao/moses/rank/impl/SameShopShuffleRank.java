package com.biyao.moses.rank.impl;

import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.rank.RankRequest;
import com.biyao.moses.rank.RecommendRank;
import com.biyao.moses.params.ProductInfo;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Auther: ws
 * @Date: 2019/5/28 17:14
 * @Description:
 */
@Slf4j
@Component("sssr")
public class SameShopShuffleRank extends NewProductRaiseRank {

	@Autowired
	ProductDetailCache productDetailCache;

	/**
	 * @param rankRequest@return
	 *            List<TotalTemplateInfo> 排序完成的数据
	 */
	@BProfiler(key = "SameShopShuffleRank.executeRecommend", monitorType = {
			MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR })
	@Override
	public List<TotalTemplateInfo> executeRecommend(RankRequest rankRequest) {

		log.info("SameShopShuffleRank请求参数，uuid={}", rankRequest.getUuid());
		rankRequest.setRankName("npr");
		List<TotalTemplateInfo> executeRecommend = super.executeRecommend(rankRequest);
		if (CollectionUtils.isEmpty(executeRecommend)) {
			return executeRecommend;
		}
		this.categorySameShopShuffleRank(executeRecommend);
		return executeRecommend;
	}

	public void categorySameShopShuffleRank(List<TotalTemplateInfo> executeRecommend) {
		try {
			executeRecommend.stream().forEach(i -> {
				ProductInfo productInfo = productDetailCache.getProductInfo(Long.valueOf(i.getId()));
				if (productInfo != null) {
					i.setSupplierId(String.valueOf(productInfo.getSupplierId()));
				}
			});
		} catch (Exception e) {
			log.error("获取后台商家id错误", e);
		}
		// 准备数据结束---------------------------
		List<TotalTemplateInfo> temProductList = new ArrayList<>();
		temProductList.addAll(executeRecommend);
		List<TotalTemplateInfo> resultList = new ArrayList<>();

		// 分类目打散开始--------------------------------------
		this.reOrderProductList(resultList, temProductList, 0, executeRecommend.size());
		executeRecommend.clear();
		executeRecommend.addAll(resultList);

	}

	public void reOrderProductList(List<TotalTemplateInfo> newProductList, List<TotalTemplateInfo> oldProductList,
			int i, int total) {
		List<TotalTemplateInfo> ret = new ArrayList<>();
		TotalTemplateInfo now = null;
		if (i == 0) {
			now = oldProductList.get(0);
			newProductList.add(now);
			oldProductList.remove(0);
			// 递归调用本方法处理后续元素
			this.reOrderProductList(newProductList, oldProductList, ++i, total);
		} else if (i > 0 && i < total - 1) {
			now = newProductList.get(newProductList.size() - 1);
			TotalTemplateInfo next = oldProductList.get(0);
			if (next.getSupplierId().equals(now.getSupplierId())) {
				boolean hit = false;
				for (int j = 1; j < oldProductList.size(); j++) {
					next = oldProductList.get(j);
					if (next.getSupplierId().equals(now.getSupplierId())) {
						// do nothing
					} else {
						hit = true;
						newProductList.add(next);
						oldProductList.remove(j);
						// 递归调用本方法处理后续元素
						this.reOrderProductList(newProductList, oldProductList, ++i, total);
					}
				}
				if (!hit) {
					now = oldProductList.get(0);
					newProductList.add(now);
					oldProductList.remove(0);
					// 递归调用本方法处理后续元素
					this.reOrderProductList(newProductList, oldProductList, ++i, total);
				}
			} else {
				now = oldProductList.get(0);
				newProductList.add(now);
				oldProductList.remove(0);
				// 递归调用本方法处理后续元素
				this.reOrderProductList(newProductList, oldProductList, ++i, total);
			}
		} else if (i == total - 1) {
			newProductList.addAll(oldProductList);
		}
	}

}
