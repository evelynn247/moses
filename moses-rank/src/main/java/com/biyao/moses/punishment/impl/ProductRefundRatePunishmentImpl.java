package com.biyao.moses.punishment.impl;
import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.cache.RedisCache;
import com.biyao.moses.common.enums.RankConfigInfoEnum;
import com.biyao.moses.model.match2.MatchItem2;
import com.biyao.moses.params.ProductInfo;
import com.biyao.moses.params.rank2.RankRequest2;
import com.biyao.moses.punishment.PunishmentService;
import com.biyao.moses.util.StringUtil;
import com.uc.domain.bean.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Slf4j
@Component
public class ProductRefundRatePunishmentImpl implements PunishmentService {

    @Autowired
    RedisCache redisCache;
    @Autowired
    ProductDetailCache productDetailCache;
    @Override
    public Map<Long, Double> getPunishment(String uuid, List<MatchItem2> matchItemList, User user) {

        double refundRateMin;
        double refundRateMax;
        Map<Long, Double> punishmentMap = new HashMap<>();
        // 商品数据为空 直接返回
        if (CollectionUtils.isEmpty(matchItemList)) {
            return punishmentMap;
        }
        //获取缓存中的配置参数 M和N
        String returnRefundRateLimit = redisCache.getRankConfigValue(RankConfigInfoEnum.ReturnRefundRateLimit);
        // 配置信息结果为空 不做惩罚
        if(StringUtil.isBlank(returnRefundRateLimit)){
            log.info("[配置校验]redis中未配置退货退款率惩罚配置参数");
            return punishmentMap;
        }
        // 格式必须为 num1-num2 且前num1<num2
        String[] min_max = returnRefundRateLimit.split("-");
        // 判断格式是否为“ num1-num2 ” 且 num1和num2 不能为空
        if(min_max.length !=2 || StringUtil.isBlank(min_max[0]) || StringUtil.isBlank(min_max[1])){
            log.error("[严重异常]退货退款率惩罚配置参数格式错误：returnRefundRateLimit={}", returnRefundRateLimit);
            return punishmentMap;
        }
        try {
            refundRateMin = Double.valueOf(min_max[0]);
            refundRateMax = Double.valueOf(min_max[1]);
            if(!(refundRateMin <= 100d&&refundRateMin >= 0d &&refundRateMax <= 100d&&refundRateMax >= 0d &&refundRateMax > refundRateMin)){
                log.error("[严重异常]退货退款率惩罚配置参数错误：refundRateMin={}，refundRateMax={}",refundRateMin,refundRateMax);
                return  punishmentMap;
            }

            for (MatchItem2 product:matchItemList){
                //获取商品详细信息
                ProductInfo productInfo = productDetailCache.getProductInfo(product.getProductId());
                if(productInfo == null || productInfo.getReturnRefundRate() == null){
                    continue;
                }
                //折算成百分比的形式 0.25 -->25
                double returnRefundRatePercent = productInfo.getReturnRefundRate()*100d;
                //商品退货退款率小于受惩罚的最小值  不惩罚
                if(returnRefundRatePercent <= refundRateMin){
                    continue;
                }
                double punishment = returnRefundRatePercent >= refundRateMax?(1/10000d):((refundRateMax-returnRefundRatePercent)/(refundRateMax-refundRateMin));
                punishmentMap.put(product.getProductId(),punishment);
            }

        } catch (NumberFormatException e){
            log.error("[严重异常]退货退款率惩罚配置参数格式错误:returnRefundRateLimit={}",returnRefundRateLimit);
            return  punishmentMap;
        }catch (Exception e){
            log.error("[严重异常]获取商品退货退款率惩罚因子异常， uuid {}", uuid, e);
            return  punishmentMap;
        }
       return punishmentMap;
    }

    @Override
    public Map<Long, Double> getPunishment(RankRequest2 rankRequest2, User user) {
        return getPunishment(rankRequest2.getUuid(),rankRequest2.getMatchItemList(),user);
    }
}
