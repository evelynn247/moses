package com.biyao.moses.punishment.impl;

import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.model.match2.MatchItem2;
import com.biyao.moses.params.ProductInfo;
import com.biyao.moses.params.rank2.RankRequest2;
import com.biyao.moses.punishment.PunishmentService;
import com.uc.domain.bean.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @ClassName ProductPriceFactor
 * @Description 商品价格奖励因子
 * @Author xiaojiankai
 * @Date 2019/12/17 16:43
 * @Version 1.0
 **/
@Slf4j
@Component
public class ProductPriceFactorImpl implements PunishmentService {
    @Autowired
    ProductDetailCache productDetailCache;

    @Override
    public Map<Long, Double> getPunishment(String uuid, List<MatchItem2> matchItemList, User user) {
        Map<Long, Double> factorMap = new HashMap<>();
        if (CollectionUtils.isEmpty(matchItemList)) {
            return factorMap;
        }
        try {
            for (MatchItem2 matchItem2 : matchItemList) {
                if(matchItem2 == null || matchItem2.getProductId() == null){
                    continue;
                }
                Long productId = matchItem2.getProductId();
                ProductInfo productInfo = productDetailCache.getProductInfo(productId);
                if(productInfo == null || productInfo.getPrice() == null){
                    continue;
                }
                Long price = productInfo.getPrice();
                factorMap.put(productId, Math.log10(price.doubleValue()/100));
            }
        }catch (Exception e){
            log.error("[严重异常]获取商品价格奖励因子异常， uuid {}", uuid, e);
        }

        return factorMap;
    }

    @Override
    public Map<Long, Double> getPunishment(RankRequest2 rankRequest2, User user) {
        return getPunishment(rankRequest2.getUuid(),rankRequest2.getMatchItemList(),user);
    }
}
