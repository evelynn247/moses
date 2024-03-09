package com.biyao.moses.model.adapter;

import com.alibaba.fastjson.JSONObject;
import com.biyao.moses.common.enums.PagePositionEnum;
import com.biyao.moses.common.enums.TemplateTypeEnum;
import com.biyao.moses.constants.CommonConstants;
import com.biyao.moses.context.ByUser;
import com.biyao.moses.exp.ExpirementSpace;
import com.biyao.moses.model.exp.ExpRequest;
import com.biyao.moses.model.exp.Expirement;
import com.biyao.moses.model.template.Block;
import com.biyao.moses.model.template.Template;
import com.biyao.moses.model.template.TemplateInfo;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.params.UIBaseRequest;
import com.biyao.moses.service.imp.ProductServiceImpl;
import com.biyao.moses.util.ApplicationContextProvider;
import com.biyao.moses.util.CacheRedisUtil;
import com.biyao.moses.util.ProductDetailUtil;
import com.biyao.moses.util.TraceLogUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.biyao.moses.common.constant.CommonConstants.ONE;

/**
 * 各个模板类型适配，新增模板要添加相应适配
 * 
 * @author monkey
 * @date 2018年09月03日
 */
@Component
@Slf4j
public class TemplateAdapterContext {

	@Autowired
	ProductDetailUtil productDetailUtil;

	@Autowired
	private ExpirementSpace expirementSpace;
	
	@Autowired
	private CacheRedisUtil cacheRedisUtil;

	@Autowired
	private ProductServiceImpl productService;


	
	@Value("${redis.pagecache.expire}")
	private int pageCacheTime;

	@Value("${home.feed.pageId}")
	private String homeFeedPageId;

	private TemplateAdapter getAdapter(String templateName) {

		TemplateAdapter templateAdapter = ApplicationContextProvider.getApplicationContext().getBean(templateName,
				TemplateAdapter.class);

		return templateAdapter;
	}

	private Template<TotalTemplateInfo> buildDynamicDataSizeTemplate(Template<TotalTemplateInfo> template, int size) {
		// 配置数量只有一个
		List<TotalTemplateInfo> totalInfoList = template.getData();
		for (int i = 1; i < size; i++) {
			totalInfoList.add(totalInfoList.get(0));
		}
		return template;
	}

	private static int getTotalPageNum(int totalDataNums, int pageNum) {

		int totalPageNum = (totalDataNums + pageNum - 1) / pageNum;
		return totalPageNum;
	}

	// ========================================================2.0===================================================================
	public Block<TemplateInfo> buildTemplateBlock(Block<TotalTemplateInfo> block,
			Map<String, List<TotalTemplateInfo>> data, Map<String, String> stp, String dataSourceType,ByUser user) throws Exception {

		// 是否跳转到最终feed页
		boolean feedTarget = StringUtils.isNotBlank(dataSourceType);

		// 返回结果集
		Block<TemplateInfo> resultBlock = new Block<TemplateInfo>();
		BeanUtils.copyProperties(block, resultBlock);
		// 用来填充模板数据的list
		List<Template<TemplateInfo>> templates = new ArrayList<Template<TemplateInfo>>();
		resultBlock.setBlock(templates);
		// 模板静态信息
		List<Template<TotalTemplateInfo>> temList = block.getBlock();

		// 不同模板的数据分页map key=dataSourceType+dataType+expId Integer为当前index
		HashMap<String, Integer> pagemap = new HashMap<String, Integer>();

		// 处理当前模板要拿到的数据，获取到的数据是当前block的数据集
		int templateIndex = -1;
		for (int j = 0; j < temList.size(); j++) {
			
			// 当前模板
			Template<TotalTemplateInfo> template = temList.get(j);
			List<TotalTemplateInfo> dataList = null;
			TemplateAdapter adapter = getAdapter(template.getTemplateName());
			// 当前模板所需要的数据量
			int totalSize = 0;
			Integer size = 0;
			// 对于动态模板，需要解析模板上配置的实验
			String key = "";
			String tid = "";
			if (template.isDynamic()) {

				if(!user.isNewExp()) {
					// 获取当前模板的实验
					tid = feedTarget ? dataSourceType : template.getDataSourceType();
					Expirement expirement = expirementSpace
							.getExpirement(ExpRequest.builder().uuid(user.getUuid()).tid(tid)
									.layerName(CommonConstants.LAYER_NAME_MATCH).build());
					// 当前模板取数据的key]
					String dataSource = feedTarget ? dataSourceType : template.getDataSourceType();
					if (expirement == null) {
						key = CommonConstants.DEFAULT_PREFIX + dataSource + "_" + CommonConstants.DEFAULT_EXPID;
					} else {
						key = CommonConstants.DEFAULT_PREFIX + dataSource + "_" + expirement.getExpId();
						// 处理上报日志的实验数据
					}
					// 当前模板 当前实验 dataSourceType+expId的数据
					dataList = data.get(key);
				}else{
					for(String dataKey : data.keySet()){
						dataList = data.get(dataKey);
						key = dataKey;
						break;
					}
				}

				size = adapter.getCurTemplateDataNum(dataList.size());
				if (pagemap.containsKey(key)) {
					totalSize = pagemap.get(key);
					pagemap.put(key, totalSize + size);
				} else {
					pagemap.put(key, size);
				}
				templateIndex++;
			}
			
			// 处理埋点数据拼装，拼装此次的aid
			String aid = TraceLogUtil.resetStpAid(stp, key, user,templateIndex);
			
			// 填充数据信息
			Template<TemplateInfo> result = null;
			if (block.isDynamic() && template.isDynamic()) {

				List<TotalTemplateInfo> subList = null;

				// TemplateTypeEnum.isDynamicDataSize 判断是否为动态数据
				TemplateTypeEnum typeEnum = TemplateTypeEnum.getTemplateTypeEnumByValue(template.getTemplateName());
				if (typeEnum.getIsDynamicDataSize()) {
					// 如果是动态数据模板，当前只有横划模板：横划模板会单独配置一个block
					subList = dataList.subList(totalSize, totalSize + size);
					// 横划模板数据配置会按照feed流模式，模板info数据只有一个，需要构造相应数据量的模板info数量
					template = buildDynamicDataSizeTemplate(template, subList.size());

				} else {
					// 如果模板是动态的
					subList = dataList.subList(totalSize, totalSize + size);
					totalSize = totalSize + size;
				}
				// 动态模板适配
				TraceLogUtil.fillPids(aid, subList,user);
				result = adapter.adapte(template, subList, JSONObject.toJSONString(stp),user);

			} else {
				// 静态模板适配
				result = adapter.adapte(template, null, JSONObject.toJSONString(stp),user);
			}

			templates.add(result);
		}

		return resultBlock;
	}

	/**
	 * 获取首页feed流的页面ID
	 * @return
	 */
	private String getHomeFeedPageId(){
		if(StringUtils.isEmpty(homeFeedPageId)){
			homeFeedPageId = CommonConstants.HOME_FEED_PAGEID;
		}
		return homeFeedPageId;
	}

	public Block<TemplateInfo> buildFeedBlock(Block<TotalTemplateInfo> block, Map<String, List<TotalTemplateInfo>> data,
											  Integer feedIndex, Map<String, String> stp, UIBaseRequest uiBaseRequest, ByUser user) throws Exception {
		Block<TemplateInfo> resultBlock = new Block<TemplateInfo>();
		BeanUtils.copyProperties(block, resultBlock);

		// 用来填充模板数据的list
		List<Template<TemplateInfo>> templates = new ArrayList<Template<TemplateInfo>>();
		resultBlock.setBlock(templates);
		String dataSourceType=uiBaseRequest.getTopicId();
		String pageId=uiBaseRequest.getPageId();
		// 是否跳转到最终feed页
		boolean feedTarget = StringUtils.isNotBlank(dataSourceType);

		user.setIsShowPrivilege(user.isShowPrivilege(dataSourceType));

		for (Template<TotalTemplateInfo> template : block.getBlock()) {
			// 当前模板
			// Template<TotalTemplateInfo> template = block.getBlock().get(0);
			// 获取当前模板的实验
//			String tid = feedTarget ? dataSourceType : template.getTid();
			if (data == null || data.size() == 0) {
				// 复制时会复制一个空模板
				resultBlock.setBlock(new ArrayList<>());
				return resultBlock;
			}
			List<TotalTemplateInfo> totalData = null;
			// 当前模板取数据的key
			String key = "";
			if(!user.isNewExp()) {
				String tid = feedTarget ? dataSourceType : template.getDataSourceType();
				Expirement expirement = expirementSpace
						.getExpirement(ExpRequest.builder().uuid(user.getUuid()).tid(tid)
								.layerName(CommonConstants.LAYER_NAME_MATCH).build());
				String dataSource = feedTarget ? dataSourceType : template.getDataSourceType();
				if (expirement == null) {
					key = CommonConstants.DEFAULT_PREFIX + dataSource + "_" + CommonConstants.DEFAULT_EXPID;
				} else {
					key = CommonConstants.DEFAULT_PREFIX + dataSource + "_" + expirement.getExpId();
				}

				if (!data.containsKey(key) || data.get(key).size() == 0) {
					// 复制时会复制一个空模板
					resultBlock.setBlock(new ArrayList<>());
					return resultBlock;
				}
				// 获得的总数据 totalData
				// 当前模板 当前实验 dataSourceType+dataType+expId的数据
				totalData = data.get(key);
			}else{
				for(String dataKey : data.keySet()){
					totalData = data.get(dataKey);
					if(CollectionUtils.isEmpty(totalData)){
						// 复制时会复制一个空模板
						resultBlock.setBlock(new ArrayList<>());
						return resultBlock;
					}
					key = dataKey;
					break;
				}
			}
			// 模板信息
			TemplateTypeEnum templateTypeEnum = TemplateTypeEnum.getTemplateTypeEnumByValue(template.getTemplateName());
			int templateDataNum = templateTypeEnum.getDataSize();
			//双排FEED流改为10个一页
//			int pageNumber =  CommonConstants.PAGENUM;
//			if(templateDataNum==2){
//				pageNumber = 5;
//			}
			// feed分页，startIndex总数据开始，endIndex总数据结束 30 39 feedIndex=4
			int startIndex = (feedIndex - 1) * CommonConstants.PAGENUM * templateDataNum;
			int endIndex = feedIndex * CommonConstants.PAGENUM * templateDataNum - 1;
			endIndex = endIndex > totalData.size() - 1 ? totalData.size() - 1 : endIndex;

			if (startIndex > totalData.size() - 1) {
				resultBlock.setBlock(new ArrayList<>());
				return resultBlock;
			}
			// 当前页使用的feed数据
			List<TotalTemplateInfo> subList = totalData.subList(startIndex, endIndex + 1);
			//缓存曝光数据
			if(getHomeFeedPageId().equals(pageId)){
			     cacheHomeFeedExposure(subList, user,pageId,dataSourceType);
			}
			// 当前feed页的总模板数
			int totalPageNum = getTotalPageNum(subList.size(), templateDataNum);
			// 处理埋点数据拼装，拼装此次的aid
			String aid = TraceLogUtil.resetStpAid(stp, key, user,feedIndex);
			TraceLogUtil.fillPids(aid, subList,user);
			//当请求为分类页商品是  将商品替换为商品组
			if ( "1".equals(uiBaseRequest.getIsShowProductGroup()) && (
					PagePositionEnum.HOMETABCAR.getPagePositionId().equals(uiBaseRequest.getPagePositionId()) ||
					PagePositionEnum.SINGLECAR.getPagePositionId().equals(uiBaseRequest.getPagePositionId())
				)  ) {
				productService.converProductToProductGroup(subList, uiBaseRequest, user, feedIndex);
			}
			// 如果需要将商品转化为视频
			if(ONE.toString().equals(uiBaseRequest.getIsConvert2Video()) && uiBaseRequest.getChannelType() != null){
				productService.converProductToVideo(subList, uiBaseRequest, user, feedIndex);
			}
			for (int i = 0; i < totalPageNum; i++) {

				TemplateAdapter adapter = getAdapter(templateTypeEnum.getTemplateType());
				// i 为当前feed层的模板分页
				Template<TemplateInfo> adapte = adapter.adapte(startIndex, template, subList, i, JSONObject.toJSONString(stp),user);
				templates.add(adapte);
			}
		}

		return resultBlock;
	}

	/**
	 * 填充liveStatus信息
	 * @param totalTemplateInfoList
	 */
//	private void fillLiveStatus(List<TotalTemplateInfo> totalTemplateInfoList){
//		if(CollectionUtils.isEmpty(totalTemplateInfoList)){
//			return;
//		}
//
//		try {
//			//填充默认值0
//			totalTemplateInfoList.forEach(info -> info.setLiveStatus("0"));
//
//			//查询商品直播状态
//			Map<String, String> productLiveStatusMap = liveManagementCenterService.queryLiveStatus(totalTemplateInfoList);
//			if (productLiveStatusMap == null || productLiveStatusMap.size() == 0) {
//				return;
//			}
//
//			totalTemplateInfoList.forEach(info -> {
//				if (info != null && info.getId() != null) {
//					info.setLiveStatus(productLiveStatusMap.getOrDefault(info.getId(), "0"));
//				}
//			});
//		}catch (Exception e){
//			log.error("[严重异常][填充直播状态标签]出现异常，", e);
//		}
//	}

	/**
	* @Description 缓存首页feed流曝光数据 
	* @param list
	* @param user
	* @param pageId
	* @param topicId
	* @return boolean 
	* @version V1.0
	* @auth 邹立强 (zouliqiang@idstaff.com)
	 */
	private boolean cacheHomeFeedExposure(List<TotalTemplateInfo> list, ByUser user, String pageId, String topicId) {
		boolean isSuccess = false;
		try {
			if (list != null && list.size() > 0) {
				List<String> pidsList = new ArrayList<String>();
				for (TotalTemplateInfo totalTemplateInfo : list) {
					String id = totalTemplateInfo.getId();
					if (totalTemplateInfo != null && StringUtils.isNotEmpty(id)) {
						pidsList.add(id);
					}
				}
				String[] pidsArray = pidsList.toArray(new String[0]);
				cacheRedisUtil.batchSadd(CommonConstants.PAGE_CACHE_PREFIX + user.getPvid() + "_" + pageId + "_" + topicId
						+ "_" + CommonConstants.HOME_FEED_CACHE_SUFFIX, pidsArray);
				cacheRedisUtil.expire(CommonConstants.PAGE_CACHE_PREFIX + user.getPvid() + "_" + pageId + "_" + topicId + "_"
						+ CommonConstants.HOME_FEED_CACHE_SUFFIX, pageCacheTime);
				isSuccess = true;
			}
		} catch (Exception e) {
			log.error("[严重异常]缓存首页feed流曝光数据失败：uuid {}, pvid {}",user.getUuid(), user.getPvid(), e);
		}
		return isSuccess;
	}
}
