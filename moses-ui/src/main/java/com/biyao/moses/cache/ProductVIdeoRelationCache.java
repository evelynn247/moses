package com.biyao.moses.cache;

import com.biyao.moses.params.ProductInfo;
import com.biyao.moses.service.imp.AdvertInfoService;
import com.biyao.moses.util.AlgorithmRedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

import static com.biyao.moses.common.constant.AlgorithmRedisKeyConstants.PRODUICT_VEDIO_REDIS;
import static com.biyao.moses.common.constant.CommonConstants.ZERO;

/**
 * @program: moses-parent
 * @description: 商品和视频关系缓存  用于视频流落地页 兜底
 * @author: changxiaowei
 * @Date: 2022-02-24 09:00
 **/
@Component
@EnableScheduling
@Slf4j
public class ProductVIdeoRelationCache {

    private final static int expNum =500;
    // 存储热门商品和视频的关系  必要商城
    Map<Long, Integer> byPidVidMap = new HashMap<>();
    // 存储热门商品和视频的关系 必要分销
    Map<Long, Integer> byFxPidVidMap = new HashMap<>();

    @Autowired
    AlgorithmRedisUtil algorithmRedisUtil;
    @Autowired
    AdvertInfoService advertInfoService;
    @Autowired
    ProductDetailCache productDetailCache;

    @PostConstruct
    protected void init() {
        refreshProductVideoRelationCache();
    }

    @Scheduled(cron = "0 0/10 * * * ? ")
    protected void refreshProductVideoRelationCache() {
        Map<Long, Integer> byPidVidMapTemp = new HashMap<>();
        Map<Long, Integer> byfxPidVidMapTemp = new HashMap<>();
        String cursor = ZERO.toString();
        ScanParams scanParams = new ScanParams();
        scanParams.count(500);
        while (true){
            if(byPidVidMapTemp.size() >= expNum && byfxPidVidMapTemp.size() >= expNum){
                break;
            }
            Map<String, String> result = new HashMap<>();
            ScanResult<Map.Entry<String, String>> tempResult = algorithmRedisUtil.hscan(PRODUICT_VEDIO_REDIS, scanParams, cursor);
            if(tempResult != null){
                if (!com.alibaba.dubbo.common.utils.CollectionUtils.isEmpty(tempResult.getResult())){
                    tempResult.getResult().forEach(item -> result.put(item.getKey(), item.getValue()));
                    for (Map.Entry<String, String> entry : result.entrySet()) {
                        //获取 必要商城和必要分销
                        Integer byvid = advertInfoService.selectOptimalVid(entry.getValue(),1);
                        Integer byfxvid = advertInfoService.selectOptimalVid(entry.getValue(),2);
                        try {
                           long pid = Long.valueOf(entry.getKey());
                            if(byvid != null && byPidVidMapTemp.size() <=expNum){
                                byPidVidMapTemp.put(pid,byvid);
                            }
                            ProductInfo productInfo = productDetailCache.getProductInfo(pid);
                            if(byfxvid != null && byfxPidVidMapTemp.size() <=expNum && productInfo != null &&
                                    Arrays.stream(productInfo.getProductPool().split(",")).collect(Collectors.toSet()).contains("1")){
                                byfxPidVidMapTemp.put(pid,byfxvid);
                            }
                        }catch (Exception e){
                            log.error("[严重异常]redis中存储商品视频关系格式错误，pid:{},videoInfo:{},异常信息:{}",entry.getKey(),entry.getValue(),e);
                        }
                    }
                }
                cursor = tempResult.getStringCursor();
                if(ZERO.toString().equals(cursor)){
                    break;
                }
            }else {
                break;
            }
        }
        if(!byPidVidMapTemp.isEmpty()){
            byPidVidMap =byPidVidMapTemp;
        }
        if(!byfxPidVidMapTemp.isEmpty()){
            byFxPidVidMap =byfxPidVidMapTemp;
        }
    }

    /**
     *  用缓存数据填充结果 es召回数量不够的时候 用兜底填充
     */
    public List<Long> fillCache(int channelType){
        Map<Long, Integer> pidVidMap = new HashMap<>();
       List<Long> resultList =  new ArrayList<>();
        switch (channelType){
            case 1:
                pidVidMap = byPidVidMap;
                break;
            case 2:
                pidVidMap=  byFxPidVidMap;
        }
        if(pidVidMap.isEmpty()){
            return resultList;
        }
        resultList = new ArrayList<>(pidVidMap.keySet());
        return resultList;
    }

    /**
     * 随机获取内存中 n个数据
     * @param expNum
     * @return
     */
    public  Map<Long, Integer> getRandomDateFromCache(int expNum,int channelType){
        Map<Long, Integer> pidVidMap = new HashMap<>();
        switch (channelType){
            case 1:
                pidVidMap = byPidVidMap;
                break;
            case 2:
                pidVidMap=  byFxPidVidMap;
        }
        Map<Long, Integer> result = new HashMap<>();
        if(pidVidMap.isEmpty()){
            return  result;
        }
        List<Long> pidList =new ArrayList<>(pidVidMap.keySet());
        Collections.shuffle(pidList);
        for (Long pid : pidList) {
            if(expNum <= 0){
                break;
            }
            result.put(pid,pidVidMap.get(pid));
            expNum--;
        }
        return result;
    }
}
