package com.biyao.moses.model.adapter.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.biyao.moses.common.enums.TemplateTypeEnum;
import com.biyao.moses.context.ByUser;
import com.biyao.moses.model.DoubleUnfillTemplateInfo;
import com.biyao.moses.model.template.TemplateInfo;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;

/**
 * 双排模板-样式1（左1右1）
 * 
 * @Description
 * @Date 2018年9月27日
 */
@Component("doubleunfill")
public class DoubleUnfillAdapter extends BaseTemplateAdapter {

	@Override
	public TemplateInfo newTemplateInfoInstance() {
		return new DoubleUnfillTemplateInfo();
	}

	@Override
	public TemplateInfo specialDataHandle(TemplateInfo templateInfo, TotalTemplateInfo totalData,
			Integer curDataIndex, ByUser user) {
		DoubleUnfillTemplateInfo curTemplateInfo = (DoubleUnfillTemplateInfo) templateInfo;
		// 处理图片类型 双排模板-样式1（左1右1）有标题
		shufflePic(totalData);
		List<String> images = new ArrayList<String>();
		List<String> imagesWebp = new ArrayList<String>();
		images.add(totalData.getLongImages().get(0));
		imagesWebp.add(totalData.getLongImagesWebp().get(0));
		curTemplateInfo.setImages(images);
		curTemplateInfo.setImagesWebp(imagesWebp);
		return curTemplateInfo;
	}
	
	@Override
	public Integer getCurTemplateDataNum(int actualSize) {
		return TemplateTypeEnum.DOUBLE_UNFILL.getDataSize();
	}
	
}
