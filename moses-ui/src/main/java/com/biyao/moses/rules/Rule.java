package com.biyao.moses.rules;

import com.biyao.moses.model.template.entity.TotalTemplateInfo;

import java.util.List;

/**
 * @author zhaiweixi@idstaff.com
 * @date 2019/10/12
 **/
public interface Rule {
    /**
     * 根据规则重排序
     * @return
     */
    List<TotalTemplateInfo> ruleRank(RuleContext ruleParam);
}
