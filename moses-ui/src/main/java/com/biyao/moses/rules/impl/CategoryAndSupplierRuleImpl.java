package com.biyao.moses.rules.impl;

import com.biyao.moses.cache.ProductDetailCache;
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
 * 双排feeds流上下左右对角商品岔开类目、商家
 *
 * @author zhaiweixi@idstaff.com
 * @date 2019/10/12
 **/
@Slf4j
@Component(value = RuleConst.RULE_CATEGORY_AND_SUPPLIER)
@Deprecated
public class CategoryAndSupplierRuleImpl implements Rule {

    @Autowired
    private ProductDetailCache productDetailCache;

    @Override
    public List<TotalTemplateInfo> ruleRank(RuleContext ruleContext) {

        List<TotalTemplateInfo> result = new ArrayList<>();//返回集合
        List<TotalTemplateInfo> list = new ArrayList<>(ruleContext.getAllProductList());//候选集

        dealCategoryAndSupplierProducts(list, result);

        return result;
    }

    /**
     * 岔开类目、商家，返回结果集
     *
     * @param allProductList
     * @param result
     */
    private void dealCategoryAndSupplierProducts(List<TotalTemplateInfo> allProductList, List<TotalTemplateInfo> result) {
        // 双排feeds流位置索引
        int index = 0;
        //商品的商家、类目集合 key:商品所在位置 value:商品对应的类目与商家
        Map<Integer, String> indexMap = new HashMap<Integer, String>();

        while (true) {
            int tempIndex = index;
            Iterator<TotalTemplateInfo> iterator = allProductList.iterator();
            while (iterator.hasNext()) {
                TotalTemplateInfo next = iterator.next();
                if(next == null || StringUtils.isBlank(next.getId())){
                    continue;
                }
                ProductInfo productInfo = productDetailCache.getProductInfo(Long.valueOf(next.getId()));
                if (productInfo == null || productInfo.getThirdCategoryId() == null
                        || productInfo.getSupplierId() == null)
                {
                    continue;
                }
                //获取后台三级类目
                String thirdCategoryId = productInfo.getThirdCategoryId().toString();
                //获取商家ID
                String supplierId = productInfo.getSupplierId().toString();
                //偶数
                if (index % 2 == 0) {
                    //第一个商品、直接插入无需比较
                    if (index == 0) {
                        result.add(next);
                        iterator.remove();
                        indexMap.put(index, thirdCategoryId + supplierId);
                        index++;
                    } else {
                        String upCategoryStr = indexMap.get(index - 2); //上方商品类目、商家
                        String obliqueCategoryStr = indexMap.get(index - 1);//右上方商品类目、商家
                        //左侧商品与上方、右上方商品是否存在相同类目、商家
                        if (upCategoryStr.contains(thirdCategoryId) ||
                                upCategoryStr.contains(supplierId) ||
                                obliqueCategoryStr.contains(thirdCategoryId) ||
                                obliqueCategoryStr.contains(supplierId)) {
                            continue;
                        } else {
                            result.add(next);
                            iterator.remove();
                            indexMap.put(index, thirdCategoryId + supplierId);
                            index++;
                        }
                    }
                }

                //奇数
                else {
                    //第二个商品 对比左侧商品类目、商家
                    if (index == 1) {
                        String leftCategoryStr = indexMap.get(index - 1);
                        if (leftCategoryStr.contains(thirdCategoryId) ||
                                leftCategoryStr.contains(supplierId)) {
                            continue;
                        } else {
                            result.add(next);
                            iterator.remove();
                            indexMap.put(index, thirdCategoryId + supplierId);
                            index++;
                        }
                    } else {
                        String upCategoryStr = indexMap.get(index - 2);
                        String leftCategoryStr = indexMap.get(index - 1);
                        String obliqueCategoryStr = indexMap.get(index - 3);
                        //右侧列商品与上方商品、左边商品是否存在相同类目、商家
                        if (upCategoryStr.contains(thirdCategoryId) ||
                                upCategoryStr.contains(supplierId) ||
                                leftCategoryStr.contains(thirdCategoryId) ||
                                leftCategoryStr.contains(supplierId) ||
                                obliqueCategoryStr.contains(thirdCategoryId) ||
                                obliqueCategoryStr.contains(supplierId)) {
                            continue;
                        } else {
                            result.add(next);
                            iterator.remove();
                            indexMap.put(index, thirdCategoryId + supplierId);
                            index++;
                        }
                    }
                }
                //重新遍历集合
                break;
            }
            //未岔开商品
            if (tempIndex == index) {
                break;
            }
        }
        result.addAll(allProductList);//加入未岔开商品
    }
}
