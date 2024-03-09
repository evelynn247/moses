package com.biyao.moses.model.adapter.impl;

import java.util.List;

import org.springframework.stereotype.Component;

import com.biyao.moses.common.enums.TemplateTypeEnum;
import com.biyao.moses.context.ByUser;
import com.biyao.moses.model.DoubledUpTemplateInfo;
import com.biyao.moses.model.template.TemplateInfo;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;

/**
 * 双排模板-样式1（左2右2）
 * @Description
 * @Date 2018年9月27日
 */
@Component("doubledup")
public class DoubledUpAdapter extends BaseTemplateAdapter {

	@Override
	public TemplateInfo newTemplateInfoInstance() {
		return new DoubledUpTemplateInfo();
	}

	@Override
	public TemplateInfo specialDataHandle(TemplateInfo templateInfo, TotalTemplateInfo totalData,
			Integer curDataIndex, ByUser user) {
		DoubledUpTemplateInfo doubledUpTemplateInfo = (DoubledUpTemplateInfo) templateInfo;
		// 处理图片类型 双排模板-样式1（左2右2） 图片为正方形短图
		shufflePic(totalData);
		List<String> images = totalData.getImages().subList(0, 2);
		List<String> imagesWebp = totalData.getImagesWebp().subList(0, 2);
		doubledUpTemplateInfo.setImages(images);
		doubledUpTemplateInfo.setImagesWebp(imagesWebp);
		return doubledUpTemplateInfo;
	}
	
	@Override
	public Integer getCurTemplateDataNum(int actualSize) {
		return TemplateTypeEnum.DOUBLED_UP.getDataSize();
	}

}
