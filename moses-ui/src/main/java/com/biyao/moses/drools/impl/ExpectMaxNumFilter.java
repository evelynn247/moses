package com.biyao.moses.drools.impl;

import com.alibaba.fastjson.JSONObject;
import com.biyao.moses.drools.Constant;
import com.biyao.moses.drools.Filter;
import com.biyao.moses.drools.FilterContext;
import com.biyao.moses.model.match2.MatchItem2;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @program: moses-parent
 * @description: 最大期望数量过滤
 * @author: changxiaowei
 * @create: 2021-03-25 11:53
 **/
@Slf4j
@Component(value = Constant.EXPECT_MAX_NUM_FILTER)
public class ExpectMaxNumFilter implements Filter {
    @Override
    public List<MatchItem2> filter(FilterContext filterContext) {
        log.info("[检查日志][规则引擎]最大期望数量过滤服务，uuid:{}",filterContext.getUuid());
        Integer expectMaxNum = filterContext.getExpectMaxNum();
        List<MatchItem2> matchItem2List = filterContext.getMatchItem2List();
        try {
            // 如果待过滤商品数量小于 期望数值 则全数返回  否则截取
            if(expectMaxNum == null || CollectionUtils.isEmpty(matchItem2List)){
                log.error("[严重异常][规则引擎]最大期望数量过滤入参异常:{},", JSONObject.toJSONString(filterContext));
                return matchItem2List;
            }
            if(matchItem2List.size()<= expectMaxNum){
                return matchItem2List;
            }else {
                return matchItem2List.subList(0,expectMaxNum);
            }
        }catch (Exception e){
            log.error("[严重异常][规则引擎]最大期望数量过滤时发生异常,参数{}", JSONObject.toJSONString(filterContext),e);
        }
       return matchItem2List;
    }
}
