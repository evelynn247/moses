package com.biyao.moses.model.adapter.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.biyao.moses.common.enums.TemplateTypeEnum;
import com.biyao.moses.context.ByUser;
import com.biyao.moses.model.DoubleRightTemplateInfo;
import com.biyao.moses.model.template.TemplateInfo;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.util.RouteParamUtil;

/**
 * 双排模板-样式1（左1右2）
 * @Description 
 * @Date 2018年9月27日
 */
@Component("doubleright")
public class DoubleRightAdapter extends BaseTemplateAdapter {

	@Autowired
	RouteParamUtil routeParamUtil;

	@Override
	public TemplateInfo newTemplateInfoInstance() {
		return new DoubleRightTemplateInfo();
	}

	@Override
	public TemplateInfo specialDataHandle(TemplateInfo templateInfo, TotalTemplateInfo totalData,
			Integer curDataIndex, ByUser user) {
		DoubleRightTemplateInfo curTemplateInfo = (DoubleRightTemplateInfo) templateInfo;
		// 处理图片类型 双排模板-样式1（左1右2）
		shufflePic(totalData);
		List<String> images = null;
		List<String> imagesWebp = null;
		if (curDataIndex == 0) {
			images = new ArrayList<String>();
			images.add(totalData.getImages().get(0));
			imagesWebp = new ArrayList<String>();
			imagesWebp.add(totalData.getImagesWebp().get(0));
		} else {
			images = totalData.getImages().subList(0, 2);
			imagesWebp = totalData.getImagesWebp().subList(0, 2);
		}
		curTemplateInfo.setImages(images);
		curTemplateInfo.setImagesWebp(imagesWebp);
		return curTemplateInfo;
	}

	@Override
	public Integer getCurTemplateDataNum(int actualSize) {
		return TemplateTypeEnum.DOUBLE_RIGHT.getDataSize();
	}
}
