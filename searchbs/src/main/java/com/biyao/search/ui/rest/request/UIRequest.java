package com.biyao.search.ui.rest.request;

import com.biyao.search.common.enums.SearchOrderByEnum;

/**
 * UI请求参数
 */
public class UIRequest {
	
    /** 搜索ID（32位字符串）。如果是新的搜索(第1页)，sid为空字符串；如果是分页（第2、3、4...页） */
	private String sid = "";
	
	/** 请求词 */
	private String query = "";
	
	/** 分页，从1开始计数 */
	private String pageIndex = "";
	
	/** 用户uuid（用来唯一标识用户，包括未登录用户） */
	private String uuid = "";
		
	/** 页面ID，刷新页面后重新生成pvid，32位随机字符串 */
	private String pvid = "";
	
	/** 前端用户行为标识符（前端产生的随机32位字符串、小写） */
	private String ubid = "";
	
	/** android、 ios、mweb */
	private String platform = "";
	
	/** 用户uid(无用户登录时为空字符串) */
	private String uid = "";
	
	/** app版本号 */
	private String appVersion = "";
	
	/** 设备 */
	private String device = "";
	
	/** 操作系统版本号  */
	private String osVersion = "";
	
	/** 设备宽度 */ 
	private String deviceWidth = "";
	
	/** 设备高度 */
	private String deviceHeight = "";
	
	/**
	 * app的数字版本号
	 */
	private String  avn;
	
	private String queryFrom; // 搜索词来源  1-点击热词搜索  2-点击搜索历史搜索  3-输入框搜索

	private String orderBy = SearchOrderByEnum.NORMAL.getCode();
	
    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public String getQuery() {
        return query;
    }

    public String getQueryFrom() {
		return queryFrom;
	}

	public String getOrderBy() {
		return orderBy;
	}

	public void setOrderBy(String orderBy) {
		this.orderBy = orderBy;
	}

	public void setQueryFrom(String queryFrom) {
		this.queryFrom = queryFrom;
	}

	public void setQuery(String query) {
        this.query = query;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getPvid() {
        return pvid;
    }

    public void setPvid(String pvid) {
        this.pvid = pvid;
    }

    public String getUbid() {
        return ubid;
    }

    public void setUbid(String ubid) {
        this.ubid = ubid;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    public String getDeviceWidth() {
        return deviceWidth;
    }

    public void setDeviceWidth(String deviceWidth) {
        this.deviceWidth = deviceWidth;
    }

    public String getDeviceHeight() {
        return deviceHeight;
    }

    public void setDeviceHeight(String deviceHeight) {
        this.deviceHeight = deviceHeight;
    }

    public String getPageIndex() {
        return pageIndex;
    }

    public void setPageIndex(String pageIndex) {
        this.pageIndex = pageIndex;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getAvn() {
        return avn;
    }

    public void setAvn(String avn) {
        this.avn = avn;
    }
}
