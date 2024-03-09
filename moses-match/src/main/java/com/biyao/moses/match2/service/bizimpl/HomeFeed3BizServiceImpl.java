package com.biyao.moses.match2.service.bizimpl;

import com.biyao.moses.common.constant.CommonConstants;
import com.biyao.moses.common.constant.BizNameConst;
import com.biyao.moses.match2.constants.MatchStrategyConst;
import com.biyao.moses.match2.param.MatchParam;
import com.biyao.moses.match2.service.BizService;
import com.biyao.moses.match2.util.MatchUtil;
import com.biyao.moses.model.match2.MatchItem2;
import com.biyao.moses.params.match2.MatchRequest2;
import com.biyao.moses.service.imp.AsyncMatchService;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author xiaojiankai@idstaff.com
 * @date 2019/11/6
 * V2.1.0版本 使用rtcbr*0.35 + ibcf*0.3 + tag*0.3 + hots*0.05召回
 **/
@Slf4j
@Component(value = BizNameConst.HOME_FEED3)
public class HomeFeed3BizServiceImpl implements BizService{
    //各召回源的权重
    private static final Double RTCBR_WEIGHT = 0.4;
    private static final Double IBCF_WEIGHT = 0.35;
    private static final Double TAG_WEIGHT = 0.2;
    private static final Double HOT_SALE_WEIGHT = 0.05;
    //返回的最大商品数量
    private static final int PRODUCT_NUM_MAX_LIMIT = 500;

    @Autowired
    AsyncMatchService asyncMatchService;
    @Autowired
    MatchUtil matchUtil;
    @BProfiler(key = "HomeFeed3BizServiceImpl.match",
            monitorType = {MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR})
    @Override
    public List<MatchItem2> match(MatchRequest2 request) {

        MatchParam matchParam = MatchParam.builder().device(request.getDevice())
                .uid(request.getUid()).uuid(request.getUuid())
                .upcUserType(request.getUpcUserType())
                .userSex(request.getUserSex())
                .build();

        Map<String, Future<List<MatchItem2>>> resultMap = new HashMap<>();
        Future<List<MatchItem2>> ibcfAsyncMatchResult = asyncMatchService.executeMatch2(matchParam, MatchStrategyConst.IBCF);
        resultMap.put(MatchStrategyConst.IBCF, ibcfAsyncMatchResult);
        Future<List<MatchItem2>> tagAsyncMatchResult = asyncMatchService.executeMatch2(matchParam, MatchStrategyConst.TAG);
        resultMap.put(MatchStrategyConst.TAG, tagAsyncMatchResult);
        Future<List<MatchItem2>> rtcbrAsyncMatchResult = asyncMatchService.executeMatch2(matchParam, MatchStrategyConst.RTCBR);
        resultMap.put(MatchStrategyConst.RTCBR, rtcbrAsyncMatchResult);

        Map<String, MatchItem2> matchMap = new HashMap<>();
        for(Map.Entry<String, Future<List<MatchItem2>>> entry : resultMap.entrySet()) {
            Future<List<MatchItem2>> asynResult = entry.getValue();
            String matchStrategy = entry.getKey();
            try {
                List<MatchItem2> matchResult = asynResult.get(CommonConstants.MATCH_MAX_WAIT_TIME, TimeUnit.MILLISECONDS);
                Double weight = 0d;
                if(MatchStrategyConst.IBCF.equals(matchStrategy)){
                    weight = IBCF_WEIGHT;
                }else if(MatchStrategyConst.TAG.equals(matchStrategy)){
                    weight = TAG_WEIGHT;
                }else if(MatchStrategyConst.RTCBR.equals(matchStrategy)){
                    weight = RTCBR_WEIGHT;
                }
                matchUtil.aggrMatchItem(matchResult, weight, matchMap,request.getSiteId());
            } catch (Exception e) {
                log.error("获取召回源商品数据异常，召回源{}，超时时间{}ms",matchStrategy, CommonConstants.MATCH_MAX_WAIT_TIME, e);
            }
        }
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