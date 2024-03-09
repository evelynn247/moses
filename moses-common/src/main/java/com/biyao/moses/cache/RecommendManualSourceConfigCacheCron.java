package com.biyao.moses.cache;

import com.alibaba.fastjson.JSON;
import com.biyao.client.model.PDCResponse;
import com.biyao.client.service.IRecommendManualSourceDubboService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @program: moses-parent
 * @description: 推荐人工配置召回源缓存
 * @author: changxiaowei
 * @create: 2021-02-18 11:21
 **/
@Slf4j
public class RecommendManualSourceConfigCacheCron {

    @Resource
    IRecommendManualSourceDubboService recommendManualSourceConfigDubboService;

    private Map<String, String> recommendManualSourceMap = new HashMap<>();

    protected void init(){
        refreshAllManualSourceConfigCache();
    }

    protected void refreshAllManualSourceConfigCache(){
        Map<String, String> resultMap= getAllManualSourceConfig();
        // 非异常情况更新缓存
         if(resultMap != null){
             recommendManualSourceMap=resultMap;
         }
    }
    private Map<String, String> getAllManualSourceConfig(){
        try {
            PDCResponse<Map<String, String>> response = recommendManualSourceConfigDubboService.getAllManualSourceInfo();
            if (null == response || response.getCode() != 1) {
                log.error("[严重异常][获取人工配置召回源信息]调用IRecommendManualSourceDubboService.getAllManualSourceInfo报错，response:{}",
                        JSON.toJSONString(response));
                return null;
            }
            return response.getData();
        }catch (Exception e){
            log.error("[严重异常][dubbo异常]获取人工配置召回源信息异常(接口IRecommendManualSourceDubboService.getAllManualSourceInfo)接口时发生异常,异常堆栈信息：",e);
            return null;
        }
    }

    /**
     * 根据召回源id获取人工配置的召回源商品和分数
     * @param sourceId 召回源id
     * @return 数据格式：pid1:score1，pid2:score2 ... ...
     */
    public String getManualSourcePidBySourceId(String sourceId){
        if(!CollectionUtils.isEmpty(recommendManualSourceMap)){
            return recommendManualSourceMap.get(sourceId);
        }
        return null;
    }

    public Set<String>  getRecommendManualSourceMapKey(){
        if(CollectionUtils.isEmpty(recommendManualSourceMap)){
            return null;
        }
        return recommendManualSourceMap.keySet();
    }
}
