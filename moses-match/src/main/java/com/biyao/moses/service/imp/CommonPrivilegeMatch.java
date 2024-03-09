package com.biyao.moses.service.imp;

import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.cache.ProductRetrievalCache;
import com.biyao.moses.common.constant.CommonConstants;
import com.biyao.moses.match2.param.MatchParam;
import com.biyao.moses.match2.service.impl.UcbMatchImpl;
import com.biyao.moses.model.exp.MatchDataSourceTypeConf;
import com.biyao.moses.model.filter.UserExtendInfo;
import com.biyao.moses.model.match2.MatchItem2;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.service.RecommendMatch;
import com.biyao.moses.util.MatchFilterUtil;
import com.biyao.moses.util.RedisUtil;
import com.biyao.moses.params.ProductInfo;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 通用特权金match
 */
@Slf4j
@Component("CommonTqjMatch")
public class CommonPrivilegeMatch implements RecommendMatch {

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private ProductDetailCache productDetailCache;

    @Autowired
    MatchFilterUtil matchFilterUtil;

    @Autowired
    UcbMatchImpl ucbMatch;
    
    @Autowired
    private ProductRetrievalCache productRetrievalCache;

    @BProfiler(key = "com.biyao.moses.service.imp.CommonPrivilegeMatch.executeRecommendMatch",
            monitorType = {MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR})
    @Override
    public Map<String, List<TotalTemplateInfo>> executeRecommendMatch(String dataKey, MatchDataSourceTypeConf mdst, String uuid) {

        Map<String, List<TotalTemplateInfo>> resultMap = new HashMap<>(); //match返回结果集
        List<TotalTemplateInfo> totalList = new ArrayList<>();  //match返回结果集模版数据
        List<Long> resultList = new ArrayList<Long>(); //返回结果
        Set<Long> existsProductSet = new HashSet<Long>(); //黑名单
        UserExtendInfo userExtendInfo = new UserExtendInfo(); // 用户信息对象
        Map<String, List<Long>> rcdDataMap = new HashMap<String, List<Long>>(); //召回商品集合
        Map<String, List<Long>> matchDetailMap = new HashMap<>(); //DCLOG日志信息集合 目前未打dclog
        List<String> view1ProductRecommendList = new ArrayList<String>();  //足迹1
        List<String> view3ProductRecommendList = new ArrayList<String>();  //足迹3
        String uid = mdst.getUid();
        String lat = mdst.getLat();
        String lng = mdst.getLng();
        // 类别推荐商品个数动态调整
        Map<Long, Integer> cateProductCountToday = new HashMap<Long, Integer>();
        Map<Long, Integer> cateProductCount7Day = new HashMap<Long, Integer>();
        try {
        // 如果个性化活动开关为关 则取ucb3的召回源
        if(!mdst.isPersonalizedRecommendActSwitch()){
            MatchParam matchParam = MatchParam.builder().device(mdst.getDevice())
                    .uid(Integer.valueOf(mdst.getUid())).uuid(uuid)
                    .upcUserType(mdst.getUpcUserType())
                    .userSex(Integer.valueOf(CommonConstants.UNKNOWN_SEX))
                    .ucbDataNum("3")
                    .build();
            List<MatchItem2> matchItem2s = ucbMatch.match(matchParam);
            if(CollectionUtils.isNotEmpty(matchItem2s)){
                resultList = matchItem2s.stream().map(MatchItem2::getProductId).collect(Collectors.toList());
            }
        }else {
            //类目设置
            if (StringUtils.isNotEmpty(uuid)) {
                String cateProductTodayStr = redisUtil.getString(CommonConstants.CATE_PRODUCT_TODAY_KEY_PREFIEX + uuid);
                String cateProduct7DayStr = redisUtil.getString(CommonConstants.CATE_PRODUCT_7DAY_KEY_PREFIEX + uuid);
                cateProductCountToday = matchFilterUtil.getCateProductCountMap(cateProductTodayStr);
                cateProductCount7Day = matchFilterUtil.getCateProductCountMap(cateProduct7DayStr);
            }
            //获取用户性别
            String sex = mdst.getSex();
            userExtendInfo.setSex(sex);
            //根据性别获取热销好评数据
            Map<String, List<Long>> feedRcdProductMap = matchFilterUtil.getFeedRcdProductMap(sex);
            //获取足迹1、足迹3商品共现集合
            matchFilterUtil.getView1and3ProductRecommendList(view1ProductRecommendList, view3ProductRecommendList, uuid, uid);
            //获取用户行为商品集合
            List<Long> userBehaviorRcdProductList = matchFilterUtil.getUserBehaviorRcdProductList(uid, existsProductSet);
            // 低决策商品推荐，数据由@{翟伟西}提供 redis的value格式：pid1:score1,pid2:score2...

            List<Long> lowDecisionRcdProductList = productRetrievalCache.getLowDecisionRdkLst();
            //获取兴趣偏好的新品
            Map<String, List<Long>> newShelfRcdProductMap = matchFilterUtil.getNewShelfRcdProductList(uuid, userExtendInfo, sex);
            //根据天气获取推荐商品
            List<Long> weatherRcdProductLst = matchFilterUtil.getWeatherRcdProductLst(uuid, lng, lat, userExtendInfo);
            //redis获取：新品曝光小于1000次商品，数据格式 pid:score,pid:score,...
            List<Long> lowExposureRcdProductList = productRetrievalCache.getHomeFeedExposureLessLst();
            //构建优先级地决策和低曝光map
            Map<String, List<Long>> lowDecisionRcdProductMap = matchFilterUtil.dealRcdDataMapBySex(sex,
                    lowDecisionRcdProductList, matchFilterUtil.MINOR_LOW_DECISION, matchFilterUtil.MAJOR_LOW_DECISION);
            Map<String, List<Long>> lowExposureRcdProductMap = matchFilterUtil.dealRcdDataMapBySex(sex,
                    lowExposureRcdProductList, matchFilterUtil.MINOR_LOW_EXPOSURE, matchFilterUtil.MAJOR_LOW_EXPOSURE);
            //组装rcd集合
            rcdDataMap.putAll(feedRcdProductMap);
            rcdDataMap.putAll(newShelfRcdProductMap);
            rcdDataMap.putAll(lowDecisionRcdProductMap);
            rcdDataMap.putAll(lowExposureRcdProductMap);
            rcdDataMap.put(matchFilterUtil.BEHAVIOR_RCD, userBehaviorRcdProductList);
            rcdDataMap.put(matchFilterUtil.WEATHER_RCD, weatherRcdProductLst);
            //过滤rcdMap 非特权金商品、过滤下载商品 抵扣金额为0商品
            dealProdctMap(rcdDataMap, mdst);
            //过滤足迹1、3集合非特权金商品、过滤下载商品 抵扣金额为0商品
            listFilter(mdst, view1ProductRecommendList);
            listFilter(mdst, view3ProductRecommendList);
            //推荐200商品处理逻辑
            for (int i = 0; i < 10; i++) {
                //热销商品集合：feedRcdProductList 修改为feedRcdProductMap.get(MAJOR_FEED_RCD)
                List<Long> tempPriorityProductList = matchFilterUtil.dealPriorityProductList(rcdDataMap, view1ProductRecommendList,
                        view3ProductRecommendList, feedRcdProductMap.get(matchFilterUtil.MAJOR_FEED_RCD),
                        uuid, uid, existsProductSet, matchDetailMap,cateProductCountToday, cateProductCount7Day,new ArrayList<>());
                //推荐结果
                resultList.addAll(tempPriorityProductList);
            }
        }
            //为最终集合设置模版，添加进返回map中
            dealResult(totalList, resultList, mdst);
            resultMap.put(dataKey, totalList);
        } catch (Exception e) {
            log.error("[严重异常]通用特权金match异常：uuid{} ", uuid, e);
            resultMap.put(dataKey, totalList);
            return resultMap;
        }
        return resultMap;
    }

    /**
     * 过滤
     *
     * @param mdst
     * @param view1ProductRecommendList
     */
    private void listFilter(MatchDataSourceTypeConf mdst, List<String> view1ProductRecommendList) {
        List<String> retList = new ArrayList<>();
        //用户类型 1 新客；2老客
        String userType = mdst.getUserType();
        Iterator<String> firstIterator = view1ProductRecommendList.iterator();
        //特权金优惠面额
        String priCouponAmountStr = mdst.getPriCouponAmountList();
        while (firstIterator.hasNext()) {
            String productIdStr = firstIterator.next();
            if (StringUtils.isBlank(productIdStr)) {
                firstIterator.remove();
                continue;
            }
            List<Long> rcdProductList = matchFilterUtil.getRcdProductList(productIdStr);
            Iterator<Long> iterator = rcdProductList.iterator();
            while (iterator.hasNext()) {
                Long pid = iterator.next();
                //过滤无特权金面额 无用户身份
                if (null == pid || StringUtils.isEmpty(priCouponAmountStr) || StringUtils.isEmpty(userType)) {
                    iterator.remove();
                    continue;
                }
                ProductInfo productInfo = null;
                productInfo = productDetailCache.getProductInfo(pid);
                if (null == productInfo) {
                    iterator.remove();
                    continue;
                }
                //过滤下架
                if (productInfo.getShelfStatus() == null || productInfo.getShelfStatus() == 0) {
                    iterator.remove();
                    continue;
                }
                //过滤老客限制金额为null 为负数
                if ("2".equals(userType) &&
                        ((productInfo.getNewPrivilateLimit() == null || productInfo.getNewPrivilateLimit().intValue() < 0)
                                || (productInfo.getOldUserPrivilege() == 0))
                ) {
                    iterator.remove();
                    continue;
                }
                //过滤新客客限制金额为null 为负数
                if ("1".equals(userType) &&
                        ((productInfo.getNewPrivilateLimit() == null || productInfo.getNewPrivilateLimit().intValue() < 0)
                                || (productInfo.getNewUserPrivilege() == 0))) {
                    iterator.remove();
                    continue;
                }
                //设置特权金抵扣金额
                int resultAmount = getResultAmount(userType, priCouponAmountStr, productInfo);
                //过滤特权金抵扣金额为0的
                if (resultAmount == 0) {
                    iterator.remove();
                    continue;
                }
            }
            //拼装回id字符串 加入新集合
            if (CollectionUtils.isNotEmpty(rcdProductList)) {
                String joinStr = StringUtils.join(rcdProductList, ",");
                retList.add(joinStr);
            }
        }
        view1ProductRecommendList.clear();
        view1ProductRecommendList.addAll(retList);
    }

    /**
     * 处理最终结果集
     *
     * @param totalList
     * @param resultList
     * @param mdst
     */
    private void dealResult(List<TotalTemplateInfo> totalList, List<Long> resultList, MatchDataSourceTypeConf mdst) {
        //用户类型 1 新客；2老客
        String userType = mdst.getUserType();
        //特权金优惠面额
        String priCouponAmountStr = mdst.getPriCouponAmountList();
        //为最终集合设置模版，添加进返回map中
        if (CollectionUtils.isNotEmpty(resultList)) {
            for (Long pid : resultList) {
                TotalTemplateInfo tti = new TotalTemplateInfo();
                ProductInfo productInfo = productDetailCache.getProductInfo(pid);

                //过滤抵扣面额为null 无用户身份
                if (null == productInfo || StringUtils.isEmpty(priCouponAmountStr) || StringUtils.isEmpty(userType)) {
                    continue;
                }
                //过滤下架
                if (productInfo.getShelfStatus() == null || "0".equals(productInfo.getShelfStatus().toString())) {
                    continue;
                }
                //过滤老客限制金额为null 为负数
                if ("2".equals(userType) &&
                        (productInfo.getOldPrivilateLimit() == null || productInfo.getOldPrivilateLimit().intValue() < 0)) {
                    continue;
                }
                //过滤新客客限制金额为null 为负数
                if ("1".equals(userType) &&
                        (productInfo.getNewPrivilateLimit() == null || productInfo.getNewPrivilateLimit().intValue() < 0)) {
                    continue;
                }
                //设置特权金抵扣金额
                int resultAmount = getResultAmount(userType, priCouponAmountStr, productInfo);
                //过滤特权金抵扣金额为0的
                if (resultAmount == 0) {
                    continue;
                }
                //设置特权金抵扣金额
                tti.setPriDeductAmount(String.valueOf(resultAmount));
                tti.setId(pid.toString());
                totalList.add(tti);
            }
        }
    }

    /**
     * 过滤非特权金商品、过滤下载商品
     *
     * @param rcdDataMap
     */
    private void dealProdctMap(Map<String, List<Long>> rcdDataMap, MatchDataSourceTypeConf mdst) {
        //用户类型 1 新客；2老客
        String userType = mdst.getUserType();
        //特权金优惠面额
        String priCouponAmountStr = mdst.getPriCouponAmountList();
        Iterator<Map.Entry<String, List<Long>>> iterator = rcdDataMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, List<Long>> next = iterator.next();
            List<Long> valueList = next.getValue();
            if (CollectionUtils.isNotEmpty(valueList)) {
                Iterator<Long> listIterator = valueList.iterator();
                while (listIterator.hasNext()) {
                    Long pid = listIterator.next();
                    //过滤pid为空 抵扣面额为空 用户身份为空
                    if (pid == null || StringUtils.isEmpty(priCouponAmountStr) || StringUtils.isEmpty(userType)) {
                        listIterator.remove();
                        continue;
                    }
                    ProductInfo productInfo = productDetailCache.getProductInfo(pid);
                    if (null == productInfo) {
                        listIterator.remove();
                        continue;
                    }
                    //过滤下架
                    if (productInfo.getShelfStatus() == null || productInfo.getShelfStatus() == 0) {
                        listIterator.remove();
                        continue;
                    }
                    //过滤老客限制金额为null 为负数
                    if ("2".equals(userType) &&
                            (productInfo.getOldPrivilateLimit() == null || productInfo.getOldPrivilateLimit().intValue() < 0)) {
                        listIterator.remove();
                        continue;
                    }
                    //过滤新客客限制金额为null 为负数
                    if ("1".equals(userType) &&
                            (productInfo.getNewPrivilateLimit() == null || productInfo.getNewPrivilateLimit().intValue() < 0)) {
                        listIterator.remove();
                        continue;
                    }
                    //设置特权金抵扣金额
                    int resultAmount = getResultAmount(userType, priCouponAmountStr, productInfo);
                    //过滤特权金抵扣金额为0的
                    if (resultAmount == 0) {
                        listIterator.remove();
                        continue;
                    }
                }
            }
        }
    }

    /**
     * 设置特权金抵扣金额
     *
     * @param userType
     * @param priCouponAmountStr
     * @param productInfo
     * @return
     */
    private int getResultAmount(String userType, String priCouponAmountStr, ProductInfo productInfo) {
        //设置特权金抵扣金额
        int prideductAmount = 0;
        int limitAmount = 0; //限额
        int resultAmount = 0;
        String[] split = priCouponAmountStr.split(":");
        prideductAmount = Integer.valueOf(split[1]);
        if ("1".equals(userType)) {
            //获取新客特权金限额
            limitAmount = productInfo.getNewPrivilateLimit().intValue();
        } else if ("2".equals(userType)) {
            //获取老客特权金限额
            limitAmount = productInfo.getOldPrivilateLimit().intValue();
        } else {
            limitAmount = productInfo.getOldPrivilateLimit().intValue();
        }
        //比较抵扣金额和 限制金额 取最小
        if (prideductAmount > limitAmount) {
            resultAmount = limitAmount;
        } else if (prideductAmount < limitAmount) {
            resultAmount = prideductAmount;
        } else {
            resultAmount = prideductAmount;
        }
        return resultAmount;
    }
}
