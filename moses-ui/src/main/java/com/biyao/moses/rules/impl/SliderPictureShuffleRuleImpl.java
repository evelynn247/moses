package com.biyao.moses.rules.impl;

import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.common.constant.CommonConstants;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.params.ProductInfo;
import com.biyao.moses.rules.Rule;
import com.biyao.moses.rules.RuleConst;
import com.biyao.moses.rules.RuleContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 轮播图6个商品岔开商家、后台三级类目
 */
@Slf4j
@Component(value = RuleConst.RULE_SHUFFLE_LBT)
public class SliderPictureShuffleRuleImpl implements Rule {

    @Autowired
    private ProductDetailCache productDetailCache;

    @Override
    public List<TotalTemplateInfo> ruleRank(RuleContext ruleContext) {
        List<TotalTemplateInfo> result = new ArrayList<>();
        Set<Long> category3Set = new HashSet<>();//存放后台三级类目
        Set<Long> supplierIdSet = new HashSet<>();//存放商家id
        List<TotalTemplateInfo> allProductList = ruleContext.getAllProductList();//候选集合
        try {
            //进行岔开 6个商品 商家、类目均不相同
            shuffleSliderList(result, category3Set, supplierIdSet, allProductList);
        } catch (Exception e) {
            log.error("[严重异常][轮播图]轮播图岔开机制异常", e);
            Collections.shuffle(allProductList);
            result = allProductList.subList(0, 6);
        }

        return result;
    }

    /**
     * 进行岔开 6个商品 商家、类目均不相同
     * @param result
     * @param category3Set
     * @param supplierIdSet
     * @param allProductList
     */
    private void shuffleSliderList(List<TotalTemplateInfo> result, Set<Long> category3Set, Set<Long> supplierIdSet, List<TotalTemplateInfo> allProductList) {
        for (TotalTemplateInfo info : allProductList) {
            if(info == null || StringUtils.isBlank(info.getId())){
                continue;
            }
            ProductInfo productInfo = productDetailCache.getProductInfo(Long.valueOf(info.getId()));
            Long secondCategoryId = productInfo.getSecondCategoryId();
            Long thirdCategoryId = productInfo.getThirdCategoryId();
            Long supplierId = productInfo.getSupplierId();
            //存在相同商家、过滤
            if (supplierIdSet.contains(supplierId)) {
                continue;
            }
            if (CommonConstants.SPECIAL_CATEGORY2_IDS.contains(secondCategoryId)) { // 眼镜类目等特殊处理
                thirdCategoryId = secondCategoryId;
            }
            //存在相同后台三级类目、过滤
            if (category3Set.contains(thirdCategoryId)) {
                continue;
            }
            category3Set.add(thirdCategoryId);
            supplierIdSet.add(supplierId);
            result.add(info);
            if (result.size() >= 6) {
                break;
            }
        }
        //岔开后不足6个，则用无法岔开的集合补充
        if (result.size() < 6) {
            allProductList.removeAll(result);
            int otherNum = 6 - result.size();
            for (int i = 0; i < otherNum; i++) {
                result.add(allProductList.get(i));
            }
        }
    }
}
