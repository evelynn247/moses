package com.biyao.moses.model.adapter.impl;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.biyao.moses.common.enums.TemplateTypeEnum;
import com.biyao.moses.context.ByUser;
import com.biyao.moses.model.adapter.TemplateAdapter;
import com.biyao.moses.model.template.Template;
import com.biyao.moses.model.template.TemplateInfo;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.util.ProductDetailUtil;
import com.biyao.moses.util.RouteParamUtil;

/**
 * 
 * @Description 
 * @Date 2018年9月27日
 */
@Component
@Deprecated
public abstract class BaseFeedTemplateAdapter implements TemplateAdapter {
	
	protected DecimalFormat df = new DecimalFormat("#.##");
	
	@Autowired
	ProductDetailUtil productDetailUtil;

	@Autowired
	RouteParamUtil routeParamUtil;

	@Override
	public Template<TemplateInfo> adapte(Integer pageIndex ,Template<TotalTemplateInfo> oriTemplate, List<TotalTemplateInfo> data,
			Integer feedTemPage, String stp,ByUser user) throws Exception {

		Template<TemplateInfo> resultTemplate = new Template<TemplateInfo>();
		BeanUtils.copyProperties(oriTemplate, resultTemplate);

		TotalTemplateInfo temTotalData = oriTemplate.getData().get(0);

		List<TemplateInfo> arrayList = new ArrayList<TemplateInfo>();
		resultTemplate.setData(arrayList);

		int templateDataNum = TemplateTypeEnum.getTemplateTypeEnumByValue(oriTemplate.getTemplateName()).getDataSize();
		int startIndex = feedTemPage * templateDataNum;
		int endIndex = startIndex + templateDataNum;

		for (int i = startIndex; i < data.size() && i < endIndex; i++) {
			TemplateInfo templateInfo = newFeedTemplateInfoInstance();
			TotalTemplateInfo totalTemplateInfo = data.get(i);

			BeanUtils.copyProperties(totalTemplateInfo, templateInfo);

			if (temTotalData.getRouterParams() == null) {
				HashMap<String, String> hashMap = new HashMap<String, String>();
				temTotalData.setRouterParams(hashMap);
			}
			
			temTotalData.getRouterParams().put("stp", stp);

			Map<String, String> routeParam = routeParamUtil.getRouteParam(temTotalData);
			templateInfo.setRouterParams(routeParam);
			templateInfo.setRouterType(temTotalData.getRouterType());
			
			// 特殊数据处理，如长短图片
			templateInfo = specialDataHandle(templateInfo, totalTemplateInfo, i);
			
			arrayList.add(templateInfo);
		}
		return resultTemplate;
	}

	public abstract TemplateInfo newFeedTemplateInfoInstance();
	
	/**
	 * 
	 * @author monkey
	 * @param templateInfo
	 *            要处理的模板
	 * @param totalData
	 *            处理所需的数据
	 * @param curDataIndex
	 *            当前模板处理到了第几个数据
	 * @return
	 */
	public abstract TemplateInfo specialDataHandle(TemplateInfo templateInfo, TotalTemplateInfo totalData,
			Integer curDataIndex);
	
}
