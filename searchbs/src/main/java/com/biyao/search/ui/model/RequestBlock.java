package com.biyao.search.ui.model;

import java.util.Date;

import com.biyao.experiment.ExperimentRequest;
import com.biyao.search.as.service.enums.OrderByEnum;
import com.biyao.search.as.service.model.response.ASSearchResponse;
import com.biyao.search.common.enums.SearchOrderByEnum;
import com.biyao.search.common.model.ASProduct;
import com.biyao.search.common.model.Status;
import com.biyao.search.ui.rest.request.UIRequest;
import com.biyao.search.ui.rest.response.UIResponse;
import com.google.common.base.Strings;

/**
 * UI各模块共享的数据
 */
public class RequestBlock extends ExperimentRequest{
	
	/** 搜索ID（32位字符串）。如果是新的搜索(第1页)，sid为空字符串；如果是分页（第2、3、4...页） */
	private String sid = ""; 
	
	/** true: 表示翻页；false表示新请求  */
    private boolean scroll = true;
    
    /** 请求词   */
    private String query = "";
    
    /** 分页，从1开始计数   */
    private Integer pageIndex = 1;
    
    /** 分页，目前取固定值20  */
    private Integer pageSize = 20;
    
    /** 用户uuid（用来唯一标识用户，包括未登录用户）  */
    private String uuid = "";
    
    /** 页面ID，刷新页面后重新生成pvid，32位随机字符串  */
    private String pvid = "";
    
    /** 前端用户行为标识符（前端产生的随机32位字符串、小写）  */
    private String ubid = "";
    
    /** android、 ios、mweb */
    private String platform = "";
    
    /** 用户uid(无用户登录时为空字符串)  */
    private Integer uid = 0;
    
    /** app版本号  */
    private String appVersion = "";
    
    /** app数字版本号  -1表示数字版本号不存在 */
    private Integer avn = -1;
    
    /** 设备类型  */
    private String device = "";
    
    /** 操作系统版本号  */
    private String osVersion = "";
    
    /** 设备宽度  */
    private String deviceWidth = "";
    
    /** 设备高度   */
    private String deviceHeight = "";
    
    /** 访客ip  */
    private String ip = "";
    
    /** 请求时间  */
    private Long requestTime = 0L;
    
    /** AS的返回结果  */
    private ASSearchResponse<ASProduct> asResponse = new ASSearchResponse<ASProduct>();
    
    /** AS请求消耗的时间 */
    private Integer asTookTime = 0;
    
    /** 最后一个模块的请求状态  */
    private Status status;
    
    private String queryFrom; // 搜索词来源  1-点击热词搜索  2-点击搜索历史搜索  3-输入框搜索
    
    private SearchOrderByEnum orderBy = SearchOrderByEnum.NORMAL; // 排序方式
    
    /** UI的返回结果  */
    private UIResponse uiResponse = new UIResponse();

	public RequestBlock() {
        super();
    }
	
	/**
	 * 使用前端直接传过来的请求参数初始化部分参数
	 */
	public void initWithUIRequest( UIRequest request ) {
	    sid = request.getSid();
	    query = request.getQuery();
	    
	    try {
	        pageIndex = Integer.valueOf(request.getPageIndex()) ;
        } catch (Exception e) {
            pageIndex = -1; //有问题
        }
	    
	    uuid = request.getUuid();
	    pvid = request.getPvid();
	    ubid = request.getUbid();
	    platform = request.getPlatform();
	    try {
	        uid = Integer.valueOf(request.getUid()) ;
        } catch (Exception e) {
            uid = -1; //有问题
        }
	    appVersion = request.getAppVersion();
	    device = request.getDevice();
	    osVersion = request.getOsVersion();
	    deviceWidth = request.getDeviceWidth();
	    deviceHeight = request.getDeviceHeight();
	    
	    try {
            avn = Integer.valueOf( request.getAvn() );
        } catch (Exception e) {
            avn = -1;   // M站、PC等、老的APP等
        }
	    
	    if (Strings.isNullOrEmpty(request.getQueryFrom())) {
	    	queryFrom = "";
	    } else {
	    	queryFrom = request.getQueryFrom();
	    }
	    
	    orderBy = SearchOrderByEnum.getBycode(request.getOrderBy());
	}

    public String getQueryFrom() {
		return queryFrom;
	}

	public void setQueryFrom(String queryFrom) {
		this.queryFrom = queryFrom;
	}

	/**
     * get、set方法
     */
    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Integer getPageIndex() {
        return pageIndex;
    }

    public void setPageIndex(Integer pageIndex) {
        this.pageIndex = pageIndex;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
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

    public Integer getUid() {
        return uid;
    }

    public void setUid(Integer uid) {
        this.uid = uid;
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

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Long getRequestTime() {
        return requestTime;
    }

    public void setRequestTime(Long requestTime) {
        this.requestTime = requestTime;
    }

    public boolean isScroll() {
        return scroll;
    }

    public void setScroll(boolean scroll) {
        this.scroll = scroll;
    }

    public ASSearchResponse<ASProduct> getAsResponse() {
        return asResponse;
    }

    public void setAsResponse(ASSearchResponse<ASProduct> asResponse) {
        this.asResponse = asResponse;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public UIResponse getUiResponse() {
        return uiResponse;
    }

    public void setUiResponse(UIResponse uiResponse) {
        this.uiResponse = uiResponse;
    }

    public Integer getAsTookTime() {
        return asTookTime;
    }

	public SearchOrderByEnum getOrderBy() {
		return orderBy;
	}

	public void setOrderBy(SearchOrderByEnum orderBy) {
		this.orderBy = orderBy;
	}

	public void setAsTookTime(Integer asTookTime) {
        this.asTookTime = asTookTime;
    }

    public Integer getAvn() {
        return avn;
    }

    public void setAvn(Integer avn) {
        this.avn = avn;
    }

	@Override
	public Date getExperimentRequestTime() {
		return new Date();
	}

	@Override
	public String getExperimentUuid() {
		return uuid;
	}
}
