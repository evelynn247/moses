package com.biyao.moses.model.adapter.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import com.biyao.moses.common.enums.TemplateTypeEnum;
import com.biyao.moses.context.ByUser;
import com.biyao.moses.model.DoubleLeftTemplateInfo;
import com.biyao.moses.model.template.TemplateInfo;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;

/**
 * 双排模板-样式1（左2右1）
 * @Description 
 * @Date 2018年9月27日
 */
@Component("doubleleft")
public class DoubleLeftAdapter extends BaseTemplateAdapter {

	@Override
	public TemplateInfo newTemplateInfoInstance() {
		return new DoubleLeftTemplateInfo();
	}

	@Override
	public TemplateInfo specialDataHandle(TemplateInfo templateInfo, TotalTemplateInfo totalData,
			Integer curDataIndex, ByUser user) {
		DoubleLeftTemplateInfo doubleLeftTemplateInfo = (DoubleLeftTemplateInfo) templateInfo;
		// 处理图片类型 双排模板-样式1（左2右1）
		shufflePic(totalData);
		List<String> images = null;
		List<String> imagesWebp = null;
		if (curDataIndex == 0) {
			images = totalData.getImages().subList(0, 2);
			if(CollectionUtils.isNotEmpty(totalData.getImagesWebp())) {
				imagesWebp = totalData.getImagesWebp().subList(0, 2);
			}
		} else {
			images = new ArrayList<String>();
			images.add(totalData.getImages().get(0));
			if(CollectionUtils.isNotEmpty(totalData.getImagesWebp())) {
				imagesWebp = new ArrayList<String>();
				imagesWebp.add(totalData.getImagesWebp().get(0));
			}
		}
		doubleLeftTemplateInfo.setImages(images);
		if(CollectionUtils.isNotEmpty(imagesWebp)) {
			doubleLeftTemplateInfo.setImagesWebp(imagesWebp);
		}
		return doubleLeftTemplateInfo;
	}

	@Override
	public Integer getCurTemplateDataNum(int actualSize) {
		return TemplateTypeEnum.DOUBLE_LEFT.getDataSize();
	}

}
