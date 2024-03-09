package com.biyao.moses.service.imp;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.cache.SwitchConfigCache;
import com.biyao.moses.common.constant.ErrorCode;
import com.biyao.moses.common.constant.RedisKeyConstant;
import com.biyao.moses.constants.CommonConstants;
import com.biyao.moses.model.match2.MatchItem2;
import com.biyao.moses.params.*;
import com.biyao.moses.params.rank.CommonRankRequest;
import com.biyao.moses.util.AlgorithmRedisUtil;
import com.biyao.moses.util.HttpClientUtil;
import com.biyao.moses.util.RedisUtil;
import com.biyao.moses.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.biyao.moses.common.constant.AlgorithmRedisKeyConstants.PRODUICT_VEDIO_REDIS;
import static com.biyao.moses.constants.CommonConstants.*;

/**
 * @ClassName CommonService
 * @Description 通用controller对应的service
 * @Author xiaojiankai
 * @Date 2019/8/9 13:40
 * @Version 1.0
 **/
@Slf4j
@Service
public class CommonService {

    //过期时间为1小时
    private static int expireTimeOneHour = 3600;

    @Autowired
    RedisUtil redisUtil;
    @Autowired
    SwitchConfigCache switchConfigCache;
    @Autowired
    AlgorithmRedisUtil algorithmRedisUtil;
    @Autowired
    ProductDetailCache productDetailCache;
    @Autowired
    AdvertInfoService advertInfoService;
    @Autowired
    ProductServiceImpl productService;


    public CommonResponse rank(UICommonRequest uiCommonRequest, List<Long> pidList){
        //页面缓存的redis key
        String page_cache_key = RedisKeyConstant.KEY_PREFIEX_COMMON_PAGE_CACHE+uiCommonRequest.getPvid()
                +RedisKeyConstant.SPLIT_LINE+uiCommonRequest.getSceneId()
                +RedisKeyConstant.SPLIT_LINE+uiCommonRequest.getTagId()
                +RedisKeyConstant.SPLIT_LINE+uiCommonRequest.getUuid();

        String pageIndexStr = uiCommonRequest.getPageIndex();
        String pageSizeStr = uiCommonRequest.getPageSize();
        List<Long> pageResult;
        Integer totalPage;
        int totalPidNum = 0;

            if (StringUtils.isBlank(pageIndexStr)) {
                //如果pageIndex未传入值，则认为是需要返回全部数据，故不需要缓存。
                pageResult = requestCommonRank(uiCommonRequest, pidList);
                totalPage = 1;
                totalPidNum = pageResult.size();
            } else {
                int pageIndex = Integer.valueOf(pageIndexStr);
                int pageSize = Integer.valueOf(pageSizeStr);
                List<Long> sortedPids;
                if (pageIndex == 1) {
                    sortedPids = requestCommonRank(uiCommonRequest, pidList);
                    //将结果缓存到Redis中
                    redisUtil.setString(page_cache_key, JSON.toJSONString(sortedPids), expireTimeOneHour);
//                    log.error("获取排好序的pids {}, uuid={}", JSON.toJSONString(sortedPids), uiCommonRequest.getUuid());
                } else {
                    String pidsStr = redisUtil.getString(page_cache_key);
                    if (StringUtils.isNotBlank(pidsStr)) {
                        //重新设置缓存的过期时间
                        redisUtil.expire(page_cache_key, expireTimeOneHour);
                        sortedPids = JSONObject.parseObject(pidsStr, new TypeReference<List<Long>>() {
                        });
//                        log.error("获取缓存中的pids {}, uuid={}", JSON.toJSONString(sortedPids), uiCommonRequest.getUuid());
                    } else {
                        //未有缓存数据或缓存数据已过期，则排好序的pids为空集合
                        sortedPids = new ArrayList<>();
                    }
                }
                //返回对应页的数据
                if (CollectionUtils.isNotEmpty(sortedPids)) {
                    totalPidNum = sortedPids.size();
                    int startIndex = (pageIndex - 1) * pageSize;
                    int endIndex = pageIndex * pageSize <= totalPidNum ? pageIndex * pageSize : totalPidNum;
                    if (startIndex >= totalPidNum) {
                        //起始索引就大于商品总数时，认为传入的pageIndex大于总页数，此时返回数据为空
                        pageResult = new ArrayList<>();
                    } else {
                        pageResult = sortedPids.subList(startIndex, endIndex);
                    }
                    totalPage = totalPidNum % pageSize == 0 ? totalPidNum / pageSize : totalPidNum / pageSize + 1;
                } else {
                    //未有缓存数据或缓存数据已过期，则返回的pid集合为空
                    pageResult = new ArrayList<>();
                    totalPage = 0;
                    totalPidNum = 0;
                }
            }

            dealExposure(uiCommonRequest, pageResult, totalPidNum);
            CommonResponse commonResponse = new CommonResponse();
            commonResponse.setPids(pageResult);
            commonResponse.setTotalPage(totalPage);
            return commonResponse;
    }

    /**
     * 取pidList中的前6个做为曝光商品，80%的商品都已曝光后，将该页面所有的曝光信息清除
     * @param uiCommonRequest
     * @param pagePidList
     * @param totalPidNum
     */
    private void dealExposure(UICommonRequest uiCommonRequest, List<Long> pagePidList, Integer totalPidNum){

        if(CollectionUtils.isEmpty(pagePidList)){
            return;
        }
        //曝光redis key
        String exposure_product_key = RedisKeyConstant.KEY_PREFIEX_COMMON_EXPOSURE+uiCommonRequest.getSceneId()
                +RedisKeyConstant.SPLIT_LINE+uiCommonRequest.getTagId()
                +RedisKeyConstant.SPLIT_LINE+uiCommonRequest.getUuid();
        StringBuffer sb = new StringBuffer();
        //格式：pid,pid,...,pid
        String exposurePidStr = redisUtil.getString(exposure_product_key);
        List<String> exposurePids = new ArrayList<>();
        if(StringUtils.isNotBlank(exposurePidStr)) {
            exposurePids = Arrays.asList(exposurePidStr.split(","));
            //如果曝光商品总商品的80%，则将曝光信息删除
            if (exposurePids.size() > totalPidNum * 8 / 10) {
                redisUtil.del(exposure_product_key);
                exposurePids = new ArrayList<>();
            } else {
                sb.append(exposurePidStr);
            }
        }
        //每页的前6个商品做为曝光商品
        int endIndex = pagePidList.size()>6 ? 6 : pagePidList.size();
        List<Long> currExposurePids = pagePidList.subList(0, endIndex);
        for(Long pid : currExposurePids){
            if(!exposurePids.contains(pid.toString())){
                sb.append(",");
                sb.append(pid.toString());
            }
        }
        //如果sb有值，并且第一个为逗号,则将逗号删除
        if(sb.length()>0 && sb.charAt(0)==','){
            sb.deleteCharAt(0);
        }
        redisUtil.setString(exposure_product_key, sb.toString(), expireTimeOneHour);
//        log.error("已曝光商品：{}, uuid={}",exposurePidStr, uiCommonRequest.getUuid());
//        log.error("本次曝光商品：{}, uuid={}",currExposurePids, uiCommonRequest.getUuid());
//        log.error("本次曝光后已曝光商品：{}, uuid={}",sb.toString(), uiCommonRequest.getUuid());
    }

    private List<Long> requestCommonRank(UICommonRequest uiCommonRequest, List<Long> pidList){
        // 如果个性化活动开关为关且场景SceneId=1 或者为2时，直接返回 不排序
        if( !switchConfigCache.getRecommendSwitchByConfigId(CommonConstants.PERSONALIZE_RECOMMEND_OFF_CONFIG_ID) &&
                ("1".equals(uiCommonRequest.getSceneId()) || "2".equals(uiCommonRequest.getSceneId()))){
            return pidList;
        }
        List<Long> result = new ArrayList<>();
        //请求commonRank
        String rankUrl = "http://mosesrank.biyao.com/recommend/commonrank";
        //String rankUrl = "http://localhost:8020/recommend/commonrank";
        CommonRankRequest commonRankRequest = CommonRankRequest.builder().pids(pidList).uuid(uiCommonRequest.getUuid())
                .uid(uiCommonRequest.getUid()).sceneId(uiCommonRequest.getSceneId())
                .tagId(uiCommonRequest.getTagId()).build();
        ApiResult<List<Long>> parseObject = new ApiResult<>();
        parseObject.setSuccess(ErrorCode.SYSTEM_ERROR_CODE);
        try {
            String jsonString = JSONObject.toJSONString(commonRankRequest);
            String sendPostJSON = HttpClientUtil.sendPostJSON(rankUrl, null, jsonString, 2000);
            parseObject = JSONObject.parseObject(sendPostJSON,new TypeReference<ApiResult<List<Long>>>() {});
        } catch (Exception e) {
            log.error("[严重异常][rank]请求commonrank出错 request {}， ", JSONObject.toJSONString(commonRankRequest), e);
            if(parseObject != null){
                parseObject.setSuccess(ErrorCode.SYSTEM_ERROR_CODE);
            }
        }

        if(parseObject!=null && parseObject.getSuccess()!=null && parseObject.getSuccess()==ErrorCode.SUCCESS_CODE){
            result = parseObject.getData();
        }else{
            log.error("[严重异常][rank]调用commonRank报错， request {}, response {}", JSONObject.toJSONString(commonRankRequest), JSON.toJSONString(parseObject));
            result.addAll(pidList);
            Collections.shuffle(result);
        }
            return result;
    }

    /**
     * 填充兜底数据
     * @param request
     * @return
     */
    public List<MatchItem2> fillCacheData(RecommendAllRequest request) {
        List<MatchItem2> cacheResult = new ArrayList<>();
        String sceneId = request.getSceneId();
        // 查询redis
        if (StringUtils.isBlank(sceneId)) {
            return cacheResult;
        }
        // 组装redis key
        List<String> redisKeyList = new ArrayList<>();
        if (!StringUtils.isBlank(request.getFrontendCategoryId())) {
            List<Long> cateGoryIdList = StringUtil.strConverToList(request.getThirdCateGoryIdList());
            if (!CollectionUtils.isEmpty(cateGoryIdList)) {
                cateGoryIdList.forEach(cateId -> redisKeyList.add(FEED_CATE_FUNC_CACHE_REDIS_PREFIX + sceneId+ "_" + cateId));
            }
            List<Long> tagIdList = StringUtil.strConverToList(request.getTagIdList());
            if (!CollectionUtils.isEmpty(tagIdList)) {
                tagIdList.forEach(tagId -> redisKeyList.add(FEED_TAG_FUNC_CACHE_REDIS_PREFIX + sceneId +  "_" +tagId));
            }
        } else {
            redisKeyList.add(FEED_FUNC_CACHE_REDIS_PREFIX + sceneId);
        }
        //  redis中取数据
        StringBuilder redisResult = new StringBuilder();
        for (String rediskey : redisKeyList) {
            redisResult.append(algorithmRedisUtil.getString(rediskey)).append(",");
        }
        if (StringUtil.isBlank(redisResult.toString())) {
            log.error("[严重异常]获取func兜底feed数据结果为空，参数：{}", JSONObject.toJSONString(request));
            return cacheResult;
        }
        String[] pidArr = redisResult.toString().split(",");
        for (String pidStr : pidArr) {
            try {
                // 兜底只做无效性校验
                ProductInfo productInfo = productDetailCache.getProductInfo(Long.valueOf(pidStr));
                if (productInfo == null || productInfo.getShelfStatus() != 1) {
                    continue;
                }
                MatchItem2 matchItem2 = new MatchItem2();
                matchItem2.setProductId(productInfo.getProductId());
                matchItem2.setScore(Math.random());
                matchItem2.setSource("cache");
                cacheResult.add(matchItem2);
            } catch (Exception e) {
                log.error("[严重异常]func 兜底feed流数据格式错误,pidStr:{}", pidStr);
            }
        }
        return cacheResult;
    }


    /**
     * 根据视频id查询算法视频分
     * @param videoIds
     * @return
     */
    public List<RecommendCommonInfo> getVideoScoreByVideoIds(List<Long> videoIds, String caller) {
        List<RecommendCommonInfo> list = new ArrayList<>();
        try {
            //从redis获取视频分信息，1007集群  Key为video_score 数据类型是Hash field是视频id，value是分数
            long start = System.currentTimeMillis();
            List<String> videoScoreList = algorithmRedisUtil.hmget(CommonConstants.VIDEO_SCORE_REDIS_KEY, StringUtils.join(videoIds, ",").split(","));
            log.info("[操作日志]调用方：{}，根据视频ids查询算法redis集群中的算法视频分耗时={}",caller, (System.currentTimeMillis() - start));
            //如果查询回来的集合为空，则直接返回空集合
            if (videoScoreList == null) {
                log.error("[严重异常]调用方：{}，根据视频id查询算法redis集群中的算法视频分异常", caller);
                return null;
            }
            //封装视频id-视频分对应关系
            for (int i = 0; i < videoIds.size(); i++) {
                String videoScoreStr = videoScoreList.get(i);
                if (videoScoreStr == null) {
                    continue;
                }
                RecommendCommonInfo map = new RecommendCommonInfo();
                map.setId(String.valueOf(videoIds.get(i)));//赋值视频id
                map.setInfo(videoScoreStr);//赋值视频分
                list.add(map);
            }
        } catch (Exception e) {
            log.error("[严重异常]调用方：{}，根据视频id查询算法redis集群中的算法视频分异常,{}", caller, JSON.toJSONString(e));
            return null;
        }
        return list;
    }

    /**
     * 根据规则将将商品id 替换为视频id
     * @param bodyRequest
     * @return
     */
    public List<RecommendCommonInfo> getVideoIdByRule(RecommendAllBodyRequest bodyRequest) {
        List<RecommendCommonInfo> result = new ArrayList<>();
        int pageSize = bodyRequest.getPageSize();
        int videoInterval = bodyRequest.getVideoInterval();
        Integer lastReplacePosition = bodyRequest.getLastReplacePosition();
        int pageIndex = bodyRequest.getPageIndex();
        List<String> pidList = bodyRequest.getPids();
        String[] pidArr = pidList.toArray(new String[0]);
        // 获取视频的信息
        Map<String, String> videoInfoMap = algorithmRedisUtil.hmgetMap(PRODUICT_VEDIO_REDIS, pidArr);
        int lastPageIndex = 0;
        // 将上一次视频的位置转化为页面位置（ pageSize = 20 时  38--> 18）
        if (lastReplacePosition != null) {
            lastPageIndex = (lastReplacePosition / pageSize) + 1;
            lastReplacePosition = lastReplacePosition - (lastPageIndex - 1) * pageSize;
        }
        for (int i = 0; i < pidList.size(); i++) {
            String pid = pidList.get(i);
            RecommendCommonInfo recommendCommonInfo = new RecommendCommonInfo();
            recommendCommonInfo.setId(pid);
            result.add(recommendCommonInfo);
            if(bodyRequest.getChannelType() == null){
                continue;
            }
            //判断当前位置是否满足替换的条件
            if (!advertInfoService.isSatisfyConverTOVideo(null, pid,
                    i, pageIndex, videoInterval, lastReplacePosition,
                    lastPageIndex, pageSize, 0, videoInfoMap.get(pid))) {
                continue;
            }
            // 替换
            Integer vid = advertInfoService.selectOptimalVid(videoInfoMap.get(pid),bodyRequest.getChannelType());
            // 替换失败 跳过
            if (Objects.isNull(vid)) {
                log.error("[严重异常]商品替换为视频时出现异常，商品:{}，视频信息:{}",
                        pid, videoInfoMap.get(pid));
                continue;
            }
            recommendCommonInfo.setInfo(vid.toString());
            lastReplacePosition = i;
            lastPageIndex = pageIndex;
        }
        return result;
    }
}
