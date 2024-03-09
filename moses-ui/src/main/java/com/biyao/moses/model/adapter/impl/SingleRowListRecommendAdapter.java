package com.biyao.moses.model.adapter.impl;

import org.springframework.stereotype.Component;

import com.biyao.moses.common.enums.TemplateTypeEnum;
import com.biyao.moses.context.ByUser;
import com.biyao.moses.model.SingleRowListRecommendTemplateInfo;
import com.biyao.moses.model.template.TemplateInfo;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.params.ProductInfo;

/**
 * feed流模板单排样式
 * 
 * @Description
 * @Date 2018年9月27日
 */
@Component("singleRowListRecommend")
public class SingleRowListRecommendAdapter extends BaseTemplateAdapter {

	@Override
	public TemplateInfo newTemplateInfoInstance() {
		return new SingleRowListRecommendTemplateInfo();
	}
	@Override
	public Integer getCurTemplateDataNum(int actualSize) {
		return TemplateTypeEnum.FEED_SINGLE.getDataSize();
	}
	@Override
	public TemplateInfo specialDataHandle(TemplateInfo templateInfo,
			TotalTemplateInfo totalData, Integer curDataIndex, ByUser user) {
	
		ProductInfo productInfo = productDetailUtil.preSaveProductInfo(totalData.getId(),user);

		SingleRowListRecommendTemplateInfo curTemplateInfo = (SingleRowListRecommendTemplateInfo) templateInfo;

		curTemplateInfo = (SingleRowListRecommendTemplateInfo) productDetailUtil.buildProductTemplate(curTemplateInfo, productInfo, curDataIndex,user, totalData);

		// 设置长图
		curTemplateInfo.setImage(productInfo.getRectPortalImg());
		// 设置webp长图
		curTemplateInfo.setImageWebp(productInfo.getRectPortalImgWebp());
		
		return curTemplateInfo;
	}

}