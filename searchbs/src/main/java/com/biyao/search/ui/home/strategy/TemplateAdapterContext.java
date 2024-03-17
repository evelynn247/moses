package com.biyao.search.ui.home.strategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSONObject;
import com.biyao.search.common.enums.PlatformEnum;
import com.biyao.search.common.model.TopicItem;
import com.biyao.search.ui.home.constant.HomeTemplateEnum;
import com.biyao.search.ui.home.model.HomeTemplate;
import com.biyao.search.ui.home.model.app.AppHomeTemplate;
import com.biyao.search.ui.home.model.app.FloorInfo;
import com.biyao.search.ui.home.model.app.HomeFloor;
import com.biyao.search.ui.home.model.app.TemplateDetailInfo;
import com.biyao.search.ui.home.strategy.impl.BaseTemplateAdapter;
import com.biyao.search.ui.home.strategy.impl.BlockLineAdapter;
import com.biyao.search.ui.home.strategy.impl.SpecialAdapter;
import com.biyao.search.ui.home.strategy.impl.TitleLineAdapter;

/**
 * 各个模板类型适配，新增模板要添加相应适配
 * 
 * @author monkey
 * @date 2018年8月15日
 */
public class TemplateAdapterContext {
	
	/**
	 * 封装老接口的构建模板方法，
	 * @author monkey
	 */
	public static AppHomeTemplate buildTemplate(JSONObject modelJson,Map<Integer, TopicItem> topicItemMap,String searchPageUrl,PlatformEnum platformEnum,String pointPrefix,StringBuffer showContent, Map<String, String> stp){
		String modelType = modelJson.getString("modelType");
		
		TemplateAdapter adapter = getAdapter(modelType);
		
		AppHomeTemplate appHomeTemplate = adapter.adapte(modelJson, topicItemMap, searchPageUrl, platformEnum, pointPrefix,showContent,stp);
		
		return appHomeTemplate;
	}
	
	/**
	 * 获取新模板适配，因为首页直接调用老接口，暂时没有用
	 * @author monkey
	 */
	private static TemplateAdapter getAdapter(String modelType) {
		
		if (modelType.equals(HomeTemplateEnum.title_line.getTemplateType())) {
			return null;
		}else if(modelType.equals(HomeTemplateEnum.title_line.getTemplateType())){
			return null;
		}else{
			return null;
		}
		
	}
	
	/**
	 * 将老接口获取的楼层数据转换为新的数据格式
	 * @author monkey
	 */
	public static void convertOldFloor2New(List<List<HomeTemplate>> oldFloorList,HomeFloor resultHomeFloor) throws Exception{
		
		//新的所有楼层
		ArrayList<FloorInfo> newFloorList = new ArrayList<FloorInfo>();
		
		for (List<HomeTemplate> curFloor : oldFloorList) {
			//每个楼层
			FloorInfo floorInfo = new FloorInfo();
			//新楼层包含的所有模板
			ArrayList<AppHomeTemplate> templateInfo = new ArrayList<AppHomeTemplate>(); 
			
			for (HomeTemplate homeTemplate : curFloor) {
				//楼层中的每个模板
				AppHomeTemplate appHomeTemplate = new AppHomeTemplate();
				
				//转换楼层中的每个节点为新模板
				TemplateAdapter adapter = getAdapterForOld(homeTemplate.getModelType());
				appHomeTemplate = adapter.adapte(homeTemplate);
				templateInfo.add(appHomeTemplate);
				//为当前模板添加分割线模板
				if (homeTemplate.getBorderBottom()) {
					AppHomeTemplate borderBottom = new AppHomeTemplate();
					borderBottom.setTemplateType(HomeTemplateEnum.border_bottom.getTemplateId());
					ArrayList<TemplateDetailInfo> data = new ArrayList<>();
					borderBottom.setData(data);
					templateInfo.add(borderBottom);
				}
				
			}
			
			floorInfo.setTemplateInfo(templateInfo);
			newFloorList.add(floorInfo);
			
		}
		resultHomeFloor.setFloor(newFloorList );
		
	}
	
	
	//添加模板时添加相应的选择类型
	private static TemplateAdapter getAdapterForOld(String modelType) {
		
		if (modelType.equals(HomeTemplateEnum.title_line.getTemplateType())) {
			return new TitleLineAdapter();
		}else if(modelType.equals(HomeTemplateEnum.block_line.getTemplateType())){
			return new BlockLineAdapter();
		}else if(modelType.equals(HomeTemplateEnum.special.getTemplateType())){
			return new SpecialAdapter();
		}else{
			return new BaseTemplateAdapter();
		}
		
	}
	
}
