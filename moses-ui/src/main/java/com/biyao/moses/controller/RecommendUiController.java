package com.biyao.moses.controller;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.biyao.dclog.service.DCLogger;
import com.biyao.moses.cache.*;
import com.biyao.moses.common.constant.ErrorCode;
import com.biyao.moses.common.constant.RedisKeyConstant;
import com.biyao.moses.common.constant.UPCUserTypeConstants;
import com.biyao.moses.common.enums.ExceptionTypeEnum;
import com.biyao.moses.common.enums.PageVersionEnum;
import com.biyao.moses.common.enums.SourceEnum;
import com.biyao.moses.constants.CommonConstants;
import com.biyao.moses.context.ByUser;
import com.biyao.moses.context.UserContext;
import com.biyao.moses.model.adapter.TemplateAdapterContext;
import com.biyao.moses.model.template.*;
import com.biyao.moses.model.template.entity.RecommendPage;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.params.*;
import com.biyao.moses.service.imp.*;
import com.biyao.moses.thread.HomeRecommendThread;
import com.biyao.moses.util.ApplicationContextProvider;
import com.biyao.moses.util.IdCalculateUtil;
import com.biyao.moses.util.MatchRedisUtil;
import com.biyao.moses.util.RedisUtil;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 
 * @Description
 * @Date 2018年9月27日
 */
@RestController
@RequestMapping(value = "/recommend/ui")
@Api("RecommendUiController相关的api")
@Slf4j
public class RecommendUiController {

	@Autowired
	MatchAndRankAnsyService matchAndRankAnsyService;

	@Autowired
	HttpMosesConfServiceImpl mosesConfService;

	@Autowired
	TemplateAdapterContext templateAdapterContext;

	@Autowired
	RedisUtil redisUtil;

	@Autowired
	HomePageCacheServiceImpl homePageCacheService;

	@Autowired
	CMSFrontendCategoryCache cmsFrontendCategoryCache;
	
	@Autowired
	CategorySortService categorySortService;

	@Autowired
	HomePageCache homePageCacheImpl;

	@Autowired
	SaleAttributesSortCache saleAttributesSortCache;

	@Autowired
	CategoryPicRefreshSevice categoryPicRefreshSevice;

	@Autowired
	BusinessFlagServiceImpl businessFlagService;

	@Autowired
	RedisCache redisCache;

	@Autowired
	MatchRedisUtil matchRedisUtil;

	@Autowired
	private ProductDetailCache productDetailCache;

	@Autowired
	private PageConfigService pageConfigService;

	private static DCLogger mosesuiDcLogger = DCLogger.getLogger("mosesui");

	private static final String BLOCK = "block";
	private static final String FEED = "feed";

	@BProfiler(key = "com.biyao.moses.controller.RecommendUiController.homerecommend", monitorType = { MonitorType.TP,
			MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR })
	@ApiOperation(value = "获取首页")
	@PostMapping("/homepage")
	public ApiResult<Page<TemplateInfo>> homerecommend(@ApiParam UIBaseRequest uibaseRequest) {

		AsyncTaskExecutor asyncTaskExecutor = ApplicationContextProvider.getBean(AsyncTaskExecutor.class);

		ApiResult<Page<TemplateInfo>> apiResult = new ApiResult<Page<TemplateInfo>>();
		apiResult.setSuccess(ErrorCode.SUCCESS_CODE);

		if(StringUtils.isBlank(uibaseRequest.getSid())){
			// 本次请求的唯一id
			String sid = IdCalculateUtil.createUniqueId();
			uibaseRequest.setSid(sid);
		}
		ByUser user = UserContext.getUser();
		try {
			HomeRecommendThread task = new HomeRecommendThread(user, uibaseRequest);

			Future<ApiResult<Page<TemplateInfo>>> future = asyncTaskExecutor.submit(task);

			apiResult = future.get(500, TimeUnit.MILLISECONDS);
		} catch (Exception e) {
			log.error("[一般异常][首页超时]获取首页信息超时，调用缓存，uuid {}， sid {}，", user == null ? 0 : user.getUuid(), uibaseRequest.getSid(),  e);
			HomePageCache homePageCache = ApplicationContextProvider.getBean(HomePageCache.class);

			Page<TemplateInfo> page = homePageCache.getCurrentPageInfo(uibaseRequest.getPageIndex(),
					uibaseRequest.getPageId(), uibaseRequest.getTopicId());

			apiResult.setData(page);
		} finally {
			UserContext.manulClose();
		}
		return apiResult;
	}

	@BProfiler(key = "com.biyao.moses.controller.RecommendUiController.homePageCache", monitorType = { MonitorType.TP,
			MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR })
	@ApiOperation(value = "获取首页")
	@PostMapping("/homePageCache")
	public ApiResult<ArrayList<Page<TemplateInfo>>> homePageCache(@ApiParam UICacheRequest uiCacheRequest) {
		log.info("获取首页缓存请求参数user={}，request={}", JSONObject.toJSONString(UserContext.getUser()),
				JSONObject.toJSONString(uiCacheRequest));
		ApiResult<ArrayList<Page<TemplateInfo>>> apiResult = new ApiResult<ArrayList<Page<TemplateInfo>>>();
		apiResult.setSuccess(ErrorCode.SUCCESS_CODE);
		try {
			HomePageCache homePageCache = ApplicationContextProvider.getBean(HomePageCache.class);

			ArrayList<Page<TemplateInfo>> homePageDetail = homePageCache.getHomePageDetail();
			apiResult.setData(homePageDetail);
		} finally {
			UserContext.manulClose();
		}
		return apiResult;
	}

	@BProfiler(key = "com.biyao.moses.controller.RecommendUiController.recommend", monitorType = { MonitorType.TP,
			MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR })
	@ApiOperation(value = "获取推荐中间页数据")
	@PostMapping("/page")
	public ApiResult<Page<TemplateInfo>> recommend(@ApiParam UIBaseRequest uibaseRequest) {

		ByUser user = UserContext.getUser();
		if(StringUtils.isBlank(uibaseRequest.getSid())){
			// 本次请求的唯一id
			String sid = IdCalculateUtil.createUniqueId();
			uibaseRequest.setSid(sid);
		}
		log.info("[推荐中间页请求参数]-[userInfo={},request={}]", JSONObject.toJSONString(user),
				JSONObject.toJSONString(uibaseRequest));

		if(user != null) {
			user.setDebug(redisCache.isHomeFeedWhite(user.getUuid()));
			user.setPageVersion(uibaseRequest.getPageVersion());
			user.setShowAdvert(uibaseRequest.getShowAdvert());
			UIBaseBody uiBaseBody = user.getUiBaseBody();
			if(uiBaseBody != null){
				if(StringUtils.isBlank(uibaseRequest.getAdvertInfo()) && StringUtils.isNotBlank(uiBaseBody.getAdvertInfo())){
					uibaseRequest.setAdvertInfo(uiBaseBody.getAdvertInfo());
				}
				if(StringUtils.isBlank(uibaseRequest.getSelectedScreenAttrs()) && StringUtils.isNotBlank(uiBaseBody.getSelectedScreenAttrs())){
					uibaseRequest.setSelectedScreenAttrs(uiBaseBody.getSelectedScreenAttrs());
				}
				if(StringUtils.isBlank(uibaseRequest.getSwiperPicConfInfo()) && StringUtils.isNotBlank(uiBaseBody.getSwiperPicConfInfo())){
					uibaseRequest.setSwiperPicConfInfo(uiBaseBody.getSwiperPicConfInfo());
				}
			}
			user.setAdvertInfoList(AdvertInfoService.parseAdvertInfo(uibaseRequest.getAdvertInfo()));
		}
		long start = System.currentTimeMillis();

		ApiResult<Page<TemplateInfo>> apiResult = new ApiResult<Page<TemplateInfo>>();
		apiResult.setSuccess(ErrorCode.SUCCESS_CODE);
		try {
			// 0 参数校验
			if (Integer.valueOf(uibaseRequest.getPageIndex()) <= 0) {
				return new ApiResult<Page<TemplateInfo>>(ErrorCode.PARAM_ERROR_CODE, "页码错误", null);
			}

			// user 校验
			if (!user.valid()) {
				return new ApiResult<Page<TemplateInfo>>(ErrorCode.PARAM_ERROR_CODE, "用户参数错误", null);
			}

			// 如果需要展示活动广告，则模板必须为支持展示活动广告的模板
			if("1".equals(uibaseRequest.getShowAdvert()) || !CollectionUtils.isEmpty(user.getAdvertInfoList())){
				if(!PageVersionEnum.PAGE_VERSION_ADVERT.getVersion().equals(uibaseRequest.getPageVersion())){
					return new ApiResult<Page<TemplateInfo>>(ErrorCode.PARAM_ERROR_CODE, "展示活动或活动信息列表入参与页面版本入参不匹配", null);
				}
			}
			// 初始化结果集
			Page<TemplateInfo> resultPage = new Page<TemplateInfo>();
			List<Block<TemplateInfo>> resultblockList = new ArrayList<Block<TemplateInfo>>();
			resultPage.setBlockList(resultblockList);
			resultPage.setPid(uibaseRequest.getPageId());
			apiResult.setData(resultPage);

			// 1 获取模板
			long pageTemStart = System.currentTimeMillis();
			user.setPageId(uibaseRequest.getPageId());
			user.setPageVersion(uibaseRequest.getPageVersion());
			Page<TotalTemplateInfo> page = pageConfigService.queryPageById(uibaseRequest.getPageId(), user);
			if (page == null) {
				return new ApiResult<Page<TemplateInfo>>(ErrorCode.SYSTEM_ERROR_CODE, "获取模板失败", null);
			}
			long pageTemEnd = System.currentTimeMillis();
			// log.info("[推荐]-[获取page模板]-[pageId={},耗时={}]", uibaseRequest.getPageId(),
			// pageTemEnd - pageTemStart);

			// 2 解析模板填充数据
			resultPage.setPageName(page.getPageName());
			// 获取当前页面名称
			if (StringUtils.isNotBlank(uibaseRequest.getTopicId())) {
				//实验配置迁移后的 临时方案，先从match集群中查询，再从原集群查询
				String pageNameKey = CommonConstants.PAGENAME_DEFAULT_PREFIX + uibaseRequest.getTopicId();
				String pageName = matchRedisUtil.getString(pageNameKey);
				if(StringUtils.isBlank(pageName)){
					pageName = redisUtil.getString(pageNameKey);
					if(StringUtils.isNotBlank(pageName)){
						log.error("[严重异常]redis key {} 未迁移到match集群", pageNameKey);
					}
				}
				if (StringUtils.isNotBlank(pageName)) {
					resultPage.setPageName(pageName);
				}
			}

			int blockSize = page.getBlockList().size();
			Integer pageIndex = Integer.valueOf(uibaseRequest.getPageIndex());
			Integer startIndex = (pageIndex - 1) * CommonConstants.PAGESIZE;

			// 楼层模板数据
			List<Block<TotalTemplateInfo>> blockList = page.getBlockList();

			// 计算feed页码
			Integer feedIndex = 0;
			Block<TotalTemplateInfo> lastBlock = blockList.get(blockList.size() - 1);
			if (lastBlock.isFeed()) {
				// 最后一个区块模板是feed
				// feed的第一页是pageIndex的第几页
				int a = blockSize % CommonConstants.PAGESIZE == 0 ? (blockSize / CommonConstants.PAGESIZE)
						: (blockSize / CommonConstants.PAGESIZE + 1);
				feedIndex = (pageIndex - a + 1) < 0 ? 0 : (pageIndex - a + 1);
			}

			ArrayList<String> sortList = new ArrayList<String>();
			Map<String, Future<Block<TemplateInfo>>> futureMap = new HashMap<String, Future<Block<TemplateInfo>>>();
			// 楼层模板处理
			for (int i = startIndex; i < blockList.size() && i < startIndex + CommonConstants.PAGESIZE; i++) {
				Block<TotalTemplateInfo> block = blockList.get(i);
				boolean feed = block.isFeed();
				if (feed) { // 如果是feed流，拉出去单独处理
					continue;
				}
				Future<Block<TemplateInfo>> futureResult = matchAndRankAnsyService.matchAndRank(block, 0,
						uibaseRequest, user, i);
				sortList.add(BLOCK + i);
				futureMap.put(BLOCK + i, futureResult);
			}

			// feed流处理
			if (feedIndex >= 1) {
				Future<Block<TemplateInfo>> feedMatchAndRank = matchAndRankAnsyService.feedMatchAndRank(lastBlock,
						feedIndex, uibaseRequest, user);
				sortList.add(FEED + feedIndex);
				futureMap.put(FEED + feedIndex, feedMatchAndRank);
			}
			long asnyStart = System.currentTimeMillis();
			// 异步数据处理
			for (int i = 0; i < sortList.size(); i++) {
				String key = sortList.get(i);
				Future<Block<TemplateInfo>> future = futureMap.get(key);

				Block<TemplateInfo> result = null;
				try {
					result = future.get(1500L, TimeUnit.MILLISECONDS);
				} catch (Exception e) {
					log.error("[严重异常]楼层数据获取超时，参数：{}",JSONObject.toJSONString(uibaseRequest));
					user.getExceptionTypeMap().put(ExceptionTypeEnum.FEED_EXCEPTION.getId(), ExceptionTypeEnum.FEED_EXCEPTION);
					continue;
				}
				if (result != null) {
					resultblockList.add(result);
				}
			}
			long asnyEnd = System.currentTimeMillis();
			// log.info("[推荐]-[异步处理]-[pageId={},耗时={}]", uibaseRequest.getPageId(), asnyEnd
			// - asnyStart);

			//Feed流异常时填充兜底数据
			if(matchAndRankAnsyService.isNeedUseCacheData(uibaseRequest, user, resultPage, lastBlock)){
				HomePageCache homePageCache = ApplicationContextProvider.getBean(HomePageCache.class);
				Page<TemplateInfo> pageCache = homePageCache.getCurrentPageInfo(uibaseRequest.getPageIndex(),
						uibaseRequest.getPageId(), uibaseRequest.getTopicId());
				apiResult.setData(pageCache);
				log.info("[检查日志]feed流异常，已走兜底，入参：{},异常信息：{}",JSONObject.toJSONString(uibaseRequest),JSONObject.toJSONString(user));
			}

			printMosesUiLog(user, uibaseRequest, lastBlock.getBid(), String.valueOf(pageIndex),
					page.getPid());
			// log.info("[推荐]-[总耗时]-[pageId={},耗时={}]", uibaseRequest.getPageId(), asnyEnd -
			// start);
		} catch (Exception e) {
			log.error("[严重异常]推荐中间页填充模板数据失败， request {} ", JSON.toJSONString(uibaseRequest), e);
			return new ApiResult<Page<TemplateInfo>>(ErrorCode.SYSTEM_ERROR_CODE, "系统未知错误", null);
		} finally {
			UserContext.manulClose();
		}
		return apiResult;
	}

	@BProfiler(key = "com.biyao.moses.controller.RecommendUiController.categoryRecommend", monitorType = {
			MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR })
	@ApiOperation(value = "获取类目数据")
	@PostMapping("/category")
	public ApiResult<Page<TemplateInfo>> categoryRecommend(@ApiParam UIBaseRequest uibaseRequest) {
		//如果传入了特权金优惠列表，需要验证格式是否正确
		if(uibaseRequest.getPriCouponAmountList() != null && ! uibaseRequest.getPriCouponAmountList().trim().isEmpty()){
			/*
			 * 验证 priCouponAmountList格式必须符合 {类型}:{金额}
			 * 类型： 1新客 2通用特权金
			 * 最多同时拥有1个新客和1个通用特权金
			 */
			try{
				//使用正则表达式验证格式
				String patten = "^((1|2):[1-9]\\d*)|(1:[1-9]\\d*,2:[1-9]\\d*)|(2:[1-9]\\d*,1:[1-9]\\d*)$";
				if(false == Pattern.matches(patten, uibaseRequest.getPriCouponAmountList())){
					return new ApiResult<Page<TemplateInfo>>(ErrorCode.PARAM_ERROR_CODE, "参数错误", null);
				}
			}catch (Exception e){
				return new ApiResult<Page<TemplateInfo>>(ErrorCode.PARAM_ERROR_CODE, "参数错误", null);
			}

			//传了特权金，用户类型必填
			if(uibaseRequest.getUserType() == null || uibaseRequest.getUserType().trim().isEmpty()){
				return new ApiResult<Page<TemplateInfo>>(ErrorCode.PARAM_ERROR_CODE, "参数错误", null);
			}
			//用户类型只能是 1或2   1新客 2老客
			if( ! "1".equals(uibaseRequest.getUserType()) && ! "2".equals(uibaseRequest.getUserType())){
				return new ApiResult<Page<TemplateInfo>>(ErrorCode.PARAM_ERROR_CODE, "参数错误", null);
			}
		}



		ApiResult<Page<TemplateInfo>> recommend = this.recommend(uibaseRequest);

		return recommend;
	}

	@BProfiler(key = "com.biyao.moses.controller.RecommendUiController.getBusinessFlag", monitorType = {
			MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR })
	@ApiOperation(value = "获取业务判断标识 hasNews:类目页上新是否展示红点")
	@PostMapping("/getBusinessFlag")
	public ApiResult<Map<String,Boolean>> getBusinessFlag(@ApiParam UIBaseRequest uibaseRequest) {
		ByUser user = UserContext.getUser();
		long start = System.currentTimeMillis();
		ApiResult<Map<String, Boolean>> apiResult = new ApiResult<>();
		apiResult.setSuccess(ErrorCode.SUCCESS_CODE);
		Map<String, Boolean> resultMap = new HashMap<>();
		apiResult.setData(resultMap);

		//参数校验
		if ((StringUtils.isBlank(uibaseRequest.getCategoryIds()) && StringUtils.isBlank(uibaseRequest.getScmIds())) ||
				StringUtils.isBlank(user.getUuid())|| StringUtils.isBlank(uibaseRequest.getFrontendCategoryId())) {
			apiResult.setSuccess(ErrorCode.PARAM_ERROR_CODE);
			apiResult.setError("后台类目ids与scmids不可都为空,uuid,前台类目ID不可为空");
			log.error("[严重异常]获取业务判断标识入参校验错误：userInfo={}, request={}",JSONObject.toJSONString(user), JSONObject.toJSONString(uibaseRequest));
			return apiResult;
		}
		Boolean rtBoolean = false;
		try {
			//判断是否展示上新红点
			rtBoolean = businessFlagService.hasNewsByCategroys
					(uibaseRequest.getCategoryIds(), uibaseRequest.getScmIds(),
							user.getUuid(),uibaseRequest.getFrontendCategoryId());
		} catch (Exception e) {
			apiResult.setSuccess(ErrorCode.SYSTEM_ERROR_CODE);
			apiResult.setError("判断是否展示上新红点异常");
			log.error("判断是否展示上新红点异常", e);
			return apiResult;
		}

		resultMap.put("hasNews", rtBoolean);

		return apiResult;
	}

	@BProfiler(key = "com.biyao.moses.controller.RecommendUiController.getScreenAttrs", monitorType = {
			MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR })
	@ApiOperation(value = "获取类目页的筛选信息，筛选信息为销售属性标准化的值")
	@PostMapping("/getScreenAttrs")
	public ApiResult<Map<String,List<String>>> getScreenAttrs(@ApiParam UIBaseRequest uibaseRequest) {

		ByUser user = UserContext.getUser();
		long start = System.currentTimeMillis();
        ApiResult<Map<String,List<String>>> apiResult = new ApiResult<>();
		apiResult.setSuccess(ErrorCode.SUCCESS_CODE);

		try {
			// user 校验
			if (!user.valid()) {
				return new ApiResult<Map<String,List<String>>>(ErrorCode.PARAM_ERROR_CODE, "用户参数错误", null);
			}
			// 本次请求的唯一id
			String uniqueId = IdCalculateUtil.createUniqueId();
			// 初始化结果集
			Map<String,List<String>> resultMap = new LinkedHashMap<>();
			apiResult.setData(resultMap);

			long pageTemStart = System.currentTimeMillis();
			user.setPageId(uibaseRequest.getPageId());
			Page<TotalTemplateInfo> page = pageConfigService.queryPageById(uibaseRequest.getPageId(), user);
			if (page == null) {
				return new ApiResult<Map<String,List<String>>>(ErrorCode.SYSTEM_ERROR_CODE, "获取模板失败", null);
			}
			long pageTemEnd = System.currentTimeMillis();
			// log.info("[推荐]-[获取page模板]-[pageId={},耗时={}]", uibaseRequest.getPageId(),
			// pageTemEnd - pageTemStart);

			// 楼层模板数据
			List<Block<TotalTemplateInfo>> blockList = page.getBlockList();

			Block<TotalTemplateInfo> lastBlock = blockList.get(blockList.size() - 1);

			Map<String, Future<Map<String, List<TotalTemplateInfo>>>> futureMap = new HashMap<>();

			int feedIndex = 1;
			Future<Map<String, List<TotalTemplateInfo>>> match = matchAndRankAnsyService.match(lastBlock,
						feedIndex, uibaseRequest, user);
			futureMap.put(BLOCK, match);
			long asnyStart = System.currentTimeMillis();
			// 异步数据处理
			Future<Map<String, List<TotalTemplateInfo>>> future = futureMap.get(BLOCK);

			Map<String, List<TotalTemplateInfo>> result = null;
			try {
				result = future.get(1500l, TimeUnit.MILLISECONDS);
			} catch (Exception e) {
				log.error("楼层数据获取超时");
			}
			long asnyEnd = System.currentTimeMillis();
			// log.info("[推荐]-[异步处理]-[pageId={},耗时={}]", uibaseRequest.getPageId(), asnyEnd - asnyStart);

			List<TotalTemplateInfo> totalList = new ArrayList<>();
			//log.error("筛选， result {}", JSON.toJSONString(result));
			if (result != null) {
				for(Map.Entry<String, List<TotalTemplateInfo>> map : result.entrySet()){
					totalList.addAll(map.getValue());
				}
			}
			//根据match返回的商品信息，汇总每一个商品的销售属性
			Map<String, Set<String>> allSpuSaleAttrs = getSalesAttrsFromProduct(totalList);
			//log.error("获取筛选属性，所有的商品信息{}", JSON.toJSONString(allSpuSaleAttrs));

			//对销售属性进行排序
			//1、获取颜色和尺码的排序字典
			String colorStr = CommonConstants.STD_SALE_ATTR_KEY_COLOR;
			String sizeStr = CommonConstants.STD_SALE_ATTR_KEY_SIZE;
			List<String> sortColorList = saleAttributesSortCache.getSaleAttrsByAttrKey(colorStr);
			List<String> sortSizeList = saleAttributesSortCache.getSaleAttrsByAttrKey(sizeStr);

			List<String> colorList = new ArrayList<>();
			List<String> sizeList = new ArrayList<>();
			if(!CollectionUtils.isEmpty(allSpuSaleAttrs)){
				Set<String> tmpColorList = allSpuSaleAttrs.get(colorStr);
				if(!CollectionUtils.isEmpty(sortColorList) && !CollectionUtils.isEmpty(tmpColorList)){
					for(String color : sortColorList){
						if(tmpColorList.contains(color)){
							colorList.add(color);
						}
					}
				}
				Set<String> tmpSizeList = allSpuSaleAttrs.get(sizeStr);
				if(!CollectionUtils.isEmpty(sortSizeList) && !CollectionUtils.isEmpty(tmpSizeList)){
					for(String size : sortSizeList){
						if(tmpSizeList.contains(size)){
							sizeList.add(size);
						}
					}
				}
			}
			//颜色筛选项在尺码筛选项前
			if(!CollectionUtils.isEmpty(colorList)){
				resultMap.put(colorStr,colorList);
			}
			if(!CollectionUtils.isEmpty(sizeList)){
				resultMap.put(sizeStr, sizeList);
			}
		} catch (Exception e) {
			log.error("[严重异常]获取筛选属性异常[userInfo={},request={}]", JSONObject.toJSONString(user), JSONObject.toJSONString(uibaseRequest), e);
			return new ApiResult<Map<String,List<String>>>(ErrorCode.SYSTEM_ERROR_CODE, "系统未知错误", null);
		} finally {
			UserContext.manulClose();
		}
		return apiResult;
	}

	private Map<String,Set<String>> getSalesAttrsFromProduct(List<TotalTemplateInfo> totalList){
		Map<String,Set<String>> result = new HashMap<>();
		if(CollectionUtils.isEmpty(totalList)){
			return result;
		}
		//有标准销售属性的SPU个数
		int haveSaleAttrSpuCount = 0;
		for(TotalTemplateInfo info : totalList){
			try {
				if (info == null || StringUtils.isBlank(info.getId())) {
					continue;
				}
				ProductInfo productInfo = productDetailCache.getProductInfo(Long.valueOf(info.getId()));
				if (productInfo == null || CollectionUtils.isEmpty(productInfo.getSpuStdSaleAttrs())) {
					continue;
				}

				haveSaleAttrSpuCount++;
				//格式如下：{
				//		"color":["红色","黑色"],
				//		"size":["29","30"]
				//		}
				Map<String, Set<String>> spuStdSaleAttrs = productInfo.getSpuStdSaleAttrs();
				for (Map.Entry<String, Set<String>> entry : spuStdSaleAttrs.entrySet()) {
					if (!result.containsKey(entry.getKey())) {
						Set<String> set = new HashSet<>();
						result.put(entry.getKey(), set);
					}
					result.get(entry.getKey()).addAll(entry.getValue());
				}
			}catch (Exception e){
				log.error("[严重异常]聚合每个商品上的标准销售属性筛选项时出现异常，", e);
			}
		}
		return haveSaleAttrSpuCount<=10 ? new HashMap<>() : result;
	}

	@BProfiler(key = "com.biyao.moses.controller.RecommendUiController.swiperPictureRecommend", monitorType = {
			MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR })
	@ApiOperation(value = "获取轮播图数据")
	@PostMapping("/swiperPicture")
	public ApiResult<Page<TemplateInfo>> swiperPictureRecommend(@ApiParam UIBaseRequest uibaseRequest) {

		ApiResult<Page<TemplateInfo>> recommend = this.recommend(uibaseRequest);
		/*try {
			//获取轮播图模版数据
			List<TemplateInfo> pictureList = recommend.getData().getBlockList().get(0).getBlock().get(0).getData();
			//打散
			Collections.shuffle(pictureList);
		} catch (Exception e) {
			log.error("随机分配轮播图出现错误", e);
		}*/
		return recommend;
	}

	private void printMosesUiLog(ByUser user, UIBaseRequest uibaseRequest, String blockId,
			String pageIndex, String realPageId) {
		StringBuffer logStr = new StringBuffer();
		logStr.append("lt=moses_ui\t");
		logStr.append("lv=1.0\t");
		String pf = StringUtils.isNotBlank(user.getPlatform().getName()) ? user.getPlatform().getName() : "";
		logStr.append("pf=" + pf + "\t");
		String uu = StringUtils.isNotBlank(user.getUuid()) ? user.getUuid() : "";
		logStr.append("uu=" + uu + "\t");

		String pageId = StringUtils.isNotBlank(uibaseRequest.getPageId()) ? uibaseRequest.getPageId() : "";
		logStr.append("pageId=" + pageId + "\t");

		String realpageId = StringUtils.isNotBlank(realPageId) ? realPageId : "";
		logStr.append("realPid=" + realpageId + "\t");

		logStr.append("pageIndex=" + pageIndex + "\t");

		blockId = StringUtils.isNotBlank(blockId) ? blockId : "";
		logStr.append("blockId=" + blockId + "\t");

		String topicId = StringUtils.isNotBlank(uibaseRequest.getTopicId()) ? uibaseRequest.getTopicId() : "";
		logStr.append("topicId=" + topicId + "\t");

		String frontendCategoryId = StringUtils.isNotBlank(uibaseRequest.getFrontendCategoryId()) ? uibaseRequest.getFrontendCategoryId() : "";
		logStr.append("fcid=").append(frontendCategoryId).append("\t");

		String upcUserType = user.getUpcUserType() == null ? "" : user.getUpcUserType().toString();
		logStr.append("upcutype=").append(upcUserType).append("\t");

		String uid = StringUtils.isNotBlank(user.getUid()) ? user.getUid() : "";
		logStr.append("u=" + uid + "\t");
		String avn = StringUtils.isNotBlank(user.getAvn()) ? user.getAvn() : "";
		logStr.append("avn=" + avn + "\t");
		String pvid = StringUtils.isNotBlank(user.getPvid()) ? user.getPvid() : "";
		logStr.append("pvid=" + pvid + "\t");
		String did = StringUtils.isNotBlank(user.getDid()) ? user.getDid() : "";
		logStr.append("did=" + did + "\t");
		String device = StringUtils.isNotBlank(user.getDevice()) ? user.getDevice() : "";
		logStr.append("d=" + device + "\t");
		logStr.append("uniqid=" + uibaseRequest.getSid() + "\t");

		logStr.append("catIds=" + uibaseRequest.getCategoryIds() + "\t");

		logStr.append("trace=" + JSONObject.toJSONString(user.getTrackMap()) + "\t");

		logStr.append("rankTrace=" + JSONObject.toJSONString(user.getRankTrackMap()) + "\t");

		String ctp = StringUtils.isNotBlank(user.getCtp()) ? user.getCtp() : "";
		logStr.append("ctp=" + ctp + "\t");
		String stp = StringUtils.isNotBlank(user.getStp()) ? user.getStp() : "";
		logStr.append("stp=" + stp + "\t");

		String logString = logStr.toString();

		mosesuiDcLogger.printDCLog(logString);
	}

	/**
	 * 根据渠道号和来源查询页面id和topicId
	 * 
	 * @param request
	 * @return
	 */
	@BProfiler(key = "com.biyao.moses.controller.RecommendUiController.getPageId", monitorType = { MonitorType.TP,
			MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR })
	@ApiOperation(value = "获取页面pageId信息")
	@PostMapping("/getPageId")
	public ApiResult<RecommendPage> getPageId(@ApiParam RecommendPageRequest request) {
		log.info("[页面信息请求参数]-[userInfo={},request={}]", JSONObject.toJSONString(UserContext.getUser()),
				JSONObject.toJSONString(request));
		ApiResult<RecommendPage> apiResult = new ApiResult<RecommendPage>();
		RecommendPage rp = new RecommendPage();
		try {
			rp.setPid("");
			rp.setTopicId("");
			String source = request.getSource();
			ByUser user = UserContext.getUser();
			String pf = user.getPf();
			String avn = user.getAvn();
			if (StringUtils.isEmpty(pf) || StringUtils.isEmpty(avn) || StringUtils.isEmpty(source)) {
				rp.setOnoff(false);
				apiResult.setData(rp);
				apiResult.setError("params error");
				apiResult.setSuccess(ErrorCode.PARAM_ERROR_CODE);
				return apiResult;
			} else {
				apiResult = homePageCacheService.getPageIdCache(source, avn, pf);
				if (apiResult != null && apiResult.getSuccess().equals(ErrorCode.SUCCESS_CODE)) {
					if (SourceEnum.FLBT.getSource().equals(source)) {
						String pid = apiResult.getData().getPid();
						String topicId = apiResult.getData().getTopicId();
						return getLbtInfo(pid, topicId);
					}
					return apiResult;
				}
				// 查询版本是否配置，未配置返回false
				// 版本控制 一级key：mosessou:source 二级key：IOS ,ANDROID
				// value：minAvn-maxAvn:version,minAvn-maxAvn:version
				String key = RedisKeyConstant.MOSES_SOURCE_PREFIX + source.trim().toUpperCase();
				String field = pf.toUpperCase();
				//实验配置迁移后的 临时方案，先从match集群中查询，再从原集群查询
				String resultStr = matchRedisUtil.hgetStr(key,field);
				if(StringUtils.isBlank(resultStr)){
					resultStr = redisUtil.hgetStr(key,field);
					if(StringUtils.isNotBlank(resultStr)){
						log.error("[严重异常]redis key {} 未迁移到match集群", key);
					}
				}

				if (!StringUtils.isEmpty(resultStr)) {
					String[] ver = resultStr.split(",");
					for (String avnStr : ver) {
						if (StringUtils.isEmpty(avnStr)) {
							rp.setOnoff(false);
							apiResult.setData(rp);
							apiResult.setSuccess(ErrorCode.SYSTEM_ERROR_CODE);
						} else {
							apiResult = queryPageAndTopicId(rp, avn, avnStr);
						}
					}

				} else {
					rp.setOnoff(false);
					apiResult.setData(rp);
					apiResult.setSuccess(ErrorCode.SYSTEM_ERROR_CODE);
				}
			}
		} catch (Exception e) {
			log.error("[严重异常]查询pageId页面错误!", e);
			rp.setOnoff(false);
			apiResult.setData(rp);
			apiResult.setError("无法获取对应配置!");
			apiResult.setSuccess(ErrorCode.SYSTEM_ERROR_CODE);
		}
		return apiResult;
	}

	private ApiResult<RecommendPage> queryPageAndTopicId(RecommendPage rp, String avn, String avnStr) {
		ApiResult<RecommendPage> apiResult = new ApiResult<RecommendPage>();
		try {
			String[] mv = avnStr.split("\\|");
			String[] minMax = mv[0].split("-");
			String version = mv[1];
			String minAvn = minMax[0];
			String maxAvn = minMax[1];
			if (Double.valueOf(minAvn) <= Double.valueOf(avn) && Double.valueOf(avn) <= Double.valueOf(maxAvn)) {
				// 查询开关和pageId，topicId
				//实验配置迁移后的 临时方案，先从match集群中查询，再从原集群查询
				String resultPage = matchRedisUtil.hgetStr(RedisKeyConstant.MOSES_SWITCH_KEY, version);
				if(StringUtils.isBlank(resultPage)){
					resultPage = redisUtil.hgetStr(RedisKeyConstant.MOSES_SWITCH_KEY, version);
					if(StringUtils.isNotBlank(resultPage)) {
						log.error("[严重异常]redis key {} 未迁移到match集群， field {}", RedisKeyConstant.MOSES_SWITCH_KEY, version);
					}
				}
				String[] switchOnOff = resultPage.split("\\|");
				String onoff = switchOnOff[0];
				String[] pageTopic = switchOnOff[1].split(",");
				if (pageTopic.length > 0 && pageTopic.length <= 1) {
					rp.setPid(pageTopic[0]);
					rp.setTopicId("");
				} else if (pageTopic.length > 0 && pageTopic.length > 1) {
					rp.setPid(pageTopic[0]);
					rp.setTopicId(pageTopic[1]);
				}
				if (!Boolean.parseBoolean(onoff)) {
					rp.setPid("");
					rp.setTopicId("");
				}
				rp.setOnoff(Boolean.parseBoolean(onoff));

			}
			apiResult.setData(rp);
		} catch (Exception e) {
			log.error("[严重异常]查询页面开关和页面id错误!", e);
			rp.setOnoff(false);
			apiResult.setData(rp);
			apiResult.setError("ErrorCode.SYSTEM_ERROR_CODE");
			apiResult.setSuccess(ErrorCode.SYSTEM_ERROR_CODE);
		}
		return apiResult;
	}

	/**
	 * 获取首页前台类目
	 * 
	 * @return
	 */
	@BProfiler(key = "com.biyao.moses.controller.RecommendUiController.getHomeFrontendCategory", monitorType = {
			MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR })
	@ApiOperation(value = "获取前台类目")
	@PostMapping("/homefrontendcategory")
	public ApiResult<List<FrontendCategory>> getHomeFrontendCategory(@ApiParam UIBaseRequest uibaseRequest) {

		ByUser user = UserContext.getUser();
		List<FrontendCategory> frontendCategoryList = new ArrayList<>();
		ApiResult<List<FrontendCategory>> result = new ApiResult<List<FrontendCategory>>();
		try {
			log.info("[获取首页类目信息]-[uuid={}，siteId={}，showCustom={}]", user.getUuid(),user.getSiteId(), uibaseRequest.getShowCustomCate());
			String siteId = user.getSiteId();
			frontendCategoryList = cmsFrontendCategoryCache.getHomeCategoryList(siteId);
			frontendCategoryList = categorySortService.filterCustomCate(frontendCategoryList, uibaseRequest.getShowCustomCate());
			frontendCategoryList = categoryPicRefreshSevice.refreshCategoryPic(frontendCategoryList, user);
			//首页一级类目排序
			categorySortService.sortCategory(frontendCategoryList, user);
			//首页三级类目排序
			if (frontendCategoryList != null) {
				for (FrontendCategory item : frontendCategoryList) {
					if (item.getThirdCategoryDtoList() != null) {
						categorySortService.sortCategory(item.getThirdCategoryDtoList(), user);
					}
				}
			}
		}catch (Exception e){
			log.error("[严重异常]获取前台类目异常-[userInfo={},request={}]", JSONObject.toJSONString(user),
					JSONObject.toJSONString(uibaseRequest), e);
			result.setSuccess(ErrorCode.SYSTEM_ERROR_CODE);
			result.setError("获取前台类目异常");
		}
		result.setData(frontendCategoryList);
		return result;
	}

	/**
	 * 获取活动页前台类目
	 * @return
	 */
	@BProfiler(key = "com.biyao.moses.controller.RecommendUiController.getActFrontendCategory", monitorType = {
			MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR })
	@ApiOperation(value = "获取活动页前台类目")
	@PostMapping("/actfrontendcategory")
	public ApiResult<List<FrontendCategoryForAct>> getActFrontendCategory(@ApiParam UIBaseRequest uibaseRequest) {
		ByUser user = UserContext.getUser();
		List<FrontendCategoryForAct> frontendCategoryList = new ArrayList<>();
		ApiResult<List<FrontendCategoryForAct>> result = new ApiResult<List<FrontendCategoryForAct>>();
		if(Objects.isNull(user)){ return result; }
		try {
			// 查询
			frontendCategoryList = cmsFrontendCategoryCache.getActCategoryList(user.getSiteId(), uibaseRequest.getPagePositionId());
			if(CollectionUtils.isEmpty(frontendCategoryList)){
				log.error("[严重异常]获取类目信息为空，[userInfo={},request={}]", JSONObject.toJSONString(user),
						JSONObject.toJSONString(uibaseRequest));
				return result;
			}
			// 排序
			categorySortService.sortCategoryForAct(frontendCategoryList, user);
		}catch (Exception e){
			log.error("[严重异常]获取活动分类页类目异常-[userInfo={},request={}]", JSONObject.toJSONString(user),
					JSONObject.toJSONString(uibaseRequest), e);
			result.setSuccess(ErrorCode.SYSTEM_ERROR_CODE);
			result.setError("获取活动分类页类目异常");
		}
		result.setData(frontendCategoryList);
		return result;
	}

	/**
	 * 获取分类页前台类目
	 * 
	 * @return
	 */
	@BProfiler(key = "com.biyao.moses.controller.RecommendUiController.getClassifyFrontendCategory", monitorType = {
			MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR })
	@ApiOperation(value = "获取分类页类目")
	@PostMapping("/allfrontendcategory")
	public ApiResult<List<FrontendCategory>> getClassifyFrontendCategory(@ApiParam UIBaseRequest uibaseRequest) {
		ByUser user = UserContext.getUser();
		List<FrontendCategory> frontendCategoryList = new ArrayList<>();
		ApiResult<List<FrontendCategory>> result = new ApiResult<List<FrontendCategory>>();
		try {
			log.info("[获取分类页类目信息]-[uuid={}，siteId={}，showCustom={}]", user.getUuid(),user.getSiteId(),uibaseRequest.getShowCustomCate());
			String siteId = user.getSiteId();
			frontendCategoryList = cmsFrontendCategoryCache.getAllCategoryList(siteId);
			frontendCategoryList = categorySortService.filterCustomCate(frontendCategoryList, uibaseRequest.getShowCustomCate());
			frontendCategoryList = categoryPicRefreshSevice.refreshCategoryPic(frontendCategoryList, user);
			//分类页一级类目排序
			categorySortService.sortCategory(frontendCategoryList, user);
			if (frontendCategoryList != null) {
				for (FrontendCategory firstCats : frontendCategoryList) {
					if (firstCats.getSubCategoryList() != null) {
						//分类页二级类目排序
						categorySortService.sortCategory(firstCats.getSubCategoryList(), user);
						for (FrontendCategory secondCates : firstCats.getSubCategoryList()) {
							if (secondCates.getSubCategoryList() != null) {
								//分类页三级类目排序
								categorySortService.sortCategory(secondCates.getSubCategoryList(), user);
							}
						}
					}
				}
			}
		}catch (Exception e){
			log.error("[严重异常]获取分类页类目异常-[userInfo={},request={}]", JSONObject.toJSONString(user),
					JSONObject.toJSONString(uibaseRequest), e);
			result.setSuccess(ErrorCode.SYSTEM_ERROR_CODE);
			result.setError("获取分类页类目异常");
		}
		result.setData(frontendCategoryList);

		return result;
	}

	private ApiResult<RecommendPage> getLbtInfo(String pid, String topicid) {
		ApiResult<RecommendPage> apiResult = new ApiResult<RecommendPage>();
		RecommendPage rp = new RecommendPage();
		try {
			//初始化请求参数
			ByUser user = homePageCacheImpl.initUser(UPCUserTypeConstants.CUSTOMER);
			UserContext userContext = new UserContext(user);
			UIBaseRequest uibaseRequest = new UIBaseRequest();
			uibaseRequest.setPageId(pid);
			uibaseRequest.setTopicId(StringUtils.isEmpty(topicid) ? "" : topicid);
			uibaseRequest.setPageIndex("1");
			//查询推荐接口
			ApiResult<Page<TemplateInfo>> recommend = recommend(uibaseRequest);
			if (recommend != null && ErrorCode.SUCCESS_CODE.equals(recommend.getSuccess())) {
				//返回首页第一张轮播图
				Page<TemplateInfo> data = recommend.getData();
				List<Block<TemplateInfo>> blockList = data.getBlockList();
				Map<String, String> routerParams = blockList.get(0).getBlock().get(0).getData().get(0)
						.getRouterParams();
				String resultTopicId = routerParams.get("topicId");
				String resultPageId = routerParams.get("pageId");
				rp.setTopicId(resultTopicId);
				rp.setPid(resultPageId);
				rp.setOnoff(true);
			}
		} catch (Exception e) {
			log.error("[严重异常]获取首页第一张轮播图接口异常!", e);
			rp.setTopicId(null);
			rp.setPid(null);
			rp.setOnoff(false);
		}
		apiResult.setData(rp);
		return apiResult;
	}

}