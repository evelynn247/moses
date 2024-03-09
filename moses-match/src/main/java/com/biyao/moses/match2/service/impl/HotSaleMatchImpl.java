package com.biyao.moses.match2.service.impl;

import com.biyao.moses.cache.HotSaleProductCache;
import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.common.constant.CommonConstants;
import com.biyao.moses.match2.constants.MatchStrategyConst;
import com.biyao.moses.match2.param.MatchParam;
import com.biyao.moses.match2.service.Match2;
import com.biyao.moses.model.match2.MatchItem2;
import com.biyao.moses.params.ProductInfo;
import com.biyao.moses.params.ProductScoreInfo;
import com.biyao.moses.util.FilterUtil;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zhaiweixi@idstaff.com
 * @date 2019/10/8
 * 热销召回
 **/
@Slf4j
@Component(value = MatchStrategyConst.HOTS)
public class HotSaleMatchImpl implements Match2 {
    @Autowired
    private HotSaleProductCache hotSaleProductCache;

    @Autowired
    private ProductDetailCache productDetailCache;

    private static  final int PID_NUM_MAX_LIMIT = 500;

    @BProfiler(key = "HotSaleMatchImpl.match",
            monitorType = {MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR})
    @Override
    public List<MatchItem2> match(MatchParam matchParam) {
        List<MatchItem2> result = new ArrayList<>();

        try {
            String userSex;
            if(matchParam.getUserSex() == null){
                userSex = CommonConstants.UNKNOWN_SEX;
            }else{
                userSex = matchParam.getUserSex().toString();
            }
            List<ProductScoreInfo> productScoreInfo = hotSaleProductCache.getProductScoreInfoBySex(userSex);
            List<MatchItem2> matchItem2List = new ArrayList<>();

            for (ProductScoreInfo p : productScoreInfo) {

                ProductInfo productInfo = productDetailCache.getProductInfo(p.getProductId());
                if(FilterUtil.isCommonFilter(productInfo)){
                    continue;
                }
                MatchItem2 matchItem2 = new MatchItem2();
                matchItem2.setProductId(p.getProductId());
                matchItem2.setScore(p.getScore() * Math.random());
                matchItem2.setSource(MatchStrategyConst.HOTS);
                matchItem2List.add(matchItem2);
            }
            matchItem2List.sort((m1,m2) -> -m1.getScore().compareTo(m2.getScore()));
            //最多取500个商品，重新计算召回分（500-i）/500, i从0开始;
            int count = 0;
            for(MatchItem2 matchItem2 : matchItem2List){
                if(count >= PID_NUM_MAX_LIMIT){
                    break;
                }
                matchItem2.setScore(Math.floor(((double)(PID_NUM_MAX_LIMIT-count)*10000)/PID_NUM_MAX_LIMIT)/10000);
                result.add(matchItem2);
                count += 1;
            }
        } catch (Exception e) {
            log.error("[严重异常][召回源]获取热销召回源数据异常， uuid {}, uid {}, e ", matchParam.getUuid(), matchParam.getUid(), e);
        }
        return result;
    }
}
