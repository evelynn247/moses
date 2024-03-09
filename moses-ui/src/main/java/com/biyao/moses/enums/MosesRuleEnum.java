package com.biyao.moses.enums;

import com.biyao.moses.rules.RuleConst;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 实验配置中的机制规则枚举
 */
public enum MosesRuleEnum {
    RULE_30DAYSBUY_FILTER(RuleConst.RULE_30DAYSBUY_FILTER, true, false, "过滤用户30天已购买商品"),
    REBUY_FILTER(RuleConst.Rebuy_FILTER, true, false, "通过复购周期，过滤用户已购买商品"),
    XSZX_FILTER(RuleConst.XSZX_FILTER, true, false, "过滤非新手专享商品"),
    RULE_CATEGORY_AND_SUPPLIER(RuleConst.RULE_CATEGORY_AND_SUPPLIER, false, false, "根据类目和商家双重规则重排序"),
    RULE_SIMILAR_CATEGORY(RuleConst.RULE_SIMILAR_CATEGORY, false, false, "根据相似三级类目进行隔断（10个都不相同）"),
    RULE_PERSONALIZATION(RuleConst.RULE_PERSONALIZATION, false, true, "个性化打散隔断规则")
    ;
    /**
     * 规则bean名称
     */
    private String ruleName;
    /**
     * 是否是过滤规则
     */
    private Boolean isFilterRule;
    /**
     * 是否支持翻页实时隔断
     */
    private Boolean isRealPartition;
    /**
     * 描述信息
     */
    private String desc;

    MosesRuleEnum(String ruleName, Boolean isFilterRule, Boolean isRealPartition, String desc){
        this.ruleName = ruleName;
        this.isFilterRule = isFilterRule;
        this.isRealPartition = isRealPartition;
        this.desc = desc;
    }

    /**
     * 根据ruleName获取枚举
     * @param ruleName
     * @return
     */
    public static MosesRuleEnum getByName(String ruleName){
        MosesRuleEnum result = null;
        if(StringUtils.isBlank(ruleName)){
            return null;
        }

        for(MosesRuleEnum ruleEnum : MosesRuleEnum.values()){
            if(ruleEnum.ruleName.equals(ruleName)){
                result = ruleEnum;
                break;
            }
        }
        return result;
    }
    /**
     * 对传入的规则名称进行排序，过滤规则在前，隔断规则在最后，如果隔断规则有多个，则只取第一个
     * @param ruleNameList
     * @return
     */
    public static List<String> sort(List<String> ruleNameList){
        if(CollectionUtils.isEmpty(ruleNameList) || ruleNameList.size() <= 1){
            return ruleNameList;
        }
        List<String> result = new ArrayList<>();
        try {
            List<String> filterRuleList = new ArrayList<>();
            List<String> partitionRuleList = new ArrayList<>();
            ruleNameList.forEach(ruleName -> {
                MosesRuleEnum mosesRuleEnum = getByName(ruleName);
                if (mosesRuleEnum != null) {
                    if (mosesRuleEnum.isFilterRule) {
                        filterRuleList.add(mosesRuleEnum.ruleName);
                    } else {
                        partitionRuleList.add(mosesRuleEnum.ruleName);
                    }
                }
            });
            result.addAll(filterRuleList);
            result.addAll(partitionRuleList);
        }catch (Exception e){
            result = ruleNameList;
        }
        return result;
    }

    /**
     * 查找第一个支持翻页实时隔断的规则
     * @param ruleNameList
     * @return
     */
    public static String findRealPartitionRule(List<String> ruleNameList){
        if(CollectionUtils.isEmpty(ruleNameList)){
            return null;
        }
        String result = null;
        for(String ruleName : ruleNameList){
            MosesRuleEnum ruleEnum = getByName(ruleName);
            if(ruleEnum != null && ruleEnum.isRealPartition){
                result = ruleName;
                break;
            }
        }
        return result;
    }
}
