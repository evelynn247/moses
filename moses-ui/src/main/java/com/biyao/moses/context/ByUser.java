package com.biyao.moses.context;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;

import com.biyao.moses.common.enums.ExceptionTypeEnum;
import com.biyao.moses.params.AdvertInfo;
import com.biyao.moses.params.match2.MatchResponse2;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;

import com.biyao.mac.client.redbag.shop.privilegebag.dto.ShowPrivilegeLogoResultDto;
import com.biyao.moses.dto.trace.TraceDetail;
import com.biyao.moses.params.UIBaseBody;
import com.biyao.search.common.enums.PlatformEnum;

import static com.biyao.moses.constants.CommonConstants.NOT_SUPPORT_SHOW_PRIVILEGE;


/**
 * 
 * @Description
 * @Date 2018年9月27日
 */
@Setter
@Getter
@Slf4j
public class ByUser {
	
	private String sessionId;

	private String uuid;

	private String uid;

	private String siteId;

	private String device;

	private String avn;

	private String pvid;

	private String did;
	
	private PlatformEnum platform;
	// 前端传递 用户追踪数据
	private String ctp;
	//从ctp中解析出来的点位
	private String p;
	// 服务器端 用户追踪数据
	private String stp;
	//渠道，字符串类型，ios，Android
	private String pf;
	// 商品信息缓存
//	private ConcurrentMap<Long, ProductInfo> localProductInfo;
	
	// 用户是否拥有特权金
	ShowPrivilegeLogoResultDto userHasPrivilege;
	
	// 20181129 houkun match服务器追踪参数  <aid,TraceDetail>
	private ConcurrentMap<String,TraceDetail> trackMap = new ConcurrentHashMap<>();
	
	// 20181129 houkun rank服务器追踪参数  <aid,TraceDetail>
	private ConcurrentMap<String,TraceDetail> rankTrackMap = new ConcurrentHashMap<>();
	
	
	// 本次请求解析出的aid
	private ConcurrentMap<String,String> aidMap;
	
	private String pageId;

//	private Boolean isNewUser;
	
	private String lat;
	private String lng;
	
	private UIBaseBody uiBaseBody;

	// 本次请求中，需要展示在feed流最前面的商品ID
	private String priorityProductId;
	/**
	 * upc 用户类型
	 */
	private Integer upcUserType;

	//性别
	private String sex;

	//是否走新实验系统
	private boolean isNewExp = false;

	//是否走规则引擎系统
	private boolean isNewRule = false;

	// 是否匹配到了感兴趣商品集的规则或者实验
	private boolean isMatchGxqspRuleOrExp = false;
	/**
	 * 用户最近深度浏览的时间
	 */
	private Long latestViewTime;


	//日志debug开关，scm白名单中的用户打开该日志开关
	private boolean debug = false;
	//是否为推荐测试效果访问
	private boolean test = false;

	/**
	 * 个性化推荐设置开关。
	 * true表示开关打开，可以使用用户个性化数据做推荐
	 * false表示开关关闭，不可以使用用户个性化数据做推荐
	 */
	private boolean personalizedRecommendSwitch = true;

	/**
	 * 展示广告入口
	 * "1":表示需要展示x元购活动广告入口，"0"：不需要展示活动广告入口。
	 */
	private String showAdvert;

	/**
	 * 页面版本，用于判断使用哪种模板返回数据
	 * 当值为”1.0”时，则按本次新增的模板返回数据。否则按原来模板返回数据。
	 * 兼容当pageVersion为空时，则按原来模板返回数据。
	 */
	private String pageVersion;

	/**
	 * 网关传入的可以展示的活动信息列表
	 */
	private List<AdvertInfo> advertInfoList;

	/**
	 * 追踪本次处理时发生的异常信息
	 */
	private ConcurrentMap<String, ExceptionTypeEnum> exceptionTypeMap = new ConcurrentHashMap<>();

	/**
	 * 异步获取match的结果
	 */
	private Future<MatchResponse2> asyncMatchResponse;

	/**
	 * 是否展示特权金标签
	 */
	private Boolean isShowPrivilege = true;

	/**
	 * 校验user
	 */
	public boolean valid() {
		if (StringUtils.isBlank(this.getUuid())) {
			log.error("[用户校验]-[uuid为空]");
			return false;
		}
		if (StringUtils.isBlank(this.getSiteId())) {
			log.error("[用户校验]-[siteId为空]");
			return false;
		}
		try {
			Integer siteid = Integer.valueOf(this.getSiteId());
			if (!validPlatform(siteid)) {
				log.error("[用户校验]-[siteId非法]-[siteId={}]", siteid);
				return false;
			}
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	/**
	 * 校验siteId
	 * 
	 * @param siteId
	 * @return
	 */
	private boolean validPlatform(Integer siteId) {
		// 本期先这么做  4 小程序A  5 小程序B 后期在优化吧
		if(siteId==4 ||siteId==5){
		   return true;
		}
		PlatformEnum[] platformEnums = PlatformEnum.values();
		for (PlatformEnum platformEnum : platformEnums) {
			if (platformEnum.getNum().equals(siteId)) {
				return true;
			}
		}
		return false;
	}


	public  Boolean isShowPrivilege(String tpoicId){

		if(StringUtils.isEmpty(tpoicId)){
			return true;
		}
		return  !NOT_SUPPORT_SHOW_PRIVILEGE.contains(tpoicId);
	}

}