package com.biyao.moses.model.adapter.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.biyao.moses.common.enums.TemplateTypeEnum;
import com.biyao.moses.context.ByUser;
import com.biyao.moses.model.DoublefillTemplateInfo;
import com.biyao.moses.model.template.TemplateInfo;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.util.RouteParamUtil;

/**
 * 双排模板-样式1（左1右1）无标题
 * 
 * @Description
 * @Date 2018年9月27日
 */
@Component("doublefill")
public class DoubleFillAdapter extends BaseTemplateAdapter {

	@Autowired
	RouteParamUtil routeParamUtil;

	@Override
	public TemplateInfo newTemplateInfoInstance() {
		return new DoublefillTemplateInfo();
	}

	@Override
	public TemplateInfo specialDataHandle(TemplateInfo templateInfo, TotalTemplateInfo totalData,
			Integer curDataIndex, ByUser user) {
		DoublefillTemplateInfo doublefillTemplateInfo = (DoublefillTemplateInfo) templateInfo;
		// 处理图片类型 双排模板-样式1（左1右1）无标题
		shufflePic(totalData);
		List<String> images = new ArrayList<String>();
		List<String> imagesWebp = new ArrayList<String>();
		images.add(totalData.getLongImages().get(0));
		imagesWebp.add(totalData.getLongImagesWebp().get(0));
		doublefillTemplateInfo.setImages(images);
		doublefillTemplateInfo.setImagesWebp(imagesWebp);
		return doublefillTemplateInfo;
	}
	
	@Override
	public Integer getCurTemplateDataNum(int actualSize) {
		return TemplateTypeEnum.DOUBLE_FILL.getDataSize();
	}
}
