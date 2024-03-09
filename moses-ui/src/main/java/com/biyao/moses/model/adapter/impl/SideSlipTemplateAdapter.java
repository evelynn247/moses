package com.biyao.moses.model.adapter.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.biyao.moses.common.constant.CommonConstants;
import com.biyao.moses.context.ByUser;
import com.biyao.moses.exception.SideSlipTemplateException;
import com.biyao.moses.model.SideSlipTemplateInfo;
import com.biyao.moses.model.template.TemplateInfo;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.util.ProductDetailUtil;
import com.biyao.moses.params.ProductInfo;

@Component("sideslip")
public class SideSlipTemplateAdapter extends BaseTemplateAdapter {

	@Autowired
	ProductDetailUtil productDetailUtil;

	@Override
	public TemplateInfo newTemplateInfoInstance() {
		return new SideSlipTemplateInfo();
	}

	@Override
	public TemplateInfo specialDataHandle(TemplateInfo templateInfo, TotalTemplateInfo totalData,
			Integer curDataIndex, ByUser user) {

		ProductInfo productInfo = productDetailUtil.preSaveProductInfo(totalData.getId(),user);

		SideSlipTemplateInfo curTemplateInfo = (SideSlipTemplateInfo) templateInfo;

		curTemplateInfo = (SideSlipTemplateInfo) productDetailUtil.buildProductTemplate(curTemplateInfo, productInfo, curDataIndex, user, totalData);

		curTemplateInfo.setImage(productInfo.getSquarePortalImg());
		
		curTemplateInfo.setImageWebp(productInfo.getSquarePortalImgWebp());

//		List<Label> resultLabels = new ArrayList<Label>();
//		for (Label label : labels) {
//			if ("特权金".equals(label.getContent()) || "一起拼".equals(label.getContent())) {
//				resultLabels.add(label);
//			}
//		}

		return curTemplateInfo;
	}

	@Override
	public Integer getCurTemplateDataNum(int actualSize) throws Exception {
		if (actualSize < CommonConstants.PRODUCT_SPLIT_NUM) {
			throw new SideSlipTemplateException();
		}
		int num = actualSize > 15 ? 15 : actualSize;
		return num;
	}

}