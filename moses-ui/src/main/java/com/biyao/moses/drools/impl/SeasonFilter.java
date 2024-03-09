package com.biyao.moses.drools.impl;

import com.alibaba.fastjson.JSONObject;
import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.cache.ProductSeasonCache;
import com.biyao.moses.cache.ProductSeasonCacheNoCron;
import com.biyao.moses.drools.Constant;
import com.biyao.moses.drools.Filter;
import com.biyao.moses.drools.FilterContext;
import com.biyao.moses.model.match2.MatchItem2;
import com.biyao.moses.params.ProductInfo;
import com.biyao.moses.rpc.UcRpcService;
import com.biyao.moses.util.PartitionUtil;
import com.biyao.moses.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @program: moses-parent
 * @description: 季节过滤
 * @author: changxiaowei
 * @create: 2021-03-25 11:37
 **/
@Slf4j
@Component(value = Constant.SEASON_FILTER)
public class SeasonFilter implements Filter {
    @Autowired
    PartitionUtil partitionUtil;
    @Autowired
    ProductSeasonCache productSeasonCache;
    @Autowired
    ProductDetailCache productDetailCache;
    @Autowired
    UcRpcService ucRpcService;
    @Override
    public List<MatchItem2> filter(FilterContext filterContext) {
        log.info("[检查日志][规则引擎]季节过滤服务，uuid:{}",filterContext.getUuid());
        List<MatchItem2> matchItem2List = filterContext.getMatchItem2List();
        String uuid =filterContext.getUuid();
        if(StringUtil.isBlank(uuid) || CollectionUtils.isEmpty(matchItem2List)){
            return matchItem2List;
        }
        try {
            // 用户季节
            int userSeason = partitionUtil.convertSeason2int(ucRpcService.getUserSeason(uuid));
            return matchItem2List.stream().filter(matchItem2 -> {
                // 商品季节
                int productSeasonValue = productSeasonCache.getProductSeasonValue(matchItem2.getProductId().toString());
                if (partitionUtil.isFilterByUserSeason(productSeasonValue, userSeason)) {
                    return false;
                }
                return true;
            }).collect(Collectors.toList());
        }catch (Exception e){
            log.error("[严重异常][规则引擎]季节过滤时发生异常,参数{}", JSONObject.toJSONString(filterContext),e);
        }
        return matchItem2List;
    }
}
