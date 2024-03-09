package com.biyao.moses.pdc.domain;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @program: moses-parent-online
 * @description: 场景规则映射关系po对象
 * @author: changxiaowei
 * @Date: 2021-12-02 16:46
 **/
@Data
public class SceneRuleRelationPo {
    /**
     * 规则id
     */
    private String ruleId;
    /**
     * 场景id
     */
    private String sceneId;
    /**
     * 召回规则
     */
    private String matchRule;
}
