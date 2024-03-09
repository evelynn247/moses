package com.biyao.moses.model.adapter.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.biyao.moses.common.enums.TemplateTypeEnum;
import com.biyao.moses.context.ByUser;
import com.biyao.moses.model.TriplegapTemplateInfo;
import com.biyao.moses.model.template.TemplateInfo;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;

/**
 * 三排模板-样式1（中间有空白间隔
 * 
 * @Description
 * @Date 2018年9月27日
 */
@Component("triplegap")
public class TriplegapAdapter extends BaseTemplateAdapter {

	@Override
	public TemplateInfo newTemplateInfoInstance() {
		return new TriplegapTemplateInfo();
	}

	@Override
	public TemplateInfo specialDataHandle(TemplateInfo templateInfo, TotalTemplateInfo totalData,
			Integer curDataIndex, ByUser user) {
		TriplegapTemplateInfo curTemplateInfo = (TriplegapTemplateInfo) templateInfo;
		shufflePic(totalData);
		// 处理图片类型 三排模板-样式2（中间有空白间隔）
		List<String> images = new ArrayList<String>();
		List<String> imagesWebp = new ArrayList<String>();
		images.add(totalData.getImages().get(0));
		imagesWebp.add(totalData.getImagesWebp().get(0));
		curTemplateInfo.setImages(images);
		curTemplateInfo.setImagesWebp(imagesWebp);
		return curTemplateInfo;
	}

	@Override
	public Integer getCurTemplateDataNum(int actualSize) {
		return TemplateTypeEnum.TRIPLE_GAP.getDataSize();
	}

}