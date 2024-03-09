package com.biyao.moses.cache.drools;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @program: moses-parent
 * @description: 规则配置缓存
 * @author: changxiaowei
 * @create: 2021-03-26 18:38
 **/
@Component
public class RuleConfigCache extends RuleConfigCacheCorn {
    /**
     * 规则配置地址
     */
    private final static String EXP_CONF_URL = "http://conf.nova.biyao.com/nova/recrule.conf";
    @PostConstruct
    public void loadRule() {
        super.loadRule(EXP_CONF_URL);
    }
}
