package com.biyao.moses.model.adapter.impl;

import org.springframework.stereotype.Component;

import com.biyao.moses.common.enums.TemplateTypeEnum;
import com.biyao.moses.context.ByUser;
import com.biyao.moses.model.CrossPicSalePointTemplateInfo;
import com.biyao.moses.model.template.TemplateInfo;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.params.ProductInfo;

/**
 * 横图模板
 * 
 * @Description
 * @Date 2018年9月27日
 */
@Component("crossPicSalePoint")
public class CrossPicSalePointAdapter extends BaseTemplateAdapter {

	@Override
	public TemplateInfo newTemplateInfoInstance() {
		return new CrossPicSalePointTemplateInfo();
	}

	@Override
	public Integer getCurTemplateDataNum(int actualSize) {
		return TemplateTypeEnum.CROSSPIC_SALEPOINT.getDataSize();
	}

	@Override
	public TemplateInfo specialDataHandle(TemplateInfo templateInfo,
			TotalTemplateInfo totalData, Integer curDataIndex,ByUser user) {

		ProductInfo productInfo = productDetailUtil.preSaveProductInfo(totalData.getId(),user);

		CrossPicSalePointTemplateInfo curTemplateInfo = (CrossPicSalePointTemplateInfo) templateInfo;

		curTemplateInfo = (CrossPicSalePointTemplateInfo) productDetailUtil.buildProductTemplate(curTemplateInfo, productInfo, curDataIndex,user, totalData);

		// 设置长图
		curTemplateInfo.setImage(productInfo.getRectPortalImg());
		// 设置webp长图
		curTemplateInfo.setImageWebp(productInfo.getRectPortalImgWebp());

		return curTemplateInfo;
	}

}
