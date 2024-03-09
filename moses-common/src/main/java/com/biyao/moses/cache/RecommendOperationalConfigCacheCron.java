package com.biyao.moses.cache;

import com.alibaba.fastjson.JSON;
import com.biyao.client.model.PDCResponse;
import com.biyao.client.model.RecommendOperationalConfig;
import com.biyao.client.service.IRecommendOperationalConfigDubboService;
import lombok.extern.slf4j.Slf4j;
import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * @program: moses-parent
 * @description: 推荐运营位规则配置缓存基类
 * @author: changxiaowei
 * @create: 2021-02-18 13:40
 **/
@Slf4j
public class RecommendOperationalConfigCacheCron {

    @Resource
    IRecommendOperationalConfigDubboService recommendOperationalConfigDubboService;

    private Map<String, RecommendOperationalConfig> recommendOperationalConfigMap=new HashMap<>();

    protected void init(){
        refreshRecommendOperationalConfigCache();
    }

    protected void refreshRecommendOperationalConfigCache(){
        recommendOperationalConfigMap = getAllRecommendOperationalConfig();
    }
    private  Map<String, RecommendOperationalConfig> getAllRecommendOperationalConfig(){

        Map<String, RecommendOperationalConfig> resultMap =new HashMap<>();
        try {
            PDCResponse<Map<String, RecommendOperationalConfig>> response = recommendOperationalConfigDubboService.getAllOperationalConfig();
            if (null == response || response.getCode() != 1) {
                log.error("[严重异常][推荐运营位规则配置缓存]调用IRecommendOperationConfigDubboService.getAllOperationalConfig报错，response:{}",
                        JSON.toJSONString(response));
                return resultMap;
            }
            resultMap= response.getData();
        }catch (Exception e){
            log.error("[严重异常][dubbo异常]获取推荐运营位规则配置(接口IRecommendOperationalConfigDubboService.getAllOperationalConfig)接口时发生异常,异常堆栈信息：",e);
        }
        return resultMap;
    }

    /**
     * 很据页面位置返回该页面位置对应的运营位配置规则
     * @param pageId
     * @return
     */
    public RecommendOperationalConfig getOperationalConfigByOperationalPositionId(String pageId){
            return recommendOperationalConfigMap.get(pageId);
    }

}
