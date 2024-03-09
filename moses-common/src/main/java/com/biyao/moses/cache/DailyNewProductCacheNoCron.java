package com.biyao.moses.cache;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.biyao.moses.common.constant.MatchRedisKeyConstant;
import com.biyao.moses.model.match.ProductImage;
import com.biyao.moses.util.MatchRedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class DailyNewProductCacheNoCron {

	@Autowired
	private MatchRedisUtil matchRedisUtil;

	private Map<Long, ProductImage> dailyNewProductInfoMap = new HashMap<>();

	/**
	 * 初始化
	 */
	protected void init() {
		refreshDailyNewProductCache();
	}

	/**
	 * 每2分钟刷新一次
	 */
	protected void refreshDailyNewProductCache() {
		try {
			getAllProductListByRedis();
		} catch (Exception e) {
			log.error("[严重异常][每日上新]获取每日上新商品信息失败", e);
			return;
		}
	}
    
	/**
	* @Description 从redis获取所有的每日上新商品 
	* @return List<ProductImage> 
	* @version V1.0
	* @auth 邹立强 (zouliqiang@idstaff.com)
	 */
	public List<ProductImage> getAllProductListByRedis() {
		List<ProductImage> list = new ArrayList<>();
		
		Map<Long, ProductImage> tmpMap = new HashMap<>();
		long start = System.currentTimeMillis();
		log.info("[任务进度][每日上新]获取每日上新商品信息开始");
		//，数据格式 [{"pid":pid1,"img":img1,"webp":webp1},{"pid":pid2,"img":img2,"webp":webp2}]
		String productImageInfoStr = matchRedisUtil.getString(MatchRedisKeyConstant.MOSES_DN_PRODUCTS);
		if (StringUtils.isNotBlank(productImageInfoStr)) {
			JSONArray parseArray = JSON.parseArray(productImageInfoStr);
			for (int i = 0; i < parseArray.size(); i++) {
				JSONObject jSONObject = parseArray.getJSONObject(i);
				try {
					String pid = jSONObject.getString("pid");
					String img = jSONObject.getString("img");
					String webp = jSONObject.getString("webp");
					ProductImage productImage = new ProductImage();
					productImage.setProductId(Long.valueOf(pid));
					productImage.setWebpImage(webp);
					productImage.setImage(img);
					list.add(productImage);
					tmpMap.put(productImage.getProductId(), productImage);
				} catch (Exception e) {
					log.error("[严重异常][每日上新]解析每日上新商品失败，jSONObject {},", jSONObject.toString(), e);
				}
			}
		}
		if(tmpMap.size() > 0) {
			dailyNewProductInfoMap = tmpMap;
		}
		log.info("[任务进度][每日上新]获取每日上新商品信息结束，耗时 {}ms, 商品个数{}", System.currentTimeMillis()-start, list.size());
		return list;
	}
     
	/**
	* @Description 从内存中获取所有的每日上新商品
	* @return List<ProductImage> 
	* @version V1.0
	* @auth 邹立强 (zouliqiang@idstaff.com)
	 */
	public List<ProductImage> getAllProductList() {
		long start = System.currentTimeMillis();
		List<ProductImage> list = new ArrayList<ProductImage>();
		for (Map.Entry<Long, ProductImage> entry : dailyNewProductInfoMap.entrySet()) {
			ProductImage productImage = entry.getValue();
			list.add(productImage);
		}
		long end = System.currentTimeMillis();
		log.info("[任务进度][每日上新]获取所有每日上新商品信息缓存耗时={},商品个数{}", end - start, list.size());
		return list;
	}
	
	public ProductImage getProductImgInfo(Long productId) {
		return dailyNewProductInfoMap.get(productId);
	}
}