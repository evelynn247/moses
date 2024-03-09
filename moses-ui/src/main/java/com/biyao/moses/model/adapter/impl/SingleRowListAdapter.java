package com.biyao.moses.model.adapter.impl;

import org.springframework.stereotype.Component;

import com.biyao.moses.common.enums.TemplateTypeEnum;
import com.biyao.moses.context.ByUser;
import com.biyao.moses.model.SingleRowListTemplateInfo;
import com.biyao.moses.model.template.TemplateInfo;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.params.ProductInfo;

/**
 * 单排商品模板
 * 
 * @Description
 * @Date 2018年9月27日
 */
@Component("singleRowList")
public class SingleRowListAdapter extends BaseTemplateAdapter {

	@Override
	public TemplateInfo newTemplateInfoInstance() {
		return new SingleRowListTemplateInfo();
	}
	@Override
	public Integer getCurTemplateDataNum(int actualSize) {
		return TemplateTypeEnum.SINGLEROW_LIST.getDataSize();
	}
	@Override
	public TemplateInfo specialDataHandle(TemplateInfo templateInfo,
			TotalTemplateInfo totalData, Integer curDataIndex, ByUser user) {
	
		ProductInfo productInfo = productDetailUtil.preSaveProductInfo(totalData.getId(),user);

		SingleRowListTemplateInfo curTemplateInfo = (SingleRowListTemplateInfo) templateInfo;

		curTemplateInfo = (SingleRowListTemplateInfo) productDetailUtil.buildProductTemplate(curTemplateInfo, productInfo, curDataIndex,user, totalData);

		// 设置长图
		curTemplateInfo.setImage(productInfo.getSquarePortalImg());
		// 设置webp长图
		curTemplateInfo.setImageWebp(productInfo.getSquarePortalImgWebp());
		return curTemplateInfo;
	}

}
