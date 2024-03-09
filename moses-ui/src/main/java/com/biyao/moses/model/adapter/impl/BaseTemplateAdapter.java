package com.biyao.moses.model.adapter.impl;

import com.alibaba.dubbo.common.URL;
import com.alibaba.fastjson.JSONObject;
import com.biyao.moses.common.enums.TemplateTypeEnum;
import com.biyao.moses.constants.CommonConstants;
import com.biyao.moses.constants.ERouterType;
import com.biyao.moses.context.ByUser;
import com.biyao.moses.model.BaseProductTemplateInfo;
import com.biyao.moses.model.adapter.TemplateAdapter;
import com.biyao.moses.model.template.Template;
import com.biyao.moses.model.template.TemplateInfo;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.params.ProductInfo;
import com.biyao.moses.util.MyBeanUtil;
import com.biyao.moses.util.ProductDetailUtil;
import com.biyao.moses.util.RouteParamUtil;
import com.biyao.moses.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.util.*;

import static com.biyao.moses.constants.CommonConstants.SHOW_TYPE_ADVERT_GLOBAL;


/**
 *
 * @Description
 * @Date 2018年9月27日
 */
@Slf4j
@Component
public abstract class BaseTemplateAdapter implements TemplateAdapter {

	protected DecimalFormat df = new DecimalFormat("#.##");

	@Autowired
	ProductDetailUtil productDetailUtil;

	@Autowired
	RouteParamUtil routeParamUtil;

	/**
	 * 正常模板的适配实现，非feed流
	 */
	@Override
	public Template<TemplateInfo> adapte(Template<TotalTemplateInfo> oriTemplate,List<TotalTemplateInfo> temData, String stp, ByUser user) throws Exception {
		Template<TemplateInfo> resultTemplate = null;
		if (oriTemplate.isDynamic()) {
			resultTemplate = getDynamicTemplate(oriTemplate, temData, stp, user);
		} else {
			resultTemplate = getStaticTemplate(oriTemplate, temData, stp, user);
		}

		return resultTemplate;
	}

	/**
	 * feed流模板适配
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Template<TemplateInfo> adapte(Integer pageIndex, Template<TotalTemplateInfo> oriTemplate, List<TotalTemplateInfo> data,Integer feedIndex, String stp,ByUser user) throws Exception {
		Template<TemplateInfo> resultTemplate = new Template<TemplateInfo>();
		BeanUtils.copyProperties(oriTemplate, resultTemplate);
		TotalTemplateInfo temTotalData = oriTemplate.getData().get(0);
		List<TemplateInfo> arrayList = new ArrayList<TemplateInfo>();
		resultTemplate.setData(arrayList);

		int templateDataNum = TemplateTypeEnum.getTemplateTypeEnumByValue(oriTemplate.getTemplateName()).getDataSize();
		int startIndex = feedIndex * templateDataNum;
		int endIndex = startIndex + templateDataNum;

		for (int i = startIndex; i < data.size() && i < endIndex; i++) {
			TemplateInfo templateInfo = newTemplateInfoInstance();
			TotalTemplateInfo totalTemplateInfo = data.get(i);

			MyBeanUtil.copyNotNullProperties(temTotalData, templateInfo);
			MyBeanUtil.copyNotNullProperties(totalTemplateInfo, templateInfo);

			if (templateInfo.getRouterParams() == null) {
				HashMap<String, String> hashMap = new HashMap<String, String>();
				templateInfo.setRouterParams(hashMap);
			}


			// 埋点stp处理，siteId.pageId.pid.b|0|1
			HashMap<String,String> stpMap = new HashMap<>();
			HashMap<String,String> resultMap = new HashMap<>();
			try {
				stpMap = JSONObject.parseObject(stp, HashMap.class);
				String spm = stpMap.get(CommonConstants.SPM);
				resultMap.putAll(stpMap);
				if (StringUtils.isNotBlank(spm)) {
					String[] split = spm.split("\\.");
					if(split.length==4) {
						spm = spm + CommonConstants.SPLIT_PIPE + i;
					}else {
						spm =StringUtil.concat(spm,pageIndex+i);
					}
					resultMap.put(CommonConstants.SPM, spm);
				}

				String expId = totalTemplateInfo.getExpId();
				String source = totalTemplateInfo.getSource();
				//如果是活动广告
				if(CommonConstants.SHOW_TYPE_ADVERT.equals(totalTemplateInfo.getShowType())){
					String advertId = totalTemplateInfo.getRouterParams().get("activityId");
					String scm = "moses."+advertId+".0000.";
					resultMap.put(CommonConstants.SCM, scm);
				}else if(CommonConstants.SHOW_TYPE_VIDEO.equals(totalTemplateInfo.getShowType())){
						//如果是商品替换的视频
				}else if (CommonConstants.SHOW_TYPE_OPE_VIDEO.equals(totalTemplateInfo.getShowType())){
						//如果是运营配置的视频
				}else if(CommonConstants.SHOW_TYPE_PRO_GROUP.equals(totalTemplateInfo.getShowType())){
					// 如果是商品组
					String scm = "moses."+"spzh"+".0000."+totalTemplateInfo.getId();
					resultMap.put(CommonConstants.SCM, scm);
				}else{
					//其他，则表示是商品
					if (StringUtils.isNotBlank(expId) && StringUtils.isNotBlank(source)) {
						//scm结构为：模块名.召回源Id.实验Id.
						String scm = "moses." + source + "." + expId + ".";
						resultMap.put(CommonConstants.SCM, scm);
					}
				}
			} catch (Exception e) {
				log.error("stp解析失败{}",stp);
			}

			templateInfo.setRouterType(temTotalData.getRouterType());
			if(CommonConstants.SHOW_TYPE_ADVERT.equals(totalTemplateInfo.getShowType())){
				templateInfo.setRouterType(ERouterType.ADVERT.getNum());
				templateInfo.setRouterParams(totalTemplateInfo.getRouterParams());
			}
			//如果是商品组　
			if(CommonConstants.SHOW_TYPE_PRO_GROUP.equals(totalTemplateInfo.getShowType())){
				templateInfo.setRouterType(ERouterType.PRODUCTGROUP.getNum());
				templateInfo.setRouterParams(totalTemplateInfo.getRouterParams());
			}
			if(CommonConstants.SHOW_TYPE_VIDEO.equals(totalTemplateInfo.getShowType())){
				templateInfo.setRouterType(ERouterType.VIDEO.getNum());
				templateInfo.setRouterParams(totalTemplateInfo.getRouterParams());
			}
			if(CommonConstants.SHOW_TYPE_OPE_VIDEO.equals(totalTemplateInfo.getShowType())){
				templateInfo.setRouterType(ERouterType.OPEVIDEO.getNum());
				templateInfo.setRouterParams(totalTemplateInfo.getRouterParams());
			}
            templateInfo.getRouterParams().put(CommonConstants.STP, URL.encode(JSONObject.toJSONString(resultMap)));

			Map<String, String> routeParam = routeParamUtil.getRouteParam(templateInfo);
			templateInfo.setRouterParams(routeParam);
			//如果不是广告
			if(!SHOW_TYPE_ADVERT_GLOBAL.contains(totalTemplateInfo.getShowType())) {
				//加入是否是低模眼镜、是否支持签名属性
				dealWithRasterAndCarve(user, (BaseProductTemplateInfo) templateInfo, totalTemplateInfo);
			}
			// 特殊数据处理，如长短图片
			templateInfo = specialDataHandle(templateInfo, totalTemplateInfo, i, user);

			arrayList.add(templateInfo);
		}

        return resultTemplate;
	}

    /**
	 * 加入是否是低模眼镜、是否支持签名属性
	 * @param user
	 * @param templateInfo
	 * @param totalTemplateInfo
	 */
	private void dealWithRasterAndCarve(ByUser user, BaseProductTemplateInfo templateInfo, TotalTemplateInfo totalTemplateInfo) {
		ProductInfo productInfo = productDetailUtil.preSaveProductInfo(totalTemplateInfo.getId(), user);
		// 加入低模商品类型：0-普通低模商品
		if (productInfo != null && productInfo.getRasterType()!=null) {
			templateInfo.setRasterType(productInfo.getRasterType());
		}
		//加入1-商品是否支持签名(0 不支持 1 支持)
		if (productInfo != null && productInfo.getSupportCarve()!=null) {
			templateInfo.setSupportCarve(productInfo.getSupportCarve());
		}
	}

	@SuppressWarnings("unchecked")
	private Template<TemplateInfo> getDynamicTemplate(Template<TotalTemplateInfo> oriTemplate,
			List<TotalTemplateInfo> data, String stp,ByUser user) throws Exception{
		// 结果集
		Template<TemplateInfo> resultTemplate = new Template<TemplateInfo>();
		// 复制模板属性
		BeanUtils.copyProperties(oriTemplate, resultTemplate);
		List<TemplateInfo> arrayList = new ArrayList<TemplateInfo>();
		resultTemplate.setData(arrayList);

		// 获取模板列表
		List<TotalTemplateInfo> temList = oriTemplate.getData();
		// 获取模板数据
		for (int i = 0; i < temList.size(); i++) {
			// newInstance
			TemplateInfo templateInfo = newTemplateInfoInstance();

			TotalTemplateInfo totalTemplateInfo = data.get(i);

			// 模板的中的静态数据复制
			BeanUtils.copyProperties(temList.get(i), templateInfo);

			// 处理动态静态都可能有的routeParam
			if (templateInfo.getRouterParams() == null) {
				HashMap<String, String> hashMap = new HashMap<String, String>();
				templateInfo.setRouterParams(hashMap);
			}
			if (data.get(i).getRouterParams() != null && data.get(i).getRouterParams().size() > 0) {
				templateInfo.getRouterParams().putAll(data.get(i).getRouterParams());
			}

			// 埋点stp处理，siteId.pageId.pid.b|0|1
			HashMap<String,String> stpMap = new HashMap<>();
			HashMap<String,String> resultMap = new HashMap<>();
			try {
				stpMap = JSONObject.parseObject(stp, HashMap.class);
				resultMap.putAll(stpMap);
				String spm = stpMap.get(CommonConstants.SPM);
				if (StringUtils.isNotBlank(spm)) {
					spm = spm + CommonConstants.SPLIT_PIPE + i;
					resultMap.put(CommonConstants.SPM, spm);
				}

				String expId = totalTemplateInfo.getExpId();
				String source = totalTemplateInfo.getSource();
				if (StringUtils.isNotBlank(expId) && StringUtils.isNotBlank(source)) {
					//scm结构为：模块名.召回源Id.实验Id.
					String scm = "moses." + source + "." + expId + ".";
					resultMap.put(CommonConstants.SCM, scm);
				}

			} catch (Exception e) {
				log.error("stp解析失败{}",stp);
			}

			templateInfo.getRouterParams().put("stp", URL.encode(JSONObject.toJSONString(resultMap)));
			// 模板中的动态数据复制
			MyBeanUtil.copyNotNullProperties(data.get(i), templateInfo);

			// 处理routeParam
			Map<String, String> routeParam = routeParamUtil.getRouteParam(templateInfo);
			templateInfo.setRouterParams(routeParam);

			// 特殊数据处理，如长短图片
			templateInfo = specialDataHandle(templateInfo, data.get(i), i, user);
			// 加入返回结果集
			arrayList.add(templateInfo);
		}

		return resultTemplate;
	}

	private Template<TemplateInfo> getStaticTemplate(Template<TotalTemplateInfo> oriTemplate,
			List<TotalTemplateInfo> temData, String stp,ByUser user) {
		Template<TemplateInfo> resultTemplate = new Template<TemplateInfo>();

		BeanUtils.copyProperties(oriTemplate, resultTemplate);
		List<TemplateInfo> arrayList = new ArrayList<TemplateInfo>();
		resultTemplate.setData(arrayList);

		for (TotalTemplateInfo totalTemInfo : oriTemplate.getData()) {
			TemplateInfo templateInfo = newTemplateInfoInstance();
			BeanUtils.copyProperties(totalTemInfo, templateInfo);

			if (totalTemInfo.getRouterParams() == null) {
				HashMap<String, String> hashMap = new HashMap<String, String>();
				hashMap.put("stp", URL.encode(stp));
				totalTemInfo.setRouterParams(new HashMap<String, String>());
			} else {
				totalTemInfo.getRouterParams().put("stp", URL.encode(stp));
			}

			Map<String, String> routeParam = routeParamUtil.getRouteParam(totalTemInfo);
			templateInfo.setRouterParams(routeParam);

			arrayList.add(templateInfo);
		}

		return resultTemplate;
	}


	public abstract TemplateInfo newTemplateInfoInstance();

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
			Integer curDataIndex,ByUser user) throws Exception;

	public void shufflePic(TotalTemplateInfo totalData){
		if (totalData!=null && totalData.getImages()!=null && totalData.getImages().size()>1) {
			Collections.shuffle(totalData.getImages());
		}

		if (totalData!=null && totalData.getImagesWebp()!=null && totalData.getImagesWebp().size()>1) {
			Collections.shuffle(totalData.getImagesWebp());
		}

		if (totalData!=null && totalData.getLongImagesWebp()!=null && totalData.getLongImagesWebp().size()>1) {
			Collections.shuffle(totalData.getLongImagesWebp());
		}

		if (totalData!=null && totalData.getLongImages()!=null && totalData.getLongImages().size()>1) {
			Collections.shuffle(totalData.getLongImages());
		}
	}
	public static void main(String[] args) {
		String[] split = "212.1.1.".split("\\.");
		System.out.println(split.length);
	}

}