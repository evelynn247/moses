package com.biyao.moses.cache;

import com.biyao.moses.common.constant.MatchRedisKeyConstant;
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
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
@EnableScheduling
public class Similar3AndNotFilterCache {

    @Autowired
    MatchRedisUtil matchRedisUtil;

    private Set<String> notFilterfCategoryIdSet;

    // 10分钟刷新一次
    @Scheduled(cron = "0 0/10 * * * ?")
    protected void refresh() {
        refreshNotFilterCustomPidFcateIdSet();
    }

    @PostConstruct
    protected void init(){
        refresh();
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


