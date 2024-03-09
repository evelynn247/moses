package com.biyao.moses.cache;

import com.biyao.moses.common.constant.MatchRedisKeyConstant;
import com.biyao.moses.common.enums.MatchConfigInfoEnum;
import com.biyao.moses.util.MatchRedisUtil;
import com.biyao.moses.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;

@Slf4j
@Component
@EnableScheduling
public class RedisCache {

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    MatchRedisUtil matchRedisUtil;

    private Set<String> notFilterfCategoryIdSet;

    /**
     * moses-match 存储在match redis中的配置信息
     */
    private Map<String, String> matchConfigMap = new HashMap<>();

    // 10分钟刷新一次
    @Scheduled(cron = "0 0/10 * * * ?")
    protected void refresh() {
        refreshNotFilterCustomPidFcateIdSet();
        refreshMatchConfig();
    }

    @PostConstruct
    protected void init(){
        refresh();
    }


    /**
     * 刷新moses-match的配置信息
     */
    private void refreshMatchConfig(){
        try{
            log.info("[任务进度][配置缓存]刷新配置信息开始");
            long startTime = System.currentTimeMillis();
            Boolean[] isRedisExceptionArray = new Boolean[]{false};
            Map<String, String> mosesConfigMapTmp = matchRedisUtil.hgetAll(MatchRedisKeyConstant.MOSES_MATCH_CONFIG, isRedisExceptionArray);
            Boolean isRedisException = isRedisExceptionArray[0];
            if(isRedisException){
                log.error("[严重异常][配置缓存]从redis中获取配置信息时发生异常");
                return;
            }
            if(mosesConfigMapTmp == null){
                mosesConfigMapTmp = new HashMap<>();
            }
            matchConfigMap = mosesConfigMapTmp;
            log.info("[任务进度][配置缓存]刷新配置信息结束，耗时{}ms，配置个数{}", System.currentTimeMillis() - startTime, mosesConfigMapTmp.size());
        }catch (Exception e){
            log.error("[严重异常][配置缓存]刷新配置信息时发生异常 ", e);
        }
    }

    /**
     * 刷新不需要过滤定制商品的前台类目ID
     */
    private void refreshNotFilterCustomPidFcateIdSet(){

        try{
            log.info("[任务进度]刷新不需要过滤定制商品的前台类目ID开始");
            Set<String> notFilterfCategoryIdSetTmp = new HashSet<>();
            String notFilterFcateIdStr = matchRedisUtil.getString(MatchRedisKeyConstant.NOT_FILTER_COSTOM_FCATEIDS);
            if(StringUtils.isNotBlank(notFilterFcateIdStr)){
                String[] notFilterFcateIdArray = notFilterFcateIdStr.split(",");
                for(String fcateId : notFilterFcateIdArray){
                    if(StringUtils.isNotBlank(fcateId)){
                        notFilterfCategoryIdSetTmp.add(fcateId);
                    }
                }
            }
            notFilterfCategoryIdSet = notFilterfCategoryIdSetTmp;
            log.info("[任务进度]刷新不需要过滤定制商品的前台类目ID结束，fcateIds {}", notFilterfCategoryIdSet);
        }catch(Exception e){
            log.error("[严重异常]刷新不需要过滤定制商品的前台类目ID出现异常，", e);
        }
    }

    /**
     * 根据配置信息获取配置对应的值
     * @param matchConfigInfoEnum
     * @return
     */
    public String getMatchConfigValue(MatchConfigInfoEnum matchConfigInfoEnum){
        if(matchConfigInfoEnum == null){
            return null;
        }
        //如果没有对应的配置项则返回该配置项对应的默认值
        return matchConfigMap.getOrDefault(matchConfigInfoEnum.getName(), matchConfigInfoEnum.getDefaultValue());
    }
    /**
     * 判断前台类目ID是否需要过滤定制商品
     * @param fcateId
     * @return
     */
    public boolean isNotFilterCustomPidFcateId(String fcateId){
        if(CollectionUtils.isEmpty(notFilterfCategoryIdSet) || StringUtils.isBlank(fcateId)){
            return false;
        }

        if(notFilterfCategoryIdSet.contains(fcateId)){
            return true;
        }
        return false;
    }
}


