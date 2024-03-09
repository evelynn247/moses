package com.biyao.moses.model.adapter.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.biyao.moses.common.enums.TemplateTypeEnum;
import com.biyao.moses.context.ByUser;
import com.biyao.moses.model.SingleGroupTemplateInfo;
import com.biyao.moses.model.template.TemplateInfo;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.util.ProductDetailUtil;
import com.biyao.moses.params.ProductInfo;

@Component("singleGroup")
public class SingleGroupAdapter extends BaseTemplateAdapter{
	
	@Autowired
	ProductDetailUtil productDetailUtil;

	@Override
	public Integer getCurTemplateDataNum(int actualSize) throws Exception {
		return TemplateTypeEnum.SINGLE_GROUP.getDataSize();
	}

	@Override
	public TemplateInfo newTemplateInfoInstance() {
		return new SingleGroupTemplateInfo();
	}

	@Override
	public TemplateInfo specialDataHandle(TemplateInfo templateInfo,
			TotalTemplateInfo totalData, Integer curDataIndex, ByUser user)
			throws Exception {
		
		ProductInfo productInfo = productDetailUtil.preSaveProductInfo(totalData.getId(),user);
		SingleGroupTemplateInfo curTemplateInfo = (SingleGroupTemplateInfo) templateInfo;
		curTemplateInfo = (SingleGroupTemplateInfo) productDetailUtil.buildProductTemplate(curTemplateInfo, productInfo, curDataIndex,user, totalData);

		curTemplateInfo.setImage(productInfo.getSquarePortalImg());
		
		curTemplateInfo.setImageWebp(productInfo.getSquarePortalImgWebp());

		return curTemplateInfo;
	}

}