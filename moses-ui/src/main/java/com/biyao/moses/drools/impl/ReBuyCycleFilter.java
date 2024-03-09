package com.biyao.moses.drools.impl;

import com.alibaba.fastjson.JSONObject;
import com.biyao.moses.cache.Category3RebuyCycleCache;
import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.drools.Constant;
import com.biyao.moses.drools.Filter;
import com.biyao.moses.drools.FilterContext;
import com.biyao.moses.model.match2.MatchItem2;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.params.ProductInfo;
import com.biyao.moses.rpc.UcRpcService;
import com.biyao.moses.rules.impl.RebuyCycleFilterRuleImpl;
import com.biyao.moses.util.FilterUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @program: moses-parent
 * @description: 复购周期过滤
 * @author: changxiaowei
 * @create: 2021-03-25 11:50
 **/
@Slf4j
@Component(value = Constant.REBUY_FILTER)
public class ReBuyCycleFilter implements Filter {

    @Autowired
    private UcRpcService ucRpcService;

    @Autowired
    private ProductDetailCache productDetailCache;

    @Autowired
    private Category3RebuyCycleCache category3RebuyCycleCache;

    @Autowired
    RebuyCycleFilterRuleImpl rebuyCycleFilterRule;

    private static long lastBuy30d2Ms = 2592000000L;//30*24*3600*1000
    @Override
    public List<MatchItem2> filter(FilterContext filterContext) {
        log.info("[检查日志][规则引擎]复购周期过滤服务，uuid:{}",filterContext.getUuid());
        Integer uid = filterContext.getUid();
        String uuid = filterContext.getUuid();
        List<MatchItem2> matchItem2List = filterContext.getMatchItem2List();
        if (uid == null || uid == 0 || CollectionUtils.isEmpty(matchItem2List)) {
            return matchItem2List;
        }
        List<MatchItem2> result = matchItem2List;

        try {
            List<String> orderPids = ucRpcService.getOrderPids(uid);
            if(CollectionUtils.isEmpty(orderPids)){
                log.error("[检查日志][规则引擎]获取用户近期购买商品为空，uid:{}",uid);
                return matchItem2List;
            }
            //解析获取X天内购买的商品
            Map<Long, Long> orderPidTimeMap = rebuyCycleFilterRule.parseAndFilterPidTime(orderPids, uuid);

            if (CollectionUtils.isEmpty(orderPidTimeMap)) {
                log.error("[严重异常][规则引擎]解析用户已购买商品失败");
                return matchItem2List;
            }
            long nowTime = System.currentTimeMillis();
            //根据后台三级类目复购周期进行过滤
            result = matchItem2List.stream()
                    .filter(info -> {
                        Long productId =info.getProductId();
                        if (!orderPidTimeMap.containsKey(productId)) {
                            return true;
                        }
                        Long buyTime = orderPidTimeMap.getOrDefault(productId, 0L);
                        ProductInfo productInfo = productDetailCache.getProductInfo(productId);
                        if (FilterUtil.isCommonFilter(productInfo)) {
                            return false;
                        }
                        Long rebuyCycleMs = category3RebuyCycleCache.getRebuyCycleMs(productInfo.getThirdCategoryId());
                        long lastBuyMs = nowTime - buyTime;
                        if (rebuyCycleMs == null && lastBuyMs > lastBuy30d2Ms) {
                            //如果没有复购周期，且上次购买时间距今超过30天则不过滤
                            return true;
                        } else if (rebuyCycleMs != null && lastBuyMs >= rebuyCycleMs) {
                            //如果有复购周期，且上次购买时间距今大于等于复购周期则不过滤
                            return true;
                        }
                        return false;
                    })
                    .collect(Collectors.toList());
        }catch (Exception e){
            log.error("[严重异常][规则引擎]根据复购周期过滤时发生异常,参数{}", JSONObject.toJSONString(filterContext),e);
        }
        return  result;
    }
}
