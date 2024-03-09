package com.biyao.moses.model.adapter.impl;

import com.biyao.moses.cache.ProductDetailCacheNoCron;
import com.biyao.moses.common.enums.TemplateTypeEnum;
import com.biyao.moses.context.ByUser;
import com.biyao.moses.model.NarrowSingleLineTemplateInfo;
import com.biyao.moses.model.template.TemplateInfo;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.util.RedisUtil;
import com.biyao.moses.params.ProductInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 单排模板-样式1(标题+单张横屏大图)
 * 
 * @Description
 * @Date 2018年9月27日
 */
@Component("narrowSingleline")
public class NarrowSingleLineAdapter extends BaseTemplateAdapter {
    
	@Autowired
	private ProductDetailCacheNoCron productDetailCacheNoCron;
	
	@Autowired
	private RedisUtil redisUtil;
	
	private static final String EMPTY_DAILYNEW_IMAGE="moses:empty_dailynew_image";


	
	@Override
	public TemplateInfo newTemplateInfoInstance() {
		return new NarrowSingleLineTemplateInfo();
	}

	@Override
	public TemplateInfo specialDataHandle(TemplateInfo templateInfo, TotalTemplateInfo totalData,
			Integer curDataIndex, ByUser user) {
		
		
		
		NarrowSingleLineTemplateInfo curTemplateInfo = (NarrowSingleLineTemplateInfo) templateInfo;
		// 处理图片类型 单排模板-样式1(标题+单张横屏大图)
		//返回结果
		List<String> images = new ArrayList<String>();
		List<String> imagesWebp = new ArrayList<String>();
		//过滤后的商品Id
		List<Long> filterFeedProducts = new ArrayList<Long>();
		//过滤后的商品图
		List<String> filterLongImages = new ArrayList<String>();
		List<String> filterLongImagesWebp = new ArrayList<String>();
		List<Long> feedProducts = new ArrayList<Long>();
		List<Long> feedLongProducts = totalData.getFeedLongProducts();
		List<String> longImages = totalData.getLongImages();
		List<String> longImagesWebp = totalData.getLongImagesWebp();
		if(CollectionUtils.isNotEmpty(feedLongProducts)) {
			for (int i = 0; i < feedLongProducts.size(); i++) {
				Long productId = feedLongProducts.get(i);
				ProductInfo productInfo = productDetailCacheNoCron.getProductInfo(productId);
				if (productInfo != null
						&& productInfo.getShelfStatus().toString().equals("1")) {
					filterFeedProducts.add(productId);
					filterLongImages.add(longImages.get(i));
					filterLongImagesWebp.add(longImagesWebp.get(i));
				}
			}
		}
		if(CollectionUtils.isNotEmpty(filterFeedProducts)&&CollectionUtils.isNotEmpty(filterLongImages)) {
			// 随机出图
			Random random = new Random();
			int nextInt = random.nextInt(filterLongImages.size());
			int nextIntWebp = random.nextInt(filterLongImagesWebp.size());
			images.add(filterLongImages.get(nextInt));
			imagesWebp.add(filterLongImagesWebp.get(nextIntWebp));
			feedProducts.add(filterFeedProducts.get(nextInt));
			curTemplateInfo.setImages(images);
			curTemplateInfo.setImagesWebp(imagesWebp);
			curTemplateInfo.setFeedProducts(feedProducts);
		}else {
			//托底处理
			images.add(redisUtil.getString(EMPTY_DAILYNEW_IMAGE));
			curTemplateInfo.setImages(images);
			curTemplateInfo.setImagesWebp(images);//TODO
			curTemplateInfo.setFeedProducts(feedProducts);
		}
	
		return curTemplateInfo;
	}
	
	@Override
	public Integer getCurTemplateDataNum(int actualSize) {
		return TemplateTypeEnum.NARROW_SINGLE_LINE.getDataSize();
	}
}