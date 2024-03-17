package com.biyao.search.ui.remote.request;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Date;

import javax.ws.rs.HeaderParam;

import com.alibaba.fastjson.JSONObject;
import com.biyao.experiment.ExperimentRequest;
import com.biyao.search.common.enums.PlatformEnum;
import com.biyao.search.ui.util.IPUtil;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author biyao
 */
public class UIBaseRequest extends ExperimentRequest{

	private Logger logger = LoggerFactory.getLogger(getClass());

	@HeaderParam("platform")
	protected String platformStr;
	
	@HeaderParam("uuid")
	protected String uuid;
	
	@HeaderParam("uid")
	protected String uidStr;
	
	@HeaderParam("appVersion")
	protected String appVersion;
	
	@HeaderParam("numVersion")
	protected String appVersionNumStr;
	
	@HeaderParam("deviceType")
	protected String device;
	
	@HeaderParam("ctp")
	protected String ctp;

	@HeaderParam("stp")
	protected String stp;

	@HeaderParam("sch")
	protected String sch;
	/**
	 * 临时参数，将转存赋值到appVersion中
	 */
	@HeaderParam("appversion")
	protected String miniappVersion;
	
	protected PlatformEnum platform;
	protected Integer uid;
	protected Integer appVersionNum;
	protected String ip;
	
	protected String siteId = "";
	protected String pageId = "";
	protected String pvid = "";

	public String getPlatformStr() {
		return platformStr;
	}

	public void setPlatformStr(String platformStr) {
		this.platformStr = platformStr;
	}

	public String getCtp() {
		return ctp;
	}

	public void setCtp(String ctp) {
		this.ctp = ctp;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getUidStr() {
		return uidStr;
	}

	public String getMiniappVersion() {
		return miniappVersion;
	}

	public void setMiniappVersion(String miniappVersion) {
		this.miniappVersion = miniappVersion;
	}

	public void setUidStr(String uidStr) {
		this.uidStr = uidStr;
	}

	public String getAppVersion() {
		return appVersion;
	}

	public void setAppVersion(String appVersion) {
		this.appVersion = appVersion;
	}

	public String getAppVersionNumStr() {
		return appVersionNumStr;
	}

	public void setAppVersionNumStr(String appVersionNumStr) {
		this.appVersionNumStr = appVersionNumStr;
	}

	public String getDevice() {
		return device;
	}

	public void setDevice(String device) {
		this.device = device;
	}

	public PlatformEnum getPlatform() {
		return platform;
	}

	public void setPlatform(PlatformEnum platform) {
		this.platform = platform;
	}

	public Integer getUid() {
		return uid;
	}

	public void setUid(Integer uid) {
		this.uid = uid;
	}

	public Integer getAppVersionNum() {
		return appVersionNum;
	}

	public void setAppVersionNum(Integer appVersionNum) {
		this.appVersionNum = appVersionNum;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getSiteId() {
		return siteId;
	}

	public void setSiteId(String siteId) {
		this.siteId = siteId;
	}

	public String getPageId() {
		return pageId;
	}

	public void setPageId(String pageId) {
		this.pageId = pageId;
	}

	public String getPvid() {
		return pvid;
	}

	public void setPvid(String pvid) {
		this.pvid = pvid;
	}

	public String getStp() {
		return stp;
	}

	public void setStp(String stp) {
		this.stp = stp;
	}

	public String getSch() {
		return sch;
	}

	public void setSch(String sch) {
		this.sch = sch;
	}
	@Override
	public Date getExperimentRequestTime() {
		return new Date();
	}

	@Override
	public String getExperimentUuid() {
		return this.uuid;
	}

	protected void preHandleParam() {
		/* 基本参数类型转换 */
		this.platform = PlatformEnum.getByName(platformStr.toLowerCase());
		this.appVersionNum = Strings.isNullOrEmpty(appVersionNumStr) ? null : Integer.valueOf(appVersionNumStr);
		this.uid = Strings.isNullOrEmpty(uidStr) ? null : Integer.valueOf(uidStr);
		this.ip = IPUtil.getRemoteIp();
		
		if (PlatformEnum.MINI.getName().equals(this.platform.getName()) 
				&& !Strings.isNullOrEmpty(miniappVersion)) {
			this.appVersion = miniappVersion;
		}
		
		if (!Strings.isNullOrEmpty(this.ctp)) {
			JSONObject ctpJson = JSONObject.parseObject(this.ctp);
			this.siteId = ctpJson.getString("stid");
			this.pageId = ctpJson.getString("p");
			this.pvid = ctpJson.getString("pvid");
		}
		
		this.siteId = Strings.isNullOrEmpty(siteId) ? "" : siteId;
		this.pageId = Strings.isNullOrEmpty(pageId) ? "" : pageId;
		this.pvid = Strings.isNullOrEmpty(pvid) ? "" : pvid;

		// TODO 搜索这儿的pageId，先写死。由于M站的ctp里的pageId传递有点问题
		if (Strings.isNullOrEmpty(this.siteId)) {
			// 结果页
			this.pageId = siteId + "-100002-500009";
		}
		try {
			this.stp = Strings.isNullOrEmpty(stp)? "" : URLDecoder.decode(stp,"utf-8");
		} catch (UnsupportedEncodingException e) {
			logger.error("[严重异常]stp decode error, stp is {}, error message is {}",stp,e);
		}
	}
	
	protected boolean checkParameter() {
		if (Strings.isNullOrEmpty(platformStr) || Strings.isNullOrEmpty(uuid)) {
			return false;
		}
		// 校验app数字版本号
		boolean validAppNumberVersion = true;
		if (PlatformEnum.IOS.getName().equalsIgnoreCase(platformStr) || PlatformEnum.ANDROID.getName().equalsIgnoreCase(platformStr)){
			validAppNumberVersion = !Strings.isNullOrEmpty(appVersionNumStr);
		}

		if (!validAppNumberVersion){
			return false;
		}

		return true;
	}
}
