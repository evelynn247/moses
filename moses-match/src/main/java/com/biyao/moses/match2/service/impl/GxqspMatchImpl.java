package com.biyao.moses.match2.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.cache.ProductSeasonCache;
import com.biyao.moses.common.constant.AlgorithmRedisKeyConstants;
import com.biyao.moses.match2.constants.MatchStrategyConst;
import com.biyao.moses.match2.param.MatchParam;
import com.biyao.moses.match2.service.Match2;
import com.biyao.moses.match2.util.MatchUtil;
import com.biyao.moses.model.match.DeepViewProductInfo;
import com.biyao.moses.model.match2.MatchItem2;
import com.biyao.moses.params.ProductInfo;
import com.biyao.moses.params.ProductScoreInfo;
import com.biyao.moses.rpc.UcRpcService;
import com.biyao.moses.util.AlgorithmRedisUtil;
import com.biyao.moses.util.FilterUtil;
import com.biyao.moses.util.StringUtil;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @program: moses-parent
 * @description: 感兴趣商品集工程选取规则召回源
 * @author: changxiaowei
 * @create: 2021-02-22 18:51
 **/
@Slf4j
@Component(value = MatchStrategyConst.GXQSP_MATCH)
public class GxqspMatchImpl implements Match2 {

    @Autowired
    UcRpcService ucRpcService;
    @Autowired
    private AlgorithmRedisUtil algorithmRedisUtil;
    @Autowired
    ProductDetailCache productDetailCache;
    @Autowired
    ProductSeasonCache productSeasonCache;
    // 最大召回数量
    private static final int PID_NUM_MAX_LIMIT = 100;
    // 一天的毫秒数
    private static final long dayMilliSecond = 60 * 60 * 24*1000;
    //当天凌晨的时间戳
    private static final long day0Time = System.currentTimeMillis() - (System.currentTimeMillis() + 8 * 3600000) % dayMilliSecond;
    @BProfiler(key = "GxqspMatchImpl.match",
            monitorType = {MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR})
    @Override
    public List<MatchItem2> match(MatchParam matchParam) {
        // 三天前的时间戳
        long day3Time = day0Time-3*dayMilliSecond;
        //七天前的时间戳
        long day7Time = day0Time-7*dayMilliSecond;

        // 记录召回商品的顺序
        int temp = 0;
        // 初始化结果集
        List<MatchItem2> resultList = new ArrayList<>();
        // 如果召回源id为空
        if(StringUtils.isEmpty(matchParam.getSource())){
            log.info("[一般异常]获取感兴趣商品失败，无召回源id，请求参数：request:{}", JSONObject.toJSONString(matchParam));
            return resultList;
        }
        //记录三级类目对应的分值
        Map<Long,Integer> categoryScoreMap =new HashMap<>();
        //召回商品按照类目分组
        Map<Long,List<ProductScoreInfo>> productScoreInfoListByCategoryMap =new HashMap<>();
        // 结果集 用于去重
        List<Long> resultPidList = new ArrayList<>();
        // 获取用户指定时间内深度浏览的商品
        String uid= null == matchParam.getUid() ? null :String.valueOf(matchParam.getUid());
        // 记录哪些深度浏览的商品 已被召回
        List<Long> matchedDeepPidList=new ArrayList<>();
        try {
            Map<String,long[]> timeMaps=new HashMap();
            timeMaps.put("0",new long[]{day0Time,Long.MAX_VALUE});
            timeMaps.put("3",new long[]{day3Time,day0Time});
            timeMaps.put("7",new long[]{day7Time,day3Time});
            Map<String, List<DeepViewProductInfo>> deepViewProductTimesMap= ucRpcService.getDeepViewProductMap(matchParam.getUuid(), uid, "mosesmatch", timeMaps);

            // 当天深度浏览商品
            List<DeepViewProductInfo> deep0ViewPidList = deepViewProductTimesMap.get("0")==null ? new ArrayList<>():deepViewProductTimesMap.get("0");
            // 0-3深度浏览商品
            List<DeepViewProductInfo> deep3ViewPidList =  deepViewProductTimesMap.get("3")==null ? new ArrayList<>():deepViewProductTimesMap.get("3");
            // 3-7天深度浏览商品
            List<DeepViewProductInfo> deep7ViewPidList =  deepViewProductTimesMap.get("7")==null ? new ArrayList<>():deepViewProductTimesMap.get("7");
            // 计算三级类目得分
            countAllCategoryScore(deep0ViewPidList,deep3ViewPidList,deep7ViewPidList,categoryScoreMap);

            log.info("[感兴趣商品集记录类目得分]：结果：{}",JSONObject.toJSONString(categoryScoreMap));

            //获取用户当天深度浏览商品相似商品 遍历用户当天深度浏览商品
            getSimilarProductByDeepViews(matchedDeepPidList,deep0ViewPidList,resultPidList,productScoreInfoListByCategoryMap);
            // 根据用户3天内深度浏览商品获取相似商品
            if(resultPidList.size()< PID_NUM_MAX_LIMIT){
                getSimilarProductByDeepViews(matchedDeepPidList,deep3ViewPidList,resultPidList,productScoreInfoListByCategoryMap);
            }
            //根据用户7天内深度浏览商品获取相似商品
            if(resultPidList.size()< PID_NUM_MAX_LIMIT){
                getSimilarProductByDeepViews(matchedDeepPidList,deep7ViewPidList,resultPidList,productScoreInfoListByCategoryMap);
            }
            ArrayList<Long> categoryList = new ArrayList<>(productScoreInfoListByCategoryMap.keySet());


            //类目排序
            categoryList.sort((o1, o2) -> {
                Integer o1Score = categoryScoreMap.getOrDefault(o1, 0);
                Integer o2Score = categoryScoreMap.getOrDefault(o2, 0);
                return o2Score.compareTo(o1Score);
            });

            for (Long categoryId:productScoreInfoListByCategoryMap.keySet()){
                List<ProductScoreInfo> productScoreInfoList = productScoreInfoListByCategoryMap.get(categoryId);
                productScoreInfoList.sort((o1, o2) ->{
                    Long o1ViewTime = getViewTime(deep0ViewPidList, deep3ViewPidList, deep7ViewPidList,o1.getParentPid());
                    Long o2ViewTime = getViewTime(deep0ViewPidList, deep3ViewPidList, deep7ViewPidList,o2.getParentPid());
                    if(!o1ViewTime.equals(o2ViewTime)){
                        return o2ViewTime.compareTo(o1ViewTime);
                    }else {
                        return o2.getScore().compareTo(o1.getScore());
                    }
                });
            }
            resultPidList.clear();
            // 按照类目排序将商品放入结果集中
            categoryList.forEach(id ->{
                List<ProductScoreInfo> productScoreInfoList = productScoreInfoListByCategoryMap.get(id);
                List<Long> pidList = productScoreInfoList.stream().map(ProductScoreInfo::getProductId).collect(Collectors.toList());
                resultPidList.addAll(pidList);
            });

            for (Long pid:resultPidList){
                MatchItem2 matchItem2=new MatchItem2();
                matchItem2.setProductId(pid);
                matchItem2.setSource(matchParam.getSource());
                Double score = (double) (PID_NUM_MAX_LIMIT - temp) / PID_NUM_MAX_LIMIT;
                matchItem2.setScore(score);
                resultList.add(matchItem2);
                temp++;
                if(temp >= PID_NUM_MAX_LIMIT){
                    break;
                }
            }

        }catch (Exception e){
            log.error("[严重异常][召回源]获取感兴趣商品集数据异常， uuid {}, uid {}, e ", matchParam.getUuid(), matchParam.getUid(), e);
        }
        return resultList;
    }



    /**
     * 根据深度浏览商品 获取相似商品
     * @return
     */
    private void dealSimilarPids(Long pid,List<Long> resultPidList,Map<Long,List<ProductScoreInfo>> productScoreInfoListByCategoryMap){
        //每个深度浏览商品召回上线
        int maxLimit=10;
        // 获取相似商品
        String likeProductsStr = algorithmRedisUtil.getString(AlgorithmRedisKeyConstants.MOSES_LBTLDY_SIMILAR_PREFIX + pid);
        // 解析数据
        List<ProductScoreInfo> productScoreInfoList = StringUtil.parseProductStr(likeProductsStr, -1,"gxqsp");
        //  过滤 去重
        for (ProductScoreInfo productScoreInfo:productScoreInfoList){
            // 上限为 10
            if(maxLimit <= 0) {
                break;
            }
            // 去重
            if(resultPidList.contains(productScoreInfo.getProductId())){
                continue;
            }
            ProductInfo deepViewProductInfo = productDetailCache.getProductInfo(pid);
            ProductInfo similarProductInfo = productDetailCache.getProductInfo(productScoreInfo.getProductId());
            // 过滤无效商品
            if(deepViewProductInfo == null || similarProductInfo==null ){
                continue;
            }
            // 下架 定制过滤
            if(FilterUtil.isCommonFilter(similarProductInfo)){
                continue;
            }
            // 性别过滤  与深度浏览商品比较
            Integer productGender = null;
            if(deepViewProductInfo.getProductGender() != null){
                try {
                    productGender = Integer.valueOf(deepViewProductInfo.getProductGender().toString());
                }catch (Exception e){
                    log.error("[一般异常]商品性别解析异常,商品id{}，商品性别{}，异常信息{}",deepViewProductInfo.getProductId(),deepViewProductInfo.getProductGender(),e);
                }
            }
            //
            if(MatchUtil.isFilterBySex(similarProductInfo,productGender)){
                continue;
            }
            // 季节过滤与深度浏览商品比较
            if(MatchUtil.isFilterBySeason(productSeasonCache.getProductSeasonValue(pid.toString()),
                    productSeasonCache.getProductSeasonValue(similarProductInfo.getProductId().toString()))){
                continue;
            }

            resultPidList.add(similarProductInfo.getProductId());
            //记录由哪个深度浏览商品召回的
            productScoreInfo.setParentPid(pid);

            //按照三级类目分组
            Long thirdCategoryId = similarProductInfo.getThirdCategoryId();
            if(productScoreInfoListByCategoryMap.keySet().contains(thirdCategoryId)){
                List<ProductScoreInfo> productScoreInfoList1 = productScoreInfoListByCategoryMap.get(thirdCategoryId);
                productScoreInfoList1.add(productScoreInfo);
            }else {
                List<ProductScoreInfo> productScoreInfoList1=new ArrayList<>();
                productScoreInfoList1.add(productScoreInfo);
                productScoreInfoListByCategoryMap.put(thirdCategoryId,productScoreInfoList1);
            }
            maxLimit--;
        }
    }


    private void getSimilarProductByDeepViews(List<Long> matchedDeepPidList,List<DeepViewProductInfo> deepViewPidList,List<Long> resultPidList,Map<Long,List<ProductScoreInfo>> productScoreInfoListByCategory){

        deepViewPidList.sort(((o1, o2) -> {
            Long o1Time= o1.getViewTime() == null ? 0:o1.getViewTime();
            Long o2Time= o2.getViewTime() == null ? 0:o2.getViewTime();
            return o2Time.compareTo(o1Time);
        }));

        for (DeepViewProductInfo deepViewProductInfo:deepViewPidList){
            // 已被召回的深度浏览商品 不会被再次召回
            if(matchedDeepPidList.contains(deepViewProductInfo.getPid())) {
                continue;
            }
            matchedDeepPidList.add(deepViewProductInfo.getPid());
            dealSimilarPids(deepViewProductInfo.getPid(),resultPidList,productScoreInfoListByCategory);
        }
    }
    /**
     * 计算三级类目得分
     * 规则：当天深度浏览  三级类目分+3 ， T0-T3 深度浏览 三级类目分 +2 ， T3-T7 当天深度浏览 三级类目分+1
     * @return
     */
    private void countAllCategoryScore(List<DeepViewProductInfo> deep0ViewPidList, List<DeepViewProductInfo> deep3ViewPidList, List<DeepViewProductInfo> deep7ViewPidList,
                                       Map<Long,Integer> categoryScoreMap) {

        // 统计当天深度浏览三级类目得分
        if (!CollectionUtils.isEmpty(deep0ViewPidList)) {
            countCategoryScore(categoryScoreMap,deep0ViewPidList,3);
        }
        // 统计T0-T3深度浏览三级类目得分
        if (!CollectionUtils.isEmpty(deep3ViewPidList)) {
            countCategoryScore(categoryScoreMap,deep3ViewPidList,2);
        }
        // 统计T3-T7深度浏览三级类目得分
        if (!CollectionUtils.isEmpty(deep7ViewPidList)) {
            countCategoryScore(categoryScoreMap,deep7ViewPidList,1);
        }
    }

    /**
     * 计算类目得分
     * @param categoryScoreMap  结果集
     * @param
     * @param scoreWeight  权重分
     */
    private void countCategoryScore(Map<Long,Integer> categoryScoreMap, List<DeepViewProductInfo> deepViewPidList,int scoreWeight){
        for (DeepViewProductInfo deepViewProductInfo : deepViewPidList) {
            ProductInfo productInfo = productDetailCache.getProductInfo(deepViewProductInfo.getPid());
            if (productInfo == null || productInfo.getThirdCategoryId() == null) {
                continue;
            }
            Long thirdCategoryId = productInfo.getThirdCategoryId();
            Set<Long> pidSet = categoryScoreMap.keySet();
            int thirdCategoryScore = scoreWeight;
            if (pidSet.contains(thirdCategoryId)) {
                thirdCategoryScore = categoryScoreMap.get(thirdCategoryId) + scoreWeight;
            }
            categoryScoreMap.put(thirdCategoryId, thirdCategoryScore);
        }
    }

    /**
     * 获取深度浏览商品对应浏览时间  取最近的浏览时间
     * @param deep0ViewPidList
     * @param deep3ViewPidList
     * @param deep7ViewPidList
     * @param pid
     * @return
     */
        private  Long getViewTime(List<DeepViewProductInfo> deep0ViewPidList,List<DeepViewProductInfo> deep3ViewPidList,List<DeepViewProductInfo> deep7ViewPidList,Long pid){

          List<DeepViewProductInfo> allDeepViewProductInfoList= new ArrayList<>();
          allDeepViewProductInfoList.addAll(deep0ViewPidList);
          allDeepViewProductInfoList.addAll(deep3ViewPidList);
          allDeepViewProductInfoList.addAll(deep7ViewPidList);

            if(CollectionUtils.isEmpty(allDeepViewProductInfoList)){
                for (DeepViewProductInfo deepViewProductInfo:allDeepViewProductInfoList){
                    if(pid.equals(deepViewProductInfo.getPid())){
                        return deepViewProductInfo.getViewTime();
                    }
                }
            }
             return  0L;
        }

}


