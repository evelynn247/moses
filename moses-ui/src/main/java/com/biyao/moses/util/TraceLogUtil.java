package com.biyao.moses.util;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;

import com.alibaba.fastjson.JSONObject;
import com.biyao.moses.common.enums.SourceEnum;
import com.biyao.moses.constants.CommonConstants;
import com.biyao.moses.context.ByUser;
import com.biyao.moses.dto.trace.TraceDetail;
import com.biyao.moses.model.template.entity.RecommendPage;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.params.ApiResult;
import com.biyao.moses.service.imp.HomePageCacheServiceImpl;

/**
 * 埋点日志工具类
 * 
 */
@Slf4j
public class TraceLogUtil {
	/**
	 * 初始化针对于当前请求的每个实验的追踪aid
	 * @return
	 */
//	public static TraceDetail initAid(ByUser user) {
//		String aid = IdCalculateUtil.createUniqueId();
//		Map<String, TraceDetail> trackMap = user.getTrackMap();
//		TraceDetail traceDetail = new TraceDetail();
//		HashSet<String> keys = new HashSet<String>();
//		traceDetail.setKeys(keys);
//		trackMap.put(aid, traceDetail);
//		
//		return traceDetail;
//	}

	/**
	 * 根据dataSource+expId获取aid
	 * trackMap 由match处理数据时保存到本地线程中使用
	 * @param trackMap
	 * @param dataexpId
	 * @return
	 */
	public static String getAidByDataSourceExpId(Map<String, TraceDetail> trackMap,String dataexpId){
		
		String result = "";
		if (trackMap == null || trackMap.isEmpty()) {
			return result;
		}
		for (Entry<String, TraceDetail> entry : trackMap.entrySet()) {
			TraceDetail td = entry.getValue();
			if (td.getExpId().equals(dataexpId)) {
				result = entry.getKey();
				break;
			}
		}
		return result;
	}
	
	
	/**
	 * 处理当前数据的stp参数，既添加对应的rcd放入aid的map中
	 * aidMap 存放在本地线程中，是从request中解析出来的
	 * 除了rcd外，别的数据都是相同的
	 * @param stp
	 * @param dataexpId
	 */
	public static String resetStpAid(Map<String, String> stp,String dataexpId,ByUser user,int loc){
		String aid = "";
		try {
			ConcurrentMap<String,TraceDetail> trackMap = user.getTrackMap();
			String spm = stp.get(CommonConstants.SPM);
			if (StringUtils.isNotBlank(spm)) {
				if(spm.split("\\.").length>3) {
				if (spm.contains("|")&&spm.lastIndexOf("|")!=spm.indexOf("|")) {
					spm = spm.substring(0, spm.lastIndexOf("|"));
				}
				spm = spm + CommonConstants.SPLIT_PIPE + loc;
				stp.put(CommonConstants.SPM, spm);
				}
			}

			for (Entry<String, TraceDetail> entry : trackMap.entrySet()) {
				TraceDetail traceDetail = entry.getValue();
				if (traceDetail.getExpId().equals(dataexpId)) {
					aid = entry.getKey();
				}
			}
			
			HomePageCacheServiceImpl homePageCacheServiceImpl = ApplicationContextProvider.getBean(HomePageCacheServiceImpl.class);
			ApiResult<RecommendPage> apiResult = homePageCacheServiceImpl.getPageIdCache(SourceEnum.HP.getSource(), "1", user.getPlatform().getName());
			
			String key = "rcd";
			if (apiResult!=null&&apiResult.getData()!=null) {
				String pid = apiResult.getData().getPid();
				if (user.getPageId().equals(pid)) {
					key = "home";
				}
			}
			
			Map<String, String> aidMap = user.getAidMap();
			aidMap.put(key, aid);
			stp.put("aid", JSONObject.toJSONString(aidMap));
		} catch (Exception e) {
			log.error("[严重异常]处理aid时出现异常 ",e);
		}
		return aid;
	}
	
//	public static String resetStpAid(Map<String, String> stp,String dataexpId,ByUser user,String topicId,int loc){
//		
//		String spm = stp.get(CommonConstants.SPM);
//		if (StringUtils.isNotBlank(spm)) {
//			spm = spm + CommonConstants.SPLIT_PIPE + loc + CommonConstants.SPLIT_PIPE +topicId;
//			stp.put(CommonConstants.SPM, spm);
//		}
//		
//		
//		ConcurrentMap<String,TraceDetail> trackMap = user.getTrackMap();
//		String aid = "";
//		for (Entry<String, TraceDetail> entry : trackMap.entrySet()) {
//			TraceDetail traceDetail = entry.getValue();
//			if (traceDetail.getExpId().equals(dataexpId)) {
//				aid = entry.getKey();
//			}
//		}
//		Map<String, String> aidMap = user.getAidMap();
//		aidMap.put("rcd", aid);
//		stp.put("aid", JSONObject.toJSONString(aidMap));
//		return aid;
//	}
	
	
	/**
	 * 处理当前数据的stp参数，既添加对应的rcd放入aid的map中
	 * aidMap 存放在本地线程中，是从request中解析出来的
	 * 除了rcd外，别的数据都是相同的
	 * @param aid
	 * @param subList
	 */
//	public static String resetStpAid(Map<String, String> stp,String key,ByUser user){
//		String aid = getAidByDataSourceExpId(user.getTrackMap(),key);
//		Map<String, String> aidMap = user.getAidMap();
//		aidMap.put("rcd", aid);
//		stp.put("aid", JSONObject.toJSONString(aidMap));
//		return aid;
//	}
	
	public static void fillPids(String aid,List<TotalTemplateInfo> subList,ByUser user){
//		log.info("填充商品数据user={}",JSONObject.toJSONString(user));
		try {
			String pids = "";
			StringBuilder scms = new StringBuilder();
			TraceDetail traceDetail = user.getTrackMap().get(aid);
			if (subList!=null&&subList.size()>0&&StringUtils.isNotBlank(subList.get(0).getId())) {
				for (int i = 0; i < subList.size(); i++) {
					TotalTemplateInfo totalTemplateInfo = subList.get(i);
					String id = totalTemplateInfo.getId();
					String expId = totalTemplateInfo.getExpId();
					String source = totalTemplateInfo.getSource();
					if (StringUtils.isNotBlank(id)) {
						pids+=id+",";
						if(StringUtils.isNotBlank(expId) && StringUtils.isNotBlank(source)) {
							scms.append("moses.").append(source).append(".").append(expId).append(".,");
						}
					}
				}
			}

			if (traceDetail!=null) {
				String pids2 = traceDetail.getPids();
				if (StringUtils.isNotEmpty(pids2)) {
					pids = pids2 + pids;
				}
				traceDetail.setPids(pids);
				traceDetail.setScms(scms.toString());
			}
		} catch (Exception e) {
			log.error("[严重异常]trace中添加pids和scms信息时出现异常，uuid {} ", user.getUuid(), e);
		}
	}
	
}