package com.biyao.moses.match2.service.ruleImpl;

import com.alibaba.fastjson.JSON;
import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.common.constant.CommonConstants;
import com.biyao.moses.match2.constants.MatchRuleNameConst;
import com.biyao.moses.match2.service.RuleService;
import com.biyao.moses.model.match2.MatchItem2;
import com.biyao.moses.params.ProductInfo;
import com.biyao.moses.params.match2.MatchRequest2;
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
 * 过滤非一起拼商品规则
 */
@Slf4j
@Component(value = MatchRuleNameConst.YQP_SELECT)
public class YqpSelectRuleServiceImpl implements RuleService {

    @Autowired
    private ProductDetailCache productDetailCache;

    @Override
    public List<MatchItem2> execute(List<MatchItem2> list, MatchRequest2 request) {

        try {
            Set<Long> discountNPidSet = new HashSet<>(productDetailCache.getProductByScmTagId(CommonConstants.DISCOUNT_N_SCM_TAGID));
            Iterator<MatchItem2> iterator = list.iterator();
            while (iterator.hasNext()) {
                MatchItem2 matchItem = iterator.next();
                if(matchItem == null || matchItem.getProductId() == null){
                    continue;
                }
                ProductInfo productInfo = productDetailCache.getProductInfo(matchItem.getProductId());
                //过滤非一起拼（新手专享）商品 低模眼镜、眼镜、定制、定制咖啡
                //if (FilterUtil.isFilteredByAllXszxCond(productInfo) || !productDetailCache.isXszxPidSalesTop(productInfo.getProductId())) {
                if (FilterUtil.isFilteredByAllXszxCond(productInfo)) {
                    iterator.remove();
                    continue;
                }

                //过滤掉N折商品池中的商品
                if(CollectionUtils.isNotEmpty(discountNPidSet) && discountNPidSet.contains(matchItem.getProductId())){
                    iterator.remove();
                    continue;
                }
            }
        } catch (Exception e) {
            log.error("[严重异常]一起拼过滤异常,request {}, ", JSON.toJSONString(request), e);
        }

        return list;
    }
}
