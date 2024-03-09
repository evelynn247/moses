package com.biyao.moses.punishment.impl;

import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.cache.SwitchConfigCache;
import com.biyao.moses.model.match2.MatchItem2;
import com.biyao.moses.params.ProductInfo;
import com.biyao.moses.params.rank2.RankRequest2;
import com.biyao.moses.punishment.PunishmentService;
import com.uc.domain.bean.User;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @program: moses-parent
 * @description: 特权金因子
 * @author: changxiaowei
 * @Date: 2022-01-27 11:36
 **/
@Slf4j
@Component
public class ProductTQJFactorImpl implements PunishmentService {


    /**
     * rsm中配置的特权金因子 A
     */
    private static final String TQJFACTOR_A = "Aexciter";
    /**
     * rsm中配置的特权金因子 M
     */
    private static final String TQJFACTOR_M = "Mexciter";

    @Autowired
    private ProductDetailCache productDetailCache;
    @Autowired
    SwitchConfigCache switchConfigCache;

    @Override
    public Map<Long, Double> getPunishment(String uuid, List<MatchItem2> matchItemList, User user) {
        return null;
    }

    @Override
    public Map<Long, Double> getPunishment(RankRequest2 rankRequest2, User user) {
        Map<Long, Double> factorMap = new HashMap<>();
        List<MatchItem2> matchItemList = rankRequest2.getMatchItemList();
        //如果用户账户内没有特权金可用金额 直接返回factorMap
        String tqjMaxLimit = rankRequest2.getTqjMaxLimit();
        if (StringUtils.isEmpty(tqjMaxLimit)) {
            return factorMap;
        }

        BigDecimal usableTqj ;
        try {
            usableTqj = new BigDecimal(tqjMaxLimit);
        } catch (Exception e) {
            log.error("[严重异常]用户可用特权金金额格式错误，uuid:{},tqjMaxLimit:{}",rankRequest2.getUuid(),tqjMaxLimit,e);
            return factorMap;
        }
        // 遍历matchItemList
        for (MatchItem2 matchItem2 : matchItemList) {
            Long productId = matchItem2.getProductId();
            ProductInfo productInfo = productDetailCache.getProductInfo(productId);
            try {
                BigDecimal newPrivilateDeduct = productInfo.getNewPrivilateDeduct();
                //newPrivilate 取值范围【1,10000】
                double realNewPrivilate = 0d;
                if (Objects.nonNull(newPrivilateDeduct)){
                    double originalNewPrivilate = newPrivilateDeduct.doubleValue();
                    realNewPrivilate = originalNewPrivilate > 10000d ? 10000d : (originalNewPrivilate < 1d ? 1d : originalNewPrivilate);
                }
                if ( new BigDecimal(realNewPrivilate).compareTo(usableTqj) >= 0) {
                    factorMap.put(productId, getTqjFactorAFromRsm());
                } else {
                    // B = M*lgT+1
                    factorMap.put(productId, getTqjFactorMFromRsm() * Math.log10(realNewPrivilate) + 1);
                }
            } catch (Exception e) {
                log.error("[严重异常]商品计算特权金因子时异常，商品id：{}，uuid：{},异常信息：{}",productId,rankRequest2.getUuid(),e);
            }
        }
        return factorMap;
    }


    /**
     * @return double
     * @Des 获取特权金因子A --- Aexciter
     * @Param
     * @Author changxiaowei
     * @Date 2022/2/8
     */
    private double getTqjFactorAFromRsm() {
        double defaultValue = 1.3;
        String aexciter = switchConfigCache.getRecommendContentByConfigId(TQJFACTOR_A);
        if (StringUtils.isEmpty(aexciter)) return defaultValue;
        try {
            double aexciterD = Double.valueOf(aexciter);
            if (aexciterD < 1D || aexciterD > 10D) return defaultValue;
            return aexciterD;
        } catch (Exception e) {
            log.error("[严重异常]特权金因子A（Aexciter）配置异常,aexciter={}", aexciter);
        }
        return defaultValue;
    }

    /**
     * @return double
     * @Des 获取特权金因子M--- Mexciter
     * @Param
     * @Author changxiaowei
     * @Date 2022/2/8
     */
    private double getTqjFactorMFromRsm() {
        double defaultValue = 0.1;
        String mexciter = switchConfigCache.getRecommendContentByConfigId(TQJFACTOR_M);
        if (StringUtils.isEmpty(mexciter)) return defaultValue;
        try {
            double mexciterD = Double.valueOf(mexciter);
            if (mexciterD < 0D || mexciterD > 1000D) return defaultValue;
            return mexciterD;
        } catch (Exception e) {
            log.error("[严重异常]特权金因子M（Mexciter）配置异常,mexciter={}", mexciter);
        }
        return defaultValue;
    }
}
