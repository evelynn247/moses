package com.biyao.moses.common.constant;

import java.util.HashSet;
import java.util.Set;

/**
 * @program: moses-parent
 * @description: 规则引擎相关常量
 * @author: changxiaowei
 * @create: 2021-03-26 17:41
 **/
public class RuleConstant {

    public static final Set<String> matchParamSet =new HashSet<String>(){
        {add("expect_num_max"); add("sex_filter");add("season_filter");add("repurchase_filter");}
    };

    public static final Set<String> rankParamSet =new HashSet<String>(){
        {add("recall_points"); add("punish_factor");add("price_factor");}
    };

    public static final Set<String> conditionParamSet =new HashSet<String>(){
        {add(""); add("");}
    };

    public static final Set<String> ruleParamSet =new HashSet<String>(){
        {add("category_partition");}
    };

}
