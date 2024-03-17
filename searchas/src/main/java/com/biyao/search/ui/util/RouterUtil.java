package com.biyao.search.ui.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.biyao.search.common.enums.PlatformEnum;
import com.biyao.search.ui.constant.CommonConstant;
import com.biyao.search.ui.constant.ERouterType;
import com.biyao.search.ui.enums.VModelTypeEnum;
import com.biyao.search.ui.remote.request.UISearchRequest;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static com.biyao.search.ui.constant.CommonConstant.ROUTE_2SEARCHPAGE_APP;
import static com.biyao.search.ui.constant.CommonConstant.ROUTE_2SEARCHPAGE_MINIAPP;
import static com.biyao.search.ui.constant.CommonConstant.ROUTE_2SEARCHPAGE_MWEB;

public class RouterUtil {

    private static final Logger logger = LoggerFactory.getLogger(RouterUtil.class);

    private static final Map<String, Map<String, String>> paramsRules;

    static {
        paramsRules = new HashMap<String, Map<String, String>>();
        //商家店铺页参数初始化
        Map<String, String> paramMap1 = new HashMap<String, String>();
        paramMap1.put("supplierId", "supplierId");
        paramsRules.put(PlatformEnum.MINI.getNum() + "_" + ERouterType.SUPPLIERPAGE.getNum(), paramMap1);
        paramsRules.put(PlatformEnum.M.getNum() + "_" + ERouterType.SUPPLIERPAGE.getNum(), paramMap1);
        Map<String, String> paramMap2 = new HashMap<String, String>();
        paramMap2.put("supplierId", "supplierID");
        paramMap2.put("suId", "suId");
        paramsRules.put(PlatformEnum.ANDROID.getNum() + "_" + ERouterType.SUPPLIERPAGE.getNum(), paramMap2);
        paramsRules.put(PlatformEnum.IOS.getNum() + "_" + ERouterType.SUPPLIERPAGE.getNum(), paramMap2);

        //专题详情页参数初始化
        Map<String, String> paramMap3 = new HashMap<String, String>();
        paramMap3.put("topicId", "specialId");
        paramsRules.put(PlatformEnum.MINI.getNum() + "_" + ERouterType.TOPICDETAIL.getNum(), paramMap3);
        Map<String, String> paramMap4 = new HashMap<String, String>();
        paramMap4.put("topicId", "topicId");
        paramsRules.put(PlatformEnum.M.getNum() + "_" + ERouterType.TOPICDETAIL.getNum(), paramMap4);
        paramsRules.put(PlatformEnum.ANDROID.getNum() + "_" + ERouterType.TOPICDETAIL.getNum(), paramMap4);
        paramsRules.put(PlatformEnum.IOS.getNum() + "_" + ERouterType.TOPICDETAIL.getNum(), paramMap4);

        //搜索中间页
        Map<String, String> paramMap5 = new HashMap<String, String>();
        paramMap5.put("sp", "sp");
        paramMap5.put("query", "query");
        paramsRules.put(PlatformEnum.MINI.getNum() + "_" + ERouterType.SEARCH.getNum(), paramMap5);
        Map<String, String> paramMap6 = new HashMap<String, String>();
        paramMap6.put("sp", "sp");
        paramsRules.put(PlatformEnum.M.getNum() + "_" + ERouterType.SEARCH.getNum(), paramMap6);
        paramsRules.put(PlatformEnum.ANDROID.getNum() + "_" + ERouterType.SEARCH.getNum(), paramMap6);
        paramsRules.put(PlatformEnum.IOS.getNum() + "_" + ERouterType.SEARCH.getNum(), paramMap6);
        
        //推荐中间页
        Map<String, String> recommendSiteParamMap = new HashMap<String, String>();
        recommendSiteParamMap.put("pageId", "pageId");
        recommendSiteParamMap.put("pageIndex", "pageIndex");
        recommendSiteParamMap.put("recTopicId", "topicId");
        paramsRules.put(PlatformEnum.M.getNum() + "_" + ERouterType.RECOMMEND.getNum(), recommendSiteParamMap);
        paramsRules.put(PlatformEnum.ANDROID.getNum() + "_" + ERouterType.RECOMMEND.getNum(), recommendSiteParamMap);
        paramsRules.put(PlatformEnum.IOS.getNum() + "_" + ERouterType.RECOMMEND.getNum(), recommendSiteParamMap);
        paramsRules.put(PlatformEnum.MINI.getNum() + "_" + ERouterType.RECOMMEND.getNum(), recommendSiteParamMap);
        
    }

    /**
     * 得到路由后的链接，无参数
     * @param platformEnum 平台类型
     * @param routerType   跳转类型
     * @return
     */
    public static String getRouterUrl(PlatformEnum platformEnum, ERouterType routerType){
        return getRouterUrl(platformEnum,routerType,null);
    }

    /**
     * 得到路由后的链接，有参数
     * @param platformEnum 平台类型
     * @param routerType   跳转类型
     * @param params       参数列表
     * @return
     */
    public static String getRouterUrl(PlatformEnum platformEnum, ERouterType routerType, Map<String, String> params) {
        String returnStr = "";
        switch (platformEnum) {
            case ANDROID:
            case IOS:
                switch (routerType) {
                    case SEARCH:
                        return getUrlWithParams(CommonConstant.ROUTE_2SEARCHPAGE_APP,platformEnum,routerType,params);
                    case DALIYPAGE:
                        return getUrlWithParams(CommonConstant.ROUTE_2NEW_APP,platformEnum,routerType,params);
                    case TOPICLIST:
                        return getUrlWithParams(CommonConstant.ROUTE_2TOPICLIST_APP,platformEnum,routerType,params);
                    case TOPICDETAIL:
                        return getUrlWithParams(CommonConstant.ROUTE_2TOPICDETAIL_APP,platformEnum,routerType,params);
                    case SUPPLIERPAGE:
                        return getUrlWithParams(CommonConstant.ROUTE_2SUPPLIERPAGE_APP,platformEnum,routerType,params);
                    case RECOMMEND:
                    	return getUrlWithParams(CommonConstant.ROUTE_2RECOMMEND_APP,platformEnum,routerType,params);
                    default:
                        return returnStr;
                }
            case M:
                switch (routerType) {
                    case SEARCH:
                        return getUrlWithParams(CommonConstant.HOME_ROUTE_2SEARCHPAGE_MWEB,platformEnum,routerType,params);
                    case DALIYPAGE:
                        return getUrlWithParams(CommonConstant.ROUTE_2NEW_MWEB,platformEnum,routerType,params);
                    case TOPICLIST:
                        return getUrlWithParams(CommonConstant.ROUTE_2TOPICLIST_MWEB,platformEnum,routerType,params);
                    case TOPICDETAIL:
                        return getUrlWithParams(CommonConstant.ROUTE_2TOPICDETAIL_MWEB,platformEnum,routerType,params);
                    case SUPPLIERPAGE:
                        return getUrlWithParams(CommonConstant.ROUTE_2SUPPLIERPAG_MWEB,platformEnum,routerType,params);
                    case RECOMMEND:
                    	return getUrlWithParams(CommonConstant.ROUTE_2RECOMMEND_MWEB,platformEnum,routerType,params);
                    default:
                        return returnStr;
                }
            case MINI:
                switch (routerType) {
                    case SEARCH:
                        return getUrlWithParams(CommonConstant.ROUTE_2SEARCHPAGE_MINIAPP,platformEnum,routerType,params);
                    case DALIYPAGE:
                        return getUrlWithParams(CommonConstant.ROUTE_2NEW_MINIAPP,platformEnum,routerType,params);
                    case TOPICLIST:
                        return returnStr;
                    case TOPICDETAIL:
                        return getUrlWithParams(CommonConstant.ROUTE_2TOPICDETAIL_MINIAPP,platformEnum,routerType,params);
                    case SUPPLIERPAGE:
                        return getUrlWithParams(CommonConstant.ROUTE_2SUPPLIERPAG_MINIAPP,platformEnum,routerType,params);
                    case RECOMMEND:
                    	return getUrlWithParams(CommonConstant.ROUTE_2RECOMMEND_MINIAPP,platformEnum,routerType,params);
                    default:
                        return returnStr;
                }
            default:
                return returnStr;
        }
    }

    /**
     * 拼装url参数
     * @param url url
     * @param platformEnum 平台类型
     * @param routerType   跳转类型
     * @param params       参数列表
     * @return 转化后的字符串
     */
    private static String getUrlWithParams(String url, PlatformEnum platformEnum, ERouterType routerType, Map<String, String> params) {
        try {
            if (params == null || params.isEmpty()) {
                return url+"?";
            }
            int platFormId = platformEnum.getNum();
            int routerTypeId = routerType.getNum();
            String key = platFormId + "_" + routerTypeId;
            Map<String, String> paramsMap = paramsRules.get(key);
            if(paramsMap==null||paramsMap.isEmpty()){
                return url+"?";
            }
            String paramsStr = "";
            Iterator<Map.Entry<String, String>> iterator = params.entrySet().iterator();
            while(iterator.hasNext()){
                Map.Entry<String, String> entry = iterator.next();
                String paramsKey = entry.getKey();
                String paramsValue = entry.getValue();
                if(paramsMap.get(paramsKey)!=null){
                    paramsStr+=paramsMap.get(paramsKey)+"="+paramsValue+"&";
                }
            }
            if(StringUtils.isBlank(paramsStr)){
                throw new Exception("params is error");
            }
            return url+"?"+paramsStr.substring(0,paramsStr.length()-1);
        } catch (Exception e) {
            logger.error("[严重异常]拼装url参数(RouterUtil#getUrlWithParams)时发生异常, url = {}, platformEnum = {}, routerType = {}, params = {}", 
            		url, JSON.toJSONString(platformEnum), JSON.toJSONString(routerType), JSON.toJSONString(params), e);
            return "";
        }
    }

    /**
     * 组装前端跳转URL 用于发起新的搜索时，比如轮播图跳转、文字按钮跳转
     *
     * @param platform
     * @param searchParam
     * @param stp
     * @return
     * @author: luozhuo
     * @date: 2018年7月26日 上午11:23:01
     */
    public static String getSearchNewPageRoute(PlatformEnum platform, String searchParam, String stp) {
        String sq = "?sp=" + URLEncoder.encode(searchParam) + "&stp=" + stp;

        switch (platform) {
            case ANDROID:
                return ROUTE_2SEARCHPAGE_APP + sq;
            case IOS:
                return ROUTE_2SEARCHPAGE_APP + sq;
            case M:
                return ROUTE_2SEARCHPAGE_MWEB + sq;
            case MINI:
                return ROUTE_2SEARCHPAGE_MINIAPP + sq;

            default:
                return ROUTE_2SEARCHPAGE_MWEB + sq;
        }
    }

    /**
     * 跳转到必要朋友大V或者企业定制用户主页的URL
     * stp埋点
     *    大V固定为  各端.500009.profile.0
     *    企业定制用户固定为 各端.500009.enterprise.0
     * @param request
     * @return
     */
    public static String buildByFriendHomePageURL(UISearchRequest request, Long vid,String  vtype) {
        JSONObject stpMap = new JSONObject();
        if(VModelTypeEnum.DAV.getCode().equals(vtype)) {
            stpMap.put("spm", String.format("%s.500009.profile.0", request.getSiteId()));
        }else if(VModelTypeEnum.ENTERPRISE.getCode().equals(vtype)){
            stpMap.put("spm", String.format("%s.500009.enterprise.0", request.getSiteId()));
        }else if(VModelTypeEnum.PLATFORM.getCode().equals(vtype)){
            stpMap.put("spm", String.format("%s.500009.platform.0", request.getSiteId()));
        }
        stpMap.put("rpvid", request.getPvid());
        String stp = URLEncoder.encode(stpMap.toJSONString());

        PlatformEnum platform = request.getPlatform();
        //todo 企业定制用户路由待确认
        switch (platform) {
            case ANDROID:
                return CommonConstant.APP_BYFRIEND_HOMEPAGE_URL + "?friendId=" + vid + "&stp=" + stp;
            case IOS:
                return CommonConstant.APP_BYFRIEND_HOMEPAGE_URL + "?friendId=" + vid + "&stp=" + stp;
            case M:
                return CommonConstant.M_BYFRIEND_HOMEPAGE_URL + "?personId=" + vid + "&fromBy=1" + "&stp=" + stp;
            case MINI:
                return CommonConstant.MINIAPP_BYFRIEND_HOMEPAGE_URL + "?accessType=2&targetUid=" + vid + "&stp=" + stp;
            default:
                return CommonConstant.M_BYFRIEND_HOMEPAGE_URL + "?personId=" + vid + "&fromBy=1" + "&stp=" + stp;
        }
    }
}
