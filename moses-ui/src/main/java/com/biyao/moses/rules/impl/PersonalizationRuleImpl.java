package com.biyao.moses.rules.impl;

import com.biyao.moses.context.ByUser;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.params.BaseRequest2;
import com.biyao.moses.rules.Rule;
import com.biyao.moses.rules.RuleConst;
import com.biyao.moses.rules.RuleContext;
import com.biyao.moses.util.PartitionUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * 相邻商品(包括斜侧)相似类目岔开
 */
@Slf4j
@Component(value = RuleConst.RULE_PERSONALIZATION)
public class PersonalizationRuleImpl implements Rule {

    @Autowired
    private PartitionUtil partitionUtil;

    @Override
    public List<TotalTemplateInfo> ruleRank(RuleContext ruleContext) {

        List<TotalTemplateInfo> result = new ArrayList<>();//返回集合
        if(CollectionUtils.isEmpty(ruleContext.getAllProductList())){
            return result;
        }
        try {
            List<TotalTemplateInfo> allProductList = new LinkedList<>(ruleContext.getAllProductList());
            List<TotalTemplateInfo> waitInsertProductList = ruleContext.getWaitInsertProductList();
            BaseRequest2 baseRequest2 = ruleContext.getBaseRequest2();
            ByUser byUser = ruleContext.getByUser();
            String pageIndex = ruleContext.getUiBaseRequest().getPageIndex();
            int pageSize = ruleContext.getUiBaseRequest().getPageSize();
            if(pageSize <= 0){
                pageSize = 20;
            }
            result = partitionUtil.dealInsert(allProductList, waitInsertProductList, baseRequest2, pageIndex, pageSize, byUser);
        }catch (Exception e){
            log.error("[严重异常]个性化岔开错误，", e);
            result = ruleContext.getAllProductList();
        }
        return result.size()> 500 ? result.subList(0, 500) : result;
    }
}
