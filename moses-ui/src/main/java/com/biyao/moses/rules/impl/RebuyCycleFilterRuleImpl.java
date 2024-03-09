package com.biyao.moses.rules.impl;

import com.biyao.moses.cache.Category3RebuyCycleCache;
import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.params.ProductInfo;
import com.biyao.moses.rpc.UcRpcService;
import com.biyao.moses.rules.Rule;
import com.biyao.moses.rules.RuleConst;
import com.biyao.moses.rules.RuleContext;
import com.biyao.moses.util.FilterUtil;
import com.uc.domain.bean.User;
import com.uc.domain.constant.UserFieldConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @ClassName RebuyCycleFilterRuleImpl
 * @Description 根据后台三级类目复购周期过滤已购买商品集合
 * @Author xiaojiankai
 * @Date 2020/1/19 13:38
 * @Version 1.0
 **/
@Slf4j
@Component(RuleConst.Rebuy_FILTER)
public class RebuyCycleFilterRuleImpl implements Rule {

    @Autowired
    private UcRpcService ucRpcService;

    @Autowired
    private ProductDetailCache productDetailCache;

    @Autowired
    private Category3RebuyCycleCache category3RebuyCycleCache;

    private static long lastBuy30d2Ms = 2592000000L;//30*24*3600*1000

    private static long lastBuy60d2Ms = 5184000000L;//60*24*3600*1000

    @Override
    public List<TotalTemplateInfo> ruleRank(RuleContext ruleContext) {
        Integer uid = ruleContext.getUid();
        String uuid = ruleContext.getUuid();
        List<TotalTemplateInfo> allProductList = ruleContext.getAllProductList();
        if (uid == null || uid == 0 || CollectionUtils.isEmpty(allProductList)) {
            return allProductList;
        }
        List<TotalTemplateInfo> result = allProductList;
        try {
            List<String> list = new ArrayList<>();
            list.add(UserFieldConstants.ORDERPIDS);
            User user = ucRpcService.getData(null, uid.toString(), list, "moses");
            if (user == null) {
                return allProductList;
            }
            List<String> orderPidsList = user.getOrderPids();
            if (CollectionUtils.isEmpty(orderPidsList)) {
                return allProductList;
            }

            //解析获取X天内购买的商品
            Map<Long, Long> orderPidTimeMap = parseAndFilterPidTime(orderPidsList, uuid);
            if (CollectionUtils.isEmpty(orderPidTimeMap)) {
                return allProductList;
            }

            long nowTime = System.currentTimeMillis();
            //根据后台三级类目复购周期进行过滤
            result = allProductList.stream()
                    .filter(info -> {
                        Long productId = Long.valueOf(info.getId());
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
            log.error("[严重异常][过滤机制]根据复购周期过滤时发生异常 ", e);
        }
        return result;
    }

    /**
     * 解析用户下单商品及其时间
     * @param orderPidTimeList
     * @param uuid
     * @return
     */
    public Map<Long, Long> parseAndFilterPidTime(List<String> orderPidTimeList, String uuid){
        Map<Long, Long> result = new HashMap<>();
        if(CollectionUtils.isEmpty(orderPidTimeList)){
            return result;
        }
        boolean isCheckError = false;
        long nowTime = System.currentTimeMillis();
        for(String pidTimeStr : orderPidTimeList){
            try {
                if (StringUtils.isBlank(pidTimeStr)) {
                    isCheckError = true;
                    continue;
                }
                String[] pidTimeArray = pidTimeStr.trim().split(":");
                if (pidTimeArray.length != 2) {
                    isCheckError = true;
                    continue;
                }
                String pidStr = pidTimeArray[0];
                String timeStr = pidTimeArray[1];
                if(StringUtils.isBlank(pidStr) || StringUtils.isBlank(timeStr)){
                    isCheckError = true;
                    continue;
                }
                Long pid = Long.valueOf(pidStr);
                Long time = Long.valueOf(timeStr);
                long orderMs = nowTime - time;
                //如果已购买时间超过X天 并且超过30天，则不过滤该商品
                if(orderMs > lastBuy60d2Ms && orderMs > lastBuy30d2Ms){
                    continue;
                }

                if(result.containsKey(pid)){
                    if(result.get(pid).longValue() < time.longValue()){
                        result.put(pid, time);
                    }
                }else{
                    result.put(pid, time);
                }
            }catch (Exception e){
                isCheckError = true;
            }
        }
        if(isCheckError){
            log.error("[一般异常]解析用户已购买商品失败， uuid {}", uuid);
        }
        return result;
    }
}
