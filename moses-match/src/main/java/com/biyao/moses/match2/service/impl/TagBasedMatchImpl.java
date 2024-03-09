package com.biyao.moses.match2.service.impl;

import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.cache.ProductSeasonCache;
import com.biyao.moses.common.constant.MatchRedisKeyConstant;
import com.biyao.moses.match2.constants.MatchStrategyConst;
import com.biyao.moses.match2.param.MatchParam;
import com.biyao.moses.match2.service.Match2;
import com.biyao.moses.match2.util.MatchUtil;
import com.biyao.moses.model.match2.MatchItem2;
import com.biyao.moses.params.ProductInfo;
import com.biyao.moses.params.ProductScoreInfo;
import com.biyao.moses.util.FilterUtil;
import com.biyao.moses.util.MatchRedisUtil;
import com.biyao.moses.util.StringUtil;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import com.uc.domain.bean.User;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName TagBasedMatchImpl
 * @Description 基于标签的召回源
 * @Author xiaojiankai
 * @Date 2019/10/23 14:43
 * @Version 1.0
 **/
@Slf4j
@Component(value = MatchStrategyConst.TAG)
public class TagBasedMatchImpl implements Match2 {

    @Autowired
    private MatchRedisUtil redisUtil;

    private static  final int PID_NUM_MAX_LIMIT = 300;

    @Autowired
    private ProductDetailCache productDetailCache;

    @Autowired
    private ProductSeasonCache productSeasonCache;

    @BProfiler(key = "TagBasedMatchImpl.match",
            monitorType = {MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR})
    @Override
    public List<MatchItem2> match(MatchParam matchParam) {
        List<MatchItem2> result = new ArrayList<>();
        Integer uid = matchParam.getUid();
        String uuid = matchParam.getUuid();

        //通过uid从redis中获取召回的商品信息
        String redisKey = null;
        String productIds = null;
        if(uid != null && uid != 0){
            redisKey = MatchRedisKeyConstant.MOSES_TAG_PREFIX + uid;
            productIds = redisUtil.getString(redisKey);
        }
        //如果uid查不到召回源数据，则根据uuid查询
        if(StringUtils.isBlank(productIds)){
            redisKey = MatchRedisKeyConstant.MOSES_TAG_PREFIX + uuid;
            productIds = redisUtil.getString(redisKey);
        }
        //如果uid和uuid都查不到召回源数据，则直接返回空集合
        if(StringUtils.isBlank(productIds)){
            log.error("[一般异常][召回源]tag召回源商品数据为空, redisKey {}", redisKey);
            return result;
        }
        List<ProductScoreInfo> productScoreInfos = StringUtil.parseProductStr(productIds, PID_NUM_MAX_LIMIT, MatchStrategyConst.TAG);
        if(CollectionUtils.isEmpty(productScoreInfos)){
            log.error("[严重异常][召回源]解析召回源{}商品数据为空，uid {}, uuid {}", MatchStrategyConst.TAG, uid, uuid);
            return result;
        }
        //过滤掉非用户季节商品
        productScoreInfos = filterByUserSeason(productScoreInfos, matchParam.getUcUser());
        if(CollectionUtils.isEmpty(productScoreInfos)){
            log.error("[一般异常][召回源]过滤季节后，召回源{}商品数据为空，uid {}, uuid {}", MatchStrategyConst.TAG, uid, uuid);
            return result;
        }

        for (ProductScoreInfo p : productScoreInfos) {

            ProductInfo productInfo = productDetailCache.getProductInfo(p.getProductId());
            if(FilterUtil.isCommonFilter(productInfo)){
                continue;
            }

            MatchItem2 matchItem2 = new MatchItem2();
            matchItem2.setProductId(p.getProductId());
            matchItem2.setScore(p.getScore());
            matchItem2.setSource(MatchStrategyConst.TAG);
            result.add(matchItem2);
        }

        return result;
    }

    /**
     * 过滤掉非用户季节商品，过滤规则如下：
     * 如果用户季节春，则选择适用季节为春或秋或四季的商品
     * 如果用户季节秋，则选择适用季节为春或秋或四季的商品
     * 如果用户季节夏，则选择适用季节为夏或四季的商品
     * 如果用户季节冬，则选择适用季节为冬或四季的商品
     * @param productScoreInfos
     * @param ucUser
     * @return
     */
    private List<ProductScoreInfo> filterByUserSeason(List<ProductScoreInfo> productScoreInfos, User ucUser){
        List<ProductScoreInfo> result = new ArrayList<>();
        if(ucUser == null){
            return productScoreInfos;
        }

        int userSeasonValue = MatchUtil.convertSeason2int(ucUser.getSeason());
        if(userSeasonValue == 0){
            return productScoreInfos;
        }

        for(ProductScoreInfo productScoreInfo : productScoreInfos){
            String productId = String.valueOf(productScoreInfo.getProductId());
            int productSeasonValue = productSeasonCache.getProductSeasonValue(productId);
            if(MatchUtil.isFilterByUserSeason(productSeasonValue, userSeasonValue)){
                continue;
            }
            result.add(productScoreInfo);
        }

        return result;
    }
}
