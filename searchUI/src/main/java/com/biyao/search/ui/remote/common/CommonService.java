package com.biyao.search.ui.remote.common;

import com.alibaba.dubbo.common.utils.StringUtils;
import com.biyao.search.ui.cache.RedisDataCache;
import com.biyao.search.ui.remote.request.UISearchRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author zj
 * @version 1.0
 * @date 2020/4/22 18:03
 * @description 公共Service，用于提供公共方法
 */
@Service
@Slf4j
public class CommonService {

    @Autowired
    private RedisDataCache redisDataCache;


    /**
     * 梦工厂流量分流校验
     * 用户uid在白名单中，返回true
     * 用户uid%100在梦工厂分流区间中，返回true
     * 其余返回false
     * @param request
     * @return
     */
    public boolean checkFlowLimit(UISearchRequest request) {
        try{
            //uid配置在白名单中，直接返回true
            String uid = request.getUid() == null? "":request.getUid().toString();
            if(redisDataCache.getDeriveWhiteList().contains(uid)){
                return true;
            }
            //用户uid%100在梦工厂分流区间中，返回true
            if(StringUtils.isNotEmpty(uid)){
                Integer uidCode = Integer.valueOf(uid) % 100;
                if(uidCode < redisDataCache.getFlowLimitR() && uidCode >= redisDataCache.getFlowLimitL()){
                    return true;
                }
            }
        }catch(Exception e){
            log.error("[严重异常]梦工厂分流判断异常：", e);
        }
        return false;
    }
}
