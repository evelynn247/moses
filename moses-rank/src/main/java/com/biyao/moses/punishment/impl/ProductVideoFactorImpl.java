package com.biyao.moses.punishment.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.biyao.moses.cache.SwitchConfigCache;
import com.biyao.moses.model.match2.MatchItem2;
import com.biyao.moses.model.video.Video;
import com.biyao.moses.params.rank2.RankRequest2;
import com.biyao.moses.punishment.PunishmentService;
import com.biyao.moses.util.AlgorithmRedisUtil;
import com.biyao.moses.util.StringUtil;
import com.uc.domain.bean.User;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.*;

import static com.biyao.moses.common.constant.AlgorithmRedisKeyConstants.MOSES_VID_PROMOTE;
import static com.biyao.moses.common.constant.AlgorithmRedisKeyConstants.PRODUICT_VEDIO_REDIS;
import static com.biyao.moses.common.constant.CommonConstants.*;
import static com.biyao.moses.common.constant.RsmConfigConstants.*;

/**
 * @program: moses-parent
 * @description: 视频因子
 * @author: changxiaowei
 * @Date: 2022-02-16 18:40
 **/
@Slf4j
@Component
public class ProductVideoFactorImpl implements PunishmentService {

    @Autowired
    AlgorithmRedisUtil algorithmRedisUtil;

    @Autowired
    SwitchConfigCache switchConfigCache;
    @Override
    public Map<Long, Double> getPunishment(String uuid, List<MatchItem2> matchItemList, User user) {
        return null;
    }

    /**
     * 计算视频因子
     * 1 获取商品和视频的关系 hmget
     * 2 遍历待排序的商品
     * 3 计算视频因子
     *   3.1 如果该商品没有轮播图视频 则视频因子=1
     *   3.2 如果有轮播图视频 视频因子= Max（1+S,V1）
     *   其中：S为视频分归一化的结果
     *   	   当视频首次上传时间距今<N 则V1取自rsm，否则V1=1
     *         V1~【1,10】 默认1.1
     *         N取值【0,100】默认为3
     *   3.3 记录视频因子大于1的商品集合
     * 4 记录被提权的商品的视频和时间
     *   4.1 从redis中取出该商品被提权的视频和时间
     *   4.2 删除距今超过C天的记录
     *   4.3 如果剩余被提权的次数小于2 则记录本次被提权的视频id 和时间
     *   4.3 如果剩余被提权的次数大于等于2 则 不记录 并将此商品的视频因子设为1
     * 5 返回本批商品的视频因子
     * @param rankRequest2
     * @param user
     * @return
     */
    @Override
    public Map<Long, Double> getPunishment(RankRequest2 rankRequest2, User user) {
        //  初始化结果集
        Map<Long, Double> resultMap= new HashMap<>();
        SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // 记录被提权的商品id 和视频id
        Map<Long,Integer> awardPidVidMap = new HashMap<>();
        List<MatchItem2> matchItemList = rankRequest2.getMatchItemList();
        if (CollectionUtils.isEmpty(matchItemList)) {
            return resultMap;
        }
        //批量获取redis中商品和视频的关系
        String[] pidArr = new String[matchItemList.size()];
        for (int i = 0; i < matchItemList.size(); i++) {
            pidArr[i] =matchItemList.get(i).getProductId().toString();
        }
        Map<String, String> pidVideoInfoMap = algorithmRedisUtil.hmgetMap(PRODUICT_VEDIO_REDIS, pidArr);
        // 遍历待排序商品 计算视频因子
        for (MatchItem2 matchItem2 : matchItemList) {
            Long pid = matchItem2.getProductId();
            // 获取轮播图视频
            Video slideVideo = getSlideVideo(pidVideoInfoMap.get(pid.toString()));
            // 有轮播图视频且发布时间不为空的需要计算视频因子
            if(slideVideo !=null && !StringUtil.isBlank(slideVideo.getPublish_time())){
                long pubLishTime; float v1 = 1;
                try {
                     pubLishTime = sf.parse(slideVideo.getPublish_time()).getTime();
                }catch (Exception e){
                    log.error("[严重异常]轮播图视频发布时间格式转换异常，视频信息：{}", JSONObject.toJSONString(slideVideo));
                    continue;
                }
                // 发布时间距今时小于N的 视频因子为v1  否则为1
                long days =(System.currentTimeMillis() - pubLishTime)/dayMilliSecond ;
                if(days < switchConfigCache.getRsmIntValue(VIDEO_N,VIDEO_N_DEFAULT,0,100)){
                    v1 = switchConfigCache.getRsmFloatValue(VIDEO_V,VIDEO_V_DEFAULT,1,10);
                }
                float videoFactor = Math.max(1 + slideVideo.getScore(), v1);
                resultMap.put(pid, (double) videoFactor);
                // 如果视频因子大于1  需记录
                if(videoFactor > 1){
                    awardPidVidMap.put(pid,slideVideo.getVid());
                }
            }
        }
        //记录被提权的商品和视频
        if(!awardPidVidMap.isEmpty()){
            updateAwardVid2Redis(awardPidVidMap,resultMap);
        }
        return resultMap;
    }


    /**
     * 更新redis中商品被提权的信息
     * @param awardPidVidMap
     * @param resultMap
     */
    private void updateAwardVid2Redis( Map<Long,Integer> awardPidVidMap, Map<Long, Double> resultMap){
        //1 获取商品在此之前被提权的记录
        Map<String, String> oldAwardpidVidMap = algorithmRedisUtil.hmgetMap(MOSES_VID_PROMOTE, StringUtils.join(awardPidVidMap.keySet(), ",").split(","));
        // 用于更新redis中商品被提权的信息
        Map<String, String> updateAwardPidVidTimeMap = new HashMap<>();
        Set<Map.Entry<Long, Integer>> entries = awardPidVidMap.entrySet();
        for (Map.Entry<Long, Integer> entry : entries) {
            Map<String,String> awardVidMap = new HashMap<>();
            // 获取该商品被提权的视频和时间 vid:time ,vid2:time2
            String awardVids = oldAwardpidVidMap.get(entry.getKey().toString());
            if (!StringUtil.isBlank(awardVids)) {
                // 不为空 则说明之前被提权过  获取被提权的记录
                String[] vidTimes = awardVids.split(",");
                for (String vidTime : vidTimes) {
                    if (StringUtil.isBlank(vidTime)) {
                        continue;
                    }
                    String[] vidAndTime = vidTime.split(":");
                    // 统计周期内被提权的记录
                    if(vidAndTime.length == 2){
                        String vid =vidAndTime[0];
                        String time =vidAndTime[1];
                        if(System.currentTimeMillis()- Long.valueOf(time) <= switchConfigCache.getRsmLongValue(VIDEO_C,VIDEO_C_DEFAULT,ZERO,ZERO)){
                            awardVidMap.put(vid,time);
                        }
                    }
                }
                // 如果在周期内被提权的次数大于2或者该商品被此视频提权过，则本次不给于提权则不给提权
                if(awardVidMap.size() >=2 || awardVidMap.containsKey(entry.getValue().toString())){
                    resultMap.put(entry.getKey(),ONE.doubleValue());
                }else {
                    awardVidMap.put(entry.getValue().toString(),String.valueOf(System.currentTimeMillis()));
                }
            }else {
                // 说明之前没有被提权过 直接记录
                awardVidMap.put(entry.getValue().toString(),String.valueOf(System.currentTimeMillis()));
            }
            StringBuilder vidTime  = new StringBuilder();
            awardVidMap.forEach((key,value)-> vidTime.append(key).append(":").append(value).append(","));
            updateAwardPidVidTimeMap.put(entry.getKey().toString(),vidTime.toString());
        }
        //更新redis
        algorithmRedisUtil.hmset(MOSES_VID_PROMOTE,updateAwardPidVidTimeMap);
    }
    /**
     * 获取轮播图视频
     * @param videoInfo
     * @return
     */
    public Video getSlideVideo(String videoInfo) {
        Video slideVideo = null;
        if (StringUtil.isBlank(videoInfo)) {
            return slideVideo;
        }
        try {
            List<Video> videoList = JSONArray.parseArray(videoInfo, Video.class);
            for (Video video : videoList) {
                if (!Video.isValid(video)) {
                    continue;
                }
                if (Video.isSlide(video)) {
                    return video;
                }
            }
        } catch (Exception e) {
            log.error("[严重异常]根据规则选择轮播图视频时出现异常,视频信息:{},异常信息:{}",videoInfo,e);
        }
        return slideVideo;
    }
}
