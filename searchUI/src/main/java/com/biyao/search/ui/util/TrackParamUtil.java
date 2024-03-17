package com.biyao.search.ui.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.biyao.search.ui.remote.request.UISearchRequest;
import com.biyao.search.ui.remote.response.SearchProduct;
import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrackParamUtil {

    private static Logger logger = LoggerFactory.getLogger(TrackParamUtil.class);

    /**
     * 完全匹配
     * qmatch.product
     * 部分匹配
     * qmatch0.product
     * 好友都在买
     * friendbuy.product
     * #轮播图
     * topiccard.${pos}
     * #文字链
     * textlink.hotword
     * #6个+2个主题
     * topic.${pos}
     * #托底商品
     * fallback.product
     * #主题页商品
     * topic.product
     * 大V主页  profile
     */
    public static String generateSTP(UISearchRequest request, String trackBlock, String trackPoint, String aid,String semStr) {
        JSONObject stpMap = new JSONObject();
        String stp = request.getStp();
        // ${siteId}.${pageId}.${模块编号}.${点位编号}
        stpMap.put("spm", String.format("%s.%s.%s.%s", request.getSiteId(), request.getPageId(),
                trackBlock, trackPoint));
        stpMap.put("rpvid", request.getPvid());
        //处理stp,如果stp中存在aid，添加本次aid；如果不存在，直接插入
        String stpstr = "";
        try {
            stpstr = Strings.isNullOrEmpty(stp) ? "" : URLDecoder.decode(stp, "utf-8");
        } catch (UnsupportedEncodingException e) {
            logger.error("[严重异常]stp decode error, stp is {}, error message is {}", stp, e);
        }
        JSONObject stpJson = JSONObject.parseObject(stpstr);
        Map<String, String> aidMap = new HashMap<String, String>();
        if (stpJson != null) {
            String aidJson = stpJson.getString("aid");
            if (!StringUtils.isBlank(aidJson)) {
                aidMap = (Map<String, String>) JSON.parse(aidJson);
            }
        }
        aidMap.put("search", aid);
        stpMap.put("aid", JSON.toJSONString(aidMap));
        stpMap.put("sem", generateSemParam(semStr));
        return URLEncoder.encode(stpMap.toJSONString());
    }

    /**
     * 构造商品追踪参数
     * @param item
     * @param blockId
     * @param request
     * @return
     */
    public static String generateProductTrackParam(SearchProduct item, String blockId, UISearchRequest request) {
        return URLEncoder.encode(String.format("sid=%s&bkId=%s&pos=%s&suid=%s", request.getSid(), blockId,
                item.getPosition(), item.getSuId()));
    }

    /**
     * 构建sem参数
     * @param semStr
     * @return
     */
    private static String generateSemParam(String semStr) {
        StringBuilder sb = new StringBuilder(128);
        if(StringUtils.isBlank(semStr)){
            return sb.toString();
        }
        try{
            String[] array = semStr.split(",");
            if(array.length == 1){
                sb.append(array[0]);
            }else{
                for (int i=0; i<array.length; i++){
                    if(i==0){
                        sb.append(array[i]).append(":");
                    }else if(i==array.length-1){
                        sb.append(array[i]);
                    }else{
                        sb.append(array[i]).append("_");
                    }
                }
            }

        }catch(Exception e){
            logger.error("[严重异常]构建sem参数异常",e);
        }
        return sb.toString();
    }
}
