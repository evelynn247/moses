package com.biyao.moses.punishment.impl;

import com.alibaba.fastjson.JSONObject;
import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.model.match2.MatchItem2;
import com.biyao.moses.params.ProductInfo;
import com.biyao.moses.params.SuProductInfo;
import com.biyao.moses.params.rank2.RankRequest2;
import com.biyao.moses.punishment.PunishmentService;
import com.uc.domain.bean.User;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * 尺码惩罚因子计算
 */
@Slf4j
@Component
public class ProductSizePunishmentImpl implements PunishmentService {

    @Autowired
    private ProductDetailCache productDetailCache;

    private static final double punishNum = 0.0001;

    @Override
    public Map<Long, Double> getPunishment(String uuid, List<MatchItem2> matchItemList, User user) {
        Map<Long, Double> punishmentMap = new HashMap<>();
        if (CollectionUtils.isEmpty(matchItemList)) {
            return punishmentMap;
        }
        Map<String, String> personalSizeMap = new HashMap<>();
        if (user != null && user.getPersonalSize() != null) {
            personalSizeMap = user.getPersonalSize();
        }

        for (MatchItem2 item : matchItemList) {
            try {
                ProductInfo productInfo = productDetailCache.getProductInfo(item.getProductId());
                List<SuProductInfo> suProductList = productInfo.getSuProductList();

                if (CollectionUtils.isEmpty(suProductList)) {
                    punishmentMap.put(item.getProductId(), punishNum);
                    continue;
                }

                //黄金尺码是决定尺码对于该商品是否重要的决定性因素，商品是否设置黄金尺码重要性要高于用户个性化尺码的重要性，当Spu未设置黄金尺码时，则不再考虑尺码惩罚
                if (productInfo.getThirdCategoryId() == null || productInfo.getThirdCategoryId() <= 0
                        || productInfo.getIsSetGoldenSize() == null || productInfo.getIsSetGoldenSize().toString().equals("0")) {
                    continue;
                }

                String sizeStr = personalSizeMap.get(productInfo.getThirdCategoryId().toString());

                //Spu后台三级类目与用户个性化尺码后台三级类目不相同（该商品不是用户个性化商品，不需要关注个性化尺码，但是需要关心黄金尺码）
                if (StringUtils.isBlank(sizeStr)) {
                    //Spu所有黄金尺码Sku不可售，则进行惩罚
                    if (CollectionUtils.isEmpty(productInfo.getGoldenSizeSu())) {
                        punishmentMap.put(item.getProductId(), punishNum);
                        continue;
                    }
                }
                //Spu后台三级类目与用户个性化尺码后台三级类目相同（该商品为用户个性化商品，需要关注个性化尺码）
                else {
                    //若该Spu下所有黄金尺码Sku都不可售 && 该Spu下个性化尺码Sku都不可售，则进行惩罚
                    if (!hasPersonalSizeSku(sizeStr, suProductList) &&
                            CollectionUtils.isEmpty(productInfo.getGoldenSizeSu())) {
                        punishmentMap.put(item.getProductId(), punishNum);
                        continue;
                    }
                }
            } catch (Exception e) {
                log.error("[严重异常]计算尺码惩罚因子异常，pid {}", item.getProductId(), e);
                punishmentMap.put(item.getProductId(), 1.0);
                continue;
            }
        }

        return punishmentMap;
    }

    @Override
    public Map<Long, Double> getPunishment(RankRequest2 rankRequest2, User user) {
        return getPunishment(rankRequest2.getUuid(),rankRequest2.getMatchItemList(),user);
    }

    /**
     * 是否存在可售个性化尺码sku
     *
     * @param sizeStr
     * @param suProductList
     * @return
     */
    private boolean hasPersonalSizeSku(String sizeStr, List<SuProductInfo> suProductList) {

        if (CollectionUtils.isEmpty(suProductList) || StringUtils.isBlank(sizeStr)) {
            return false;
        }

        //存放用户个性化size
        Set<String> personalSizeSet = new HashSet<>();
        String[] split = sizeStr.split(",");
        for (int i = 0; i < split.length; i++) {
            personalSizeSet.add(split[i]);
        }

        for (SuProductInfo su : suProductList) {
            JSONObject facetJson = JSONObject.parseObject(su.getFacet());
            if (facetJson == null) {
                continue;
            }
            String size = facetJson.getString("size");
            if (StringUtils.isNotBlank(size) && personalSizeSet.contains(size)) {
                return true;
            }
        }
        return false;
    }

}
