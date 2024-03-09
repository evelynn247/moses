package com.biyao.moses.rank;

import com.alibaba.fastjson.JSON;
import com.biyao.moses.common.constant.BizNameConst;
import com.biyao.moses.common.constant.CommonConstants;
import com.biyao.moses.common.constant.ErrorCode;
import com.biyao.moses.common.constant.RankNameConstants;
import com.biyao.moses.common.enums.SortTypeEnum;
import com.biyao.moses.dto.trace.TraceDetail;
import com.biyao.moses.exp.ExpirementSpace;
import com.biyao.moses.model.exp.AlgorithmConf;
import com.biyao.moses.model.exp.ExpRequest;
import com.biyao.moses.model.exp.Expirement;
import com.biyao.moses.model.rank2.RankItem2;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.params.ApiResult;
import com.biyao.moses.params.BaseRequest2;
import com.biyao.moses.params.rank.RankResponse;
import com.biyao.moses.params.rank.RecommendRankRequest;
import com.biyao.moses.params.rank2.RankRequest2;
import com.biyao.moses.params.rank2.RankResponse2;
import com.biyao.moses.rank2.exp.RankExpConst;
import com.biyao.moses.rank2.exp.RankExperimentSpace;
import com.biyao.moses.rank2.service.Rank2;
import com.biyao.moses.rank2.service.impl.CateGoryRankByFactorService;
import com.biyao.moses.rank2.service.impl.DefaultRankImpl;
import com.biyao.moses.util.ApplicationContextProvider;
import com.biyao.moses.util.TraceLogUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


@Component
@Slf4j
public class RecommendRankContext {
	
	@Autowired
	private ExpirementSpace expirementSpace;

	@Autowired
	private DefaultRankImpl defaultRank2;

	@Autowired
	private RankExperimentSpace rankExperimentSpace;
	@Autowired
	private CateGoryRankByFactorService cateGoryRankByFactorService;

	
	public ApiResult<RankResponse> rank(RecommendRankRequest rankRequest){
		
		//获取实验，如果没有配置实验直接返回match数据
		//获取实验中配置rank，如果没有直接返回match数据
		//根据rankName获取实体类，执行rank
		
		ApiResult<RankResponse> apiResult = initRankResponse(rankRequest);
		
		// rank请求数据 key = dataSource-expId
		Map<String, List<TotalTemplateInfo>> matchData = apiResult.getData().getRankResult();
//		log.info("调试日志{}",JSONObject.toJSONString(matchData));
		if (matchData == null || matchData.isEmpty()) {
			log.error("[rank请求失败]-[rankRequest无数据,请查看模板配置是否正确]");
			return apiResult;
		}
		
		List<TraceDetail> traceList = new ArrayList<TraceDetail>();
		
		String uuid = rankRequest.getUuid();
		String uid = rankRequest.getUid();
		
		// 获取rank进行排序
		try {
			for (Entry<String, List<TotalTemplateInfo>> entry : matchData.entrySet()) {
				String expId = "";
				String topicId = "";
				try {
					String[] split = entry.getKey().split(CommonConstants.SPLIT_COLON);
					String datasource_expId = split[1];
					expId = datasource_expId.split(CommonConstants.SPLIT_LINE)[1];
					topicId = datasource_expId.split(CommonConstants.SPLIT_LINE)[0];
				} catch (Exception e) {
					log.error("rank接收参数key不符合规定，key={}",entry.getKey());
					break;
				}
				//根据expId查询具体的实验域
				ExpRequest expRequest = ExpRequest.builder().expId(expId).uuid(uuid).tid(topicId).layerName(CommonConstants.LAYER_NAME_RANK).build();
				Expirement exp = expirementSpace.getExpirement(expRequest);
				if (exp!=null&&exp.getConfList()!=null&&exp.getConfList().size()>0) {
					List<AlgorithmConf> confList = exp.getConfList();
					AlgorithmConf algorithmConf = confList.get(0); 
					String rankName = algorithmConf.getName();
					String dataNum = algorithmConf.getExpNum();

					RecommendRank recommendRank = getRecommendRank(rankRequest,rankName);

					RankRequest request = RankRequest.builder().dataNum(dataNum).rankName(rankName).
							uuid(uuid).oriData(entry.getValue()).topicId(topicId).uid(uid).
							sortType(rankRequest.getSortType()).sortValue(rankRequest.getSortValue()).
							upcUserType(rankRequest.getUpcUserType()).siteId(rankRequest.getSiteId())
							.frontendCategoryId(rankRequest.getFrontendCategoryId()).categoryIds(rankRequest.getCategoryIds()).
							build();
					List<TotalTemplateInfo> executeRank = recommendRank.executeRecommend(request);
					apiResult.getData().getRankResult().put(entry.getKey(), executeRank);
					
					//埋点
					TraceDetail trace = trace(exp, rankName, dataNum);
					traceList.add(trace);
				}
			}
			apiResult.getData().setTraceDetails(traceList);
		} catch (Exception e) {
			log.error("rank中心未知错误",e);
		}
		return apiResult;
	}


	private TraceDetail trace(Expirement exp, String rankName, String dataNum) {
		TraceDetail traceDetail = TraceLogUtil.initAid();
		traceDetail.setExpId(exp.getExpId());
		traceDetail.getKeys().add(rankName+"_"+dataNum);
		return traceDetail;
	}
	

	private ApiResult<RankResponse> initRankResponse(RecommendRankRequest rankRequest) {
		ApiResult<RankResponse> apiResult = new ApiResult<RankResponse>();
		RankResponse rankResponse = new RankResponse();

		rankResponse.setRankResult(rankRequest.getMatchData());
		apiResult.setData(rankResponse);
		apiResult.setSuccess(ErrorCode.SUCCESS_CODE);
		return apiResult;
	}
	
	
	private RecommendRank getRecommendRank(RecommendRankRequest rankRequest,String rankName){
		RecommendRank recommendRank = null;
		String sortType = rankRequest.getSortType();
		
		if (StringUtils.isNotBlank(sortType)&&!sortType.equals(SortTypeEnum.ALL.getType())) {
			// 根据sortType选择rank
			SortTypeEnum sortTypeEnum = SortTypeEnum.getSortTypeEnumByType(sortType);
			if (sortTypeEnum == null) {
				recommendRank = ApplicationContextProvider.getApplicationContext().getBean(rankName,RecommendRank.class);
			}else{
				recommendRank = ApplicationContextProvider.getApplicationContext().getBean(sortTypeEnum.getRankName(),RecommendRank.class);
			}
			
		}else{
			recommendRank = ApplicationContextProvider.getApplicationContext().getBean(rankName,RecommendRank.class);
		}
		
		return recommendRank;
	}

	/**
	 * 新实验rank
	 * @param request
	 * @return
	 */
	public ApiResult<RankResponse2> productRank(RankRequest2 request){
		ApiResult<RankResponse2> result = new ApiResult<>();
		RankResponse2 rankResponse2 = new RankResponse2();

		String rankName = request.getRankName();
		if(StringUtils.isNotBlank(rankName)){
			List<RankItem2> rankItem2 = rankByRankName(request);
			rankResponse2.setRankItem2List(rankItem2);
			rankResponse2.setExpId("");
		}else{
			rankResponse2 = dealRankByExpConf(request);
		}

		if(CollectionUtils.isEmpty(rankResponse2.getRankItem2List())){
			result.setSuccess(ErrorCode.SYSTEM_ERROR_CODE);
			result.setError("productRank失败");
		}else{
			result.setSuccess(ErrorCode.SUCCESS_CODE);
			result.setError("productRank成功");
		}
		result.setData(rankResponse2);
		return result;
	}

	/**
	 * 通过实验配置处理Rank
	 * @param request
	 * @return
	 */
	private RankResponse2 dealRankByExpConf(RankRequest2 request){
		RankResponse2 rankResponse2 = new RankResponse2();
		String expId = "";
		List<RankItem2> rankItem2 = null;
		try {
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
			rankExperimentSpace.divert(baseRequest2);
			HashMap<String, String> flags = baseRequest2.getFlags();
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
			//如果是分类页rank
			if (BizNameConst.CATEGORY.equals(request.getBizName())) {
				//构造入参
				String rankName = flags.getOrDefault(RankExpConst.FLAG_CATEGORY_RANK_NAME, RankNameConstants.CATEGORY_ALGORITHM_RANK);
				request.setDataNum(flags.getOrDefault(RankExpConst.FALG_ALGORITHM_DATA_NUM, RankExpConst.VALUE_DEFAULT_ALGORITHM_DATA_NUM));
				request.setRankName(rankName);
				rankItem2 = rankByRankName(request);
				//类目排序考虑其他排序因子
				rankItem2 = cateGoryRankByFactorService.rank(rankItem2,request);
			}
			//排序出现异常则走默认rank
			if(CollectionUtils.isEmpty(rankItem2)){
				rankItem2 = defaultRank2.rank(request);
			}
		}catch(Exception e){
			log.error("[严重异常]dealRankByExpConf， request {}", JSON.toJSONString(request), e);
			rankItem2 = defaultRank2.rank(request);
		}
		rankResponse2.setExpId(expId);
		rankResponse2.setRankItem2List(rankItem2);
		return rankResponse2;
	}
	/**
	 * 通过rankName调用对应的rank返回结果
	 * @param request
	 * @return
	 */
	private List<RankItem2> rankByRankName(RankRequest2 request){
		List<RankItem2> rankItemList;
		String rankName = request.getRankName();
		try {
			Rank2 rank = ApplicationContextProvider.getApplicationContext().getBean(rankName, Rank2.class);
			rankItemList = rank.rank(request);

		}catch(Exception e){
			log.error("[严重异常]rankByRankName方法，rankName {}", rankName, e);
			rankItemList = defaultRank2.rank(request);
		}
		return rankItemList;
	}
	
}