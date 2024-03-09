package com.biyao.moses.model.adapter.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.biyao.moses.common.enums.TemplateTypeEnum;
import com.biyao.moses.context.ByUser;
import com.biyao.moses.model.FourfoldTemplateInfo;
import com.biyao.moses.model.template.TemplateInfo;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;

/**
 * 四排模板
 * 
 * @Description
 * @Date 2018年9月27日
 */
@Component("fourfold")
public class FourfoldAdapter extends BaseTemplateAdapter {

	@Override
	public TemplateInfo newTemplateInfoInstance() {
		return new FourfoldTemplateInfo();
	}

	@Override
	public TemplateInfo specialDataHandle(TemplateInfo templateInfo, TotalTemplateInfo totalData,
			Integer curDataIndex, ByUser user) {
		FourfoldTemplateInfo curTemplateInfo = (FourfoldTemplateInfo) templateInfo;
		// 处理图片类型 四排模板
		shufflePic(totalData);
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
		return TemplateTypeEnum.FOUR_FOLD.getDataSize();
	}

}
