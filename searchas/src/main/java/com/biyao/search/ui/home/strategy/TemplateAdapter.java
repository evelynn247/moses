package com.biyao.search.ui.home.strategy;

import java.util.Map;

import com.alibaba.fastjson.JSONObject;
import com.biyao.search.common.enums.PlatformEnum;
import com.biyao.search.common.model.TopicItem;
import com.biyao.search.ui.home.model.HomeTemplate;
import com.biyao.search.ui.home.model.app.AppHomeTemplate;

public interface TemplateAdapter {
	
	/**
	 * 模板处理
	 * @author monkey
	 * @param modelJson 模板配置模型
	 * @param topicItemMap 主题配置内容
	 * @param searchPageUrl 搜索中间页跳转路径
	 * @param platformEnum 请求平台信息，兼容不同版本使用
	 * @param pointPrefix 埋点统计信息
	 * @param showContent 日志拼接
	 * @return AppHomeTemplate
	 */
	AppHomeTemplate adapte(JSONObject modelJson,Map<Integer, TopicItem> topicItemMap,String searchPageUrl,PlatformEnum platformEnum,String pointPrefix,StringBuffer showContent, Map<String, String> stp);
	
	
	/**
	 * 调用老接口适配
	 * @author monkey
	 * @param homeTemplate 老接口解析返回数据
	 * @return AppHomeTemplate 新的app首页模板
	 */
	AppHomeTemplate adapte(HomeTemplate homeTemplate) throws Exception;

}
