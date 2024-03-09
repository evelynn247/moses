package com.biyao.moses.model.adapter.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import com.biyao.moses.common.enums.TemplateTypeEnum;
import com.biyao.moses.context.ByUser;
import com.biyao.moses.model.SingleLineTemplateInfo;
import com.biyao.moses.model.template.TemplateInfo;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;

/**
 * 单排模板-样式1(标题+单张横屏大图)
 * 
 * @Description
 * @Date 2018年9月27日
 */
@Component("singleline")
public class SingleLineAdapter extends BaseTemplateAdapter {

	@Override
	public TemplateInfo newTemplateInfoInstance() {
		return new SingleLineTemplateInfo();
	}

	@Override
	public TemplateInfo specialDataHandle(TemplateInfo templateInfo, TotalTemplateInfo totalData,
			Integer curDataIndex, ByUser user) {
		SingleLineTemplateInfo curTemplateInfo = (SingleLineTemplateInfo) templateInfo;
		// 处理图片类型 单排模板-样式1(标题+单张横屏大图)
		shufflePic(totalData);
		List<String> images = new ArrayList<String>();
		List<String> imagesWebp = new ArrayList<String>();
		images.add(totalData.getLongImages().get(0));
		if(CollectionUtils.isNotEmpty(totalData.getLongImagesWebp())) {
			imagesWebp.add(totalData.getLongImagesWebp().get(0));
		}
		curTemplateInfo.setImages(images);
		if(CollectionUtils.isNotEmpty(imagesWebp)) {
			curTemplateInfo.setImagesWebp(imagesWebp);
		}
		return curTemplateInfo;
	}
	
	@Override
	public Integer getCurTemplateDataNum(int actualSize) {
		return TemplateTypeEnum.SINGLE_LINE.getDataSize();
	}

}