package com.biyao.moses.service;

import com.alibaba.fastjson.JSON;
import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.common.constant.BizNameConst;
import com.biyao.moses.common.constant.CommonConstants;
import com.biyao.moses.common.constant.ErrorCode;
import com.biyao.moses.common.constant.ExpFlagsConstants;
import com.biyao.moses.common.enums.MatchSourceEnum;
import com.biyao.moses.common.enums.ProductFeedEnum;
import com.biyao.moses.common.enums.TemplateTypeEnum;
import com.biyao.moses.dto.trace.TraceDetail;
import com.biyao.moses.exp.ExpirementSpace;
import com.biyao.moses.match2.constants.MatchStrategyConst;
import com.biyao.moses.match2.exp.MatchExpConst;
import com.biyao.moses.match2.exp.MatchExperimentSpace;
import com.biyao.moses.match2.param.MatchParam;
import com.biyao.moses.match2.service.BizService;
import com.biyao.moses.match2.service.RuleService;
import com.biyao.moses.match2.util.MatchUtil;
import com.biyao.moses.model.exp.AlgorithmConf;
import com.biyao.moses.model.exp.ExpRequest;
import com.biyao.moses.model.exp.Expirement;
import com.biyao.moses.model.exp.MatchDataSourceTypeConf;
import com.biyao.moses.model.match.MatchItem;
import com.biyao.moses.model.match2.MatchItem2;
import com.biyao.moses.model.template.Template;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.params.ApiResult;
import com.biyao.moses.params.BaseRequest2;
import com.biyao.moses.params.ProductInfo;
import com.biyao.moses.params.match.MatchRequest;
import com.biyao.moses.params.match.MatchResponse;
import com.biyao.moses.params.match.ProductMatchRequest;
import com.biyao.moses.params.match2.MatchRequest2;
import com.biyao.moses.params.match2.MatchResponse2;
import com.biyao.moses.rpc.UcRpcService;
import com.biyao.moses.service.imp.AsyncMatchService;
import com.biyao.moses.util.*;
import com.uc.domain.bean.User;
import com.uc.domain.constant.UserFieldConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 实验context
 * 
 * @Description
 * @author zyj
 * @Date 2018年9月27日
 */
@Component
@Slf4j
public class RecommendMatchContext {
	@Autowired
	ApplicationContextProvider applicationContextProvider;

	@Autowired
	ExpirementSpace expirementSpace;

	@Autowired
	AsyncMatchService asyncMatchService;

	@Autowired
	RedisUtil redisUtil;
	
	@Autowired
	FilterUtil filterUtil;

	@Autowired
	MatchUtil matchUtil;

	@Autowired
	ProductDetailCache productDetailCache;

	@Resource
	MatchFilterUtil matchFilterUtil;

	@Autowired
	MatchExperimentSpace matchExperimentSpace;

	//返回的最大商品数量
	private static final int PRODUCT_NUM_MAX_LIMIT = 500;
	//返回的最小商品数量
	private static final int PRODUCT_NUM_MIN_LIMIT = 100;

    @Autowired
    private UcRpcService ucRpcService;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public ApiResult<MatchResponse> match(MatchRequest matchRequest) {

		ApiResult<MatchResponse> apiResult = new ApiResult<>();

		Map<String, List<TotalTemplateInfo>> apiResultMap = new HashMap<>();
		try {
			// String sessionId=matchRequest.getSessionId();

			// Map<String, String> redisResult =
			// redisUtil.hgetAll(RedisKeyConstant.MOSES_SESSION_PREFIX + sessionId);
			// String uuId = redisResult.get("uuid");
			// if(StringUtils.isEmpty(uuId)) {
			// log.error("uuId为空！", uuId);
			// apiResult.setSuccess(ErrorCode.SYSTEM_ERROR_CODE);
			// apiResult.setError("uuId为空！");
			// return apiResult;
			// }
			String uuId = matchRequest.getUuId();
			String siteId = matchRequest.getSiteId();
			String pageId = matchRequest.getPageId();

			Set<String> topicIdList = new HashSet<>();
			String uid = matchRequest.getUid();
			String device = matchRequest.getDevice();
			// 查询合并相同的 key=dataSourceType+matchName+expNum
			Map<String, MatchDataSourceTypeConf> algorithmConfMap = new HashMap<>();
			// 返回数据合并相同的key=dataSourceType+expId
			Map<String, List<MatchDataSourceTypeConf>> expConfMap = new HashMap<>();
			int feedPageNum = matchRequest.getFeedPageNum();
			// 1,遍历block，调用实验方法（tid,uuid）
			List<Template> template = matchRequest.getBlock().getBlock();

			for (Template temp : template) {

				// 分割线和空白模板不查询数据,静态数据不查询
				if (TemplateTypeEnum.expTemplateName(temp.getTemplateName()) || !temp.isDynamic()) {
					continue;
				}
				MatchDataSourceTypeConf mdsConf = new MatchDataSourceTypeConf();
				mdsConf.setDataSourceType(temp.getDataSourceType());
				mdsConf.setFeedPageNum(feedPageNum);

				List<MatchDataSourceTypeConf> mdstList = new ArrayList<>();
				// dataSourceType为空，取tid作为实验id查询实验内容。否则通过dataSourceType查询

				String dataSourceType = matchRequest.getDataSourceType();
				String dataSource = StringUtils.isEmpty(dataSourceType) ? temp.getDataSourceType() : dataSourceType;
				topicIdList.add(dataSource);
				if (!StringUtils.isEmpty(matchRequest.getDataSourceType())) {
					temp.setDataSourceType(dataSourceType);
				}

				ExpRequest expRequest = ExpRequest.builder().tid(dataSource).uuid(uuId)
						.layerName(CommonConstants.LAYER_NAME_MATCH).build();
				Expirement exp = expirementSpace.getExpirement(expRequest);

				if (exp != null) {
					String expId = exp.getExpId();
					String expConfKey = CommonConstants.DEFAULT_PREFIX + temp.getDataSourceType() + "_" + expId;

					List<AlgorithmConf> confList = exp.getConfList();
					// 2，实验返回 Expirement ，合并相同的dataSourceType-matchName-expNum,减少查询次数
					for (int i = 0; i < confList.size(); i++) {
						AlgorithmConf aconf = confList.get(i);
						String mapkey = CommonConstants.DEFAULT_PREFIX + temp.getDataSourceType() + "_"
								+ aconf.getName() + "_" + aconf.getExpNum();
						if (algorithmConfMap.containsKey(mapkey)) {
							continue;
						} else {
							MatchDataSourceTypeConf expMdsConf = new MatchDataSourceTypeConf();
							BeanUtils.copyProperties(aconf, expMdsConf);
							expMdsConf.setDataSourceType(temp.getDataSourceType());
							algorithmConfMap.put(mapkey, expMdsConf);
							mdstList.add(expMdsConf);
						}
					}

					if (!expConfMap.containsKey(expConfKey)) {
						expConfMap.put(expConfKey, mdstList);
					}

				} else {
					// 没有查询到实验，使用兜底
					mdsConf.setName(CommonConstants.DEFAULT_MATCH_NAME);
					mdsConf.setExpNum(CommonConstants.DEFAULT_EXPNUM);
					mdsConf.setDataSourceType(temp.getDataSourceType());
					String mapkey = CommonConstants.DEFAULT_PREFIX + temp.getDataSourceType() + "_"
							+ CommonConstants.DEFAULT_MATCH_NAME + "_" + CommonConstants.DEFAULT_EXPNUM;
					algorithmConfMap.put(mapkey, mdsConf);

					MatchDataSourceTypeConf expMdsConf = new MatchDataSourceTypeConf();
					BeanUtils.copyProperties(mdsConf, expMdsConf);

					mdstList.add(expMdsConf);
					String expConfKey = CommonConstants.DEFAULT_PREFIX + temp.getDataSourceType() + "_"
							+ CommonConstants.DEFAULT_EXPID;
					expConfMap.put(expConfKey, mdstList);

				}
			}

			// 3，循环调用match，在redis获取数据，
			Iterator<Entry<String, MatchDataSourceTypeConf>> iterator = algorithmConfMap.entrySet().iterator();

			Map<String, List<TotalTemplateInfo>> resultMap = new HashMap<>();
			Map<String, Future<Map<String, List<TotalTemplateInfo>>>> futureMap = new HashMap<String, Future<Map<String, List<TotalTemplateInfo>>>>();
			int num = 0;
			while (iterator.hasNext()) {
				++num;
				Map.Entry<String, MatchDataSourceTypeConf> entry = iterator.next();
				MatchDataSourceTypeConf mdst = entry.getValue();
				mdst.setFeedPageNum(feedPageNum);
				String key = entry.getKey();
				mdst.setUid(uid);
				mdst.setSiteId(siteId);
				mdst.setDevice(device);
				mdst.setCategoryIds(matchRequest.getCategoryIds());
				mdst.setScmIds(matchRequest.getScmIds());

				//新手专享页面查询一起拼商品的前台一级类目ID
				mdst.setNovicefrontcategoryOneId(matchRequest.getNovicefrontcategoryOneId());

				//平台核心转化提升V1.7 新增筛选特权金商品参数
				mdst.setUserType(matchRequest.getUserType());
				mdst.setPriCouponAmount(matchRequest.getPriCouponAmount());
				mdst.setPriCouponType(matchRequest.getPriCouponType());
				mdst.setSortType(matchRequest.getSortType());
				mdst.setLat(matchRequest.getLat());
				mdst.setLng(matchRequest.getLng());
				mdst.setUiBaseBody(matchRequest.getUiBaseBody());

				//特权金2.8.1增加特权金优惠列表
				mdst.setPriCouponAmountList(matchRequest.getPriCouponAmountList());
				// upc用户类型
				mdst.setUpcUserType(matchRequest.getUpcUserType());
				// 优先展示商品
				mdst.setPriorityProductId(matchRequest.getPriorityProductId());
				//标准销售属性筛选信息
				mdst.setSelectedScreenAttrs(matchRequest.getSelectedScreenAttrs());
				//前台类目ID
				mdst.setFrontendCategoryId(matchRequest.getFrontendCategoryId());
				//是否展示定制商品
				mdst.setIsShowCustomProduct(matchRequest.getIsShowCustomProduct());
				//设置用户性别
				mdst.setSex(matchRequest.getSex());
				//设置用户个性化推荐开关状态
				mdst.setPersonalizedRecommendSwitch(matchRequest.isPersonalizedRecommendSwitch());
				mdst.setPersonalizedRecommendActSwitch(matchRequest.isPersonalizedRecommendActSwitch());
				
				Future<Map<String, List<TotalTemplateInfo>>> future = asyncMatchService.executeRecommendMatch(mdst, key,
						uuId);
				futureMap.put(num + "", future);
			}

			for (Entry<String, Future<Map<String, List<TotalTemplateInfo>>>> futureEntry : futureMap.entrySet()) {
				Future<Map<String, List<TotalTemplateInfo>>> future = futureEntry.getValue();
				Map<String, List<TotalTemplateInfo>> result = new HashMap<>();
				try {
					result = future.get(10000, TimeUnit.MILLISECONDS);
				} catch (Exception e) {
					log.error("[严重异常]调用对应match获取数据发送异常， key {}，",futureEntry.getKey(), e);
				}
				resultMap.putAll(result);
			}

			// 4, 商品去重，过滤，加threadlocal缓存
			Map<String, List<TotalTemplateInfo>> repeatData = repeatData(resultMap, siteId, uuId, topicIdList, pageId);
			// 5，合并,排序，dataSourceType-expId

			Iterator<Entry<String, List<MatchDataSourceTypeConf>>> expConfIterator = expConfMap.entrySet().iterator();

			ArrayList<TraceDetail> traceList = new ArrayList<TraceDetail>();

			while (expConfIterator.hasNext()) {

				TraceDetail traceDetail = TraceLogUtil.initAid();

				Entry<String, List<MatchDataSourceTypeConf>> next = expConfIterator.next();
				List<MatchDataSourceTypeConf> list = next.getValue();
				String mapKey = next.getKey();

				List<TotalTemplateInfo> totalTemplateList = new ArrayList<>();
				for (MatchDataSourceTypeConf mdstc : list) {

					String prefix = CommonConstants.DEFAULT_PREFIX + mdstc.getDataSourceType() + "_" + mdstc.getName()
							+ "_";

					String key = prefix + mdstc.getExpNum();

					MatchDataSourceTypeConf matchDataSourceTypeConf = algorithmConfMap.get(key);

					List<TotalTemplateInfo> templateList = repeatData.get(key);
					if (templateList != null) {
						totalTemplateList.addAll(templateList);

						// 20181129 houkun 埋点数据
						if (matchDataSourceTypeConf.isDefalutData()) {
							traceDetail.getKeys().add(prefix + CommonConstants.DIFFERENTIATE_NUM);
						} else {
							traceDetail.getKeys().add(key);
						}
					}
				}

				// 按照score倒序排序
				Collections.sort(totalTemplateList, new Comparator<TotalTemplateInfo>() {
					@Override
					public int compare(TotalTemplateInfo t1, TotalTemplateInfo t2) {
						Double o2Score = t2.getScore() != null ? t2.getScore() : 0;
						Double o1Score = t1.getScore() != null ? t1.getScore() : 0;
						return o2Score.compareTo(o1Score);
					}

				});

				// zhaiweixi 20190522 增加白名单展示
				//推荐系统_首页feeds流迭代V1.0 增加经纬度参数，matchRequest中取  20190606 huangyq				 
				totalTemplateList = matchFilterUtil.gpWhitelistFilter(mapKey, totalTemplateList, matchRequest);

				//如果不展示定制商品，则将结果中的定制商品删除
				if(matchRequest.getIsShowCustomProduct() != null && !matchRequest.getIsShowCustomProduct()){
					commonFilter(totalTemplateList);
				}

				apiResultMap.put(mapKey, totalTemplateList);

				// 20181129 houkun 埋点数据
				traceDetail.setExpId(mapKey);
				traceList.add(traceDetail);
			}

			// 6，返回ApiResult<MatchResponse>
			MatchResponse mr = new MatchResponse();
			mr.setResultMap(apiResultMap);
			mr.setTraceDetail(traceList);
			apiResult.setData(mr);
		} catch (Exception e) {
			log.error("[严重异常]match异常！", e);
			apiResult.setSuccess(ErrorCode.SYSTEM_ERROR_CODE);
			apiResult.setError("match服务异常");
		}
		return apiResult;
	}

	/**
	 * 将返回结果进行通用过滤
	 * @param totalTemplateInfoList
	 */
	private void commonFilter(List<TotalTemplateInfo> totalTemplateInfoList){
		if(CollectionUtils.isEmpty(totalTemplateInfoList)){
			return;
		}
		try{
			Iterator<TotalTemplateInfo> iterator = totalTemplateInfoList.iterator();
			while (iterator.hasNext()){
				TotalTemplateInfo info = iterator.next();
				if(StringUtils.isBlank(info.getId())){
					continue;
				}
				ProductInfo productInfo = productDetailCache.getProductInfo(Long.valueOf(info.getId()));
				if(FilterUtil.isCommonFilter(productInfo)){
					iterator.remove();
					continue;
				}
			}
		}catch (Exception e){
			log.error("[一般异常]最终结果过滤定制商品失败，e ", e);
		}
	}

	/**
	 * 去除重复数据
	 * 
	 * @param reslutMap
	 * @param siteId
	 * @param uuid
	 * @param topicIdList
	 * @param pageId
	 * @return
	 */
	private Map<String, List<TotalTemplateInfo>> repeatData(Map<String, List<TotalTemplateInfo>> reslutMap,
			String siteId, String uuid, Set<String> topicIdList, String pageId) {
		Map<String, List<TotalTemplateInfo>> repeatMap = new HashMap<>();
		LinkedHashMap<String, TotalTemplateInfo> totalLinkMap = new LinkedHashMap<>();

		Iterator<Entry<String, List<TotalTemplateInfo>>> entry = reslutMap.entrySet().iterator();

		// 循环商品集合,计算权重使用
		List<ProductInfo> totalProductInfoList = new ArrayList<>();
		while (entry.hasNext()) {
			Entry<String, List<TotalTemplateInfo>> entryMap = entry.next();
			// list=去重复前的list
			List<TotalTemplateInfo> list = entryMap.getValue();
			// 判断是否有商品id，没有商品id则为模板数据类型，不需要去重和查询商详
			if (list != null && list.size() > 0 && !StringUtils.isEmpty(list.get(0).getId())) {
				// 去重复后的list
				for (TotalTemplateInfo templateInfo : list) {
					if (totalLinkMap.containsKey(templateInfo.getId())) {
						// 累加计算权重最终得分
						Double score = totalLinkMap.get(templateInfo.getId()).getScore();
						Double score2 = templateInfo.getScore();
						if (score == null) {
							score = 0D;
						}
						if (score2 == null) {
							score2 = 0D;
						}
						templateInfo.setScore(score + score2);
						totalLinkMap.put(templateInfo.getId(), templateInfo);
					} else {
						totalLinkMap.put(templateInfo.getId(), templateInfo);

					}
				}

			}
		}
		// 循环商品最终得分的集合,过滤商品
		Iterator<Entry<String, List<TotalTemplateInfo>>> productEntry = reslutMap.entrySet().iterator();
		while (productEntry.hasNext()) {
			Entry<String, List<TotalTemplateInfo>> entryMap = productEntry.next();
			String mapkey = entryMap.getKey();
			List<TotalTemplateInfo> list = entryMap.getValue();
			if (list != null && list.size() > 0 && !StringUtils.isEmpty(list.get(0).getId())) {
				List<TotalTemplateInfo> newlist = new ArrayList<>();
				for (TotalTemplateInfo templateInfo : list) {
					if (totalLinkMap.containsKey(templateInfo.getId())) {
						newlist.add(templateInfo);
						totalLinkMap.remove(templateInfo.getId());
					}
				}

				// 商品去重复后，查询商品详情
				List<ProductInfo> productInfoList = queryProductDetail(newlist);
				totalProductInfoList.addAll(productInfoList);
				// 过滤已下架商品
				List<TotalTemplateInfo> filterNotRecommend = filterUtil.filterNotRecommend(newlist, productInfoList,
						siteId, uuid, topicIdList, pageId);
				repeatMap.put(mapkey, filterNotRecommend);
			} else {
				repeatMap.put(mapkey, list);
			}
		}

		// 缓存全量商品信息到threadlocal
		// ByUser user = UserContext.getUser();
		// if (!StringUtils.isEmpty(user)) {
		// Map<Long, ProductInfo> productInfo = totalProductInfoList.stream()
		// .collect(Collectors.toMap(ProductInfo::getProductId, a -> a, (k1, k2) ->
		// k1));
		// user.setLocalProductInfo(productInfo);
		// }

		return repeatMap;

	}

	/**
	 * 查询商品详情
	 * 
	 * @param repeatList
	 * @return
	 */
	private List<ProductInfo> queryProductDetail(List<TotalTemplateInfo> repeatList) {
		List<ProductInfo> productInfoList = new ArrayList<>();
		if (repeatList.size() > 0) {
			for (TotalTemplateInfo templateInfo : repeatList) {
				try {
					ProductInfo productInfo = productDetailCache.getProductInfo(Long.valueOf(templateInfo.getId()));
					if (productInfo != null) {
						productInfoList.add(productInfo);
					}
				} catch (NumberFormatException e) {
					continue;
				}
			}
		}
		return productInfoList;
	}

	/**
	 * 商品match方法
	 * @param request
	 * @return
	 */
	public List<MatchItem> productFeedMatch(ProductMatchRequest request){
		List<MatchItem> result = new ArrayList<>();
		try {
			if (ProductFeedEnum.HOME.getName().equals(request.getDataSourceType())) {
				MatchRequest matchRequest = MatchRequest.builder()
						.uuId(request.getUuid())
						.uid(request.getUid())
						.lat(request.getLat())
						.lng(request.getLng())
						.pvid(request.getPvid())
						.siteId(request.getSiteId().toString())
						.upcUserType(request.getUpcUserType())
						.build();

                //获取uc不感兴趣商品，加入黑名单
                Set<Long> uninterested = new HashSet<>();
                List<String> filedList = new ArrayList<>();
                filedList.add(UserFieldConstants.DISINTERESTPIDS);
                User ucUser = ucRpcService.getData(matchRequest.getUuId(),null, filedList, "mosesmatch");
                if(ucUser!=null&& org.apache.commons.collections.CollectionUtils.isNotEmpty(ucUser.getDisinterestPids())){
                    uninterested = ucUser.getDisinterestPids();
                }

				List<Long> productIdList = matchFilterUtil.getPriorityProductList(matchRequest,uninterested);
				if (productIdList != null && productIdList.size() > 0){
					// todo 如果没有分数，是否可以不设置 source加一个自己的枚举，不使用match名称 策略模式
					productIdList.forEach(pid-> {
						MatchItem matchItem = MatchItem.builder()
								.productId(pid)
								.score(1.0)
								.source(ProductFeedEnum.HOME.getName())
								.build();
						result.add(matchItem);
					});
				}
			}
		}catch (Exception e){
			log.error("[严重异常]请求商品productFeedMatch时发生异常，request {}", JSON.toJSONString(request), e);
		}
		return result;
	}

	/**
	 * 新实验系统，通过实验配置获取召回源信息
	 * 支持两种方式：1、通过配置召回源及其权重获取召回源信息 2、通过配置召回源bean名称来获取召回源信息
	 * @param request
	 * @return
	 */
	public ApiResult<MatchResponse2>  productMatch(MatchRequest2 request) {

		ApiResult<MatchResponse2> result = new ApiResult<>();
		MatchResponse2 matchResponse2 = new MatchResponse2();
		result.setData(matchResponse2);

		List<MatchItem2> matchItem2List = null;
		String expId = "";
		String rankName = "";

		try {
			//如果传入了具体的召回源及其权重，则直接从指定的召回源中召回商品
			if (StringUtils.isNotBlank(request.getSourceAndWeight()) && !MatchExpConst.VALUE_DEFAULT.equals(request.getSourceAndWeight())) {
				List<Map<String, Double>> sourceWeightLayerList = parseSourceAndWeight(request.getSourceAndWeight());
				Map<String, String> flag = new HashMap<>();
				if (request.getExpNum() != null && request.getExpNum() > 0) {
					flag.put(MatchExpConst.FLAG_EXPECT_NUM_MAX, request.getExpNum().toString());
				} else {
					if (BizNameConst.FEED_INSERT.equals(request.getBiz())) {
						//横插默认返回的商品个数上限为100
						flag.put(MatchExpConst.FLAG_EXPECT_NUM_MAX, "100");
					}
				}
				flag.put(ExpFlagsConstants.SFLAG_UCB_DATA_NUM, request.getUcbDataNum());
				flag.put(ExpFlagsConstants.SFLAG_SOURCE_REDIS, request.getSourceRedis());
				flag.put(ExpFlagsConstants.SFLAG_SOURCE_DATA_STRATEGY, request.getSourceDataStrategy());
				flag.put(MatchExpConst.FLAG_RULE_NAME, request.getRuleName());
				matchItem2List = queryAndAggrBySourceAndWeight(request, sourceWeightLayerList, flag);
				if (CollectionUtils.isEmpty(matchItem2List)) {
					int sourceNum = 0;
					for (Map<String, Double> sourceWeightMap : sourceWeightLayerList) {
						if (sourceWeightMap == null) {
							continue;
						}
						sourceNum += sourceWeightMap.size();
					}
					//如果召回源数量小于等于2，则可能召回不了商品
					if (sourceNum <= 2) {
						log.error("[一般异常]获取召回源数据为空，sid {}, request {}", request.getSid(), JSON.toJSONString(request));
					} else {
						log.error("[严重异常]获取召回源数据为空，sid {}, request {}", request.getSid(), JSON.toJSONString(request));
					}
				}
			} else {
				// 如果没有传入具体的召回源及其权重，则按照是否传入biz
				// 如果biz也没有传入，则走实验进行分流
				if (StringUtils.isEmpty(request.getBiz())) {
					BaseRequest2 baseRequest2 = new BaseRequest2();
					baseRequest2.setUuid(request.getUuid());
					baseRequest2.setUid(request.getUid());
					baseRequest2.setUserSex(request.getUserSex());
					baseRequest2.setUpcUserType(request.getUpcUserType());
					baseRequest2.setAv(request.getAv());
					baseRequest2.setAvn(request.getAvn());
					baseRequest2.setDevice(request.getDevice());
					baseRequest2.setSiteId(request.getSiteId());
					baseRequest2.setLatestViewTime(request.getLatestViewTime());
					baseRequest2.setFrontPageId(request.getFrontPageId());
					matchExperimentSpace.divert(baseRequest2);
					Map<String, String> flags = baseRequest2.getFlags();
					List<Integer> expIds = baseRequest2.getExpIds();
					if (!CollectionUtils.isEmpty(expIds)) {
						int size = expIds.size();
						for (int i = 0; i < size; i++) {
							Integer tmpExpId = expIds.get(i);
							if (tmpExpId == null) {
								continue;
							}
							if (i == size - 1) {
								expId += tmpExpId.toString();
							} else {
								expId += tmpExpId.toString() + CommonConstants.SPLIT_LINE;
							}
						}
					}

					if (flags.containsKey(MatchExpConst.FLAG_SOURCE_AND_WEIGHT) && !MatchExpConst.VALUE_DEFAULT.equals(flags.get(MatchExpConst.FLAG_SOURCE_AND_WEIGHT))) {
						List<Map<String, Double>> sourceWeightLayerList = parseSourceAndWeight(flags.get(MatchExpConst.FLAG_SOURCE_AND_WEIGHT));
						matchItem2List = queryAndAggrBySourceAndWeight(request, sourceWeightLayerList, flags);
					}

					//填充rankName返回值
					if (flags.containsKey(MatchExpConst.FLAG_RANK_NAME) && !MatchExpConst.VALUE_DEFAULT.equals(flags.get(MatchExpConst.FLAG_RANK_NAME))) {
						rankName = flags.get(MatchExpConst.FLAG_RANK_NAME);
					}
				} else {
					matchItem2List = queryByBizName(request);
				}

				if (CollectionUtils.isEmpty(matchItem2List)) {
					log.error("[严重异常]获取召回源数据为空，sid {}, request {}, expId {}", request.getSid(), JSON.toJSONString(request), expId);
				}
			}
		}catch (Exception e){
			log.error("[严重异常][新match]处理新match时出现异常，uuid {}，sid {}， e", request.getUuid(), request.getSid(), e);
		}

		if(CollectionUtils.isEmpty(matchItem2List)){
			result.setSuccess(ErrorCode.SYSTEM_ERROR_CODE);
			result.setError("获取召回源数据为空");
		}else{
			result.setSuccess(ErrorCode.SUCCESS_CODE);
			result.setError("success");
		}
		matchResponse2.setMatchItemList(matchItem2List);
		matchResponse2.setExpId(expId);
		matchResponse2.setRankName(rankName);
		return result;
	}

	/**
	 * 解析获取召回源及其权重信息，格式为：source:weight|source:weight...|source:weight
	 * @param sourceAndWeightLayerStr
	 * @return
	 */
	private List<Map<String, Double>> parseSourceAndWeight(String sourceAndWeightLayerStr){
		List<Map<String, Double>> result = new ArrayList<>();
		if(StringUtils.isEmpty(sourceAndWeightLayerStr)){
			return result;
		}
		String[] sourceAndWeightLayerArray = sourceAndWeightLayerStr.trim().split(";");
		//遍历每一层的sourceAndWeight配置
		for(String sourceAndWeightStr : sourceAndWeightLayerArray) {
			if(StringUtils.isBlank(sourceAndWeightStr)){
				continue;
			}
			Map<String, Double> sourceWeightMap = new HashMap<>();
			String[] sourceAndWeightArray = sourceAndWeightStr.trim().split("\\|");
			for (String sourceAndWeight : sourceAndWeightArray) {
				try {
					String[] str = sourceAndWeight.trim().split(",");
					if (str.length != 2) {
						log.error("[严重异常][实验配置]参数格式错误，跳过该召回源配置，sourceAndWeight {} ", sourceAndWeightLayerStr);
						continue;
					}
					String source = str[0].trim();
					Double weight = Double.valueOf(str[1].trim());
					sourceWeightMap.put(source, weight);
				} catch (Exception e) {
					log.error("[严重异常][实验配置]参数解析失败，跳过该召回源配置，sourceAndWeight {} ", sourceAndWeightLayerStr, e);
				}
			}
			result.add(sourceWeightMap);
		}
		return result;
	}

	/**
	 * 解析获取召回源及其权重信息，格式为：source:value|source:value...|source:value
	 * @param sourceStr
	 * @return
	 */
	private Map<String, String> parseSourceAndValue(String sourceStr){
		Map<String, String> result = new HashMap<>();
		if(StringUtils.isEmpty(sourceStr) || ExpFlagsConstants.VALUE_DEFAULT.equals(sourceStr)){
			return result;
		}

		String[] sourceAndValueArray = sourceStr.trim().split("\\|");
		for (String sourceAndValue : sourceAndValueArray) {
			try {
				String[] str = sourceAndValue.trim().split(",");
				if(str.length != 2){
					log.error("[严重异常][实验配置]参数格式错误， sourceStr {}", sourceStr);
					continue;
				}
				String source = str[0].trim();
				result.put(source, str[1].trim());
			}catch (Exception e){
				log.error("[严重异常][实验配置]参数解析失败， sourceStr {}", sourceStr, e);
			}
		}
		return result;
	}

	/**
	 * 通过bizName获取召回源数据
	 * @param request
	 * @return
	 */
	private List<MatchItem2> queryByBizName(MatchRequest2 request){
		List<MatchItem2> matchItemList = new ArrayList<>();
		try {
			BizService bizService = (BizService) ApplicationContextProvider.getBean(request.getBiz());
			matchItemList = bizService.match(request);
		}catch (Exception e){
			log.error("[严重异常]通过bizName获取召回源数据异常, bizName {}", request.getBiz(), e);
		}
		return matchItemList;
	}

	/**
	 * 通过召回源source和权重 获取商品信息
	 * @param request
	 * @param sourceWeightLayerList
	 * @return
	 */
	private List<MatchItem2> queryAndAggrBySourceAndWeight(MatchRequest2 request, List<Map<String, Double>> sourceWeightLayerList,Map<String, String> flags){
        if(CollectionUtils.isEmpty(sourceWeightLayerList)){
            return new ArrayList<>();
        }

        Set<Long> blackList = new HashSet<>();
		Map<String, Double> allSourceWeightMap = getAllSource(sourceWeightLayerList);
		User ucUser = queryUserFromUc(request, allSourceWeightMap.keySet());
        if(ucUser != null && !CollectionUtils.isEmpty(ucUser.getDisinterestPids())){
            blackList = ucUser.getDisinterestPids();
        }

        //如果个性化推荐设置开关关闭时，则将用户性别设置为未知，用户季节设置为空。
        if(!request.isPersonalizedRecommendSwitch() ){
			request.setUserSex(Integer.valueOf(CommonConstants.UNKNOWN_SEX));
			if(ucUser != null && StringUtils.isNotBlank(ucUser.getSeason())){
				ucUser.setSeason("");
			}
		}

		//获取日志debug开关
		boolean debug = request.getDebug() == null ? false : request.getDebug();
		String sid = request.getSid();
		String uuid = request.getUuid();

		//检查是否存在规则
		RuleService ruleService = null;
		String ruleName = flags.get(MatchExpConst.FLAG_RULE_NAME);
		if(!StringUtils.isEmpty(ruleName) && !ruleName.equals(MatchExpConst.VALUE_DEFAULT)){
			ruleService = (RuleService)ApplicationContextProvider.getBean(ruleName);
		}

		String sourceDataStrategy = flags.get(ExpFlagsConstants.SFLAG_SOURCE_DATA_STRATEGY);
		Map<String, String> sourceDataStrategyMap = parseSourceAndValue(sourceDataStrategy);
		String sourceRedis = flags.get(ExpFlagsConstants.SFLAG_SOURCE_REDIS);
		Map<String, String> sourceRedisMap = parseSourceAndValue(sourceRedis);

		boolean isExistNotNormalProductSource = false;
		Map<String, Future<List<MatchItem2>>> resultMap = new HashMap<>();
		boolean isContainHots = false;
		boolean isContainNchs = false;
		for(String source : allSourceWeightMap.keySet()){
			if(StringUtils.isBlank(source)){
				log.error("[严重异常][召回源]source为空，sid {}， uuid {}", sid, uuid);
				continue;
			}

			if(!isExistNotNormalProductSource && MatchSourceEnum.isNotNormalProduct(source)){
				isExistNotNormalProductSource = true;
			}

			if(MatchStrategyConst.HOTS.equals(source)){
				isContainHots = true;
				continue;
			}else if (MatchStrategyConst.NCHS.equals(source)){
				isContainNchs = true;
				continue;
			}
			String realSource = source;
			//对ucb系列召回源进行处理，如果source不是ucb和ucb2,并且以ucb开头，则认为是ucb系列召回源，尝试获取ucbDataNum
			String ucbDataNum = flags.get(ExpFlagsConstants.SFLAG_UCB_DATA_NUM);
			if(!MatchStrategyConst.UCB2.equals(source) && !MatchStrategyConst.UCB.equals(source)
				&& source.startsWith(MatchExpConst.VALUE_UCB_PREFIX)){
				try{
					String ucbDataNumStr = source.substring(MatchExpConst.VALUE_UCB_PREFIX.length());
					if(StringUtils.isNotBlank(ucbDataNumStr) && StringUtils.isNumeric(ucbDataNumStr)){
						Integer.valueOf(ucbDataNumStr);
						ucbDataNum = ucbDataNumStr;
						realSource = MatchStrategyConst.UCB;
					}
				}catch(Exception e){
					log.error("[严重异常]处理ucb系列召回源时出现异常，sid {}, uuid {}, source {}，", sid, uuid, source, e);
				}
			}
			MatchParam matchParam = MatchParam.builder().device(request.getDevice())
					.uid(request.getUid()).uuid(request.getUuid())
					.upcUserType(request.getUpcUserType())
					.userSex(request.getUserSex())
                    .pidList(request.getPidList())
					.source(realSource)
					.dataStrategy(sourceDataStrategyMap.get(source))
					.redis(sourceRedisMap.get(source))
					.ucbDataNum(ucbDataNum)
					.ucUser(ucUser)
					.responseMapKeys(request.getResponseMapKeys())
					.isMunmanualSource(request.isManualSource(source))
					.isFilterByZw(request.getIsFilterByZw())
					.build();

			Future<List<MatchItem2>> asyncMatchResult = asyncMatchService.executeMatch2(matchParam, realSource);
			resultMap.put(source, asyncMatchResult);
		}

		int expNumMax = PRODUCT_NUM_MAX_LIMIT;
		String expNumMaxStr = flags.get(MatchExpConst.FLAG_EXPECT_NUM_MAX);
		if(!StringUtils.isEmpty(expNumMaxStr) && !MatchExpConst.VALUE_DEFAULT.equals(expNumMaxStr)){
			try{
				int productNumMinLimit = request.isDrools() ? 0 : PRODUCT_NUM_MIN_LIMIT;
				expNumMax = Integer.valueOf(expNumMaxStr);
				if(!isExistNotNormalProductSource  && StringUtil.isBlank(request.getResponseMapKeys()) &&(expNumMax < productNumMinLimit || expNumMax > 1000)){
					expNumMax = PRODUCT_NUM_MAX_LIMIT;
					log.error("[严重异常][实验配置]参数sflag_expect_num_max值超过1000或小于100，值为 {}", expNumMaxStr);
				}
			}catch(Exception e){
				log.error("[严重异常][实验配置]参数sflag_expect_num_max值格式错误，值为 {} ", expNumMaxStr);
			}
		}

		Map<String, MatchItem2> matchMap = new HashMap<>();
		//遍历每个召回源层
		for(Map<String, Double> sourceWeightMap : sourceWeightLayerList) {
			Map<String, MatchItem2> currentLayerMatchMap = new HashMap<>();
			//遍历该层中的每一个召回源
			for (Map.Entry<String, Double> entry : sourceWeightMap.entrySet()) {
				String source = entry.getKey();
				Double weight = entry.getValue();
				if(MatchStrategyConst.HOTS.equals(source) || MatchStrategyConst.NCHS.equals(source)){
					continue;
				}

				if(!resultMap.containsKey(source)){
					log.error("[严重异常]汇总召回源数据时，缺少召回源{}商品数据", source);
					continue;
				}
				Future<List<MatchItem2>> asynResult = resultMap.get(source);
				try {
					List<MatchItem2> matchResult = asynResult.get(CommonConstants.MATCH_MAX_WAIT_TIME, TimeUnit.MILLISECONDS);
					if (debug) {
						log.error("[DEBUG]sid {}， uuid {}, 从召回源 {} 获取的数据： {}", sid, uuid, source, JSON.toJSONString(matchResult));
					}
					//目前只有普通商品需要进行过滤规则
					if(!isExistNotNormalProductSource) {
						//执行规则
						if (ruleService != null) {
							matchResult = ruleService.execute(matchResult, request);
							if (debug) {
								log.error("[DEBUG]sid {}，uuid {}， 召回源 {}，规则 {}， 规则处理后结果：{}", sid, uuid, source, ruleName, JSON.toJSONString(matchResult));
							}
						}
						blackProductFilter(matchResult, blackList);
					}
					// 若传了responseMapKeys，则不用召回源之间聚合，也不用按照分值排序了 直接返回match返回结果。
					if(!StringUtil.isBlank(request.getResponseMapKeys())){
					    return matchResult;
                    }
					matchUtil.aggrMatchItem(matchResult, weight, currentLayerMatchMap,request.getSiteId());
					if (debug) {
						log.error("[DEBUG]sid {}，uuid {}，召回源 {}，权重 {}，当前召回源层聚合后的结果：{}", sid, uuid, source, weight, JSON.toJSONString(currentLayerMatchMap.values()));
					}
				} catch (Exception e) {
					log.error("[严重异常]获取召回源商品数据异常，召回源{}，uid {}，超时时间{}ms", source, request.getUid(), CommonConstants.MATCH_MAX_WAIT_TIME, e);
				}
			}
			if(currentLayerMatchMap.isEmpty()){
				continue;
			}
			//获取当前层召回商品并按分值进行排序
			List<MatchItem2> matchItem2List = new ArrayList<>(currentLayerMatchMap.values());
			matchItem2List = matchItem2List.stream().sorted((m1, m2) -> -m1.getScore().compareTo(m2.getScore()))
					.collect(Collectors.toList());
			//使用该层的结果填充
			List<MatchItem2> fillProductList = matchUtil.getFillProduct(matchItem2List, expNumMax, matchMap, false,request.getSiteId());
			matchUtil.aggrMatchItem(fillProductList, 1d, matchMap,request.getSiteId());
			if (debug) {
				log.error("[DEBUG]sid {}，uuid {}，当前召回源层 {}，聚合当前召回源层后的结果：{}", sid, uuid, JSON.toJSONString(sourceWeightMap), JSON.toJSONString(matchMap.values()));
			}
			if(matchMap.size() >= expNumMax){
				break;
			}
		}

		MatchParam matchParam = MatchParam.builder().device(request.getDevice())
				.uid(request.getUid()).uuid(request.getUuid())
				.upcUserType(request.getUpcUserType())
				.userSex(request.getUserSex())
				.build();

		if(isContainHots) {
			List<MatchItem2> matchItem2List = MatchUtil.executeMatch2(matchParam, MatchStrategyConst.HOTS);
			if(!CollectionUtils.isEmpty(matchItem2List)) {
				//执行规则
				if (ruleService != null) {
					matchItem2List = ruleService.execute(matchItem2List,request);
				}
                blackProductFilter(matchItem2List,blackList);
				List<MatchItem2> hotsFillProduct = matchUtil.getFillProduct(matchItem2List, expNumMax, matchMap, true,request.getSiteId());
				Double weight = allSourceWeightMap.get(MatchStrategyConst.HOTS);
				matchUtil.aggrMatchItem(hotsFillProduct, weight, matchMap,request.getSiteId());
				if (debug) {
					log.error("[DEBUG]sid {}，uuid {}，召回源 hots，权重 {}， 期望的数量 {}， 聚合后的结果： {}", sid, uuid, weight, expNumMax, JSON.toJSONString(matchMap.values()));
				}
			}
		}else if (isContainNchs){
			List<MatchItem2> matchItem2List = MatchUtil.executeMatch2(matchParam, MatchStrategyConst.NCHS);
			if(!CollectionUtils.isEmpty(matchItem2List)) {
				//执行规则
				if (ruleService != null) {
					matchItem2List = ruleService.execute(matchItem2List,request);
				}
                blackProductFilter(matchItem2List,blackList);
				List<MatchItem2> hotsFillProduct = matchUtil.getFillProduct(matchItem2List, expNumMax, matchMap, true,request.getSiteId());
				Double weight = allSourceWeightMap.get(MatchStrategyConst.NCHS);
				matchUtil.aggrMatchItem(hotsFillProduct, weight, matchMap,request.getSiteId());
				if (debug) {
					log.error("[DEBUG]sid {}，uuid {}，召回源 nchs，权重 {}， 期望的数量 {}，  聚合后的结果： {}", sid, uuid, weight, expNumMax, JSON.toJSONString(matchMap.values()));
				}
			}
		}

		Collection<MatchItem2> values = matchMap.values();
		List<MatchItem2> matchSorted = values.stream().sorted((m1, m2) -> -m1.getScore().compareTo(m2.getScore()))
				.collect(Collectors.toList());

		List<MatchItem2> result = matchSorted.size()>expNumMax ? matchSorted.subList(0,expNumMax) : matchSorted;
		if(debug){
			log.error("[DEBUG]sid {}，uuid {}，最终返回的结果： {}", sid,  uuid, JSON.toJSONString(result));
		}
		return result;
	}

	/**
	 * 获取所有配置的召回源信息
	 * @param sourceWeightLayerList
	 * @return
	 */
	private Map<String, Double> getAllSource(List<Map<String, Double>> sourceWeightLayerList){
		Map<String, Double> result = new HashMap<>();
		if(CollectionUtils.isEmpty(sourceWeightLayerList)){
			return result;
		}

		for(Map<String, Double> sourceWeightMap : sourceWeightLayerList){
			if(sourceWeightMap == null || sourceWeightMap.isEmpty()){
				continue;
			}
			result.putAll(sourceWeightMap);
		}

		return result;
	}
	/**
	 * 根据召回源信息从UC中查询数据
	 * @param request
	 * @param sourceSet
	 * @return
	 */
	private User queryUserFromUc(MatchRequest2 request, Set<String> sourceSet){
		User result = null;
		if(CollectionUtils.isEmpty(sourceSet)){
			return null;
		}
		Set<String> fieldSet = matchUtil.getUcFieldBySource(sourceSet);
		fieldSet.add(UserFieldConstants.DISINTERESTPIDS);
		List<String> filedList = new ArrayList<>(fieldSet);
		String uuid = request.getUuid();
		String uid = null;
		if(request.getUid() != null && request.getUid() > 0){
			uid = request.getUid().toString();
		}

		result = ucRpcService.getData(uuid, uid, filedList, "mosesmatch");
		return result;
	}
    /**
     * 过滤不感兴趣商品
     * @param list
     * @param blackList
     * @return
     */
    private void blackProductFilter(List<MatchItem2> list, Set<Long> blackList) {

        try {
            if (CollectionUtils.isEmpty(blackList)) {
                return;
            }
            Iterator<MatchItem2> iterator = list.iterator();
            while (iterator.hasNext()) {
                MatchItem2 next = iterator.next();
				Long disInterestPid=next.getProductId();
             	if(Objects.isNull(disInterestPid)){
					if (!Objects.isNull(next.getId()) && next.getId().length() < 12){
						disInterestPid = Long.valueOf(next.getId());
					}else {
						continue;
					}
				}
                if (blackList.contains(disInterestPid)) {
                    iterator.remove();
                }
            }
        } catch (Exception e) {
            log.error("[严重异常]黑名单过滤异常 ", e);
        }
    }
}
