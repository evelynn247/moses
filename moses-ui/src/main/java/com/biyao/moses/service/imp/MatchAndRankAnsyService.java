package com.biyao.moses.service.imp;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.biyao.moses.cache.*;
import com.biyao.moses.cache.drools.RuleConfigCache;
import com.biyao.moses.common.constant.*;
import com.biyao.moses.common.enums.SortTypeEnum;
import com.biyao.moses.common.enums.SourceSpmEnum;
import com.biyao.moses.common.enums.TemplateTypeEnum;
import com.biyao.moses.config.drools.KiaSessionConfig;
import com.biyao.moses.constants.CommonConstants;
import com.biyao.moses.context.ByUser;
import com.biyao.moses.context.FacetManager;
import com.biyao.moses.drools.DroolsCommonUtil;
import com.biyao.moses.drools.DroolsService;
import com.biyao.moses.dto.trace.TraceDetail;
import com.biyao.moses.enums.MosesRuleEnum;
import com.biyao.moses.exception.SideSlipTemplateException;
import com.biyao.moses.exp.ExpirementSpace;
import com.biyao.moses.exp.MosesExpConst;
import com.biyao.moses.exp.UIExperimentSpace;
import com.biyao.moses.model.BaseProductTemplateInfo;
import com.biyao.moses.model.TemplateNewuserTemplateInfo;
import com.biyao.moses.model.TemplateSingleNewuserTemplateInfo;
import com.biyao.moses.model.adapter.TemplateAdapterContext;
import com.biyao.moses.model.drools.BuildBaseFactParam;
import com.biyao.moses.model.drools.RuleBaseFact;
import com.biyao.moses.model.drools.RuleFact;
import com.biyao.moses.model.exp.ExpRequest;
import com.biyao.moses.model.exp.Expirement;
import com.biyao.moses.model.match.ProductImage;
import com.biyao.moses.model.match2.MatchItem2;
import com.biyao.moses.model.rank2.RankItem2;
import com.biyao.moses.model.template.Block;
import com.biyao.moses.model.template.Page;
import com.biyao.moses.model.template.Template;
import com.biyao.moses.model.template.TemplateInfo;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.params.*;
import com.biyao.moses.params.match.MatchRequest;
import com.biyao.moses.params.match.MatchResponse;
import com.biyao.moses.params.match2.MatchRequest2;
import com.biyao.moses.params.match2.MatchResponse2;
import com.biyao.moses.params.matchOnline.MatchOnlineRequest;
import com.biyao.moses.params.rank.RankResponse;
import com.biyao.moses.params.rank.RecommendRankRequest;
import com.biyao.moses.params.rank2.RankRequest2;
import com.biyao.moses.params.rank2.RankResponse2;
import com.biyao.moses.rpc.UcRpcService;
import com.biyao.moses.rules.Rule;
import com.biyao.moses.rules.RuleConst;
import com.biyao.moses.rules.RuleContext;
import com.biyao.moses.service.MatchAndRankService2;
import com.biyao.moses.service.NoFirstPageResult;
import com.biyao.moses.service.SwiperPictureService;
import com.biyao.moses.util.*;
import com.uc.domain.bean.User;
import com.uc.domain.constant.UserFieldConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.text.DecimalFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Slf4j
@Service
public class MatchAndRankAnsyService {
	
	@Autowired
	TemplateAdapterContext templateAdapterContext;

	@Autowired
	HttpMosesMatchServiceImpl mosesMatchService;
	
	@Autowired
	HttpMosesRankServiceImpl mosesRankService;	

	@Autowired
	CacheRedisUtil cacheRedisUtil;

	@Autowired
	FilterUtil filterUtil;
	
	@Autowired
	ExpirementSpace expirementSpace;

	@Autowired
	UIExperimentSpace uiExperimentSpace;

	@Autowired
	MatchAndRankService2 matchAndRankService2;

	@Autowired
	UcRpcService ucRpcService;

	@Autowired
	CMSFrontendCategoryCache cmsFrontendCategoryCache;

	@Autowired
	PartitionUtil partitionUtil;

	@Autowired
	private RedisCache redisCache;

	@Autowired
	private AdvertInfoService advertInfoService;

	@Value("${redis.pagecache.expire}")
	private int pageCacheTime;

	@Value("${home.swiperPicture.pageId}")
	private String homeSwiperPicturePageId;

	@Value("${home.feed.pageId}")
	private String homeFeedPageId;

	@Autowired
	private ProductDetailCache productDetailCache;

	@Autowired
	private FacetManager facetManager;

	@Autowired
	private MatchAndRank2AnsyService matchAndRank2AnsyService;

	@Autowired
	private SwiperPictureService swiperPictureService;

	@Autowired
	SwitchConfigCache switchConfigCache;

	@Autowired
	RuleConfigCache ruleConfigCache;

	@Autowired
	KiaSessionConfig kiaSessionConfig;
	@Autowired
	DroolsService droolsService;
	@Autowired
	RecommendManualSourceConfigCache recommendManualSourceConfigCache;
	@Autowired
	DroolsCommonUtil droolsCommonUtil;

	private final String DATA = "data";
	private final String MATCH_TRACE = "matchTrace";
	private final String RANK_TRACE = "rankTrace";
	private final String NOCACHE = "nocache";
	//错误的ctp点位
	private final String ERRORCTP = "000000";
	protected DecimalFormat df = new DecimalFormat("#.##");

	//新首页feeds流曝光缓冲数
	private final int NEW_HOME_FEED_CACHE_NUM = 4;

	//新首页feeds流曝光缓冲数
	private final int NEW_HOME_FEED_EXP_NUM = 999;

	/**
	 * 第一页假曝光的后16个缓存 过期时间为1天
	 */
	private final int EXPIRE_TIME_1DAY = 86400; // 3600 * 24

	/**
	 * 假曝光库的过期时间为3天
	 */
	private final int EXPIRE_TIME_3DAY = 259200; // 3600 * 24 * 3

	/**
	 * 类目页单独的假曝光商品条数上限
	 */
	private final int CATEGORY_FAKE_EXP_NUM_UP_LIMIT = 199;


	@Async
	public Future<Block<TemplateInfo>> matchAndRank(Block<TotalTemplateInfo> block,Integer feedIndex, UIBaseRequest uibaseRequest,ByUser user,int blockIndex){

		Block<TemplateInfo> buildTemplateBlock = null;
		try {
			List<TraceDetail> matchTraceDetailList = new ArrayList<TraceDetail>();
			List<TraceDetail> rankTraceDetailList = new ArrayList<TraceDetail>();
			Map<String, List<TotalTemplateInfo>> expData = new HashMap<>();
			if (block.isDynamic()) { // 是动态数据,走match获取数据
				// 匹配match
				long start = System.currentTimeMillis();

				if(StringUtils.isEmpty(homeSwiperPicturePageId)){
					homeSwiperPicturePageId = CommonConstants.HOME_SWIPER_PICTURE_PAGEID;
				}
				//如果是首页轮播图，则解析配置的轮播图信息
				List<TotalTemplateInfo> swiperPicConfInfo = null;
				if(homeSwiperPicturePageId.equals(uibaseRequest.getPageId())){
					swiperPicConfInfo = SwiperPictureService.parseSwiperPicConf(uibaseRequest.getSwiperPicConfInfo());
				}
				boolean isFull = SwiperPictureService.isFull(swiperPicConfInfo);
				if(!isFull) {
					//新版非feeds流match rank
					dealNoFeedForNewExp(uibaseRequest, user, expData, matchTraceDetailList);
					//如果没有走新实验系统，则还是走老实验系统
					if (!user.isNewExp()) {
						MatchRequest mr = buildMatchRequest(block, feedIndex, uibaseRequest, user);
						ApiResult<MatchResponse> match = mosesMatchService.match(mr, user);
						expData = match.getData().getResultMap();
						matchTraceDetailList = match.getData().getTraceDetail();
						long end = System.currentTimeMillis();
						ApiResult<RankResponse> rankResult = mosesRankService.rank(RecommendRankRequest.builder().uid(user.getUid()).
								uuid(user.getUuid()).sessionId(user.getSessionId()).
								matchData(expData).frontendCategoryId(uibaseRequest.getFrontendCategoryId()).categoryIds(uibaseRequest.getCategoryIds()).build());
						if (rankResult != null && rankResult.getSuccess() != null && rankResult.getSuccess() == ErrorCode.SUCCESS_CODE) {
							expData = rankResult.getData().getRankResult();
						}
						rankTraceDetailList = rankResult.getData().getTraceDetails();
					}
					expData = SwiperPictureService.composeSwiperPicData(swiperPicConfInfo, expData, matchTraceDetailList, user);
				}else{
					expData = SwiperPictureService.composeSwiperPicData(swiperPicConfInfo, expData, matchTraceDetailList, user);
				}
				//使用配置的轮播图替换
				replaceSliderPicAddr(uibaseRequest, expData);
				//处理首页轮播图曝光
				if(homeSwiperPicturePageId.equals(uibaseRequest.getPageId())){
					swiperPictureService.dealSwiperPicExposure(expData, user);
				}
				//循环处理埋点日志信息
				handelAid(matchTraceDetailList, rankTraceDetailList, user);

				long rankend = System.currentTimeMillis();
//				log.info("[推荐]-[模板异步rank]-[pageId={},topicId={},耗时={}]", uibaseRequest.getPageId(),uibaseRequest.getTopicId(), rankend-end);
			} else {
				expData = new HashMap<String, List<TotalTemplateInfo>>();
			}

			// 处理埋点数据，match和rank统一aid


			// 处理当前block下的所有模板
			long uistart = System.currentTimeMillis();
			Map<String, String> stp = buildStp(uibaseRequest, block.getBid(),user,blockIndex,feedIndex);
			buildTemplateBlock = templateAdapterContext.buildTemplateBlock(block, expData, stp, uibaseRequest.getTopicId(),user);
			long uiend = System.currentTimeMillis();

//			log.info("[推荐]-[模板异步ui]-[pageId={},topicId={},耗时={}]", uibaseRequest.getPageId(),uibaseRequest.getTopicId(), uiend-uistart);


		} catch (Exception e) {
			if (e instanceof SideSlipTemplateException) {
				log.error("[严重异常][block填充失败]-[pageId={},bid={},message={}]", uibaseRequest.getPageId(), block.getBid(), e.getMessage());
			}else{
				log.error("[严重异常][block填充失败]-[pageId={},bid={}]", uibaseRequest.getPageId(), block.getBid(), e);
			}
		}
		return new AsyncResult<Block<TemplateInfo>>(buildTemplateBlock);
	}

	/**
	 * 使用置顶的轮播图替换
	 * @param uibaseRequest
	 */
	private void replaceSliderPicAddr(UIBaseRequest uibaseRequest, Map<String, List<TotalTemplateInfo>> expData){
		if(StringUtils.isEmpty(homeSwiperPicturePageId)){
			homeSwiperPicturePageId = CommonConstants.HOME_SWIPER_PICTURE_PAGEID;
		}
		//判断是否是轮播图
		if (!homeSwiperPicturePageId.equals(uibaseRequest.getPageId())) {
			return;
		}

		if(expData == null || expData.size() <= 0){
			return;
		}

		List<ProductImage> topSliderPicList = redisCache.getTopSliderPicList();
		if(CollectionUtils.isEmpty(topSliderPicList)){
			return;
		}
		//expData中只有1个元素
		for(List<TotalTemplateInfo> totalTemplateInfoList : expData.values()){
			if(CollectionUtils.isEmpty(totalTemplateInfoList)){
				continue;
			}
			int replacePicNum = Math.min(topSliderPicList.size(), totalTemplateInfoList.size());

			for(int i = 0; i < replacePicNum; i++){
				TotalTemplateInfo totalTemplateInfo = totalTemplateInfoList.get(i);
				ProductImage productImage = topSliderPicList.get(i);
				//替换轮播图
				totalTemplateInfo.setImage(productImage.getImage());
				totalTemplateInfo.setImageWebp(productImage.getWebpImage());
				List<String> list = new ArrayList<>();
				List<String> listWebp = new ArrayList<>();
				list.add(productImage.getImage());
				listWebp.add(productImage.getWebpImage());
				totalTemplateInfo.setLongImages(list);
				totalTemplateInfo.setLongImagesWebp(listWebp);
				totalTemplateInfo.setImages(list);
				totalTemplateInfo.setImagesWebp(listWebp);
			}
			break;
		}
	}
	/**
	 * 针对非feeds流，判断是否走新实验系统，如果是则走新实验系统，如果不是则不处理。
	 * @param uibaseRequest
	 * @param user
	 * @param feedexpData
	 * @param matchTraceDetailList
	 */
	private void dealNoFeedForNewExp(UIBaseRequest uibaseRequest,ByUser user,Map<String, List<TotalTemplateInfo>> feedexpData, List<TraceDetail> matchTraceDetailList){
		if(StringUtils.isEmpty(homeSwiperPicturePageId)){
			homeSwiperPicturePageId = CommonConstants.HOME_SWIPER_PICTURE_PAGEID;
		}
		//判断是否是轮播图，走新实验
		if (homeSwiperPicturePageId.equals(uibaseRequest.getPageId())){
			BaseRequest2 baseRequest2 = constructBaseRequest2(user, uibaseRequest);
			String bizName = BizNameConst.SLIDER_PICTURE3;
			String expId = MosesExpConst.SLIDER_EXP;

			//如果个性化推荐开关设置为关闭，则走轮播图热销规则召回
			if(!user.isPersonalizedRecommendSwitch()){
				bizName = BizNameConst.SLIDER_PICTURE_HOTS;
			}

			MatchResponse2 matchResponse2 = newExpMatch(bizName, uibaseRequest.getSid(), baseRequest2, user);
			List<MatchItem2> matchResult2 = matchResponse2.getMatchItemList();
			//如果结果为空，走旧轮播图落match
			if (CollectionUtils.isEmpty(matchResult2) || matchResult2.size() < 6) {
				log.error("[一般异常][轮播图]新轮播图数量不足6个，sid {}，uuid {}，baseRequest2 {}", uibaseRequest.getSid(), user.getUuid(), JSON.toJSONString(baseRequest2));
				return;
			}
			String rankName = RankNameConstants.BASE_REDUCE_WEIGHT_RANK;
			List<RankItem2> rankResult2 = newExpRank(rankName, uibaseRequest.getSid(), baseRequest2,matchResult2);
			List<TotalTemplateInfo> allProductList = matchAndRankService2.convert2TotalTemplateInfo(rankResult2);
			if(CollectionUtils.isNotEmpty(rankResult2)) {

				List<TotalTemplateInfo> lastResultList = dealRankListByRule(user, allProductList,expId);

				//只有调用新实验系统返回了数据才认为走了新实验系统
				//如果没有返回数据，则还是走实验系统兜底
				user.setNewExp(true);
				//将新实验系统的返回结果转换成老实验系统的返回结果
				List<TotalTemplateInfo> totalTemplateInfoList = matchAndRankService2.convert2TotalTemplate(matchResult2, lastResultList, expId, null);

				//如果个性化推荐开关设置为关闭，则将实验id去除，即设置为空字符串（目的是不出现scm埋点）
				if(!user.isPersonalizedRecommendSwitch()){
					for(TotalTemplateInfo totalTemplateInfo : totalTemplateInfoList){
						if(totalTemplateInfo != null){
							totalTemplateInfo.setExpId("");
						}
					}
				}

				String dataKey = "moses:" + CommonConstants.HOME_SWIPER_PICTURE_TOPICID + CommonConstants.SPLIT_LINE + expId;
				feedexpData.put(dataKey, totalTemplateInfoList);
				//构造matchTrace
				TraceDetail traceDetail = new TraceDetail();
				//该key必须
				traceDetail.setExpId(dataKey);
				Set<String> keys = new HashSet<>();
				keys.add("moses:" + CommonConstants.HOME_SWIPER_PICTURE_TOPICID + CommonConstants.SPLIT_LINE + expId + CommonConstants.SPLIT_LINE + "0000");
				traceDetail.setKeys(keys);
				matchTraceDetailList.add(traceDetail);
			}


		}
	}


	private void handelAid(List<TraceDetail> matchTraceDetailList, List<TraceDetail> rankTraceDetailList, ByUser user){

		ConcurrentMap<String,TraceDetail> trackMap = user.getTrackMap();
		ConcurrentMap<String,TraceDetail> rankTrackMap = user.getRankTrackMap();

		if (matchTraceDetailList != null && matchTraceDetailList.size() > 0) {

			for (TraceDetail traceDetail : matchTraceDetailList) {

				String prefix_expId = traceDetail.getExpId();
				String[] split = prefix_expId.split(CommonConstants.SPLIT_COLON);
				String datasource_expId = split[1];
				String expId = datasource_expId.split(CommonConstants.SPLIT_LINE)[1];
				String topicId = datasource_expId.split(CommonConstants.SPLIT_LINE)[0];

				String aid = IdCalculateUtil.createUniqueId();
				trackMap.put(aid, traceDetail);
				//通过 topicId 查询rank层实验
				ExpRequest expRequest = ExpRequest.builder().expId(expId).uuid(user.getUuid()).tid(topicId).layerName(CommonConstants.LAYER_NAME_RANK).build();
				Expirement exp = expirementSpace.getExpirement(expRequest);
				// 校验rank服务返回的trace中是否真正包含实验
				if (exp!=null ) {
					if (rankTraceDetailList!=null && rankTraceDetailList.size() > 0) {
						for (TraceDetail rankTrace : rankTraceDetailList) {
							if (rankTrace.getExpId().equals(exp.getExpId())) {
								rankTrackMap.put(aid, rankTrace);
							}
						}
					}
				}
			}
		}
	}

	/**
	 * feed流match
	 * @param lastBlock
	 * @param feedIndex
	 * @param uibaseRequest
	 * @param user
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@Async
	public Future<Block<TemplateInfo>> feedMatchAndRank(Block<TotalTemplateInfo> lastBlock,Integer feedIndex, UIBaseRequest uibaseRequest,ByUser user){
		Block<TemplateInfo> buildBlock = null;
		List<TraceDetail> matchTraceDetailList = new ArrayList<TraceDetail>();
		List<TraceDetail> rankTraceDetailList = new ArrayList<TraceDetail>();
		boolean isDebug = user.isDebug();
		try{
			// 初步判断感兴趣商品集活动是否应该被展示  如果可能会被展示 则异步调用match  获取数据
			if(advertInfoService.isShowGXQSPAdvert(uibaseRequest,user)){
				MatchRequest2 matchRequest2 = buildMatchRequestForGxqsp(uibaseRequest, user);
				//异步获取
				try {
					Future<MatchResponse2> asyncMatchResponse = matchAndRankService2.asyncMatch(matchRequest2, user);
					user.setAsyncMatchResponse(asyncMatchResponse);
				}catch (Exception e){
					log.error("[严重异常]异步调用感兴趣商品集match异常，异常信息：",e);
				}
			}

			// feed流不是第一页，且feed block第一个模板是标题,删除掉标题模板
			if (feedIndex > 1 && lastBlock.getBlock().get(0).getTemplateName()
					.equals(TemplateTypeEnum.TITLE_LINE.getTemplateType())) {
				lastBlock.getBlock().remove(0);
			}
			Map<String, List<TotalTemplateInfo>> feedexpData = null;
			if (feedIndex == 1) {
				Map<String, Object> feedInfo = getFeedInfo(lastBlock, feedIndex, uibaseRequest, user);
				feedexpData = (Map<String, List<TotalTemplateInfo>>) feedInfo.get(DATA);
				matchTraceDetailList = (List<TraceDetail>) feedInfo.get(MATCH_TRACE);
				rankTraceDetailList = (List<TraceDetail>) feedInfo.get(RANK_TRACE);
			}else{
				Map<String, String> hgetAll;
				if(StringUtils.isNotBlank(uibaseRequest.getFrontendCategoryId())){
					hgetAll = cacheRedisUtil.hgetAll(CommonConstants.PAGE_CACHE_PREFIX+user.getPvid()+"_"+uibaseRequest.getPageId()+"_"+uibaseRequest.getTopicId()+"_"+uibaseRequest.getFrontendCategoryId());
				}else{
					hgetAll = cacheRedisUtil.hgetAll(CommonConstants.PAGE_CACHE_PREFIX+user.getPvid()+"_"+uibaseRequest.getPageId()+"_"+uibaseRequest.getTopicId());
				}
				if (hgetAll == null) {
					Map<String, Object> feedInfo = getFeedInfo(lastBlock, feedIndex, uibaseRequest, user);
					feedexpData = (Map<String, List<TotalTemplateInfo>>) feedInfo.get(DATA);
					matchTraceDetailList = (List<TraceDetail>) feedInfo.get(MATCH_TRACE);
					rankTraceDetailList = (List<TraceDetail>) feedInfo.get(RANK_TRACE);

				}else{
					long start = System.currentTimeMillis();
					String result = hgetAll.get(DATA);
					String matchTrace = hgetAll.get(MATCH_TRACE);
					String rankTrace = hgetAll.get(RANK_TRACE);
					matchTraceDetailList = JSONObject.parseObject(matchTrace,new TypeReference<List<TraceDetail>>() {});
					rankTraceDetailList = JSONObject.parseObject(rankTrace,new TypeReference<List<TraceDetail>>() {});

					if(StringUtils.isNotBlank(uibaseRequest.getFrontendCategoryId())){
						cacheRedisUtil.expire(CommonConstants.PAGE_CACHE_PREFIX+user.getPvid()+"_"+uibaseRequest.getPageId()+"_"+uibaseRequest.getTopicId()+"_"+uibaseRequest.getFrontendCategoryId(), pageCacheTime);
					}else{
						cacheRedisUtil.expire(CommonConstants.PAGE_CACHE_PREFIX+user.getPvid()+"_"+uibaseRequest.getPageId()+"_"+uibaseRequest.getTopicId(), pageCacheTime);
					}
					feedexpData = JSONObject.parseObject(result,new TypeReference<Map<String, List<TotalTemplateInfo>>>() {});
					//构建规则引擎条件对象
					RuleBaseFact ruleBaseFact = droolsCommonUtil.buildRuleBaseFact(BuildBaseFactParam.builder().
							sceneId(uibaseRequest.getPagePositionId()).
							siteId(user.getSiteId()).
							topicId(uibaseRequest.getTopicId()).
							uid(user.getUid()).uuid(user.getUuid()).utype(user.getUpcUserType().toString()).build());
					// 获取命中的配置规则
					RuleFact ruleFact = ruleConfigCache.getRuleFactByCondition(ruleBaseFact, "layer_mosesui_feed");
					//待插入商品集合
					NoFirstPageResult noFirstPageResult = dealNoFirstPageForNewExp(uibaseRequest, user,ruleFact);
					List<TotalTemplateInfo> waitInsertPidInfoList = noFirstPageResult.getWaitInsertPidInfoList();
					BaseRequest2 baseRequest2 = noFirstPageResult.getBaseRequest2();

					//通过判断是否是翻页实时隔断的规则，如果是，则需要处理
					HashMap<String, String> flags = baseRequest2.getFlags();
					String ruleNameStr = flags.get(MosesExpConst.FLAG_RULE_NAME);
					String realPartitionRuleName = null;
					if (StringUtils.isNotBlank(ruleNameStr) && !MosesExpConst.VALUE_DEFAULT.equals(ruleNameStr)) {
						String[] ruleNameArray = ruleNameStr.trim().split(",");
						List<String> ruleNameList = new ArrayList<>();
						Collections.addAll(ruleNameList, ruleNameArray);
						realPartitionRuleName = MosesRuleEnum.findRealPartitionRule(ruleNameList);
					}
					//如果匹配了规则 且隔断机制个性化类目隔断 则需要走规则引擎中的类目隔断机制
					if(ruleFact != null && "2".equals(ruleFact.getCategoryPartition())){
						List<TotalTemplateInfo> totalTemplateInfoList = new ArrayList<>();
						String key = "";
						for (Entry<String, List<TotalTemplateInfo>> entry : feedexpData.entrySet()) {
							//新实验的数据只有1个key
							key = entry.getKey();
							totalTemplateInfoList = entry.getValue();
							break;
						}
						KieSession kieSession = kiaSessionConfig.kieSession();
						FactHandle insert = kieSession.insert(ruleFact);
						List<TotalTemplateInfo> totalTemplateInfoList2=new ArrayList<>();
						try {
							RuleContext ruleContext = buildRuleContext(totalTemplateInfoList, null, user, uibaseRequest);
							totalTemplateInfoList2 = droolsService.dealCategory(kieSession, ruleContext);
						}catch (Exception e){
							log.error("[严重异常]规则引擎处理实时类目隔断时异常",e);
						}finally {
							kieSession.delete(insert);
							kieSession.dispose();
						}
						if(CollectionUtils.isNotEmpty(totalTemplateInfoList2)){
							feedexpData.put(key, totalTemplateInfoList2);
						}
						//将横叉后的数据重新放到redis中
						storePageCacheToRedis(feedexpData, matchTraceDetailList, rankTraceDetailList, uibaseRequest, user);
					} else if (StringUtils.isNotBlank(realPartitionRuleName) && user.isNewExp() && user.isPersonalizedRecommendSwitch()){
						List<TotalTemplateInfo> totalTemplateInfoList = new ArrayList<>();
						String key = "";
						for (Entry<String, List<TotalTemplateInfo>> entry : feedexpData.entrySet()) {
							//新实验的数据只有1个key
							key = entry.getKey();
							totalTemplateInfoList = entry.getValue();
							break;
						}
						List<String> ruleNameList = new ArrayList<>();
						ruleNameList.add(realPartitionRuleName);
						totalTemplateInfoList = matchAndRankService2.dealRuleByNames(uibaseRequest, user, totalTemplateInfoList, waitInsertPidInfoList, ruleNameList, baseRequest2);
						feedexpData.put(key, totalTemplateInfoList);
						//将横叉后的数据重新放到redis中
						storePageCacheToRedis(feedexpData, matchTraceDetailList, rankTraceDetailList, uibaseRequest, user);
					}

					long end = System.currentTimeMillis();
//					log.info("[推荐]-[feed异步分页缓存]-[pageId={},topicId={},耗时={}]", uibaseRequest.getPageId(),uibaseRequest.getTopicId(), end - start);
				}
			}
			handelAid(matchTraceDetailList, rankTraceDetailList, user);

			// 服务器追踪参数处理
			long uistart = System.currentTimeMillis();
			//对页面缓存信息进行后置处理
			postprocessPageCache(feedexpData);

			Map<String, String> stp = buildStp(uibaseRequest, lastBlock.getBid(),user,0,feedIndex);
			buildBlock = templateAdapterContext.buildFeedBlock(lastBlock, feedexpData, feedIndex, stp,uibaseRequest,user);
			long uiend = System.currentTimeMillis();
//			log.info("[推荐]-[feed异步ui]-[pageId={},topicId={},耗时={}]", uibaseRequest.getPageId(),uibaseRequest.getTopicId(), uiend-uistart);

			//对homefeeds2.0 首页、购物车、个人中心 feeds流进行曝光处理
			//对新手专享新实验进行曝光处理
			try {
				dealHomeFeed2Exposure(feedIndex, user, buildBlock,uibaseRequest.getTopicId(),uibaseRequest.getFrontendCategoryId());
			} catch (Exception e) {
				log.error("[严重异常]homefeeds2.0保存用户曝光信息失败 uuid {}, sid {} ",user.getUuid(), uibaseRequest.getSid(), e);
			}

			//非类目页筛选 并且不是价格或新手专享价格排序
			if(StringUtils.isBlank(uibaseRequest.getSelectedScreenAttrs())
			    && !SortTypeEnum.PRICE.getType().equals(uibaseRequest.getSortType())
				&& !SortTypeEnum.NOVICEPRICERANK.getType().equals(uibaseRequest.getSortType())) {
				//调用sdk选择su 并对结果集进行更换
				replaceSuBySdk(user, buildBlock);
			}

		}catch(Exception e){
			log.error("[严重异常][block的feed流填充失败] uuid {}, sid {} ",user.getUuid(), uibaseRequest.getSid(), e);
		}
		return new AsyncResult<Block<TemplateInfo>>(buildBlock);
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

	/**
	 * 获取待插入的商品集合
	 * @param baseRequest2
	 * @return
	 */
	public List<TotalTemplateInfo> queryWaitInsertPidList(UIBaseRequest uibaseRequest, ByUser user, BaseRequest2 baseRequest2){
		//日志debug开关
		boolean isDebug = user.isDebug();
		if(isDebug) {
			log.error("[DEBUG]异步获取待横插商品数据开始，sid {}， uuid {}", uibaseRequest.getSid(), user.getUuid());
		}
		//异步获取待横叉的商品信息
		List<TotalTemplateInfo> waitInsertPidInfoList = new ArrayList<>();

		//如果个性化推荐设置开关关闭，则横插召回源配置无效，即不从横插召回源中获取数据
		if(!user.isPersonalizedRecommendSwitch()){
			return waitInsertPidInfoList;
		}

		HashMap<String, String> flags = baseRequest2.getFlags();
		String insertSourceAndWeight = flags.getOrDefault(MosesExpConst.FLAG_INSERT_MATCH_SOURCE_WEIGHT, MosesExpConst.VALUE_DEFAULT_INSERT_MATCH_SOURCE_WEIGHT);
		//如果没有配置横插的召回源信息，则直接返回空集合
		if(StringUtils.isBlank(insertSourceAndWeight) || MosesExpConst.VALUE_DEFAULT.equals(insertSourceAndWeight)){
			return waitInsertPidInfoList;
		}

		Future<List<TotalTemplateInfo>> insertFuture = matchAndRank2AnsyService.matchAndRank(uibaseRequest, user, baseRequest2, RankNameConstants.REDUCE_EXPOSURE_WEIGHT, BizNameConst.FEED_INSERT);
		try{
			waitInsertPidInfoList = insertFuture.get(200, TimeUnit.MILLISECONDS);
		}catch (Exception e){
			log.error("[一般异常]获取待横叉商品集合异常，uuid {}, uid {}", baseRequest2.getUuid(), baseRequest2.getUid(), e);
		}
		if(isDebug) {
			log.error("[DEBUG]异步获取待横插商品数据结束，sid {}， uuid {}", uibaseRequest.getSid(), user.getUuid());
		}
		return waitInsertPidInfoList;
	}

	/**
	 * 非第一页处理，返回待横叉的商品集合
	 * @param uibaseRequest
	 * @param user
	 * @return
	 */
	private NoFirstPageResult dealNoFirstPageForNewExp(UIBaseRequest uibaseRequest, ByUser user,RuleFact ruleFact){
		boolean isEntryNewExp = false;
		BaseRequest2 baseRequest2 = new BaseRequest2();
		List<TotalTemplateInfo> waitInsertPidInfoList = new ArrayList<>();
		if(CommonConstants.HOME_FEED_TOPICID.equals(uibaseRequest.getTopicId())
				|| getHomeFeedPageId().equals(uibaseRequest.getPageId())) {
			if(ruleFact != null){
				isEntryNewExp=true;
			}else {
				baseRequest2 = constructBaseRequest2(user, uibaseRequest);
				baseRequest2.setSid(uibaseRequest.getSid());
				uiExperimentSpace.divert(baseRequest2);
				HashMap<String, String> flags = baseRequest2.getFlags();
				String expValue = flags.get(MosesExpConst.FLAG_HOME_FEED);
				if (expValue != null && !MosesExpConst.VALUE_DEFAULT.equals(expValue) && !MosesExpConst.VALUE_HOME_FEED_OLD.equals(expValue)) {
					isEntryNewExp = true;
					//获取待插入的商品集合
					waitInsertPidInfoList = queryWaitInsertPidList(uibaseRequest, user, baseRequest2);
				}
			}
			if(!user.isPersonalizedRecommendSwitch()){
				isEntryNewExp = true;
			}
		}else if(CommonConstants.XSZXY_FEED_TOPICID.equals(uibaseRequest.getTopicId())){
			baseRequest2 = constructBaseRequest2(user, uibaseRequest);
			baseRequest2.setSid(uibaseRequest.getSid());
			uiExperimentSpace.divert(baseRequest2);
			String expValue = baseRequest2.getFlags().get(MosesExpConst.FLAG_NEW_USER_FEED);
			if (expValue != null && !MosesExpConst.VALUE_NEW_USER_FEED_OLD.equals(expValue)
				&& !MosesExpConst.VALUE_DEFAULT.equals(expValue)) {
				isEntryNewExp = true;

				//获取待插入的商品集合
				waitInsertPidInfoList = queryWaitInsertPidList(uibaseRequest, user, baseRequest2);
			}

			if(!user.isPersonalizedRecommendSwitch()){
				isEntryNewExp = true;
			}
		}else if(CommonConstants.SLIDER_MIDDLE_PAGE_TOPICID.equals(uibaseRequest.getTopicId())){
			isEntryNewExp = true;
		}else if(CommonConstants.M2F1_PAGE_TOPICID.equals(uibaseRequest.getTopicId())){
			isEntryNewExp = true;
		}else if (CommonConstants.GXQSP_PAGE_TOPICID.equals(uibaseRequest.getTopicId())){
			isEntryNewExp = true;
		}

		if(isEntryNewExp){
			user.setNewExp(true);
		}
		NoFirstPageResult result = new NoFirstPageResult();
		result.setBaseRequest2(baseRequest2);
		result.setWaitInsertPidInfoList(waitInsertPidInfoList);
		return result;
	}

	/**
	 * 对homefeeds2.0 首页、购物车、个人中心、类目页feeds流进行曝光处理
	 * @param feedIndex
	 * @param user
	 * @param buildBlock
	 */
	private void dealHomeFeed2Exposure(Integer feedIndex, ByUser user, Block<TemplateInfo> buildBlock,String topicId,String frontendCategoryId) {
		//为推荐效果测试接口访问
		if(user!=null&&user.isTest()){
			return;
		}
		//如果没有进入新实验 、不为类目页请求
		if (!user.isNewExp() && !CommonConstants.CATEGORY_MIDDLE_PAGE_TOPICID.equals(topicId)) {
			return;
		}

		//数据为空
		List<Template<TemplateInfo>> block = buildBlock.getBlock();
		if (CollectionUtils.isEmpty(block)) {
			return;
		}
		//转换结果块为BaseProductTemplateInfo list
		List<BaseProductTemplateInfo> baseProductTemplateInfoList = convert2BaseProductInfoList(block);
		if(CollectionUtils.isEmpty(baseProductTemplateInfoList)){
			return;
		}
		long ms = System.currentTimeMillis();
		if (feedIndex.equals(1)) {
			//类目页第一页
			if (CommonConstants.CATEGORY_MIDDLE_PAGE_TOPICID.equals(topicId)) {
				List<String> resultList = new ArrayList<>();
				for (BaseProductTemplateInfo info : baseProductTemplateInfoList) {
					if(CommonConstants.INVALID_PRODUCT_ID.equals(info.getId())){
						continue;
					}
					resultList.add(info.getId() + ":" + ms);
				}
				String[] strs = resultList.toArray(new String[0]);
				//清空缓冲
				cacheRedisUtil.ltrim(CacheRedisKeyConstant.NEW_CATEGORY_FEED_EXPOSURE_CACHE_PREFIX +
						frontendCategoryId + "_" +user.getUuid(), 1, 0);
				//把类目页第一页商品加入曝光
				cacheRedisUtil.lpush(CacheRedisKeyConstant.NEW_CATEGORY_FEED_EXPOSURE_CACHE_PREFIX +
						frontendCategoryId + "_" + user.getUuid(), strs);
				cacheRedisUtil.expire(CacheRedisKeyConstant.NEW_CATEGORY_FEED_EXPOSURE_CACHE_PREFIX +
						frontendCategoryId + "_" + user.getUuid(), EXPIRE_TIME_1DAY);
				dealCategoryFakeExposure(feedIndex, user, frontendCategoryId, baseProductTemplateInfoList);
			} else {
				List<String> beforeList = new ArrayList<>();
				List<String> afterList = new ArrayList<>();
				for(int i = 0; i < baseProductTemplateInfoList.size(); i++){
					BaseProductTemplateInfo info = baseProductTemplateInfoList.get(i);
					String pid = info.getId();
					if(CommonConstants.INVALID_PRODUCT_ID.equals(pid)){
						continue;
					}
					//获取召回源source信息
					String source = getSource(info);
					if(i < NEW_HOME_FEED_CACHE_NUM){
						beforeList.add(pid + ":" + ms + ":" + source);
					}else{
						afterList.add(pid + ":" + ms + ":" + source);
					}
				}
				String[] StrsInFirst = beforeList.toArray(new String[0]);
				String[] StrsInLast = afterList.toArray(new String[0]);
				//把头N个加入曝光
				if(StrsInFirst.length > 0) {
					cacheRedisUtil.lpush(CacheRedisKeyConstant.NEW_HOME_FEED_EXPOSURE + user.getUuid(), StrsInFirst);
				}
				//清空缓冲
				cacheRedisUtil.ltrim(CacheRedisKeyConstant.NEW_HOME_FEED_EXPOSURE_CACHE + user.getUuid(), 1, 0);
				//加入缓冲
				if(StrsInLast.length > 0) {
					cacheRedisUtil.lpush(CacheRedisKeyConstant.NEW_HOME_FEED_EXPOSURE_CACHE + user.getUuid(), StrsInLast);
					cacheRedisUtil.expire(CacheRedisKeyConstant.NEW_HOME_FEED_EXPOSURE_CACHE + user.getUuid(), EXPIRE_TIME_1DAY);
				}
			}
		} else {
			if (CommonConstants.CATEGORY_MIDDLE_PAGE_TOPICID.equals(topicId)) {
				List<String> cacheList = cacheRedisUtil.lrange(CacheRedisKeyConstant.NEW_CATEGORY_FEED_EXPOSURE_CACHE_PREFIX +
						frontendCategoryId + "_" + user.getUuid(), 0, -1);
				String[] rtstrs = null;
				List<String> resultList = new ArrayList<>();
				for (BaseProductTemplateInfo info : baseProductTemplateInfoList) {
					String pid = info.getId();
					if(CommonConstants.INVALID_PRODUCT_ID.equals(pid)){
						continue;
					}
					resultList.add(pid + ":" + ms);
				}
				if (CollectionUtils.isNotEmpty(cacheList)) {
					resultList.addAll(cacheList);
					//清空缓冲
					cacheRedisUtil.ltrim(CacheRedisKeyConstant.NEW_CATEGORY_FEED_EXPOSURE_CACHE_PREFIX +
							frontendCategoryId + "_" +user.getUuid(), 1, 0);
					rtstrs = resultList.toArray(new String[resultList.size()]);
				} else {
					rtstrs = resultList.toArray(new String[resultList.size()]);
				}
				cacheRedisUtil.lpush(CacheRedisKeyConstant.NEW_HOME_FEED_EXPOSURE + user.getUuid(), rtstrs);
				dealCategoryFakeExposure(feedIndex, user, frontendCategoryId, baseProductTemplateInfoList);
			} else {
				List<String> cacheList = cacheRedisUtil.lrange(CacheRedisKeyConstant.NEW_HOME_FEED_EXPOSURE_CACHE
						+ user.getUuid(), 0, 20 - NEW_HOME_FEED_CACHE_NUM - 1);
				String[] rtstrs = null;
				List<String> resultList = new ArrayList<>();
				for (BaseProductTemplateInfo info : baseProductTemplateInfoList) {
					String pid = info.getId();
					if(CommonConstants.INVALID_PRODUCT_ID.equals(pid)){
						continue;
					}
					//获取召回源source信息
					String source = getSource(info);
					resultList.add(pid + ":" + ms + ":" + source);
				}
				if (CollectionUtils.isNotEmpty(cacheList)) {
					resultList.addAll(cacheList);
					//清空缓冲
					cacheRedisUtil.ltrim(CacheRedisKeyConstant.NEW_HOME_FEED_EXPOSURE_CACHE + user.getUuid(), 1, 0);
					rtstrs = resultList.toArray(new String[resultList.size()]);
				} else {
					rtstrs = resultList.toArray(new String[resultList.size()]);
				}
				cacheRedisUtil.lpush(CacheRedisKeyConstant.NEW_HOME_FEED_EXPOSURE + user.getUuid(), rtstrs);
			}
		}
		//限制曝光为1000个
		cacheRedisUtil.ltrim(CacheRedisKeyConstant.NEW_HOME_FEED_EXPOSURE + user.getUuid(), 0, NEW_HOME_FEED_EXP_NUM);
		cacheRedisUtil.expire(CacheRedisKeyConstant.NEW_HOME_FEED_EXPOSURE + user.getUuid(), EXPIRE_TIME_3DAY);
	}

	/**
	 * 更新类目页单独的假曝光库
	 * @param feedIndex
	 * @param user
	 * @param frontendCategoryId
	 * @param baseProductTemplateInfoList
	 */
	private void dealCategoryFakeExposure(Integer feedIndex, ByUser user, String frontendCategoryId, List<BaseProductTemplateInfo> baseProductTemplateInfoList){
		if(CollectionUtils.isEmpty(baseProductTemplateInfoList)){
			return;
		}
		long currentMs = System.currentTimeMillis();
		try {
			String fakeExposureKey = CacheRedisKeyConstant.CATEGORY_FAKE_EXPOSURE_PREFIX + user.getUuid();
			String fakeExposureCacheKey = CacheRedisKeyConstant.CATEGOTY_FAKE_EXPOSURE_FIRST_PAGE_CACHE_PREFIX + frontendCategoryId + "_" +user.getUuid();

			if (feedIndex.equals(1)) {
				List<String> firstPartList = new ArrayList<>();
				List<String> secondPartList = new ArrayList<>();
				int index = 0;
				for(BaseProductTemplateInfo info : baseProductTemplateInfoList){
					index = index + 1;
					String pid = info.getId();

					//pid为-1时，表示的是广告运营位
					if(CommonConstants.INVALID_PRODUCT_ID.equals(pid)){
						continue;
					}

					if(index <= NEW_HOME_FEED_CACHE_NUM){
						firstPartList.add(pid + ":" + currentMs);
					}else{
						secondPartList.add(pid + ":" + currentMs);
					}
				}

				String[] firstPartArray = firstPartList.toArray(new String[0]);
				String[] secondPartArray = secondPartList.toArray(new String[0]);
				//把头N个加入曝光
				if(firstPartArray.length > 0) {
					cacheRedisUtil.lpush(fakeExposureKey, firstPartArray);
				}
				//清空缓冲
				cacheRedisUtil.ltrim(fakeExposureCacheKey, 1, 0);
				//加入缓冲
				if(secondPartArray.length > 0) {
					cacheRedisUtil.lpush(fakeExposureCacheKey, secondPartArray);
					cacheRedisUtil.expire(fakeExposureCacheKey, EXPIRE_TIME_1DAY);
				}

			} else {
				List<String> resultList = new ArrayList<>();
				List<String> cacheList = cacheRedisUtil.lrange(fakeExposureCacheKey, 0, 20 - NEW_HOME_FEED_CACHE_NUM - 1);
				if (CollectionUtils.isNotEmpty(cacheList)) {
					resultList.addAll(cacheList);
					//清空缓冲
					cacheRedisUtil.ltrim(fakeExposureCacheKey, 1, 0);
				}

				for (BaseProductTemplateInfo info : baseProductTemplateInfoList) {
					String pid = info.getId();
					if(CommonConstants.INVALID_PRODUCT_ID.equals(pid)){
						continue;
					}
					resultList.add(pid + ":" + currentMs);
				}

				if(resultList.size() > 0) {
					cacheRedisUtil.lpush(fakeExposureKey, resultList.toArray(new String[0]));
				}
			}
			//限制曝光为200个
			cacheRedisUtil.ltrim(fakeExposureKey, 0, CATEGORY_FAKE_EXP_NUM_UP_LIMIT);
			cacheRedisUtil.expire(fakeExposureKey, EXPIRE_TIME_3DAY);
		}catch (Exception e){
			log.error("[严重异常]处理类目页单独的假曝光时出现异常，uuid {}，fcateId {} ", user.getUuid(), frontendCategoryId, e);
		}
	}

	/**
	 * 转换块结果为pid list
	 * @param buildBlock
	 * @return
	 */
	private List<Long> convertBuildBlock2List(List<Template<TemplateInfo>> buildBlock) {
		List<Long> pidList = new ArrayList<>();
		if (CollectionUtils.isEmpty(buildBlock)) {
			return pidList;
		}
		for (Template<TemplateInfo> ttiList : buildBlock) {
			List<TemplateInfo> dataList = ttiList.getData();
			if (CollectionUtils.isNotEmpty(dataList)) {
				for (TemplateInfo tti : dataList) {
					BaseProductTemplateInfo curTemplateInfo = (BaseProductTemplateInfo) tti;
					if (curTemplateInfo != null) {
						pidList.add(Long.parseLong(curTemplateInfo.getId()));
					}
				}
			}
		}
		return pidList;
	}

	/**
	 * 获取召回源信息
	 * @param info
	 * @return
	 */
	private String getSource(BaseProductTemplateInfo info){
		String result = "";
		Map<String, String> routerParams = info.getRouterParams();
		if (routerParams == null || routerParams.size() == 0) {
			return result;
		}
		String stpStr = routerParams.get(CommonConstants.STP);
		if (StringUtils.isBlank(stpStr)) {
			return result;
		}
		try {
			String decodeStr = URLDecoder.decode(stpStr, "UTF-8");
			Map<String, String> stpMap = JSONObject.parseObject(decodeStr, new TypeReference<Map<String,String>>(){});
			String scmStr = stpMap.get(CommonConstants.SCM);
			if (StringUtils.isBlank(scmStr)) {
				return result;
			}
			String[] scmArray = scmStr.split("\\.");
			if(scmArray.length < 2){
				return result;
			}
			result = scmArray[1];
		}catch (Exception e){
			log.error("[严重异常]从模板类中获取召回源信息出现异常， stp {}，", stpStr, e);
		}
		return result;
	}
	/**
	 * 转换块结果为BaseProductTemplateInfo
	 * @param buildBlock
	 * @return
	 */
	private List<BaseProductTemplateInfo> convert2BaseProductInfoList(List<Template<TemplateInfo>> buildBlock) {
		List<BaseProductTemplateInfo> baseProductTemplateInfoList = new ArrayList<>();
		if (CollectionUtils.isEmpty(buildBlock)) {
			return baseProductTemplateInfoList;
		}
		for (Template<TemplateInfo> ttiList : buildBlock) {
			List<TemplateInfo> dataList = ttiList.getData();
			if (CollectionUtils.isNotEmpty(dataList)) {
				for (TemplateInfo tti : dataList) {
					BaseProductTemplateInfo curTemplateInfo = (BaseProductTemplateInfo) tti;
					if (curTemplateInfo != null) {
						baseProductTemplateInfoList.add(curTemplateInfo);
					}
				}
			}
		}
		return baseProductTemplateInfoList;
	}

	/**
	 * 调用sdk选择su 并对结果集进行更换
	 * @param user
	 * @param buildBlock
	 */
	private void replaceSuBySdk(ByUser user, Block<TemplateInfo> buildBlock) {
		//从uc中获取个性化尺码信息
		List<String> fieldsList = new ArrayList<>();
		Map<String, String> personalSizeMap = new HashMap<>();
		fieldsList.add(UserFieldConstants.PERSONALSIZE);
		User moses = ucRpcService.getData(user.getUuid(), user.getUid(), fieldsList, "moses");
		if (moses != null && moses.getPersonalSize() != null) {
			personalSizeMap = moses.getPersonalSize();
		}
		List<Template<TemplateInfo>> block = buildBlock.getBlock();

		//组装pid集合
		if (CollectionUtils.isEmpty(block)) {
			return;
		}
		Map<Long, Long> selectedSuMap = null;
		try {
			List<Long> pidList = convertBuildBlock2List(block);
			//调用facet sdk对所有结果选择su
			selectedSuMap = facetManager.selectSu(pidList, new ArrayList<>(), personalSizeMap);
		} catch (Exception e) {
			log.error("[一般异常]moses中更换结果su时,获取入口su信息异常", e);
			return;
		}

		//更换入口su
		for (Template<TemplateInfo> ttiList : block) {
			List<TemplateInfo> dataList = ttiList.getData();
			if (CollectionUtils.isEmpty(dataList)) {
				continue;
			}
			for (TemplateInfo tti : dataList) {
				try {
					BaseProductTemplateInfo curTemplateInfo = (BaseProductTemplateInfo) tti;
					if (curTemplateInfo == null) {
						continue;
					}
					ProductInfo productInfo = productDetailCache.getProductInfo(Long.parseLong(curTemplateInfo.getId()));
					Long suId = selectedSuMap.get(Long.parseLong(curTemplateInfo.getId()));
					if (suId == null) {
						continue;
					}
					List<SuProductInfo> suProductList = productInfo.getSuProductList();
					if (CollectionUtils.isNotEmpty(suProductList)) {
						for (SuProductInfo su : suProductList) {
							if (!suId.equals(su.getSuId())) {
								continue;
							}
							Long price = su.getPrice();
							String squarePortalImg = su.getSquarePortalImg();
							String squarePortalImgWebp = su.getSquarePortalImgWebp();
							if (price != null && su.getNovicePrice().signum()==1) {
								if (curTemplateInfo.getRouterParams().containsKey("suId")) {
									curTemplateInfo.setPriceStr(df.format(Long.valueOf(price) / 100.00));
									curTemplateInfo.setPriceCent(price.toString());
									curTemplateInfo.getRouterParams().put("suId", suId.toString());
									// 如果是新手专享的模版 则需要替换新手专享的价格
									if(tti instanceof TemplateNewuserTemplateInfo){
										TemplateNewuserTemplateInfo templateNewuserTemplateInfo = (TemplateNewuserTemplateInfo) tti;
										templateNewuserTemplateInfo.setNovicePrice(su.getNovicePrice().toString());
									}
									if(tti instanceof TemplateSingleNewuserTemplateInfo){
										TemplateSingleNewuserTemplateInfo templateSingleNewuserTemplateInfo = (TemplateSingleNewuserTemplateInfo) tti;
										templateSingleNewuserTemplateInfo.setNovicePrice(su.getNovicePrice().toString());
									}
									//只要有1个不为空，则更新入口图
									if (StringUtils.isNotBlank(squarePortalImg) || StringUtils.isNotBlank(squarePortalImgWebp)) {
										curTemplateInfo.setImage(squarePortalImg);
										curTemplateInfo.setImageWebp(squarePortalImgWebp);
									}
								}
							}
						}
					}
				} catch (Exception e) {
					log.error("[一般异常]moses中获取结果对应的su后，更换发生异常", e);
					continue;
				}
			}
		}
	}

	/**
	 * match完后，填充价格。重新选择sku后，修改价格
	 * @param
	 */
	private void fillPrice(Map<String, List<TotalTemplateInfo>> feedexpData, UIBaseRequest uibaseRequest, ByUser user){
		// 只有价格排序需要填充价格
		if(!SortTypeEnum.PRICE.getType().equals(uibaseRequest.getSortType())
				&& !SortTypeEnum.NOVICEPRICERANK.getType().equals(uibaseRequest.getSortType())){
			return;
		}
		// 只有没有选择筛选项时，才重选sku
		if(StringUtils.isBlank(uibaseRequest.getSelectedScreenAttrs())){
			//从uc中获取个性化尺码信息
			List<String> fieldsList = new ArrayList<>();
			Map<String, String> personalSizeMap = new HashMap<>();
			fieldsList.add(UserFieldConstants.PERSONALSIZE);
			User moses = ucRpcService.getData(user.getUuid(), user.getUid(), fieldsList, "moses");
			if (moses != null && moses.getPersonalSize() != null) {
				personalSizeMap = moses.getPersonalSize();
			}
			List<TotalTemplateInfo> infoList = new ArrayList<>();
			for(List<TotalTemplateInfo> infoListTmp : feedexpData.values()){
				infoList = infoListTmp;
				break;
			}
			Map<Long, Long> selectedSuMap = new HashMap<>();
			try {
					List<Long> pidList = infoList.stream().map(info -> Long.valueOf(info.getId())).collect(Collectors.toList());
					//调用facet sdk对所有结果选择su
					selectedSuMap = facetManager.selectSu(pidList, new ArrayList<>(), personalSizeMap);

			} catch (Exception e) {
				log.error("[一般异常]moses中更换结果su时,获取入口su信息异常", e);
				selectedSuMap = new HashMap<>();
			}

			for(TotalTemplateInfo info : infoList){
				ProductInfo productInfo = productDetailCache.getProductInfo(Long.valueOf(info.getId()));
				if(productInfo == null){
					continue;
				}
				List<SuProductInfo> suProductList = productInfo.getSuProductList();
				if(CollectionUtils.isEmpty(suProductList)){
					continue;
				}

				Long skuId = selectedSuMap.get(productInfo.getProductId());
				if(skuId == null){
					skuId = productInfo.getSuId();
				}

				for(SuProductInfo su : suProductList){
					if (!skuId.equals(su.getSuId())) {
						continue;
					}
					Long price = su.getPrice();
					BigDecimal novicePrice = su.getNovicePrice();
					if(SortTypeEnum.PRICE.getType().equals(uibaseRequest.getSortType()) && price != null){
						info.setSkuId(skuId.toString());
						info.setSkuPrice(price.toString());
					}else if(SortTypeEnum.NOVICEPRICERANK.getType().equals(uibaseRequest.getSortType()) && novicePrice != null){
						info.setSkuId(skuId.toString());
						info.setNovicePrice(novicePrice.toString());
					}
					break;
				}

			}
		}
	}
	/**
	 * 获取feed流模板
	 * @param lastBlock
	 * @param feedIndex
	 * @param uibaseRequest
	 * @param user
	 * @return
	 */
	private Map<String, Object> getFeedInfo(Block<TotalTemplateInfo> lastBlock,Integer feedIndex, UIBaseRequest uibaseRequest,ByUser user){

		List<TraceDetail> matchTraceDetailList = new ArrayList<TraceDetail>();
		List<TraceDetail> rankTraceDetailList = new ArrayList<TraceDetail>();
		HashMap<String, Object> resultMap = new HashMap<>();
		Map<String, List<TotalTemplateInfo>> feedexpData = new HashMap<>();

		Map<String, String> expValueMap = new HashMap<>();
		try {
			//先走规则引擎
			dealForNewRule(uibaseRequest, user, feedexpData, matchTraceDetailList, expValueMap);
			// 如果没有走规则引擎
			if(!user.isNewRule()){
				//处理新实验
				dealForNewExp(uibaseRequest, user, feedexpData, matchTraceDetailList, expValueMap);
			}else {
				user.setNewExp(true);
			}
		}catch (Exception e){
			log.error("[严重异常]处理新实验发生异常，uuid {}，sid {}", user.getUuid(), uibaseRequest.getSid(), e);
		}
		String source = expValueMap.get("source");
		String expId = expValueMap.get("expId");

		ApiResult<MatchResponse> match = null;
		ApiResult<RankResponse> rankResult = null;
		try {
			//如果没有走新实验系统，则还是走老实验系统
			if (!user.isNewExp()) {
				String pageId = uibaseRequest.getPageId();
				//首页的Feeds流，请求第一页数据时清除曝光数据
				if (feedIndex == 1 && StringUtils.isNotBlank(pageId) && getHomeFeedPageId().equals(pageId)) {
					cacheRedisUtil.del(CommonConstants.PAGE_CACHE_PREFIX + user.getPvid() + "_" + pageId + "_" + uibaseRequest.getTopicId() + "_"
							+ CommonConstants.HOME_FEED_CACHE_SUFFIX);
				}

				MatchRequest mr = buildMatchRequest(lastBlock, feedIndex, uibaseRequest, user);

				match = mosesMatchService.match(mr, user);
				feedexpData = match.getData().getResultMap();

				//价格排序时重选sku
				fillPrice(feedexpData, uibaseRequest, user);

				//处理类目页rank（新实验）
				Map<String, List<TotalTemplateInfo>> feedexpDataNewRank = dealCategoryNewRank(uibaseRequest, user, feedexpData);

				//如果新实验rank没有返回数据，则还是走老实验rank
				if (feedexpDataNewRank == null || feedexpDataNewRank.size() == 0) {
					// 构建rank请求参数
					RecommendRankRequest rankRequest = RecommendRankRequest.builder().matchData(feedexpData).uid(user.getUid()).
							uuid(user.getUuid()).sessionId(user.getSessionId()).upcUserType(user.getUpcUserType())
							.frontendCategoryId(uibaseRequest.getFrontendCategoryId()).siteId(user.getSiteId()).categoryIds(uibaseRequest.getCategoryIds()).build();

					if (StringUtils.isNotBlank(uibaseRequest.getSortType())) {
						rankRequest.setSortType(uibaseRequest.getSortType());
					}

					if (StringUtils.isNotBlank(uibaseRequest.getSortValue())) {
						rankRequest.setSortValue(uibaseRequest.getSortValue());
					}

					rankResult = mosesRankService.rank(rankRequest);
					if (rankResult != null && rankResult.getSuccess() != null && ErrorCode.SUCCESS_CODE.equals(rankResult.getSuccess())) {
						feedexpData = rankResult.getData().getRankResult();
					}

					// 在老实验系统的结果中填充expId和source
					if (StringUtils.isNotBlank(expId) && StringUtils.isNotBlank(source)) {
						for (String key : feedexpData.keySet()) {
							List<TotalTemplateInfo> totalTemplateInfoList = feedexpData.get(key);
							if (CollectionUtils.isNotEmpty(totalTemplateInfoList)) {
								for (TotalTemplateInfo totalTemplateInfo : totalTemplateInfoList) {
									if (totalTemplateInfo != null) {
										totalTemplateInfo.setExpId(expId);
										totalTemplateInfo.setSource(source);
									}
								}
							}
						}
					}
				} else {
					feedexpData = feedexpDataNewRank;
				}
				//插入活动入口
				advertInfoService.dealDirectInsertAdvertList(feedexpData, uibaseRequest, user);
			}
		}catch (Exception e){
			log.error("[严重异常]处理老实验发生异常，uuid {}，sid {}",user.getUuid(), uibaseRequest.getSid(), e);
		}
		
		// 处理 feedexpData，通过pid 判断是否为商品 feed，之后TotalTemplateInfo可添加当前模板信息类型字段来进行判断
//		feedexpData = filterUtil.insertEnterPics(feedexpData, uibaseRequest.getTopicId(), user.getUuid());

		// 插入优先展示的商品
		if (!StringUtils.isBlank(user.getPriorityProductId())){
			feedexpData = filterUtil.insertPriorityProduct(feedexpData, user.getPriorityProductId(),user.getSiteId());
		}

		 //插入类目页置顶的商品
		if (!StringUtils.isBlank(uibaseRequest.getPriorityProductId())){
		feedexpData = filterUtil.insertPriorityProduct(feedexpData, uibaseRequest.getPriorityProductId(),user.getSiteId());
	}

		resultMap.put(DATA, feedexpData);
		/**
		 * trace={"06c51fe4c9f816d5.1571121612096":{"expId":"moses:10300128_match.zwx.rexiaohaoping.20190412","keys":["moses:10300128_DefaultMatch_1234","moses:10300128_DefaultMatch_4321","moses:10300128_UDM_1000"],"pids":"1301025040,1301325003,1301725033,1300655061,1300835517,1301185079,1300225154,1301485266,1300515010,1301135403,1301725072,1301485169,1303205040,1300225161,1301185022,1301325011,1301185021,1301645246,1301065868,1301095024,"}}	rankTrace={}
		 */
		if(!user.isNewExp()) {
			if (match != null && match.getData().getTraceDetail() != null) {
				matchTraceDetailList = match.getData().getTraceDetail();
				resultMap.put(MATCH_TRACE, matchTraceDetailList);
			}
		}else{
			resultMap.put(MATCH_TRACE, matchTraceDetailList);
		}
		if (rankResult != null && rankResult.getData() != null && rankResult.getData().getTraceDetails() != null) {
			rankTraceDetailList = rankResult.getData().getTraceDetails();
			resultMap.put(RANK_TRACE, rankTraceDetailList);
		}
		//首页的Feeds流 若没有走新实验则不添加到页面缓存中，若走新实验则添加页面缓存
		if (!(getHomeFeedPageId().equals(uibaseRequest.getPageId())&&!user.isNewExp())
				&&isNeedCache(feedexpData)) {
			storePageCacheToRedis(feedexpData, matchTraceDetailList, rankTraceDetailList, uibaseRequest, user);
		}
		
		long rankend = System.currentTimeMillis();
//		log.info("[推荐]-[feed异步rank]-[pageId={},topicId={},耗时={}]", uibaseRequest.getPageId(),uibaseRequest.getTopicId(), rankend - end);
		
		return resultMap;
	}

	/**
	 * 将页面缓存存储到redis中
	 * @param feedexpData
	 * @param matchTraceDetailList
	 * @param rankTraceDetailList
	 * @param uibaseRequest
	 * @param user
	 */
	private void storePageCacheToRedis(Map<String, List<TotalTemplateInfo>> feedexpData, List<TraceDetail> matchTraceDetailList,
									   List<TraceDetail> rankTraceDetailList, UIBaseRequest uibaseRequest, ByUser user){
		//预处理页面缓存信息
		preprocessPageCache(feedexpData);

		Map<String, String> hash = new HashMap<>();
		hash.put(DATA, JSONObject.toJSONString(feedexpData));
		hash.put(MATCH_TRACE, JSONObject.toJSONString(matchTraceDetailList));
		hash.put(RANK_TRACE, JSONObject.toJSONString(rankTraceDetailList));
		if(StringUtils.isNotBlank(uibaseRequest.getFrontendCategoryId())){
			cacheRedisUtil.hmset(CommonConstants.PAGE_CACHE_PREFIX+user.getPvid()+"_"+uibaseRequest.getPageId()+"_"+uibaseRequest.getTopicId()+"_"+uibaseRequest.getFrontendCategoryId(), hash);
			cacheRedisUtil.expire(CommonConstants.PAGE_CACHE_PREFIX+user.getPvid()+"_"+uibaseRequest.getPageId()+"_"+uibaseRequest.getTopicId()+"_"+uibaseRequest.getFrontendCategoryId(), pageCacheTime);
		}else{
			cacheRedisUtil.hmset(CommonConstants.PAGE_CACHE_PREFIX+user.getPvid()+"_"+uibaseRequest.getPageId()+"_"+uibaseRequest.getTopicId(), hash);
			cacheRedisUtil.expire(CommonConstants.PAGE_CACHE_PREFIX+user.getPvid()+"_"+uibaseRequest.getPageId()+"_"+uibaseRequest.getTopicId(), pageCacheTime);
		}
	}

	/**
	 * 在将页面缓存存储到redis中之前，对缓存中的商品信息进行预处理，删除不需要缓存的信息
	 * @param feedexpData
	 */
	private void preprocessPageCache(Map<String, List<TotalTemplateInfo>> feedexpData){
		if(feedexpData == null || feedexpData.size() == 0){
			return;
		}

		for(List<TotalTemplateInfo> infoList : feedexpData.values()){
			if(CollectionUtils.isEmpty(infoList)){
				continue;
			}
			for(TotalTemplateInfo info : infoList){
				if(info == null){
					continue;
				}
				if(StringUtils.isNotBlank(info.getSkuId())) {
					//删除能通过skuId获取到的数据
					info.setSkuPrice(null);
					info.setImage(null);
					info.setImageWebp(null);
				}
				//删除默认值
				if(CommonConstants.BLANK_LINE_HEIGHT_DEFAULT_VALUE.equals(info.getHeight())){
					info.setHeight(null);
				}
				//删除没有使用的数据
				info.setScore(null);
			}
		}
	}

	/**
	 * 当将页面缓存从redis获取后，对缓存中的商品信息进行处理，增加一些信息
	 * 与preprocessPageCacheBeforeRedis方法对应
	 * @param feedexpData
	 */
	private void postprocessPageCache(Map<String, List<TotalTemplateInfo>> feedexpData){
		if(feedexpData == null || feedexpData.size() == 0){
			return;
		}

		for(List<TotalTemplateInfo> infoList : feedexpData.values()){
			if(CollectionUtils.isEmpty(infoList)){
				continue;
			}
			for(TotalTemplateInfo info : infoList){
				try {
					if (info == null || StringUtils.isBlank(info.getId())) {
						continue;
					}
					ProductInfo productInfo = productDetailCache.getProductInfo(Long.valueOf(info.getId()));
					if(productInfo == null){
						continue;
					}
					if(StringUtils.isNotBlank(info.getSkuId())) {
						SuProductInfo suInfo = ProductDetailUtil.getSuInfo(productInfo, info.getSkuId());
						if(suInfo != null && suInfo.getPrice() != null) {
							//添加通过skuId获取的数据
							info.setSkuPrice(suInfo.getPrice().toString());
							info.setImage(suInfo.getSquarePortalImg());
							info.setImageWebp(suInfo.getSquarePortalImgWebp());
						}else{
							//使用默认sku填充
							log.error("[一般异常]后置处理商品缓存信息时未获取到skuId对应的详细信息，使用默认skuId填充，原skuId {}，默认skuId {}",
									info.getSkuId(), productInfo.getSuId());
							info.setSkuId(productInfo.getSuId().toString());
							info.setSkuPrice(productInfo.getPrice().toString());
							info.setImage(productInfo.getSquarePortalImg());
							info.setImageWebp(productInfo.getSquarePortalImgWebp());
						}

					}
					//添加默认值
					if(info.getHeight() == null){
						info.setHeight(CommonConstants.BLANK_LINE_HEIGHT_DEFAULT_VALUE);
					}
				}catch (Exception e){
					log.error("[严重异常]后置处理商品缓存信息时出现异常，", e);
				}
			}
		}
	}

	/**
	 * 类目页热门走新rank
	 * @param uibaseRequest
	 * @param user
	 * @param feedexpData
	 */
	private Map<String, List<TotalTemplateInfo>>  dealCategoryNewRank(UIBaseRequest uibaseRequest,ByUser user, Map<String, List<TotalTemplateInfo>> feedexpData){

		Map<String, List<TotalTemplateInfo>> result = new HashMap<>();
		Map<String, TotalTemplateInfo> oldMatchResultMap = new HashMap<>();
		try {
			String sortType = uibaseRequest.getSortType();
			if (StringUtils.isNotBlank(sortType) && sortType.equals(SortTypeEnum.ALL.getType())
					&& CommonConstants.CATEGORY_MIDDLE_PAGE_TOPICID.equals(uibaseRequest.getTopicId())) {
				BaseRequest2 baseRequest2 = constructBaseRequest2(user, uibaseRequest);
				baseRequest2.setSid(uibaseRequest.getSid());
				uiExperimentSpace.divert(baseRequest2);
				HashMap<String, String> flags = baseRequest2.getFlags();
				String categoryRankName = flags.getOrDefault(ExpFlagsConstants.FLAG_CATEGORY_RANK_NAME, ExpFlagsConstants.VALUE_DEFAULT);
				String dataNum = flags.getOrDefault(ExpFlagsConstants.FALG_ALGORITHM_DATA_NUM, ExpFlagsConstants.VALUE_DEFAULT);
				String mosesExpId = matchAndRankService2.composeExpId(baseRequest2.getExpIds());
				//构造rank入参
				RankRequest2 rankRequest2 = new RankRequest2();
				if(!ExpFlagsConstants.VALUE_DEFAULT.equals(categoryRankName)){
					rankRequest2.setRankName(categoryRankName);
				}
				if(!ExpFlagsConstants.VALUE_DEFAULT.equals(dataNum)){
					rankRequest2.setDataNum(dataNum);
				}
                rankRequest2.setBizName(BizNameConst.CATEGORY);
				rankRequest2.setUuid(user.getUuid());
				int uid = 0;
				if (StringUtils.isNotBlank(user.getUid())) {
					try {
						uid = Integer.valueOf(user.getUid());
					} catch (Exception e) {
						log.error("[严重异常]用户uid不合法，uuid {}, uid {}", user.getUuid(), user.getUid(), e);
					}
				}
				rankRequest2.setUid(uid);
				rankRequest2.setUpcUserType(user.getUpcUserType());

				if (StringUtils.isNotBlank(user.getSex())) {
					try {
						rankRequest2.setUserSex(Integer.valueOf(user.getSex()));
					} catch (Exception e) {
						log.error("[严重异常]用户性别数据非法， sex {}， uuid {} , uid {}", user.getSex(), user.getUuid(), user.getUid(), e);
					}
				}

				rankRequest2.setCategoryIds(uibaseRequest.getCategoryIds());
				rankRequest2.setFrontendCategoryId(uibaseRequest.getFrontendCategoryId());
				rankRequest2.setSid(uibaseRequest.getSid());
				List<MatchItem2> matchItem2List = new ArrayList<>();
				String key = "";
				for (Entry<String, List<TotalTemplateInfo>> entry : feedexpData.entrySet()) {
					//类目页match后的feedexpData中只有1个key
					key = entry.getKey();
					List<TotalTemplateInfo> infoList = entry.getValue();
					if (CollectionUtils.isEmpty(infoList)) {
						continue;
					}
					for (TotalTemplateInfo info : infoList) {
						if (info == null || info.getId() == null) {
							continue;
						}
						try {
							MatchItem2 matchItem2 = new MatchItem2();
							matchItem2.setProductId(Long.valueOf(info.getId()));
							matchItem2.setSource("old");
							matchItem2List.add(matchItem2);
							oldMatchResultMap.put(info.getId(), info);
						} catch (Exception e) {
							log.error("[严重异常]商品pid非法， pid {}", info.getId(), e);
						}
					}
				}
				rankRequest2.setMatchItemList(matchItem2List);
				if (CollectionUtils.isNotEmpty(matchItem2List)) {
					RankResponse2 rankResponse2 = matchAndRankService2.rank(rankRequest2);
					String expId = rankResponse2.getExpId();
					//如果掉rank接口时传入了rank名称和数据版本号，则说明ui层指定了分类页的规则，故实验id为ui层实验id
					if(StringUtils.isNotBlank(rankRequest2.getRankName()) && StringUtils.isNotBlank(rankRequest2.getDataNum())){
						expId = mosesExpId;
					}
					List<RankItem2> rankItem2List = rankResponse2.getRankItem2List();
					List<TotalTemplateInfo> allProductList = matchAndRankService2.convert2TotalTemplateInfo(rankItem2List);
					if (CollectionUtils.isNotEmpty(rankItem2List)) {
						List<TotalTemplateInfo> totalTemplateInfoList = matchAndRankService2.convert2TotalTemplate(matchItem2List, allProductList, expId, oldMatchResultMap);
						if(CollectionUtils.isNotEmpty(totalTemplateInfoList)) {
							result.put(key, totalTemplateInfoList);
						}
					}
				}

			}
		}catch (Exception e){
			log.error("[严重异常]类目页新实验rank失败，uuid {}, uid {}", user.getUuid(), user.getUid(), e);
		}
		return result;
	}

	/**
	 * 判断是否走规则系统，如果是 则走规则系统 如果不是则不出处理
	 * @param uibaseRequest
	 * @param user
	 * @param feedexpData
	 * @param matchTraceDetailList
	 * @param expValueMap
	 */
	private void dealForNewRule(UIBaseRequest uibaseRequest,ByUser user,Map<String, List<TotalTemplateInfo>> feedexpData, List<TraceDetail> matchTraceDetailList, Map<String, String> expValueMap) {

		try {
			//构建规则引擎条件对象
			RuleBaseFact ruleBaseFact = droolsCommonUtil.buildRuleBaseFact(BuildBaseFactParam.builder().
					sceneId(uibaseRequest.getPagePositionId()).
					siteId(user.getSiteId()).
					topicId(uibaseRequest.getTopicId()).
					uid(user.getUid()).uuid(user.getUuid()).utype(user.getUpcUserType().toString()).build());
			// 获取命中的配置规则
			RuleFact ruleFact = ruleConfigCache.getRuleFactByCondition(ruleBaseFact, "layer_mosesui_feed");
			// 2 如果ruleEntity 不为null 说明命中的相应的规则 则走下面商品召回逻辑
			if (ruleFact == null) {
				return;
			}
			//一  召回
            MatchResponse2 matchResponse2;
            // 如果为在线召回 则走新版在线召回站点
            if(ruleFact.getMatchType().intValue()==1){
            	// 构建在线召回match 请求参数;
				MatchOnlineRequest matchRequest = droolsCommonUtil.buildMatchOnlineRequest(uibaseRequest, user, ruleFact);
				matchResponse2 =matchAndRankService2.matchOnline(matchRequest, user);
            }else {
				// 1 构建match参数
				MatchRequest2 matchRequest2 = droolsCommonUtil.bulidFeedMatchRequestForDrools(uibaseRequest, user, ruleFact);
				matchRequest2.setDrools(true);
                matchResponse2 = matchAndRankService2.match(matchRequest2, user);
            }
			if (matchResponse2 == null || CollectionUtils.isEmpty(matchResponse2.getMatchItemList())) {
				log.error("[一般异常]规则引擎召回商品数据为空,参数：{}", JSONObject.toJSONString(uibaseRequest));
				return;
			}
			KieSession kieSession = kiaSessionConfig.kieSession();
			// 工作内存注入fact对象
			FactHandle insert = kieSession.insert(ruleFact);
			try {
				//  3 执行过滤规则
				List<MatchItem2> filterResult = droolsService.filter(kieSession, ruleFact, matchResponse2.getMatchItemList(), user.getUid(),user.getUuid());
				if (CollectionUtils.isEmpty(filterResult)) {
					log.error("[一般异常]执行规则引擎中过滤规则后商品数量为空");
					return;
				}
				// 二 排序 构建rank请求参数
				RankRequest2 rankRequest2 = droolsCommonUtil.buildRankRequestForDrools(filterResult, ruleFact);
				MyBeanUtil.copyNotNullProperties(constructBaseRequest2(user, uibaseRequest), rankRequest2);
				// 执行排序规则
				RankResponse2 rankResponse2 = droolsService.rank(kieSession, rankRequest2);
				List<RankItem2> rankItem2List;
				if (rankResponse2 == null || CollectionUtils.isEmpty(rankResponse2.getRankItem2List())) {
					log.error("[严重异常]执行规则引擎rank规则后商品数量为空");
					rankItem2List = matchAndRankService2.convertToRankItem2List(filterResult);
				} else {
					rankItem2List = rankResponse2.getRankItem2List();
				}
				// 三 机制
				// 构建隔断机制参数
				RuleContext ruleContext = buildRuleContext(null,rankItem2List, user, uibaseRequest);
				List<TotalTemplateInfo> totalTemplateInfoList = droolsService.dealCategory(kieSession, ruleContext);
				// 四 将结果转化成旧实验系统的结构
				List<TotalTemplateInfo> result = matchAndRankService2.convert2TotalTemplate(filterResult, totalTemplateInfoList, ruleFact.getRuleId().toString(), null);
				if(!CollectionUtils.isEmpty(result)){
					// 五 填充结果
					// String expId = ;  实验id 取规则id
					fillDateAndTrace(feedexpData, matchTraceDetailList, result, ruleFact.getRuleId(), uibaseRequest.getTopicId());
					user.setNewRule(true);
				}
			} catch (Exception e) {
				log.error("[严重异常]执行规则引擎逻辑出现异常,异常信息",e);
			} finally {
				kieSession.delete(insert);
				kieSession.dispose();
			}
		}catch (Exception e){
			log.error("[严重异常]规则引擎未知异常,异常信息",e);
		}
	}


	public RuleContext buildRuleContext(List<TotalTemplateInfo> totalTemplateInfoList,List<RankItem2> rankItem2List,ByUser user,UIBaseRequest uiBaseRequest){

		if(CollectionUtils.isEmpty(totalTemplateInfoList)){
			totalTemplateInfoList = matchAndRankService2.convert2TotalTemplateInfo(rankItem2List);
		}
		List<TotalTemplateInfo> totalTemplateInfos = new ArrayList<>();
		BaseRequest2 baseRequest2 = constructBaseRequest2(user, uiBaseRequest);

		return RuleContext.builder()
				.uid(baseRequest2.getUid())
				.upcUserType(baseRequest2.getUpcUserType())
				.uuid(baseRequest2.getUuid())
				.userSex(baseRequest2.getUserSex())
				.allProductList(totalTemplateInfoList)
				.advertInfoList(advertInfoService.getAdvertInfoListByRule(baseRequest2.getShowAdvert(), baseRequest2.getUpcUserType(), baseRequest2.getAdvertInfoList(),user,uiBaseRequest.getPagePositionId()))
				.baseRequest2(baseRequest2)
				.byUser(user)
				.uiBaseRequest(uiBaseRequest)
				.waitInsertProductList(totalTemplateInfos)
				.build();

	}
	/**
	 * 判断是否走新实验系统，如果是则走新实验系统，如果不是则不处理。
	 * @param uibaseRequest
	 * @param user
	 * @param feedexpData
	 * @param matchTraceDetailList
	 */
	private void dealForNewExp(UIBaseRequest uibaseRequest,ByUser user,Map<String, List<TotalTemplateInfo>> feedexpData, List<TraceDetail> matchTraceDetailList, Map<String, String> expValueMap){
		//日志debug开关
		boolean isDebug = user.isDebug();
		String sid = uibaseRequest.getSid();
		String uuid = user.getUuid();

		//如果是首页feed流的topicId,则判断是否满足新实验系统条件，若满足，则走新实验系统
		if(CommonConstants.HOME_FEED_TOPICID.equals(uibaseRequest.getTopicId())
				|| getHomeFeedPageId().equals(uibaseRequest.getPageId())) {
			BaseRequest2 baseRequest2 = constructBaseRequest2(user, uibaseRequest);
			uiExperimentSpace.divert(baseRequest2);
			HashMap<String, String> flags = baseRequest2.getFlags();
			String ruleExpId = matchAndRankService2.composeExpId(baseRequest2.getExpIds());

			if(!user.isPersonalizedRecommendSwitch()){
                flags.put(MosesExpConst.FLAG_HOME_FEED, MosesExpConst.VALUE_HOME_FEED_EXP);
                flags.put(ExpFlagsConstants.FLAG_SOURCE_AND_WEIGHT, CommonConstants.NON_PERSONALIZED_CUSTOMER_SOURCE_WEIGHT);
                flags.put(MosesExpConst.FLAG_RANK_NAME, RankNameConstants.REDUCE_EXPOSURE_WEIGHT);
                flags.put(MosesExpConst.FLAG_RULE_NAME, RuleConst.RULE_SIMILAR_CATEGORY);
            }

            String homeFeedFlag = flags.getOrDefault(MosesExpConst.FLAG_HOME_FEED, MosesExpConst.VALUE_HOME_FEED_EXP);
			//判断是否是首页feed流实验层。如果个性化推荐设置开关关闭，则不管实验配置是否走新系统还是老系统，强制走新实验系统
			if(MosesExpConst.VALUE_HOME_FEED_EXP.equals(homeFeedFlag)){
				//获取待插入的商品集合
				List<TotalTemplateInfo> waitInsertPidInfoList = queryWaitInsertPidList(uibaseRequest, user, baseRequest2);
				String bizName = "";
				List<TotalTemplateInfo> totalTemplateInfoList = matchAndRankService2.matchAndRank(uibaseRequest, user, baseRequest2, waitInsertPidInfoList, RankNameConstants.REDUCE_EXPOSURE_WEIGHT, bizName);
				if(CollectionUtils.isNotEmpty(totalTemplateInfoList)){

                    String expId = totalTemplateInfoList.get(0).getExpId();
					fillDateAndTrace(feedexpData, matchTraceDetailList, totalTemplateInfoList, expId, CommonConstants.HOME_FEED_TOPICID);
					//只有调用新实验系统返回了数据才认为走了新实验系统
					//如果没有返回数据，则还是走实验系统兜底
					user.setNewExp(true);
				}
			}else if(MosesExpConst.VALUE_HOME_FEED_OLD.equals(homeFeedFlag)){
				expValueMap.put("expId", ruleExpId);
				expValueMap.put("source", MosesExpConst.VALUE_HOME_FEED_OLD);
			}

		}
		else if (CommonConstants.SLIDER_MIDDLE_PAGE_TOPICID.equals(uibaseRequest.getTopicId())) {
			uibaseRequest.setFrontPageId(CommonConstants.LBTLDY_FRONT_PAGE_ID);
			BaseRequest2 baseRequest2 = constructBaseRequest2(user, uibaseRequest);
			uiExperimentSpace.divert(baseRequest2);
			HashMap<String, String> flags = baseRequest2.getFlags();
			String expId = matchAndRankService2.composeExpId(baseRequest2.getExpIds());
			String lbtldyFeedFlag = flags.getOrDefault(MosesExpConst.FLAG_LBTLDY_FEED, MosesExpConst.VALUE_DEFAULT);
			if(MosesExpConst.VALUE_LBTLDY_FEED_NEW.equals(lbtldyFeedFlag)){
				String bizName = flags.getOrDefault(MosesExpConst.FLAG_LBTLDY_MATCH_NAME, MosesExpConst.VALUE_DEFAULT_LBTLDY_MATCH_NAME);
				String rankName = RankNameConstants.BASE_REDUCE_WEIGHT_RANK;
				if(BizNameConst.SLIDER_MIDDLE_PAGE2.equals(bizName)){
					rankName = null;
				}
				List<TotalTemplateInfo> totalTemplateInfoList = matchAndRankService2.matchAndRank(uibaseRequest, user, baseRequest2, null, rankName, bizName);
				if(CollectionUtils.isNotEmpty(totalTemplateInfoList)){
					expId = totalTemplateInfoList.get(0).getExpId();
					fillDateAndTrace(feedexpData, matchTraceDetailList, totalTemplateInfoList, expId, CommonConstants.SLIDER_MIDDLE_PAGE_TOPICID);
					//只有调用新实验系统返回了数据才认为走了新实验系统
					//如果没有返回数据，则还是走老实验系统兜底
					user.setNewExp(true);
				}else{
					log.error("[严重异常][轮播图落地页]新轮播图落地页数据为空，sid {}, baseRequest2 {}", sid, JSON.toJSONString(baseRequest2));
				}
			}else if(MosesExpConst.VALUE_LBTLDY_FEED_OLD.equals(lbtldyFeedFlag)){
				expValueMap.put("expId", expId);
				expValueMap.put("source", MosesExpConst.VALUE_LBTLDY_FEED_OLD);
			}
		}
		else if(CommonConstants.XSZXY_FEED_TOPICID.equals(uibaseRequest.getTopicId()) && StringUtils.isBlank(uibaseRequest.getNovicefrontcategoryOneId())){
			BaseRequest2 baseRequest2 = constructBaseRequest2(user, uibaseRequest);
			uiExperimentSpace.divert(baseRequest2);
			HashMap<String, String> flags = baseRequest2.getFlags();
			String ruleExpId = matchAndRankService2.composeExpId(baseRequest2.getExpIds());
            if(!user.isPersonalizedRecommendSwitch()){
                //如果个性化推荐设置开关关闭，则不管实验配置是否走新系统还是老系统，强制走新实验系统
                flags.put(MosesExpConst.FLAG_NEW_USER_FEED, MosesExpConst.VALUE_NEW_USER_FEED_EXP);
                flags.put(ExpFlagsConstants.FLAG_SOURCE_AND_WEIGHT, CommonConstants.NON_PERSONALIZED_OLDV_NEWV_SOURCE_WEIGHT);
                flags.put(MosesExpConst.FLAG_RANK_NAME, RankNameConstants.BASE_REDUCE_WEIGHT_RANK);
                flags.put(MosesExpConst.FLAG_RULE_NAME, RuleConst.RULE_SIMILAR_CATEGORY);
                flags.put(MosesExpConst.FLAG_MATCH_RULE_NAME, RuleConst.YQP_SELECT);
            }
			String newUserFlagValue = flags.getOrDefault(MosesExpConst.FLAG_NEW_USER_FEED, MosesExpConst.VALUE_DEFAULT);
			if(MosesExpConst.VALUE_NEW_USER_FEED_EXP.equals(newUserFlagValue)){
			    //获取待插入的商品集合
				List<TotalTemplateInfo> waitInsertPidInfoList = queryWaitInsertPidList(uibaseRequest, user, baseRequest2);
				String bizName = "";
				List<TotalTemplateInfo> totalTemplateInfoList = matchAndRankService2.matchAndRank(uibaseRequest, user, baseRequest2, waitInsertPidInfoList, RankNameConstants.CTVR_RANK, bizName);
				if(CollectionUtils.isNotEmpty(totalTemplateInfoList)){
					String expId = totalTemplateInfoList.get(0).getExpId();
					fillDateAndTrace(feedexpData, matchTraceDetailList, totalTemplateInfoList, expId, CommonConstants.XSZXY_FEED_TOPICID);
					user.setNewExp(true);
				}
			}else if(MosesExpConst.VALUE_NEW_USER_FEED_OLD.equals(newUserFlagValue)){
				expValueMap.put("expId", ruleExpId);
				expValueMap.put("source", MosesExpConst.VALUE_NEW_USER_FEED_OLD);
			}
		}
		else if(CommonConstants.M2F1_PAGE_TOPICID.equals(uibaseRequest.getTopicId())){
			//买二返一频道页热门获取数据
			user.setNewExp(true);
			String expId = "";
			List<MatchItem2> matchItem2List = null;
			HashMap<String, String> flags = null;
			uibaseRequest.setFrontPageId(CommonConstants.M2F1_FRONT_PAGE_ID);
			BaseRequest2 baseRequest2 = constructBaseRequest2(user, uibaseRequest);
			//只对老客进行实验分流
			if(UPCUserTypeConstants.CUSTOMER == user.getUpcUserType()) {
				uiExperimentSpace.divert(baseRequest2);
				flags = baseRequest2.getFlags();
				expId = matchAndRankService2.composeExpId(baseRequest2.getExpIds());
			}
			//如果进入了实验
			if(StringUtils.isNotBlank(expId)) {
				MatchRequest2 matchRequest2 = matchAndRankService2.buildMatchRequest2(baseRequest2, flags, "");
				if(isDebug) {
					log.error("[DEBUG]进入实验，调用match开始，sid {}， uuid {}，matchRequest2 {}", sid, uuid, JSON.toJSONString(matchRequest2));
				}
				//调用match接口
				MatchResponse2 match=new MatchResponse2();

				if(switchConfigCache.getRecommendSwitchByConfigId(CommonConstants.PERSONALIZE_RECOMMEND_OFF_CONFIG_ID)){
					match = matchAndRankService2.match(matchRequest2, user);
					if(CollectionUtils.isEmpty(match.getMatchItemList())){
						log.error("[严重异常][买二返一频道页]召回商品为空，sid {}，uuid {}，matchRequest2 {}", sid, uuid, JSON.toJSONString(matchRequest2));
					}
				}
				//如果进入了对照组，则需要考虑置顶商品
				String m2f1Flag = flags.get(MosesExpConst.FLAG_HOME_FEED);
				if (MosesExpConst.VALUE_HOME_FEED_OLD.equals(m2f1Flag)) {
					matchItem2List = aggrTopAndMatchPids(match.getMatchItemList(), user);
				}else{
					matchItem2List = match.getMatchItemList();
				}
				if(isDebug) {
					log.error("[DEBUG]进入实验，调用match结束，sid {}， uuid {}，matchResult {}", sid, uuid, JSON.toJSONString(matchItem2List));
				}
			}
			//如果没有进入实验 或者 进入实验后没有获取到召回商品，则按默认流程处理
			if(StringUtils.isBlank(expId) || CollectionUtils.isEmpty(matchItem2List)){
				MatchRequest2 matchRequest2 = new MatchRequest2();
				copyBaseRequest2(baseRequest2, matchRequest2);
				matchRequest2.setSourceAndWeight(CommonConstants.M2F1_PAGE_SOURCE_WEIGHT);
				matchRequest2.setExpNum(100);
				matchRequest2.setSid(uibaseRequest.getSid());
				if(isDebug) {
					log.error("[DEBUG]未进入实验，调用match开始，sid {}， uuid {}，matchRequest2 {}", sid, uuid, JSON.toJSONString(matchRequest2));
				}
				MatchResponse2 matchResponse2=new MatchResponse2();
				if(switchConfigCache.getRecommendSwitchByConfigId(CommonConstants.PERSONALIZE_RECOMMEND_OFF_CONFIG_ID)){
					matchResponse2 =  matchAndRankService2.match(matchRequest2, user);
				}
				matchItem2List = aggrTopAndMatchPids(matchResponse2.getMatchItemList(), user);
				expId = MosesExpConst.M2F1_PAGE_EXP_ID;
				if(isDebug) {
					log.error("[DEBUG]未进入实验，调用match结束，sid {}， uuid {}，matchResult {}", sid, uuid, JSON.toJSONString(matchItem2List));
				}
				if(CollectionUtils.isEmpty(matchItem2List)){
					log.error("[严重异常][买二返一频道页]最终召回商品为空，sid {}，uuid {}，matchRequest2 {}", sid, uuid, JSON.toJSONString(matchRequest2));
				}
			}
			List<TotalTemplateInfo> totalTemplateInfoList = matchListConvert2TotalTemplate(matchItem2List,expId);
			fillDateAndTrace(feedexpData, matchTraceDetailList, totalTemplateInfoList, expId, CommonConstants.M2F1_PAGE_TOPICID);
			if(isDebug){
				log.error("[DEBUG]sid {}, uuid {}, 转换后最终结果 {}", sid, uuid, JSON.toJSONString(feedexpData));
			}
		}
		else  if(CommonConstants.GXQSP_PAGE_TOPICID.equals(uibaseRequest.getTopicId())){
		   // 感兴趣商品集活动获取数据
			user.setNewExp(true);
			// 分流
			BaseRequest2 baseRequest2 = constructBaseRequest2(user, uibaseRequest);
			baseRequest2.setFrontPageId(CommonConstants.GXQSP_FRONT_PAGE_ID);
			uiExperimentSpace.divert(baseRequest2);
			List<TotalTemplateInfo> totalTemplateInfos = matchAndRankService2.matchAndRank(uibaseRequest, user, baseRequest2, null, "", "");
			fillDateAndTrace(feedexpData, matchTraceDetailList, totalTemplateInfos, null, CommonConstants.GXQSP_PAGE_TOPICID);
		}
	}

	/**
	 * 填充结果数据和trace信息
	 * @param feedexpData
	 * @param matchTraceDetailList
	 * @param totalTemplateInfoList
	 * @param expId
	 * @param topicId
	 */
	private void fillDateAndTrace(Map<String, List<TotalTemplateInfo>> feedexpData,List<TraceDetail> matchTraceDetailList,
								  List<TotalTemplateInfo> totalTemplateInfoList, String expId, String topicId){
		if(StringUtils.isBlank(expId)) {
			expId = "0000";
		}
		String dataKey = "moses:" + topicId + CommonConstants.SPLIT_LINE + expId;
		feedexpData.put(dataKey, totalTemplateInfoList);
		//构造matchTrace
		TraceDetail traceDetail = new TraceDetail();
		//该key必须
		traceDetail.setExpId(dataKey);
		Set<String> keys = new HashSet<>();
		keys.add("moses:"+topicId+CommonConstants.SPLIT_LINE+expId+CommonConstants.SPLIT_LINE+"0000");
		traceDetail.setKeys(keys);
		matchTraceDetailList.add(traceDetail);
	}
	/**
	 * 将置顶商品和召回商品组合在一起
	 * @param matchResult2
	 * @param user
	 * @return
	 */
	private List<MatchItem2> aggrTopAndMatchPids(List<MatchItem2> matchResult2, ByUser user){
		List<MatchItem2> lastMatchResult = new ArrayList<>();
		//先获取置顶的商品信息
		UIBaseBody uiBaseBody = user.getUiBaseBody();
		Set<Long> topPidSet = new HashSet<>();
		if(uiBaseBody != null && CollectionUtils.isNotEmpty(uiBaseBody.getTpids())){
			boolean isCheckError = false;
			int i = 0;
			for(String pid : uiBaseBody.getTpids()){
				try {
					Long productId = Long.valueOf(pid);
					ProductInfo productInfo = productDetailCache.getProductInfo(productId);
					if (FilterUtil.isCommonFilter(productInfo)) {
						continue;
					}

					// 过滤掉不支持用户端的商品
					if(filterUtil.isFilteredBySiteId(productId,user.getSiteId())){
						continue;
					}

					topPidSet.add(productId);
					MatchItem2 matchItem2 = new MatchItem2();
					matchItem2.setSource("m2f1Top");
					matchItem2.setProductId(productId);
					lastMatchResult.add(matchItem2);
					i++;
					//置顶商品最多100个
					if(i >= 100){
						break;
					}
				}catch (Exception e){
					isCheckError = true;
				}
			}
			if(isCheckError){
				log.error("[严重异常]解析买二返一频道页热门置顶商品出现错误， {}", JSON.toJSONString(uiBaseBody.getTpids()));
			}
		}
		//再存储从召回源获取到的商品，需与置顶商品去重
		if(CollectionUtils.isNotEmpty(matchResult2)){
			for (MatchItem2 matchItem2 : matchResult2){
				if(topPidSet.contains(matchItem2.getProductId())){
					continue;
				}
				lastMatchResult.add(matchItem2);
			}
		}
		return lastMatchResult;
	}

	/**
	 * 调用新实验系统的match
	 * @param matchName
	 * @param sid
	 * @param baseRequest2
	 * @return
	 */
	private MatchResponse2 newExpMatch(String matchName,String sid, BaseRequest2 baseRequest2, ByUser user){
		MatchResponse2 result = new MatchResponse2();
		try {
			MatchRequest2 matchRequest2 = new MatchRequest2();
			matchRequest2.setPriorityProductId(baseRequest2.getPriorityProductId());
			matchRequest2.setBiz(matchName);
			copyBaseRequest2(baseRequest2, matchRequest2);
			matchRequest2.setSid(sid);
			result =  matchAndRankService2.match(matchRequest2, user);
		}catch (Exception e){
			log.error("[严重异常]调用新match异常 ", e);
			List<MatchItem2> matchItem2List = new ArrayList<>();
			result.setMatchItemList(matchItem2List);
			result.setExpId("");
		}
		return result;
	}

	/**
	 * 调用新实验系统的rank
	 * @param rankName
	 * @param sid
	 * @param baseRequest2
	 * @return
	 */
	private List<RankItem2> newExpRank(String rankName,String sid, BaseRequest2 baseRequest2,List<MatchItem2> matchItem2List){
		List<RankItem2> result = new ArrayList<>();
		try {
			RankRequest2 rankRequest2 = new RankRequest2();
			copyBaseRequest2(baseRequest2, rankRequest2);
			rankRequest2.setRankName(rankName);
			rankRequest2.setSid(sid);
			rankRequest2.setMatchItemList(matchItem2List);
			result =  matchAndRankService2.rank(rankRequest2).getRankItem2List();
		}catch (Exception e){
			log.error("[严重异常]调用新rank异常 ", e);
		}
		return result;
	}

	/**
	 * 构造新实验系统的分流判断参数
	 * @param user
	 * @return
	 */
	private BaseRequest2 constructBaseRequest2(ByUser user, UIBaseRequest uiBaseRequest){
		//根据uuid获取用户最近浏览的时间，后续如果有多个地方使用，可以放到headFilter中获取
		List<String> fields = new ArrayList<>();
		Long latestViewTime = null;
		fields.add(UserFieldConstants.LASTVIEWTIME);
		User ucUser = ucRpcService.getData(user.getUuid(), null, fields, "moses");
		if(ucUser != null){
			latestViewTime = ucUser.getLastViewTime();
		}
		BaseRequest2 baseRequest2 = new BaseRequest2();
		baseRequest2.setLatestViewTime(latestViewTime);
		if(StringUtils.isNotBlank(user.getUid())) {
			try{
				baseRequest2.setUid(Integer.valueOf(user.getUid()));
			}catch(Exception e){
				log.error("[严重异常][入参]用户uid数据非法， uid {}", user.getUid());
				baseRequest2.setUid(0);
			}
		}

		if(StringUtils.isNotBlank(user.getSiteId())) {
			try {
				Integer siteId = Integer.valueOf(user.getSiteId().trim());
				baseRequest2.setSiteId(siteId);
			} catch (Exception e) {
				log.error("[严重异常][入参]用户siteId数据非法， siteId {}", user.getSiteId());
			}
		}
		baseRequest2.setPriorityProductId(user.getPriorityProductId());
		baseRequest2.setUuid(user.getUuid());
		baseRequest2.setUpcUserType(user.getUpcUserType());
		baseRequest2.setDebug(user.isDebug());
		baseRequest2.setShowAdvert(user.getShowAdvert());
		if(StringUtils.isNotBlank(user.getSex())) {
			try {
				baseRequest2.setUserSex(Integer.valueOf(user.getSex()));
			}catch (Exception e){
				log.error("[严重异常]用户性别数据非法，uuid {}, sex {}",user.getUuid(), user.getSex());
			}
		}
		baseRequest2.setSid(uiBaseRequest.getSid());
		baseRequest2.setAdvertInfoList(user.getAdvertInfoList());
		baseRequest2.setFrontPageId(uiBaseRequest.getFrontPageId());
		baseRequest2.setPagePositionId(uiBaseRequest.getPagePositionId());
		return baseRequest2;
	}

	/**
	 * 将baseRequest source的属性值复制到baseRequest target中
	 * @param source
	 * @param target
	 */
	private void copyBaseRequest2(BaseRequest2 source, BaseRequest2 target){
		target.setUpcUserType(source.getUpcUserType());
		target.setUid(source.getUid());
		target.setUserSex(source.getUserSex());
		target.setDevice(source.getDevice());
		target.setAv(source.getAv());
		target.setAvn(source.getAvn());
		target.setUuid(source.getUuid());
		target.setSiteId(source.getSiteId());
		target.setLatestViewTime(source.getLatestViewTime());
		target.setDebug(source.getDebug());
	}
	/**
	 * 进行规则处理  后面通过实验参数等配置进行动态获取bean 目前写死30天购买规则、上下左右类目、商家完全岔开规则
	 * @param user
	 * @param allProductList
	 * @return
	 */
	private List<TotalTemplateInfo> dealRankListByRule(ByUser user, List<TotalTemplateInfo> allProductList,String expId) {

		if (expId.equals(MosesExpConst.VALUE_HOME_FEED_EXP) || expId.equals(MosesExpConst.VALUE_HOME_FEED_EXP2) || expId.equals(MosesExpConst.VALUE_HOME_FEED_EXP3)) {
			//后期改为配置，通过bean名称动态创建规则实现
			Rule rule30DaysBuyFilterImpl = ApplicationContextProvider.getApplicationContext().getBean(RuleConst.RULE_30DAYSBUY_FILTER,
					Rule.class);
			Rule ruleCategoryAndSupplierImpl = ApplicationContextProvider.getApplicationContext().getBean(RuleConst.RULE_CATEGORY_AND_SUPPLIER,
					Rule.class);

			RuleContext context30Days = new RuleContext().builder()
					.uid(Integer.valueOf(user.getUid()))
					.upcUserType(user.getUpcUserType())
					.uuid(user.getUuid())
					.userSex(Integer.valueOf(user.getSex()))
					.allProductList(allProductList)
					.build();

			List<TotalTemplateInfo> listBy30DaysFilter = null;
			try {
				listBy30DaysFilter = rule30DaysBuyFilterImpl.ruleRank(context30Days);
			} catch (Exception e) {
				log.error("过滤30天购买rank异常 {}", e);
				listBy30DaysFilter = allProductList;
			}

			RuleContext contextCategoryAndSupplier = new RuleContext().builder()
					.uid(Integer.valueOf(user.getUid()))
					.upcUserType(user.getUpcUserType())
					.uuid(user.getUuid())
					.userSex(Integer.valueOf(user.getSex()))
					.allProductList(listBy30DaysFilter)
					.build();
			List<TotalTemplateInfo> rtList = null;
			try {
				rtList = ruleCategoryAndSupplierImpl.ruleRank(contextCategoryAndSupplier);
			} catch (Exception e) {
				log.error("过滤30天购买rank异常 {}", e);
				rtList = listBy30DaysFilter;
			}
			return rtList;
		}
		//新首页轮播图
		else if (expId.equals(MosesExpConst.SLIDER_EXP)) {

			Rule sliderRuleImpl = ApplicationContextProvider.getApplicationContext().getBean(RuleConst.RULE_SHUFFLE_LBT,
					Rule.class);
			RuleContext sliderRuleContext = new RuleContext().builder()
					.uid(Integer.valueOf(user.getUid()))
					.upcUserType(user.getUpcUserType())
					.uuid(user.getUuid())
					.userSex(Integer.valueOf(user.getSex()))
					.allProductList(allProductList)
					.build();
			List<TotalTemplateInfo> result = null;
			try {
				result = sliderRuleImpl.ruleRank(sliderRuleContext);
			} catch (Exception e) {
				log.error("[严重异常][轮播图]轮播图岔开机制异常", e);
				result = allProductList;
			}
			return result;
		}

		return allProductList;
	}

	/**
	 * 将新实验系统matchList返回结构转化为旧实验系统的返回结构
	 * @param matchResult
	 * @return
	 */
	private List<TotalTemplateInfo> matchListConvert2TotalTemplate(List<MatchItem2> matchResult,String expId) {
		List<TotalTemplateInfo> result = new ArrayList<>();
		if (CollectionUtils.isEmpty(matchResult)) {
			return result;
		}

		for (MatchItem2 item : matchResult) {
			TotalTemplateInfo totalTemplateInfo = new TotalTemplateInfo();
			if(item == null || item.getProductId() == null){
				continue;
			}
			Long productId = item.getProductId();
			totalTemplateInfo.setId(productId.toString());
			totalTemplateInfo.setExpId(expId);
			totalTemplateInfo.setSource(item.getSource());
			result.add(totalTemplateInfo);
		}

		return result;
	}

	private boolean isNeedCache(Map<String, List<TotalTemplateInfo>> feedexpData){
		boolean flag = true;
		if (feedexpData != null && !feedexpData.isEmpty()) {
			
			for (Entry<String, List<TotalTemplateInfo>> entry : feedexpData.entrySet()) {
				
				if (entry.getKey().contains(NOCACHE)) {
					flag = false;
					break;
				}
			}
		}
		return flag;
	}
	
	private Map<String, String> buildStp(UIBaseRequest uiBaseRequest, String blockId, ByUser user, int blockIndex,
			int feedIndex) {
		String pageId = uiBaseRequest.getPageId();
		HashMap<String, String> hashMap = new HashMap<>();
		hashMap.put("rpvid", user.getPvid());
		String p = user.getP();
		if (StringUtil.isBlank(p)) {
			p = ERRORCTP;
		} else {
			String[] pArray = p.split("-");
			if(pArray.length > 2){
				p = pArray[2];
			}else{
				p = ERRORCTP;
			}
		}

		String four = "";
		if (feedIndex > 0) {
			String value = "";
			if (StringUtil.isBlank(uiBaseRequest.getTopicId())) {
				value = StringUtil.concat("true|", pageId);
			} else {
				value = StringUtil.concat("true|", pageId, ",", uiBaseRequest.getTopicId());
			}
			String spmModle = "";
			if (HomePageCacheServiceImpl.HOME_AVN_MAP != null && HomePageCacheServiceImpl.HOME_AVN_MAP.size() > 0) {
				for (Map.Entry<String, String> entry : HomePageCacheServiceImpl.HOME_AVN_MAP.entrySet()) {
					String entryValue = entry.getValue();
					String entryKey = entry.getKey();
					if (value.equals(entryValue)) {
						spmModle = SourceSpmEnum.getSourceSpmModle(entryKey);
						if (!StringUtil.isBlank(spmModle)) {
							break;
						}
					}
				}
			}
			if (!StringUtil.isBlank(spmModle)) {
				pageId=spmModle;
			} else {
				four = "f" + CommonConstants.SPLIT_PIPE + feedIndex;
			}
		} else {
			four = "b" + CommonConstants.SPLIT_PIPE + blockIndex;
		}
		String spm = StringUtil.concat(user.getSiteId(), ".", p, ".", pageId, ".", four);
		hashMap.put("spm", spm);
		return hashMap;
	}
	
	private ArrayList<String> str2List(String str,String split){
		ArrayList<String> resultList = new ArrayList<>();
		
		if (StringUtils.isNotBlank(str)) {
			String[] data = str.split(split);
			if (data!=null) {
				for (int i = 0; i < data.length; i++) {
					resultList.add(data[i]);
				}
			}
		}
		
		return resultList;
	}

	private Map<String, List<String>> parseMap(String str) {
		try {
			if (null == str || str.trim().length() == 0) return null;

			JSONObject json = JSONObject.parseObject(str);
			Map<String, List<String>> map = new HashMap<>();
			for (String k : json.keySet()) {
				List<String> list = new ArrayList<>();
				JSONArray jsonArray = json.getJSONArray(k);
				if(jsonArray == null) continue;
				for(Object k1 : jsonArray.toArray()){
					if(k1 == null) continue;
					list.add(k1.toString());
				}
				map.put(k, list);
			}

			return map;
		} catch (Exception e) {
			log.error("matchAndRankAnsyService parseMap error, str {}, e {}",str, e);
		}

		return null;
	}

	@Async
	public Future<Map<String, List<TotalTemplateInfo>>> match(Block<TotalTemplateInfo> block,Integer feedIndex, UIBaseRequest uibaseRequest,ByUser user){
		Map<String, List<TotalTemplateInfo>> expData = null;
		try {
			if (block.isDynamic()) { // 是动态数据,走match获取数据
				// 匹配match
				long start = System.currentTimeMillis();


				MatchRequest mr = buildMatchRequest(block, feedIndex, uibaseRequest, user);

				ApiResult<MatchResponse> match = mosesMatchService.match(mr, user);
				if(match.getSuccess() == ErrorCode.SUCCESS_CODE && match.getData() != null){
					expData = match.getData().getResultMap();
				}else{
					expData = new HashMap<String, List<TotalTemplateInfo>>();
				}
				long end = System.currentTimeMillis();
				//log.info("[筛选]-[模板异步match]-[pageId={},topicId={},耗时={}, expData{}]", uibaseRequest.getPageId(),uibaseRequest.getTopicId(), JSON.toJSONString(expData));

			} else {
				expData = new HashMap<String, List<TotalTemplateInfo>>();
			}
		} catch (Exception e) {
			log.error("[筛选match执行失败]-[pageId={},bid={}]", uibaseRequest.getPageId(), block.getBid(), e);
			expData = new HashMap<String, List<TotalTemplateInfo>>();
		}
		return new AsyncResult<Map<String, List<TotalTemplateInfo>>>(expData);
	}

	private MatchRequest buildMatchRequest(Block<TotalTemplateInfo> block,Integer feedIndex, UIBaseRequest uibaseRequest,ByUser user){

		// 匹配match试验
		ArrayList<String> cateList = str2List(uibaseRequest.getCategoryIds(),",");
		ArrayList<String> scmIdList = str2List(uibaseRequest.getScmIds(),",");

		Map<String, List<String>> selectAttrsMap = parseMap(uibaseRequest.getSelectedScreenAttrs());
		//log.error("解析筛选条件，入参{}，转化后{}",uibaseRequest.getSelectedScreenAttrs(),JSON.toJSONString(selectAttrsMap));

		Boolean isShowCustomProduct = cmsFrontendCategoryCache.isCustomFcate(user.getSiteId(), uibaseRequest.getFrontendCategoryId());
		MatchRequest mr = MatchRequest.builder()
				.block(block)
				.feedPageNum(feedIndex)
				.dataSourceType(uibaseRequest.getTopicId())
				.sessionId(user.getSessionId())
				.uuId(user.getUuid())
				.siteId(user.getSiteId())
				.categoryIds(cateList)
				.scmIds(scmIdList)
				.pageId(uibaseRequest.getPageId())
				.uid(user.getUid())
				.device(user.getDevice())
				.uiBaseBody(user.getUiBaseBody())
				.sortType(uibaseRequest.getSortType())
				.priCouponType(uibaseRequest.getPriCouponType())
				.priCouponAmount(uibaseRequest.getPriCouponAmount())
				.userType(uibaseRequest.getUserType())
				.lat(user.getLat())
				.lng(user.getLng())
				.pvid(user.getPvid())
				//特权金2.8.1增加特权金优惠列表
				.priCouponAmountList(uibaseRequest.getPriCouponAmountList())
				// 增加upc用户类型
				.upcUserType(user.getUpcUserType())
				.novicefrontcategoryOneId(uibaseRequest.getNovicefrontcategoryOneId())
				// 增加优先展示商品
				.priorityProductId(user.getPriorityProductId())
				//增加标准属性筛选信息
				.selectedScreenAttrs(selectAttrsMap)
				//增加性别
				.sex(user.getSex())
				//增加前台类目ID
				.frontendCategoryId(uibaseRequest.getFrontendCategoryId())
				.isShowCustomProduct(isShowCustomProduct)
				.personalizedRecommendSwitch(user.isPersonalizedRecommendSwitch())
				.build();
		if(CommonConstants.PERSONAL_ACT_TOPIC_ID.contains(uibaseRequest.getTopicId())){
			mr.setPersonalizedRecommendActSwitch(switchConfigCache.getRecommendSwitchByConfigId(CommonConstants.PERSONALIZE_RECOMMEND_OFF_CONFIG_ID));
		}
		return mr;
	}

	/**
	 * 判断是否是双排feed流模板
	 * @param block
	 * @return
	 */
	private boolean isDoubleRowListTemplate(Block<TotalTemplateInfo> block){
		//如果非feed流，则不需要使用缓存
		if(block == null || !block.isFeed()){
			return false;
		}

		//如果非双排模板，则不需要使用缓存
		List<Template<TotalTemplateInfo>> templateList = block.getBlock();
		if(CollectionUtils.isEmpty(templateList)){
			return false;
		}
		//最后一个模板
		Template<TotalTemplateInfo> template = templateList.get(templateList.size() - 1);
		if(template == null){
			return false;
		}

		return TemplateTypeEnum.isDoubleRowList(template.getTemplateType());
	}

	/**
	 * 判断是否需要使用缓存
	 * @return
	 */
	public boolean isNeedUseCacheData(UIBaseRequest uiBaseRequest, ByUser user, Page<TemplateInfo> resultPage, Block<TotalTemplateInfo> lastBlock){

		try{
			//如果没有异常，则不需要使用缓存
			if(org.springframework.util.CollectionUtils.isEmpty(user.getExceptionTypeMap())){
				return false;
			}

			//如果不是双排feed流（左1右1）模板，则不需要使用缓存
			if(!isDoubleRowListTemplate(lastBlock)){
				return false;
			}

			//如果是分类页feed流，则不需要使用缓存
			if(CommonConstants.CATEGORY_MIDDLE_PAGE_TOPICID.equals(uiBaseRequest.getTopicId())){
				return false;
			}

			//如果resultPage中有Feed流数据，说明有异常但不影响数据返回，则不需要使用缓存
			if(resultPage != null && CollectionUtils.isNotEmpty(resultPage.getBlockList())){
				List<Block<TemplateInfo>> blockList = resultPage.getBlockList();
				//最后一层的数据不为空
				Block<TemplateInfo> templateInfoBlock = blockList.get(blockList.size() - 1);
				if(templateInfoBlock != null && templateInfoBlock.isFeed() && CollectionUtils.isNotEmpty(templateInfoBlock.getBlock())){
					return false;
				}

			}
		}catch(Exception e){
			log.error("[一般异常][首页缓存]判断是否需要使用缓存出现错误 ", e);
			return false;
		}
		return true;
	}


private MatchRequest2 buildMatchRequestForGxqsp(UIBaseRequest uibaseRequest,ByUser user){
	MatchRequest2 matchRequest2;
	RuleBaseFact ruleBaseFact = droolsCommonUtil.buildRuleBaseFact(BuildBaseFactParam.builder().
			sceneId(uibaseRequest.getPagePositionId()).
			siteId(user.getSiteId()).
			topicId(uibaseRequest.getTopicId()).
			uid(user.getUid()).uuid(user.getUuid()).utype(user.getUpcUserType().toString()).build());
	ruleBaseFact.setScene("8");
	RuleFact ruleFact = ruleConfigCache.getRuleFactByCondition(ruleBaseFact, "layer_mosesui_feed");
	if(ruleFact != null){
		matchRequest2= droolsCommonUtil.bulidFeedMatchRequestForDrools(uibaseRequest, user, ruleFact);
	}else {
		BaseRequest2 baseRequest2 = constructBaseRequest2(user, uibaseRequest);
		baseRequest2.setFrontPageId(CommonConstants.GXQSP_FRONT_PAGE_ID);
		uiExperimentSpace.divert(baseRequest2);
		matchRequest2 = matchAndRankService2.buildMatchRequest2(baseRequest2, baseRequest2.getFlags(), "");
	}
	return matchRequest2;
}
}