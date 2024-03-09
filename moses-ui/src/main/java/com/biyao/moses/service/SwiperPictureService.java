package com.biyao.moses.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.biyao.moses.common.constant.CacheRedisKeyConstant;
import com.biyao.moses.constants.CommonConstants;
import com.biyao.moses.constants.ERouterType;
import com.biyao.moses.context.ByUser;
import com.biyao.moses.dto.trace.TraceDetail;
import com.biyao.moses.exp.MosesExpConst;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.util.CacheRedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @ClassName SwiperPictureService
 * @Description 首页轮播图相
 * @Author xiaojiankai
 * @Date 2020/5/9 16:49
 * @Version 1.0
 **/
@Slf4j
@Component
public class SwiperPictureService {

    @Autowired
    CacheRedisUtil cacheRedisUtil;

    private static final int SWIPER_PIC_NUM = 6;

    //新首页feeds流曝光缓冲数
    private final int NEW_HOME_FEED_EXP_NUM = 999;

    //新首页曝光缓存过期时间为3天
    private final int EXPIRE_TIME_3DAY = 259200;

    /**
     * 解析并校验参数
     * @param swiperPicConf
     * @return
     */
    public static List<TotalTemplateInfo> parseSwiperPicConf(String swiperPicConf){
        List<TotalTemplateInfo> result = new ArrayList<>();
        if(StringUtils.isBlank(swiperPicConf)){
            return result;
        }

        for(int i = 0; i < SWIPER_PIC_NUM; i++){
            result.add(null);
        }

        try{
            JSONArray parseArray = JSON.parseArray(swiperPicConf);
            for (int i = 0; i < parseArray.size(); i++) {
                JSONObject jSONObject = parseArray.getJSONObject(i);
                try {
                    String id = jSONObject.getString("id");
                    String pos = jSONObject.getString("pos");
                    int position = StringUtils.isNotBlank(pos) ? Integer.valueOf(pos) : 0;
                    String image = jSONObject.getString("image");
                    String imageWebp = jSONObject.getString("imageWebp");
                    String liveId = jSONObject.getString("liveId");
                    String router = jSONObject.getString("router");
                    if(StringUtils.isBlank(id) || StringUtils.isBlank(pos)
                        || StringUtils.isBlank(image) || StringUtils.isBlank(imageWebp)
                        || (position > SWIPER_PIC_NUM) || (position <= 0)
                        || (StringUtils.isBlank(liveId) && StringUtils.isBlank(router))){
                        log.error("[严重异常][首页轮播图] 配置的轮播图{}校验失败", swiperPicConf);
                        continue;
                    }

                    TotalTemplateInfo totalTemplateInfo = new TotalTemplateInfo();
                    Map<String, String> routerParam = new HashMap<>();
                    routerParam.put("id", id);
                    if(StringUtils.isNotBlank(liveId)) {
                        routerParam.put("liveId", liveId);
                    }
                    if(StringUtils.isNotBlank(router)){
                        routerParam.put("router", router);
                    }
                    totalTemplateInfo.setRouterParams(routerParam);
                    totalTemplateInfo.setRouterType(ERouterType.SWIPERPICCONF.getNum());
                    totalTemplateInfo.setId("-1");
                    totalTemplateInfo.setImage(image);
                    totalTemplateInfo.setImageWebp(imageWebp);
                    List<String> list = new ArrayList<>();
                    List<String> listWebp = new ArrayList<>();
                    list.add(image);
                    listWebp.add(imageWebp);
                    totalTemplateInfo.setLongImages(list);
                    totalTemplateInfo.setLongImagesWebp(listWebp);
                    totalTemplateInfo.setImages(list);
                    totalTemplateInfo.setImagesWebp(listWebp);
                    result.set(position-1, totalTemplateInfo);
                } catch (Exception e) {
                    log.error("[严重异常][首页轮播图] 解析配置的轮播图信息{}失败，", swiperPicConf, e);
                    log.error(jSONObject.toString());
                }
            }
        }catch (Exception e){
            log.error("[严重异常][首页轮播图] 解析配置的轮播图信息{}失败，", swiperPicConf, e);
        }
        return result;
    }

    /**
     * 轮播图的个数是否达到上限
     * @param totalTemplateInfoList
     * @return
     */
    public static boolean isFull(List<TotalTemplateInfo> totalTemplateInfoList){
        if(CollectionUtils.isEmpty(totalTemplateInfoList)){
            return false;
        }

        for(TotalTemplateInfo info : totalTemplateInfoList){
            if(info == null){
                return false;
            }
        }
        return true;
    }

    /**
     * 使用配置的轮播图信息和根据算法获取的轮播图信息，组装最终的结果
     * @param swiperPicConfInfo 配置的轮播图信息
     * @param expData 根据算法获取的轮播图信息
     * @return
     */
    public static Map<String, List<TotalTemplateInfo>> composeSwiperPicData(List<TotalTemplateInfo> swiperPicConfInfo, Map<String, List<TotalTemplateInfo>> expData, List<TraceDetail> matchTraceDetailList, ByUser user){
        Map<String, List<TotalTemplateInfo>> result = expData;
        //如果没有配置轮播图信息，则直接返回算法结果
        if(CollectionUtils.isEmpty(swiperPicConfInfo)){
            return expData;
        }
        //如果算法获取的轮播图信息为空，则说明配置的轮播图已经达到上限6张
        if(expData == null || expData.size() <= 0){
            String expId = MosesExpConst.SLIDER_EXP;
            String dataKey = "moses:" + CommonConstants.HOME_SWIPER_PICTURE_TOPICID + CommonConstants.SPLIT_LINE + expId;
            result = new HashMap<>();
            result.put(dataKey, swiperPicConfInfo);
            //构造matchTrace
            TraceDetail traceDetail = new TraceDetail();
            //该key必须
            traceDetail.setExpId(dataKey);
            Set<String> keys = new HashSet<>();
            keys.add("moses:" + CommonConstants.HOME_SWIPER_PICTURE_TOPICID + CommonConstants.SPLIT_LINE + expId + CommonConstants.SPLIT_LINE + "0000");
            traceDetail.setKeys(keys);
            matchTraceDetailList.add(traceDetail);
            user.setNewExp(true);
        }else{
            try {
                List<TotalTemplateInfo> data = null;
                String key = null;
                for (Map.Entry<String, List<TotalTemplateInfo>> map : expData.entrySet()) {
                    data = map.getValue();
                    key = map.getKey();
                    break;
                }

                int i = 0;
                ListIterator<TotalTemplateInfo> iterator = swiperPicConfInfo.listIterator();
                while (iterator.hasNext()) {
                    TotalTemplateInfo info = iterator.next();
                    if (info != null) {
                        continue;
                    }
                    iterator.set(data.get(i));
                    i++;
                }
                expData.put(key, swiperPicConfInfo);
            }catch (Exception e){
                log.error("[严重异常][首页轮播图]使用配置的轮播图和算法获取的轮播图组装数据失败 ", e);
            }
        }
        return result;
    }

    /**
     * 处理首页轮播图曝光
     * @param expData
     * @param user
     */
    public void dealSwiperPicExposure(Map<String, List<TotalTemplateInfo>> expData, ByUser user){
        try {
            if (user.isNewExp()) {
                List<TotalTemplateInfo> data = null;
                for (Map.Entry<String, List<TotalTemplateInfo>> map : expData.entrySet()) {
                    data = map.getValue();
                    break;
                }

                List<String> expPidList = new ArrayList<>();
                long currentTime = System.currentTimeMillis();
                for (int i = 0; i < 6; i++) {
                    TotalTemplateInfo info = data.get(i);
                    if (info == null || "-1".equals(info.getId())) {
                        continue;
                    }
                    expPidList.add(info.getId() + ":" + currentTime);
                }
                if (CollectionUtils.isEmpty(expPidList)) {
                    return;
                }
                String[] expPids = expPidList.toArray(new String[0]);
                cacheRedisUtil.lpush(CacheRedisKeyConstant.NEW_HOME_FEED_EXPOSURE + user.getUuid(), expPids);
                //限制曝光为500个
                cacheRedisUtil.ltrim(CacheRedisKeyConstant.NEW_HOME_FEED_EXPOSURE + user.getUuid(), 0, NEW_HOME_FEED_EXP_NUM);
                //加入过期时间
                cacheRedisUtil.expire(CacheRedisKeyConstant.NEW_HOME_FEED_EXPOSURE + user.getUuid(), EXPIRE_TIME_3DAY);
            }
        }catch (Exception e){
            log.error("[严重异常][首页轮播图]处理曝光失败 ", e);
        }
    }
}
