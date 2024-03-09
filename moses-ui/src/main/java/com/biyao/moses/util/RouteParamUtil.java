package com.biyao.moses.util;

import com.alibaba.dubbo.common.URL;
import com.biyao.moses.constants.ERouterType;
import com.biyao.moses.model.template.TemplateInfo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * RouteParamUtil
 * 
 * @Description
 * @Date 2018年9月27日
 */
@Component
public class RouteParamUtil {

	/**
	 * 根据routeType获取要传递参数
	 * 
	 * @author monkey
	 * @param
	 * @return
	 */
	public Map<String, String> getRouteParam(TemplateInfo totalTemplateInfo) {

		if (totalTemplateInfo.getRouterType() == null) {
			// 没有跳转
			return null;
		}

		HashMap<String, String> result = new HashMap<String, String>();

		Map<String, String> routerParam = new HashMap<String, String>();
		if (totalTemplateInfo.getRouterParams() != null && totalTemplateInfo.getRouterParams().size() > 0) {
			routerParam = totalTemplateInfo.getRouterParams();
		}

		result.put("stp", routerParam.get("stp"));
		if (totalTemplateInfo.getRouterType().equals(ERouterType.DALIYPAGE.getNum())) {
			// 每日上新跳转处理

		} else if (totalTemplateInfo.getRouterType().equals(ERouterType.SUPPLIERPAGE.getNum())) {
			// 商家店铺,routerParam中处理
			result.put("supplierId", routerParam.get("supplierId"));
			result.put("suId", routerParam.get("suId"));

		} else if (totalTemplateInfo.getRouterType().equals(ERouterType.TOPICLIST.getNum())) {
			// 专题列表

		} else if (totalTemplateInfo.getRouterType().equals(ERouterType.TOPICDETAIL.getNum())) {
			// 专题详情 在match中对专题详情的做处理
			result.put("topicId", routerParam.get("cmsTopicId"));

		} else if (totalTemplateInfo.getRouterType().equals(ERouterType.SEARCH.getNum())) {
			// 搜索中间页
			String q = routerParam.get("q");
			String tpid = routerParam.get("topicId");
			String sp = "q=" + q + "&tpid=" + tpid;
			if (routerParam.containsKey("fb")) {
				sp += "&toFB=1";
			} else {
				sp += "&toTP=1";
			}
			result.put("sp", URL.encode(sp));
			result.put("query", q);

		} else if (totalTemplateInfo.getRouterType().equals(ERouterType.RECOMMEND.getNum())) {
			// 推荐中间页,从match数据中来获取
			result.put("pageId", routerParam.get("pageId"));
			result.put("topicId", routerParam.get("topicId"));
			result.put("priorityProductIds", routerParam.get("priorityProductIds"));

		} else if (totalTemplateInfo.getRouterType().equals(ERouterType.PRODUCTDETAIL.getNum())) {
			// 商品详情
			result.put("suId", routerParam.get("id"));
		}else if (totalTemplateInfo.getRouterType().equals(ERouterType.CHOUJIANG.getNum())) {
			// 抽奖
		}else if (totalTemplateInfo.getRouterType().equals(ERouterType.ZHENGCAN.getNum())) {
			result.put("categoryId", routerParam.get("categoryId"));
			result.put("title", routerParam.get("title"));
		}else if (totalTemplateInfo.getRouterType().equals(ERouterType.XINSHOU.getNum())) {
			// 新手专享
			result.put("suId", routerParam.get("id"));
		}else if (totalTemplateInfo.getRouterType().equals(ERouterType.ADVERT.getNum())) {
			// 活动广告入口
			result.put("activityId", routerParam.get("activityId"));
			result.put("position", routerParam.get("position"));
			String router = routerParam.get("router");
			if(StringUtils.isNotBlank(router)){
				result.put("router", router);
			}
		}else if(totalTemplateInfo.getRouterType().equals(ERouterType.SWIPERPICCONF.getNum())){
			result.put("id", routerParam.get("id"));
			String key = "liveId";
			if(routerParam.containsKey(key)){
				result.put(key, routerParam.get(key));
			}
			key = "router";
			if(routerParam.containsKey(key)){
				result.put(key, routerParam.get(key));
			}
		}else if (totalTemplateInfo.getRouterType().equals(ERouterType.PRODUCTGROUP.getNum())){
			result.put("productGroupId", routerParam.get("productGroupId"));
		}else if (totalTemplateInfo.getRouterType().equals(ERouterType.VIDEO.getNum())){
			result.put("videoId", routerParam.get("videoId"));
			result.put("position", routerParam.get("position"));
		}else if (totalTemplateInfo.getRouterType().equals(ERouterType.OPEVIDEO.getNum())){
			result.put("videoId", routerParam.get("videoId"));
			result.put("position", routerParam.get("position"));
		}
		return result;
	}

}
