package com.biyao.moses.model.adapter.impl;

import com.biyao.moses.context.ByUser;
import com.biyao.moses.model.SwiperPictureTemplateInfo;
import com.biyao.moses.model.template.TemplateInfo;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import java.util.List;
import java.util.Random;
import org.springframework.stereotype.Component;

/**
 * 轮播图模板
 * 
 * @Description
 * @Date 2018年9月27日
 */
@Component("swiperPicture")
public class SwiperPictureAdapter extends BaseTemplateAdapter {

	@Override
	public TemplateInfo newTemplateInfoInstance() {
		return new SwiperPictureTemplateInfo();
	}

	@Override
	public TemplateInfo specialDataHandle(TemplateInfo templateInfo, TotalTemplateInfo totalData,
			Integer curDataIndex, ByUser user) {
		if (totalData==null||totalData.getImages()==null||totalData.getImages().size()==0) {
			return templateInfo;
		}
		
		Random random = new Random();
		int nextInt = random.nextInt(totalData.getImages().size());
		int nextIntWebp = random.nextInt(totalData.getImagesWebp().size());
		SwiperPictureTemplateInfo cur = (SwiperPictureTemplateInfo)templateInfo;
		List<String> images = totalData.getImages();
		List<String> imagesWebp = totalData.getImagesWebp();
		cur.setImageUrl(images.get(nextInt));
		cur.setImageUrlWebp(imagesWebp.get(nextIntWebp));
		return cur;
	}
	
	@Override
	public Integer getCurTemplateDataNum(int actualSize) {
		return actualSize;
	}
}
