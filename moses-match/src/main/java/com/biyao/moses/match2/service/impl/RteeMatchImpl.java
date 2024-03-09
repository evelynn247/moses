package com.biyao.moses.match2.service.impl;

import com.biyao.moses.cache.*;
import com.biyao.moses.match2.constants.MatchStrategyConst;
import com.biyao.moses.match2.param.MatchParam;
import com.biyao.moses.match2.service.Match2;
import com.biyao.moses.match2.util.MatchUtil;
import com.biyao.moses.model.match2.MatchItem2;
import com.biyao.moses.params.ProductInfo;
import com.biyao.moses.rpc.UcRpcService;
import com.biyao.moses.util.FilterUtil;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import com.uc.domain.bean.User;
import com.uc.domain.constant.UserFieldConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;


/**
 * @ClassName RteeMatchImpl
 * @Description 实时兴趣探索召回源
 * @Author xiaojiankai
 * @Date 2019/11/29 20:43
 * @Version 1.0
 **/
@Slf4j
@Component(value = MatchStrategyConst.RTEE)
@Deprecated
public class RteeMatchImpl implements Match2 {
    //最多从5个后台三级类目组中推出商品
    private static final int  CATEGORY_NUM_MAX = 5;
    //每个后台三级类目组最多推出5个商品
    private static  final int CATEGORY_PRODUCT_NUM_MAX = 5;
    //最多推出的商品数量上限
    private static  final int PRODUCT_NUM_MAX = 25;
    //正向反馈率阈值
    private static  final double DEEP_VIEW_DIV_IMPRESSION = 0.2d;
    //60天内深度浏览商品的下限
    private static final int VIEW_PRODUCT_NUM_LIMIT = 15;

    //60*60*24*60*1000 60天毫秒数
    private static final long SIXTY_DAY_MS = 5184000000L;

    //60*60*24*7*1000 7天毫秒数
    private static final long SEVEN_DAY_MS = 604800000L;

    @Autowired
    private ProductDetailCache productDetailCache;

    @Autowired
    private UcRpcService ucRpcService;

    @Autowired
    private SimilarCategory3IdCache similarCategory3IdCache;

    @Autowired
    private CandidateCate3ProductCache candidateCate3ProductCache;

    @Autowired
    private ProductSeasonCache productSeasonCache;

    @BProfiler(key = "RteeMatchImpl.match",
            monitorType = {MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR})
    @Override
    public List<MatchItem2> match(MatchParam matchParam) {
        List<MatchItem2> result = new ArrayList<>();
        Integer uid = matchParam.getUid();
        String uuid = matchParam.getUuid();
        try {
            Integer upcUserType = matchParam.getUpcUserType();
            Map<String, List<Long>> candidateCate3ProductMap = candidateCate3ProductCache.getCacheMap(upcUserType);
            if (candidateCate3ProductMap == null || candidateCate3ProductMap.size() == 0) {
                log.error("[严重异常][召回源]该用户类型候选三级类目及候选商品为空，uuid {}, uid {}, upcUserType {}", uuid, uid, upcUserType);
                return result;
            }

            List<Long> view60dPidList = new ArrayList<>();
            List<Long> viewTodayPidList = new ArrayList<>();
            long currentTime = System.currentTimeMillis();
            long zeroTime = currentTime - (currentTime + TimeZone.getDefault().getRawOffset()) % (1000 * 3600 * 24);
            List<String> viewPidTimeStrList = getViewProductFromUc(uuid);
            if (!CollectionUtils.isEmpty(viewPidTimeStrList)) {
                List<Long> viewTimeList = new ArrayList<>();
                viewTimeList.add(zeroTime - SIXTY_DAY_MS);
                viewTimeList.add(zeroTime);
                List<List<Long>> viewPidList = parseAndGetPidByTime(viewPidTimeStrList, viewTimeList);
                view60dPidList = viewPidList.get(0);
                viewTodayPidList = viewPidList.get(1);
            }
            //第一个元素为60天+当日的深度浏览商品id集合
            if (view60dPidList != null && view60dPidList.size() >= VIEW_PRODUCT_NUM_LIMIT) {
                return result;
            }

            User ucUser = getDeepViewImpSeasonFromUc(uuid);
            //用户季节
            int userSeasonValue = MatchUtil.convertSeason2int(ucUser.getSeason());
            //60天离线后台三级类目组的曝光商品次数和深度浏览商品次数,String格式为cate3Id:count
            List<String> cate3View60dNumList = ucUser.getViewNum60d();
            List<String> cate3Imp60dNumList = ucUser.getExpNum60d();

            //60天(不含当天)各后台三级类目组对应的深度浏览次数
            Map<String, Long> cate3View60dNumMap = parseCate3IdCount(cate3View60dNumList);
            //60天(不含当天)各后台三级类目组对应的曝光次数
            Map<String, Long> cate3Imp60dNumMap = parseCate3IdCount(cate3Imp60dNumList);

            //曝光商品集合
            List<String> impPids = ucUser.getExpPids();
            //当日曝光商品Id集合
            List<Long> impPidTodayList = new ArrayList<>();
            //当日+一周曝光商品Id集合
            Set<Long> impPidSevenDaySet = new HashSet<>();
            if (CollectionUtils.isNotEmpty(impPids)) {
                List<Long> impTimeList = new ArrayList<>();
                impTimeList.add(zeroTime);
                impTimeList.add(zeroTime - SEVEN_DAY_MS);
                List<List<Long>> impPidsList = parseAndGetPidByTime(impPids, impTimeList);
                impPidTodayList = impPidsList.get(0);
                List<Long> impPidSevenDayList = impPidsList.get(1);
                impPidSevenDaySet.addAll(impPidSevenDayList);
            }

            //当日各后台三级类目组对应的曝光次数
            Map<String, Long> cate3impTodayNumMap = getCate3PidNumByPidList(impPidTodayList);
            //当日各后台三级类目组对应的深度浏览次数
            Map<String, Long> cate3ViewTodayNumMap = getCate3PidNumByPidList(viewTodayPidList);

            int similarCate3IdCount = 0;
            //遍历候选三级类目组ID集合
            for (Map.Entry<String, List<Long>> map : candidateCate3ProductMap.entrySet()) {
                if (similarCate3IdCount >= CATEGORY_NUM_MAX) {
                    break;
                }
                int similarCate3IdProductCount = 0;
                String similarCate3Id = map.getKey();
                List<Long> candidateProductIdList = map.getValue();
                double view60dCount = cate3View60dNumMap.getOrDefault(similarCate3Id, 0L).doubleValue();
                double viewTodayCount = cate3ViewTodayNumMap.getOrDefault(similarCate3Id, 0L).doubleValue();
                double imp60dCount = cate3Imp60dNumMap.getOrDefault(similarCate3Id, 0L).doubleValue();
                double impTodayCount = cate3impTodayNumMap.getOrDefault(similarCate3Id, 0L).doubleValue();
                double viewDivImp = (view60dCount + viewTodayCount + 1) / (imp60dCount + impTodayCount + 1);
                if (viewDivImp < DEEP_VIEW_DIV_IMPRESSION) {
                    continue;
                }
                int cate3PidNumLimitTmp = (int) Math.floor(viewDivImp * CATEGORY_PRODUCT_NUM_MAX);
                int cate3PidNumMaxLimit = cate3PidNumLimitTmp > CATEGORY_PRODUCT_NUM_MAX ? CATEGORY_PRODUCT_NUM_MAX : cate3PidNumLimitTmp;
                //遍历三级类目组ID下的候选商品集合
                for (Long pid : candidateProductIdList) {
                    if (similarCate3IdProductCount >= cate3PidNumMaxLimit) {
                        break;
                    }
                    ProductInfo productInfo = productDetailCache.getProductInfo(pid);
                    if (FilterUtil.isCommonFilter(productInfo)) {
                        continue;
                    }
                    //过滤掉一周内已曝光商品
                    if (impPidSevenDaySet.contains(pid)) {
                        continue;
                    }
                    //过滤掉非用户季节商品(用户季节未获取到则不进行用户季节过滤)
                    if (MatchUtil.isFilterByUserSeason(productSeasonCache.getProductSeasonValue(pid.toString()), userSeasonValue)) {
                        continue;
                    }
                    //过滤掉非用户性别商品
                    if (MatchUtil.isFilterBySex(productInfo, matchParam.getUserSex())) {
                        continue;
                    }
                    MatchItem2 matchItem2 = new MatchItem2();
                    matchItem2.setProductId(pid);
                    matchItem2.setScore((double) (PRODUCT_NUM_MAX - result.size()) / PRODUCT_NUM_MAX);
                    matchItem2.setSource(MatchStrategyConst.RTEE);
                    result.add(matchItem2);
                    //该类目推出的商品数加1
                    similarCate3IdProductCount++;
                }
                //如果该类目下推出了商品，则推出的类目数加1
                if (similarCate3IdProductCount > 0) {
                    similarCate3IdCount++;
                }
            }
        }catch (Exception e){
            log.error("[严重异常][召回源]rtee召回源处理异常， uuid {}, uid {}, e", uuid, uid);
        }

        return result;
    }

    /**
     * 从uc中获取离线60天各后台三级类目组的深度浏览次数、曝光次数、曝光商品信息、用户季节
     * @param uuid
     * @return
     */
    private User getDeepViewImpSeasonFromUc(String uuid){
        User result = new User();
        List<String> fields = new ArrayList<>();
        //60天（不包括当日）的各后台三级类目组的深度浏览商品次数
        fields.add(UserFieldConstants.VIEWNUM60D);
        //60天（不包括当日）的各后台三级类目组的曝光商品次数
        fields.add(UserFieldConstants.EXPNUM60D);
        //曝光商品ID 最多500条
        fields.add(UserFieldConstants.EXPPIDS);
        //用户季节
        fields.add(UserFieldConstants.SEASON);
        //获取深度用户浏览记录
        User ucUser = ucRpcService.getData(uuid, null, fields, "mosesmatch");
        if(ucUser == null){
            result.setUuid(uuid);
        }else{
            result = ucUser;
        }
        return result;
    }
    /**
     * 从uc获取用户浏览商品信息
     * @param uuid
     * @return
     */
    private List<String> getViewProductFromUc(String uuid){
        List<String> result = new ArrayList<>();
        List<String> fields = new ArrayList<>();
        fields.add(UserFieldConstants.VIEWPIDS);
        //获取深度用户浏览记录
        User ucUser = ucRpcService.getData(uuid, null, fields, "mosesmatch");
        if(ucUser == null || CollectionUtils.isEmpty(ucUser.getViewPids())){
            return result;
        }
        result = ucUser.getViewPids();
        return result;
    }
    /**
     * 解析并获取每个后台三级类目组对应的次数，格式为：similarCate3Id：count
     * @param cate3IdCountList
     * @return
     */
    private Map<String, Long> parseCate3IdCount(List<String> cate3IdCountList){
        Map<String, Long> result = new HashMap<>();
        if(CollectionUtils.isEmpty(cate3IdCountList)){
            return result;
        }
        boolean isCheckError = false;
        for(String cate3IdCountStr : cate3IdCountList){
            if(StringUtils.isBlank(cate3IdCountStr)){
                isCheckError = true;
                continue;
            }
            String[] cate3IdCountArray = cate3IdCountStr.trim().split(":");
            if(cate3IdCountArray.length != 2){
                isCheckError = true;
                continue;
            }
            try{
                Long similarCate3Id = Long.valueOf(cate3IdCountArray[0].trim());
                Long count = Long.valueOf(cate3IdCountArray[1].trim());
                String similar3CategoryIdStr = similarCate3Id.toString();
                if(result.containsKey(similar3CategoryIdStr)){
                    result.put(similar3CategoryIdStr,result.get(similar3CategoryIdStr)+count);
                }else{
                    result.put(similar3CategoryIdStr, count);
                }
            }catch (Exception e){
                isCheckError = true;
            }
        }

        if(isCheckError){
            log.error("[一般异常][召回源]解析从uc中获取的类目：次数失败，格式不正确");
        }
        return result;
    }

    /**
     * 解析商品id集合，转化成后台三级类目组对应的商品个数
     * @param pidList
     * @return
     */
    private Map<String, Long> getCate3PidNumByPidList(List<Long> pidList){
        Map<String, Long> result = new HashMap<>();
        if(CollectionUtils.isEmpty(pidList)){
            return result;
        }

        for(Long pid : pidList){
            ProductInfo productInfo = productDetailCache.getProductInfo(pid);
            if(productInfo == null || productInfo.getThirdCategoryId() == null){
                continue;
            }
            Long similar3CategoryId = similarCategory3IdCache.getSimilarCate3Id(productInfo.getThirdCategoryId());
            if(similar3CategoryId == null){
                similar3CategoryId = productInfo.getThirdCategoryId();
            }
            String similar3CategoryIdStr = similar3CategoryId.toString();
            if(result.containsKey(similar3CategoryIdStr)){
                result.put(similar3CategoryIdStr, result.get(similar3CategoryIdStr)+1);
            }else{
                result.put(similar3CategoryIdStr, 1L);
            }
        }

        return result;
    }
    /**
     * 获取指定时间内的商品ID集合
     * @param pidTimeStrList pid:time的集合
     * @param timeList 预期时间集合
     * @return
     */
    private List<List<Long>> parseAndGetPidByTime(List<String> pidTimeStrList, List<Long> timeList){
        List<List<Long>> result = new ArrayList<>();
        if(CollectionUtils.isEmpty(timeList)){
            return result;
        }
        //初始化返回结果中的集合
        int size = timeList.size();
        for(int i = 0; i < size; i++){
            List<Long> pidList = new ArrayList<>();
            result.add(pidList);
        }

        if(CollectionUtils.isEmpty(pidTimeStrList)){
            return result;
        }

        boolean isCheckError = false;
        for(String pidTimeStr : pidTimeStrList){
            if(StringUtils.isBlank(pidTimeStr)){
                continue;
            }
            String[] pidTimeArray = pidTimeStr.trim().split(":");
            if(pidTimeArray.length != 2){
                isCheckError = true;
                continue;
            }
            try {
                Long pid = Long.valueOf(pidTimeArray[0].trim());
                Long time = Long.valueOf(pidTimeArray[1].trim());
                for(int i = 0; i < size; i++) {
                    if (timeList.get(i) <= time) {
                        result.get(i).add(pid);
                    }
                }
            }catch(Exception e){
                isCheckError = true;
            }
        }

        if(isCheckError){
            log.error("[一般异常][召回源]获取指定时间内的商品Id集合，解析pid：time时格式不正确");
        }
        return result;
    }


}
