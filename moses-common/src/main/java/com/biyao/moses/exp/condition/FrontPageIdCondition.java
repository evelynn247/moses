package com.biyao.moses.exp.condition;

import com.biyao.experiment.ExperimentCondition;
import com.biyao.moses.common.constant.ExpConditionConstants;
import com.biyao.moses.common.constant.ExpFlagsConstants;
import com.biyao.moses.params.BaseRequest2;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * @ClassName FrontPageIdCondition
 * @Description 前端页面ID条件，用于判断该页面是否能进入此实验
 * @Author xiaojiankai
 * @Date 2020/4/20 10:07
 * @Version 1.0
 **/
@Component(value = ExpConditionConstants.COND_FRONT_PAGEID)
public class FrontPageIdCondition implements ExperimentCondition {
    @Override
    public Boolean satisfied(Object o) {
        BaseRequest2 request = (BaseRequest2) o;

        String frontPageId = request.getFrontPageId();

        HashMap<String, String> flags = request.getCurrentExpFlags();
        String applyFrontPageId = ExpFlagsConstants.VALUE_DEFAULT;
        if(flags != null){
            applyFrontPageId = flags.getOrDefault(ExpFlagsConstants.SFLAG_APPLY_FRONT_PAGEID, ExpFlagsConstants.VALUE_DEFAULT);
        }
        //如果没有配置sflag_apply_front_pageid参数或为默认值DEFAULT，则认为所有页面都适用，此时返回true
        if(applyFrontPageId.equals(ExpFlagsConstants.VALUE_DEFAULT)){
            return true;
        }

        boolean result = false;
        boolean reverse = applyFrontPageId.startsWith("!");
        if(reverse){
            applyFrontPageId = applyFrontPageId.substring(1);
        }
        Set<String> fpageIdSet = new HashSet<>(Arrays.asList(applyFrontPageId.split(",")));
        if(fpageIdSet.contains(frontPageId)){
            result = true;
        }

        if(reverse){
            result = !result;
        }

        return result;
    }
}
