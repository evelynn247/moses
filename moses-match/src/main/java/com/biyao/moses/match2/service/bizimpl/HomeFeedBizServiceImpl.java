package com.biyao.moses.match2.service.bizimpl;

import com.biyao.moses.common.constant.CommonConstants;
import com.biyao.moses.common.constant.BizNameConst;
import com.biyao.moses.match2.constants.MatchStrategyConst;
import com.biyao.moses.match2.param.MatchParam;
import com.biyao.moses.match2.util.MatchUtil;
import com.biyao.moses.model.match2.MatchItem2;
import com.biyao.moses.params.match2.MatchRequest2;
import com.biyao.moses.match2.service.BizService;
import com.biyao.moses.service.imp.AsyncMatchService;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author zhaiweixi@idstaff.com
 * @date 2019/10/9
 * V2.0.0版本 使用ibcf*0.9 + hots*0.1召回
 **/
@Slf4j
@Component(value = BizNameConst.HOME_FEED)
public class HomeFeedBizServiceImpl implements BizService{
    //各召回源的权重
    private static final Double IBCF_WEIGHT = 0.9;
    private static final Double HOT_SALE_WEIGHT = 0.1;
    //返回的最大商品数量
    private static final int PRODUCT_NUM_MAX_LIMIT = 500;

    @Autowired
    AsyncMatchService asyncMatchService;
    @Autowired
    MatchUtil matchUtil;

    @BProfiler(key = "HomeFeedBizServiceImpl.match",
            monitorType = {MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR})
    @Override
    public List<MatchItem2> match(MatchRequest2 request) {

        MatchParam matchParam = MatchParam.builder().device(request.getDevice())
                .uid(request.getUid()).uuid(request.getUuid())
                .upcUserType(request.getUpcUserType())
                .userSex(request.getUserSex())
                .build();
        List<MatchItem2> ibcfMatchResult = null;
        Future<List<MatchItem2>> ibcfAsyncMatchResult = asyncMatchService.executeMatch2(matchParam, MatchStrategyConst.IBCF);
        try{
            ibcfMatchResult = ibcfAsyncMatchResult.get(CommonConstants.MATCH_MAX_WAIT_TIME, TimeUnit.MILLISECONDS);
        }catch (Exception e){
            log.error("获取召回源商品数据异常，召回源ibcf，超时时间{}ms", CommonConstants.MATCH_MAX_WAIT_TIME, e);
        }

        Map<String, MatchItem2> matchMap = new HashMap<>();
        matchUtil.aggrMatchItem(ibcfMatchResult, IBCF_WEIGHT, matchMap,request.getSiteId());
        List<MatchItem2> matchItem2List = MatchUtil.executeMatch2(matchParam, MatchStrategyConst.HOTS);
        List<MatchItem2> hotsFillProduct = matchUtil.getFillProduct(matchItem2List, PRODUCT_NUM_MAX_LIMIT, matchMap, true,request.getSiteId());
        matchUtil.aggrMatchItem(hotsFillProduct, HOT_SALE_WEIGHT, matchMap,request.getSiteId());

        Collection<MatchItem2> values = matchMap.values();
        List<MatchItem2> matchSorted = values.stream().sorted((m1, m2) -> -m1.getScore().compareTo(m2.getScore()))
                                        .collect(Collectors.toList());
        //保留小数点后4位，4位后的直接抛弃
        matchSorted.forEach(m -> m.setScore(Math.floor(m.getScore()*10000)/10000));
        List<MatchItem2> result = matchSorted.size()>PRODUCT_NUM_MAX_LIMIT ? matchSorted.subList(0,PRODUCT_NUM_MAX_LIMIT) : matchSorted;
        return result;
    }
}