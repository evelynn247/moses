package com.biyao.moses.model.adapter.impl;

import com.alibaba.fastjson.JSONObject;
import com.biyao.moses.cache.RedisCache;
import com.biyao.moses.cache.SimilarCategory3IdCache;
import com.biyao.moses.constants.CommonConstants;
import com.biyao.moses.params.ProductInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.biyao.moses.common.enums.TemplateTypeEnum;
import com.biyao.moses.context.ByUser;
import com.biyao.moses.model.DoubleRowListTemplateInfo;
import com.biyao.moses.model.template.TemplateInfo;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;

import java.net.URLDecoder;
import java.util.Map;

/**
 * feed流模板双排样式
 * 
 * @Description
 * @Date 2018年9月27日
 */
@Slf4j
@Component("doubleRowList")
public class DoubleRowListAdapter extends BaseTemplateAdapter {

	@Autowired
	private RedisCache redisCache;

	@Autowired
	private SimilarCategory3IdCache similarCategory3IdCache;

	@Override
	public TemplateInfo newTemplateInfoInstance() {
		return new DoubleRowListTemplateInfo();
	}
	
	@Override
	public Integer getCurTemplateDataNum(int actualSize) {
		return TemplateTypeEnum.FEED_DOUBLE.getDataSize();
	}

	@Override
	public TemplateInfo specialDataHandle(TemplateInfo templateInfo,
			TotalTemplateInfo totalData, Integer curDataIndex, ByUser user) {
		
		ProductInfo productInfo = productDetailUtil.preSaveProductInfo(totalData.getId(),user);

		DoubleRowListTemplateInfo curTemplateInfo = (DoubleRowListTemplateInfo) templateInfo;

		curTemplateInfo = (DoubleRowListTemplateInfo) productDetailUtil.buildProductTemplate(curTemplateInfo, productInfo, curDataIndex,user, totalData);

		// 白名单用户制造商背景更换为scm参数，用于观察数据
		if(redisCache.isHomeFeedWhite(user.getUuid()) && !user.isTest()){
			changeSubtitleForWhiteList(curTemplateInfo,productInfo);
		}

		// 设置短图
		curTemplateInfo.setImage(productInfo.getSquarePortalImg());
		// 设置webp短图
		curTemplateInfo.setImageWebp(productInfo.getSquarePortalImgWebp());

		if(StringUtils.isNotBlank(totalData.getSkuId()) && StringUtils.isNotBlank(totalData.getSkuPrice())){
			if(curTemplateInfo.getRouterParams().containsKey("suId")){
				curTemplateInfo.setPriceStr(df.format(Long.valueOf(totalData.getSkuPrice()) / 100.00));
				curTemplateInfo.setPriceCent(totalData.getSkuPrice());
				curTemplateInfo.getRouterParams().put("suId", totalData.getSkuId());
				//只要有1个不为空，则更新入口图
				if(StringUtils.isNotBlank(totalData.getImage()) || StringUtils.isNotBlank(totalData.getImageWebp())){
					curTemplateInfo.setImage(totalData.getImage());
					curTemplateInfo.setImageWebp(totalData.getImageWebp());
				}
			}
		}

		
		return curTemplateInfo;
	}

	/**
	 * 白名单用户制造商背景更换为scm参数，用于观察数据
	 * @param curTemplateInfo
	 */
	private void changeSubtitleForWhiteList(DoubleRowListTemplateInfo curTemplateInfo,ProductInfo productInfo) {
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
