package com.biyao.moses.drools.impl;

import com.alibaba.fastjson.JSONObject;
import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.common.constant.CommonConstants;
import com.biyao.moses.drools.Constant;
import com.biyao.moses.drools.Filter;
import com.biyao.moses.drools.FilterContext;
import com.biyao.moses.model.match2.MatchItem2;
import com.biyao.moses.params.ProductInfo;
import com.biyao.moses.rpc.UcRpcService;
import com.biyao.moses.util.PartitionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @program: moses-parent
 * @description: 性别过滤
 * @author: changxiaowei
 * @create: 2021-03-25 11:33
 **/
@Slf4j
@Component(value = Constant.SEX_FILTER)
public class SexFilter implements Filter {
    @Autowired
    ProductDetailCache productDetailCache;
    @Autowired
    UcRpcService ucRpcService;
    @Override
    public List<MatchItem2> filter(FilterContext filterContext) {
        log.info("[检查日志][规则引擎]性别过滤服务，uuid:{}",filterContext.getUuid());
        List<MatchItem2> matchItem2List = filterContext.getMatchItem2List();
        if(CollectionUtils.isEmpty(matchItem2List)){
            return matchItem2List;
        }
        String userSex = ucRpcService.getUserSexFromUc(filterContext.getUid().toString(), filterContext.getUuid());
        if(CommonConstants.UNKNOWN_SEX.equals(userSex)){
            return matchItem2List;
        }
        try {
            return matchItem2List.stream().filter(matchItem2 -> {
                ProductInfo productInfo = productDetailCache.getProductInfo(matchItem2.getProductId());
                if(productInfo == null || productInfo.getProductGender() == null){
                    return false;
                }
                if(CommonConstants.FEMALE_SEX.equals(userSex) && CommonConstants.MALE_SEX.equals(productInfo.getProductGender().toString())){
                    return false;
                }else if(CommonConstants.MALE_SEX.equals(userSex) && CommonConstants.FEMALE_SEX.equals(productInfo.getProductGender().toString())){
                    return false;
                }
                return true;
            }).collect(Collectors.toList());
        }catch (Exception e){
            log.error("[严重异常][规则引擎]性别过滤时发生异常,参数{}", JSONObject.toJSONString(filterContext),e);
        }
        return matchItem2List;
    }
}
