package com.biyao.moses.cache;

import com.biyao.moses.common.constant.MatchRedisKeyConstant;
import com.biyao.moses.common.enums.RankConfigInfoEnum;
import com.biyao.moses.util.MatchRedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@EnableScheduling
public class RedisCache {

    @Autowired
    MatchRedisUtil matchRedisUtil;
    /**
     * moses-rank 存储在match redis中的配置信息
     */
    private Map<String, String> rankConfigMap = new HashMap<>();


    // 10分钟刷新一次
    @Scheduled(cron = "0 0/10 * * * ?")
    protected void refresh() {
        refreshMatchConfig();
    }
    @PostConstruct
    protected void init(){
        refresh();
    }

    /**
     * 刷新moses-rank的配置信息
     */
    private void refreshMatchConfig(){
        try{
            log.info("[任务进度][配置缓存]刷新配置信息开始");
            long startTime = System.currentTimeMillis();
            Boolean[] isRedisExceptionArray = new Boolean[]{false};
            Map<String, String> mosesConfigMapTmp = matchRedisUtil.hgetAll(MatchRedisKeyConstant.MOSES_RANK_CONFIG, isRedisExceptionArray);
            Boolean isRedisException = isRedisExceptionArray[0];
            if(isRedisException){
                log.error("[严重异常][配置缓存]从redis中获取配置信息时发生异常");
                return;
            }
            if(mosesConfigMapTmp == null){
                mosesConfigMapTmp = new HashMap<>();
            }
            rankConfigMap = mosesConfigMapTmp;
            log.info("[任务进度][配置缓存]刷新配置信息结束，耗时{}ms，配置个数{}", System.currentTimeMillis() - startTime, mosesConfigMapTmp.size());
        }catch (Exception e){
            log.error("[严重异常][配置缓存]刷新配置信息时发生异常 ", e);
        }
    }

    /**
     * 根据配置信息获取配置对应的值
     * @param rankConfigInfoEnum
     * @return
     */
    public String getRankConfigValue(RankConfigInfoEnum rankConfigInfoEnum){
        if(rankConfigInfoEnum == null || rankConfigMap == null){
            return null;
        }
        //如果没有对应的配置项则返回该配置项对应的默认值 默认值为null
        // 即 从redis中取数据 异常情况或者redis中没有数据都返回空
        return rankConfigMap.getOrDefault(rankConfigInfoEnum.getName(), rankConfigInfoEnum.getDefaultValue());
    }

}
