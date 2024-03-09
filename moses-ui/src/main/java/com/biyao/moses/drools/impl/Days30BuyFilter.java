package com.biyao.moses.drools.impl;

import com.alibaba.fastjson.JSONObject;
import com.biyao.moses.drools.Constant;
import com.biyao.moses.drools.Filter;
import com.biyao.moses.drools.FilterContext;
import com.biyao.moses.model.match2.MatchItem2;
import com.biyao.moses.rpc.UcRpcService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @program: moses-parent
 * @description: 过滤30天内购买的数据
 * @author: changxiaowei
 * @create: 2021-03-25 11:40
 **/
@Slf4j
@Component(value = Constant.DAY30SBUY_FILTER)
public class Days30BuyFilter implements Filter {

    @Autowired
    UcRpcService ucRpcService;

    @Override
    public List<MatchItem2> filter(FilterContext filterContext) {
        log.info("[检查日志][规则引擎]过滤30天内购买的数据，uuid:{}",filterContext.getUuid());
        Integer uid = filterContext.getUid();
        List<MatchItem2> matchItem2List = filterContext.getMatchItem2List();
        if (uid == null || CollectionUtils.isEmpty(matchItem2List)) {
            return matchItem2List;
        }
        List<MatchItem2> result = matchItem2List;
        try {
            // 获取用户30天已购买的商品
            Set<Long> orderPids30d = ucRpcService.getOrderPids30d(uid);
            if (CollectionUtils.isEmpty(orderPids30d)) {
                log.info("[检查日志]获取用户{}30天已购买商品为空", uid);
                return matchItem2List;
            }
            // 30日购买记录全部过滤
            result = matchItem2List.stream()
                    .filter(matchItem2 -> !orderPids30d.contains(matchItem2.getProductId()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("[严重异常][规则引擎]用户{}30天已购买商品过滤时发生异常,参数{}", JSONObject.toJSONString(filterContext),e);
        }
        return result;
    }
}
