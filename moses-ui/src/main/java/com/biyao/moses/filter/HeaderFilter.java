package com.biyao.moses.filter;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.alibaba.fastjson.TypeReference;
import com.biyao.moses.common.constant.CommonConstants;
import com.biyao.moses.common.constant.UPCUserTypeConstants;
import com.biyao.moses.rpc.PushTokenService;
import com.biyao.moses.rpc.UcRpcService;
import com.biyao.moses.util.*;
import com.biyao.upc.dubbo.client.business.toc.IBusinessTocDubboService;
import com.biyao.upc.dubbo.dto.VisitorInfoDTO;
import com.biyao.upc.dubbo.param.business.VisitorInfoParam;
import com.uc.domain.bean.User;
import com.uc.domain.constant.UserFieldConstants;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.IOUtils;
import com.alibaba.fastjson.JSONObject;
import com.biyao.mac.client.redbag.shop.privilegebag.dto.ShowPrivilegeLogoResultDto;
import com.biyao.moses.common.constant.HeaderString;
import com.biyao.moses.context.ByUser;
import com.biyao.moses.context.UserContext;
import com.biyao.moses.dto.trace.TraceDetail;
import com.biyao.moses.params.UIBaseBody;
import com.biyao.search.common.enums.PlatformEnum;

/**
 * 
 * @Description
 * @Date 2018年9月27日
 */
@Slf4j
public class HeaderFilter extends OncePerRequestFilter {

	private static final String UID_DEFAULT_VALUE= "0";

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		
		//缓存初始化时绕过filter，自己初始化user对象
		if (UserContext.getUser()!=null) {
			filterChain.doFilter(request, response);
		}
		String uid = request.getHeader(HeaderString.UID);
		String uuid = request.getHeader(HeaderString.UUID);

		ByUser byUser = new ByUser();
		byUser.setUuid(uuid);
		//uid默认设置为0
		byUser.setUid(UID_DEFAULT_VALUE);
		if(StringUtils.isNotBlank(uid)) {
			try{
				Integer.valueOf(uid);
				byUser.setUid(uid);
			}catch(Exception e){
				log.error("[严重异常][HeadFilter]解析用户uid异常，uid={} ", uid, e);
			}
		}
		byUser.setSex(getUserSexFromUc(byUser.getUid(),uuid));
		byUser.setPersonalizedRecommendSwitch(ApplicationContextProvider.getBean(PartitionUtil.class).getPersonalizedRecommendSwitch(byUser.getUid()));
		String avn = request.getHeader(HeaderString.AVN);
		if (StringUtils.isEmpty(avn)) {
			byUser.setAvn("1");
		}else{
			byUser.setAvn(avn);
		}
		byUser.setDevice(request.getHeader(HeaderString.DEVICE));
		byUser.setDid(request.getHeader(HeaderString.DID));
		byUser.setPf(request.getHeader(HeaderString.PF));	
		byUser.setSiteId(request.getHeader(HeaderString.SITEID));
		//如果siteId为空 或者传错  siteId赋值为空字符串  Platform默认为M站
		if(StringUtils.isNotBlank(byUser.getSiteId()) && CommonConstants.SITEID.contains(byUser.getSiteId().trim())) {
			byUser.setPlatform(PlatformEnumUtil.getPlatformEnumBySiteId(Integer.valueOf(byUser.getSiteId().trim())));
		}else {
			byUser.setSiteId("");
			byUser.setPlatform(PlatformEnum.M);
			log.error("[严重异常][HeadFilter]siteId参数格式错误 siteId：{},uuid：{}", byUser.getSiteId(),byUser.getUuid());
		}
		String ctp = request.getHeader(HeaderString.CTP);
		if (StringUtils.isNotBlank(ctp)) {
			byUser.setCtp(URL.decode(ctp));
			try {
				HashMap<String, String> ctpMap = JSONObject.parseObject(byUser.getCtp(), new TypeReference<HashMap<String,String>>(){});
				byUser.setP(ctpMap.get(HeaderString.P));
				String pvid = ctpMap.get(HeaderString.PVID);
				if (StringUtils.isNotBlank(pvid)) {
					byUser.setPvid(pvid);
				}
				String lat = ctpMap.get(HeaderString.LAT);
				String lng = ctpMap.get(HeaderString.LNG);
				byUser.setLat(lat);
				byUser.setLng(lng);
				
			} catch (Exception e) {
				log.error("[严重异常][HeadFilter]解析ctp失败,ctp={} ", ctp, e);
			}
		}

		if(!request.getServletPath().startsWith("/recommend/common")) {
			// 确保pvid一定不为空
			if (StringUtils.isBlank(byUser.getPvid())) {
				byUser.setPvid(IdCalculateUtil.createUniqueId());
			}
		}
		
		String stp = request.getHeader(HeaderString.STP);
		
		ConcurrentMap<String, String> aidMap = new ConcurrentHashMap<>();
		if (StringUtils.isNotBlank(stp)) {
			byUser.setStp(URL.decode(stp));
			//解析 aidMap
			try {
				HashMap<String, String> stpMap = JSONObject.parseObject(byUser.getStp(), new TypeReference<HashMap<String,String>>(){});
				if (stpMap != null && !stpMap.isEmpty()) {
					String aidJson = stpMap.get("aid");
					if (StringUtils.isNotBlank(aidJson)) {
						aidMap = JSONObject.parseObject(aidJson, new TypeReference<ConcurrentMap<String,String>>(){});
					}
					if (stpMap.containsKey("ppids")) {
						String priorityProductId = stpMap.get("ppids");
						byUser.setPriorityProductId(priorityProductId);
					}
				}
			} catch (Exception e) {
				log.error("[严重异常][HeadFilter]解析stp失败, stp={} ", byUser.getStp(), e);
			}
		}
		byUser.setAidMap(aidMap);
		byUser.setUpcUserType(getUpcUserType(uuid,uid));
		ProductDetailUtil productDetailUtil = ApplicationContextProvider.getBean(ProductDetailUtil.class);

		if(!StringUtils.isBlank(uid)||!"0".equals(uid)){
			// 用户是否拥有特权金
			ShowPrivilegeLogoResultDto userHasPrivilege = productDetailUtil.isUserHasPrivilege(byUser.getUid(),
					byUser.getPlatform().getName());

			if (!PlatformEnum.M.getName().equals(byUser.getPlatform().getName())
					&& !PlatformEnum.PC.getName().equals(byUser.getPlatform().getName())) {
				byUser.setUserHasPrivilege(userHasPrivilege);
			}
		}
		byUser.setTrackMap(new ConcurrentHashMap<>());
		byUser.setSessionId(IdCalculateUtil.createUniqueId());
		String requestBody = "";
		try {
			BufferedReader reader = request.getReader();
			requestBody = IOUtils.read(reader);
			if (StringUtils.isNotBlank(requestBody)) {
				UIBaseBody uiBaseBody = JSONObject.parseObject(requestBody, UIBaseBody.class);
				byUser.setUiBaseBody(uiBaseBody);
			}
		} catch (Exception e) {
			log.error("[严重异常][HeadFilter]解析请求体失败, requestBody={} ", requestBody, e);
			e.printStackTrace();
		}
		
		try (UserContext userContext = new UserContext(byUser)) {
			filterChain.doFilter(request, response);
		}
	}


	/**
	 * 获取用户类型
	 * @param uuid
	 * @return
	 */
	private Integer getUpcUserType(String uuid,String uid) {
		Integer result = UPCUserTypeConstants.NEW_VISITOR; // 默认新访客
		if (StringUtils.isBlank(uuid)){
			return result;
		}
		try {
			VisitorInfoParam vi = new VisitorInfoParam();
			vi.setCallSysName("moses.biyao.com");

			if(StringUtils.isNotBlank(uid) && Long.parseLong(uid) > 0){
				vi.setCustomerId(Long.parseLong(uid));
			}else {
				vi.setCustomerId(null);
			}
			vi.setUuid(uuid);
			IBusinessTocDubboService businessTocDubboService = ApplicationContextProvider.getBean(IBusinessTocDubboService.class);
			com.biyao.bsrd.common.client.model.result.Result<VisitorInfoDTO> visitorInfo =
					businessTocDubboService.getVisitorInfo(vi);
			if(visitorInfo != null && visitorInfo.getObj() != null ){
			    if (!visitorInfo.getObj().isMatch()){ // 老客
                    result = UPCUserTypeConstants.CUSTOMER;
                }else if (visitorInfo.getObj().getVisitorType() == 1){ // 老访客
                    result = UPCUserTypeConstants.OLD_VISITOR;
                }
			}
		} catch (Exception e) {
			log.error("调用upc接口查询用户身份出错",e);
		}

		return result;
	}

	/**
	 * 从UC中获取用户性别
	 * @param uid
	 * @param uuid
	 * @return
	 */
	private String getUserSexFromUc(String uid,String uuid) {
		String result = CommonConstants.UNKNOWN_SEX;
		if(UID_DEFAULT_VALUE.equals(uid) && StringUtils.isBlank(uuid)){
			log.error("[严重异常]headerFilter获取用户性别，uuid 和 uid都为空");
			return result;
		}
		try {
			UcRpcService ucRpcService = ApplicationContextProvider.getBean(UcRpcService.class);
			List<String> fields = new ArrayList<>();
			fields.add(UserFieldConstants.SEX);
			String uidParam = UID_DEFAULT_VALUE.equals(uid) ? null : uid;
			User user = ucRpcService.getData(uuid, uidParam, fields, "moses");
			if (user != null && user.getSex() != null) {
				result = user.getSex().toString();
			}
		}catch (Exception e){
			log.error("[严重异常]获取用户性别异常， uuid {}, uid {}, e ", uuid, uid, e);
		}
		return result;
	}

	/**
	 * 获取用户个性化推荐设置开关状态
	 * @param uid
	 * @return
	 */
}