package com.biyao.moses.model.adapter.impl;

import com.alibaba.fastjson.JSONObject;
import com.biyao.moses.cache.RedisCache;
import com.biyao.moses.cache.SimilarCategory3IdCache;
import com.biyao.moses.common.enums.TemplateTypeEnum;
import com.biyao.moses.constants.CommonConstants;
import com.biyao.moses.context.ByUser;
import com.biyao.moses.model.TemplateSingleNewuserTemplateInfo;
import com.biyao.moses.model.template.TemplateInfo;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.params.ProductInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.util.Map;

/**
* @Description 新手专享单排feed样式
* @date 2019年5月6日下午5:17:17
* @version V1.0 
* @author 邹立强 (zouliqiang@idstaff.com)
* <p>Copyright (c) Department of Research and Development/Beijing.</p>
 */
@Slf4j
@Component("templateSingleNewuser")
public class TemplateSingleNewuserAdapter extends BaseTemplateAdapter {

	@Autowired
	private RedisCache redisCache;

	@Autowired
	private SimilarCategory3IdCache similarCategory3IdCache;

	@Override
	public TemplateInfo newTemplateInfoInstance() {
		return new TemplateSingleNewuserTemplateInfo();
	}
	
	@Override
	public Integer getCurTemplateDataNum(int actualSize) {
		return TemplateTypeEnum.TEMPLATE_SINGLE_NEWUSER.getDataSize();
	}

	@Override
	public TemplateInfo specialDataHandle(TemplateInfo templateInfo,
			TotalTemplateInfo totalData, Integer curDataIndex, ByUser user) {
		
		ProductInfo productInfo = productDetailUtil.preSaveProductInfo(totalData.getId(),user);

		TemplateSingleNewuserTemplateInfo curTemplateInfo = (TemplateSingleNewuserTemplateInfo) templateInfo;
		
		curTemplateInfo = (TemplateSingleNewuserTemplateInfo) productDetailUtil.buildProductTemplate(curTemplateInfo, productInfo, curDataIndex,user, totalData);
		
		// 设置方图
		curTemplateInfo.setImage(productInfo.getSquarePortalImg());
		// 设置Webp方图
		curTemplateInfo.setImageWebp(productInfo.getSquarePortalImgWebp());

		// 白名单用户制造商背景更换为scm参数，用于观察数据
		if(redisCache.isHomeFeedWhite(user.getUuid()) && !user.isTest()){
			changeSubtitleForWhiteList(curTemplateInfo,productInfo);
		}
		//(填充新手专享价格);
		if (productInfo.getNovicePrice() != null && productInfo.getNovicePrice().signum() == 1) {
			curTemplateInfo.setNovicePrice(String.valueOf(productInfo.getNovicePrice()));
		}
		
		return curTemplateInfo;
	}

	/**
	 * 白名单用户制造商背景更换为scm参数，用于观察数据
	 * @param curTemplateInfo
	 */
	private void changeSubtitleForWhiteList(TemplateSingleNewuserTemplateInfo curTemplateInfo, ProductInfo productInfo) {
		try {
			Map<String, String> routerParams = curTemplateInfo.getRouterParams();
			if (routerParams == null || routerParams.size() == 0) {
				return;
			}
			String stpStr = routerParams.get(CommonConstants.STP);
			if (StringUtils.isBlank(stpStr)) {
				return;
			}
			String decodeStr = URLDecoder.decode(stpStr, "UTF-8");
			Map<String, String> stpMap = JSONObject.parseObject(decodeStr, Map.class);
			String scmStr = stpMap.get(CommonConstants.SCM);
			if (StringUtils.isBlank(scmStr)) {
				return;
			}
			curTemplateInfo.setSubtitle(scmStr);
			String mainTitle = curTemplateInfo.getMainTitle();
			if(productInfo!=null && productInfo.getThirdCategoryId()!=null){
				Long thirdCategoryId = productInfo.getThirdCategoryId();
				Long similar3CategoryId = similarCategory3IdCache.getSimilarCate3Id(thirdCategoryId);
				if(similar3CategoryId==null){
					similar3CategoryId = thirdCategoryId;
				}
				mainTitle = similar3CategoryId.toString()+ "_" + mainTitle;
			}
			curTemplateInfo.setMainTitle(mainTitle);
		} catch (Exception e) {
			log.error("为白名单更换scm参数出错",e);
		}
	}

}
