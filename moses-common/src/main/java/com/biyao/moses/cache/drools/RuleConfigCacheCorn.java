package com.biyao.moses.cache.drools;

import com.alibaba.fastjson.JSONObject;
import com.biyao.moses.model.drools.RuleBaseFact;
import com.biyao.moses.model.drools.RuleFact;
import com.biyao.moses.util.drools.RuleFileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @program: moses-parent
 * @description: 规则配置缓存
 * @author: changxiaowei
 * @create: 2021-03-26 17:28
 **/
@Slf4j
public class RuleConfigCacheCorn {

    private List<RuleFact> ruleFactCache = new ArrayList<>();

    /**
     * 加载规则
     */
    protected void loadRule(String fileUrl) {
        ruleFactCache = RuleFileUtil.loadFile(fileUrl);
        log.info("[检查日志]加载规则配置文件结束，文件信息：{}", JSONObject.toJSONString(ruleFactCache));
    }

    /**
     * 获取满足条件的规则
     */
    public RuleFact getRuleFactByCondition(RuleBaseFact ruleBaseFact,String caller) {
        if (CollectionUtils.isEmpty(ruleFactCache)) {
            return null;
        }
        RuleFact result = null;
        for (RuleFact ruleFact : ruleFactCache) {
            try {
                // 场景id
                if(!ruleFact.getScene().equals(ruleBaseFact.getScene())){
                    continue;
                }
                // 用户类型
                List<String> utypeList = Arrays.asList(ruleFact.getUtype().split(","));
                if(!(utypeList.contains("0")||utypeList.contains(ruleBaseFact.getUtype()))){
                    continue;
                }
                //分端隐藏
                List<String> siteIdList = Arrays.asList(ruleFact.getSiteId().split(","));
                if(!(siteIdList.contains("0")||siteIdList.contains(ruleBaseFact.getSiteId()))){
                    continue;
                }
                // 个性化
                if(!ruleFact.getIsPersonal().equals(ruleBaseFact.getIsPersonal())){
                    continue;
                }
                // 条件大于白名单 白名单大于流量
                if(ruleFact.getWhiteList().contains(ruleBaseFact.getUuid())){
                    return ruleFact;
                }
                // 流量
                Integer hash = RuleFileUtil.hash(ruleBaseFact.getUuid(), caller);
                if(hash >= ruleFact.getBucketMaxValue() || hash < ruleFact.getBucketMinValue()){
                    continue;
                }
                result = ruleFact;
            }catch (Exception e){
                log.error("[严重异常]根据条件获取满足的规则异常，异常信息：",e);
            }
        }
        return result;
    }
}
