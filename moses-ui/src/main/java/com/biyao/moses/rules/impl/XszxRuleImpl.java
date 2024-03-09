package com.biyao.moses.rules.impl;

import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.common.constant.CommonConstants;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.params.ProductInfo;
import com.biyao.moses.rules.Rule;
import com.biyao.moses.rules.RuleConst;
import com.biyao.moses.rules.RuleContext;
import com.biyao.moses.util.FilterUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @ClassName XszxRuleImpl
 * @Description 新手专享过滤规则
 * @Author xiaojiankai
 * @Date 2019/12/19 12:56
 * @Version 1.0
 **/

@Slf4j
@Component(RuleConst.XSZX_FILTER)
public class XszxRuleImpl implements Rule {

    @Autowired
    ProductDetailCache productDetailCache;

    @Override
    public List<TotalTemplateInfo> ruleRank(RuleContext ruleParam) {
        List<TotalTemplateInfo> allProductList = ruleParam.getAllProductList();
        if(CollectionUtils.isEmpty(allProductList)){
            return allProductList;
        }
        try {
            Set<Long> discountNPidSet = new HashSet<>(productDetailCache.getProductByScmTagId(CommonConstants.DISCOUNT_N_SCM_TAGID));
            Iterator<TotalTemplateInfo> iterator = allProductList.iterator();
            while (iterator.hasNext()) {
                TotalTemplateInfo info = iterator.next();
                Long pid = Long.valueOf(info.getId());
                ProductInfo productInfo = productDetailCache.getProductInfo(pid);

                //if (FilterUtil.isFilteredByAllXszxCond(productInfo) || !productDetailCache.isXszxPidSalesTop(productInfo.getProductId())) {
                if (FilterUtil.isFilteredByAllXszxCond(productInfo)) {
                    iterator.remove();
                    continue;
                }

                //过滤掉N折商品池中的商品
                if(CollectionUtils.isNotEmpty(discountNPidSet) && discountNPidSet.contains(pid)){
                    iterator.remove();
                    continue;
                }
            }
        } catch (Exception e) {
            log.error("[严重异常]新手专享过滤异常 uuid {} ", ruleParam.getUuid(), e);
        }
        return allProductList;
    }
}
