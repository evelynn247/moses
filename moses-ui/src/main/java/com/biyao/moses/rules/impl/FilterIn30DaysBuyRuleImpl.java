package com.biyao.moses.rules.impl;


import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.rpc.UcRpcService;
import com.biyao.moses.rules.Rule;
import com.biyao.moses.rules.RuleConst;
import com.biyao.moses.rules.RuleContext;
import com.uc.domain.bean.User;
import com.uc.domain.constant.UserFieldConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author gaojian
 * @date 过滤30天下单商品规则
 */
@Slf4j
@Component(RuleConst.RULE_30DAYSBUY_FILTER)
public class FilterIn30DaysBuyRuleImpl implements Rule {

    @Autowired
    private UcRpcService ucRpcService;

    @Override
    public List<TotalTemplateInfo> ruleRank(RuleContext ruleContext) {
        Integer uid = ruleContext.getUid();
        List<TotalTemplateInfo> allProductList = ruleContext.getAllProductList();
        if (uid == null || CollectionUtils.isEmpty(allProductList)) {
            return allProductList;
        }
        List<TotalTemplateInfo> result = allProductList;
        try {
            List<String> list = new ArrayList<>();
            list.add(UserFieldConstants.ORDERPIDS30D);
            User user = ucRpcService.getData(null, uid.toString(), list, "moses");
            if (user == null) {
                return allProductList;
            }
            Set<Long> orderPids30dSet = user.getOrderPids30d();

            if (CollectionUtils.isEmpty(orderPids30dSet)) {
                return allProductList;
            }

            // 30日购买记录全部过滤
            result = allProductList.stream()
                    .filter(info -> !orderPids30dSet.contains(info.getId()))
                    .collect(Collectors.toList());
        } catch(Exception e){
            log.error("[严重异常][过滤机制]过滤用户已购买商品时发生异常 ", e);
        }
        return result;
    }


}
