package com.biyao.moses.model.adapter.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.common.enums.TemplateTypeEnum;
import com.biyao.moses.constants.ERouterType;
import com.biyao.moses.context.ByUser;
import com.biyao.moses.model.TripleTemplateInfo;
import com.biyao.moses.model.template.TemplateInfo;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.params.ProductInfo;
import org.springframework.util.CollectionUtils;

/**
 * 三排模板-样式1（中间无空白间隔）
 * 
 * @Description
 * @Date 2018年9月27日
 */
@Component("triple")
public class TripleAdapter extends BaseTemplateAdapter {

	@Autowired
	ProductDetailCache productDetailCache;
	
	@Override
	public TemplateInfo newTemplateInfoInstance() {
		return new TripleTemplateInfo();
	}

	@Override
	public TemplateInfo specialDataHandle(TemplateInfo templateInfo, TotalTemplateInfo totalData,
			Integer curDataIndex, ByUser user) throws Exception{
		TripleTemplateInfo curTemplateInfo = (TripleTemplateInfo) templateInfo;
		shufflePic(totalData);
		// 处理图片类型 三排模板-样式1（中间无空白间隔）
		List<String> images = new ArrayList<String>();
		images.add(totalData.getImages().get(0));
		if(!CollectionUtils.isEmpty(totalData.getImagesWebp())) {
			List<String> imagesWebp = new ArrayList<String>();
			imagesWebp.add(totalData.getImagesWebp().get(0));
			curTemplateInfo.setImagesWebp(imagesWebp);
		}
		curTemplateInfo.setImages(images);

		//TODO 三排模板当前 可以配置商品跳转类型
		Integer routerType = curTemplateInfo.getRouterType();
		if (routerType!=null && routerType.equals(ERouterType.PRODUCTDETAIL.getNum())) {
			Map<String, String> routerParams = curTemplateInfo.getRouterParams();
			String spuId = routerParams.get("suId");
			if (StringUtils.isNotBlank(spuId)) {
				ProductInfo productInfo = productDetailCache.getProductInfo(Long.valueOf(spuId));
				if (productInfo!=null) {
					String shelfStatus = productInfo.getShelfStatus().toString();
					if (shelfStatus.equals("0")) {
						throw new Exception(spuId+"商品已下架");
					}
					routerParams.put("suId", productInfo.getSuId()+"");
				}else{
					throw new Exception("没有配置的商品"+spuId);
				}
			}
		}
		
		return curTemplateInfo;
	}
	
	@Override
	public Integer getCurTemplateDataNum(int actualSize) {
		return TemplateTypeEnum.TRIPLE.getDataSize();
	}
}