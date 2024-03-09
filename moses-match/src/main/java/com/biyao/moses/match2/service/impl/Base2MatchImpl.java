package com.biyao.moses.match2.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.biyao.moses.cache.*;
import com.biyao.moses.common.constant.CommonConstants;
import com.biyao.moses.common.constant.RedisKeyConstant;
import com.biyao.moses.match2.constants.MatchStrategyConst;
import com.biyao.moses.match2.param.MatchParam;
import com.biyao.moses.match2.service.Match2;
import com.biyao.moses.match2.util.MatchUtil;
import com.biyao.moses.model.match2.MatchItem2;
import com.biyao.moses.params.ProductInfo;
import com.biyao.moses.params.ProductScoreInfo;
import com.biyao.moses.rpc.UcRpcService;
import com.biyao.moses.util.AlgorithmRedisUtil;
import com.biyao.moses.util.FilterUtil;
import com.biyao.moses.util.StringUtil;
import com.uc.domain.bean.User;
import com.uc.domain.constant.UserFieldConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @program: moses-parent
 * @description: 基础流量召回源2
 * @author: changxiaowei
 * @create: 2021-07-05 14:30
 **/

@Slf4j
@Component(value = MatchStrategyConst.BASE2)
public class Base2MatchImpl implements Match2 {
    // 当天的小时数 小于此大小则不推商品
    private final static int HOUR_MAX = 12;
    // base2 对用户的最大曝光数
    private final static int EXP_MAX = 40;
    // 60*60*24*1000*3 3天
    private final static int days3ByMs = 259200000;
    // 最大召回商品数
    private final static int PID_NUM_MAX_LIMIT = 20;
    // 最大召回新品数
    private final static int NEW_PRODUCT_MAX_NUM = 5;
    @Autowired
    AlgorithmRedisUtil algorithmRedisUtil;
    @Autowired
    SwitchConfigCache switchConfigCache;
    @Autowired
    ProductDetailCache productDetailCache;
    @Autowired
    ProductSeasonCache productSeasonCache;
    @Autowired
    SimilarCategory3IdCache similarCategory3IdCache;
    @Autowired
    UcRpcService ucRpcService;


    @Override
    public List<MatchItem2> match(MatchParam matchParam) {
        Long startTime=System.currentTimeMillis();
        // 初始化结果集
        List<MatchItem2> result = new ArrayList<>();
        // 如果当前时间在0点-12点之间 则直接返回空list
        if (getHour() < HOUR_MAX) {
            return result;
        }
        // 从uc中获取用户感兴趣类目(UserFieldConstants.LEVEL3HOBBY::level3Hobby)和新品偏好(UserFieldConstants.NEWSINTEREST::newsInterest)和用户近24小时对base2的曝光商品数
        User user = getDataFromUc(matchParam.getUuid(), matchParam.getUid());
        //如果base2对该用户的曝光数已经超过40 则直接返回
        List<Long> base2ExpPids = user.getBase2ExpPids();
        if (!CollectionUtils.isEmpty(base2ExpPids) && base2ExpPids.size() >= EXP_MAX) {
            return result;
        }
        // 从redis中获取全部的流量扶持商品
        String validSupport = algorithmRedisUtil.getString(RedisKeyConstant.KEY_MOSES_VALID_SUPPORT);
        // 解析validSupport ---> List<Long>
        List<Long> validSupportPidList = StringUtil.parseProductStr(validSupport, -1, "base2").
                stream().map(ProductScoreInfo::getProductId).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(validSupportPidList)) {
            log.error("[严重异常]流量扶持商品数据为空，uuid：{}", matchParam.getUuid());
            return result;
        }
        try {
            // 从算法redis获取基础曝光数
            List<Integer> baseExpList = getBaseExp(validSupportPidList);
            // 从算法redis获取实时曝光数
            List<Integer> baseRtExpList = getBaseRtExp(validSupportPidList);
            // 过滤掉不符合的商品
            commonFilter(validSupportPidList, base2ExpPids, baseExpList, baseRtExpList, matchParam.getUserSex(), user.getSeason(),matchParam.getUuid());
            // 对validSupportPidList 进行聚合 按照相似三级类目的形式  key 为相似三级类目 value 为商品id list
            Map<Long, List<Long>> similar3dPrductIdMap = aggregationProductBySimilar3d(validSupportPidList);
            // 获取用户感兴趣相似三级类目集合
            Set<Long> interestSimilar3dSet = getInterestSimilarCate3dSet(user);
            // 去除用户感兴趣后的相似三级类目
            Set<Long> unInterestSimilar3dset = getNotInterestSimilarCate3dSet(similar3dPrductIdMap,interestSimilar3dSet);
            // 获取用户新品偏好
            boolean isNewsInterest = getNewsInterest(user);
            List<Long> resultList = new ArrayList<>();
            // 用户有新品偏好
            if (isNewsInterest) {
                // 如果用户有感兴趣类目
                if (interestSimilar3dSet.size() > 0) {
                    // 获取新品
                    getNewProduct(similar3dPrductIdMap, interestSimilar3dSet, unInterestSimilar3dset, resultList, NEW_PRODUCT_MAX_NUM);
                    // 获取老品
                    getProduct(similar3dPrductIdMap, interestSimilar3dSet, unInterestSimilar3dset, resultList, true);
                } else {
                    // 获取新品
                    getNewProduct(similar3dPrductIdMap, new HashSet<>(similar3dPrductIdMap.keySet()), null, resultList, NEW_PRODUCT_MAX_NUM);
                    // 获取老品
                    getProduct(similar3dPrductIdMap, new HashSet<>(similar3dPrductIdMap.keySet()), null, resultList, true);
                }
            } else {
                if (interestSimilar3dSet.size() > 0) {
                    // 根据用户感兴趣的类目获取商品
                    getProduct(similar3dPrductIdMap, interestSimilar3dSet, unInterestSimilar3dset, resultList, false);
                } else {
                    // 获取商品
                    getProduct(similar3dPrductIdMap, new HashSet<>(similar3dPrductIdMap.keySet()), null, resultList, false);
                }
            }
            // 组装商品
            for (int i=0;i< resultList.size();i++){
                MatchItem2 matchItem2=new MatchItem2();
                matchItem2.setProductId(resultList.get(i));
                matchItem2.setSource("base2");
                Double score = (double) (20 - i) / PID_NUM_MAX_LIMIT;
                matchItem2.setScore(score);
                result.add(matchItem2);
            }
        }catch (Exception e){
            log.error("[严重异常][召回源]获取base2数据异常， uuid {}, uid {}, e ", matchParam.getUuid(), matchParam.getUid(), e);
        }
        log.info("[检查日志]base2召回源召回商品结束，召回数量:{}，耗时:{}ms，UUID:{}",result.size(),System.currentTimeMillis()-startTime,matchParam.getUuid());
        return result;

    }

    /**
     * 获取用户感兴趣相似三级类目
     *
     * @return
     */
    private Set<Long> getInterestSimilarCate3dSet(User user) {
        //初始化结果集
        Set<Long> similar3dSet = new HashSet<>();
        // 获取用户感兴趣三级类目
        Map<String, BigDecimal> level3HobbyMap = user.getLevel3Hobby();
        // 遍历 level3HobbyMap
        for (Map.Entry<String, BigDecimal> entry : level3HobbyMap.entrySet()) {
            // 如果感兴趣三级类目偏好分大于5
            try {
                if (entry.getValue().compareTo(new BigDecimal(5)) >= 0) {
                    // 则获取此三级类目的相似三级类目
                    similar3dSet.add(similarCategory3IdCache.getSimilarCate3Id(Long.valueOf(entry.getKey())));
                }
            }catch (Exception e){
                log.error("[严重异常]获取用户感兴趣相似三级类目异常，uuid:{},异常信息:{}",user.getUuid(),e);
            }
        }
        return similar3dSet;
    }
    private Set<Long> getNotInterestSimilarCate3dSet(Map<Long, List<Long>> similar3dPrductIdMap,Set<Long> interestSimilar3dSet) {
        //初始化结果集
        Set<Long> unInterestSimilar3dset = new HashSet<>();
        for (Long similar3d :similar3dPrductIdMap.keySet()) {
            if(!interestSimilar3dSet.contains(similar3d)){
                unInterestSimilar3dset.add(similar3d);
            }
        }
        return unInterestSimilar3dset;
    }
    /**
     * 用户是否有新品偏好
     *
     * @param user
     * @return
     */
    private boolean getNewsInterest(User user) {
        String newsInterest = user.getNewsInterest();
        try {
            if (Double.valueOf(newsInterest) >= 0.5d) {
                return true;
            }
        }catch (Exception e){
            log.error("[严重异常] 用户新品偏好格式错误user：{}", JSONObject.toJSONString(user));
        }
        return false;
    }

    /**
     * 获取新品
     *
     * @param similar3dPrductIdMap 商品池
     * @param preferenceSet       首选
     * @param candidateSet        候选
     * @param targetNum            期望数量
     */
    private void getNewProduct(Map<Long, List<Long>> similar3dPrductIdMap, Set<Long> preferenceSet,  Set<Long> candidateSet, List<Long> result, int targetNum) {
        // 从首选preferenceList 中获取新品
        getNewProductFromSimilarSet(similar3dPrductIdMap,preferenceSet,targetNum,result);
        // 如果 首选preferenceList中没选中targetNum 则从候选candidateList中补齐
        if(result.size()<targetNum){
            getNewProductFromSimilarSet(similar3dPrductIdMap,candidateSet,targetNum-result.size(),result);
        }
    }

    /**
     * 从指定的集合中获取新品
     * @param similar3dPrductIdMap
     * @param similar3dSet
     * @param targetNum
     * @param result
     */
    private void getNewProductFromSimilarSet(Map<Long, List<Long>> similar3dPrductIdMap, Set<Long> similar3dSet,int targetNum,List<Long> result){
        if(CollectionUtils.isEmpty(similar3dSet)){
            return;
        }
        long currentTime = System.currentTimeMillis();
        for (Long similar3d:similar3dSet){
            List<Long> pidList = similar3dPrductIdMap.getOrDefault(similar3d,new ArrayList<>());
            for (Long pid:pidList){
                if(result.size()>=targetNum){
                    break;
                }
                ProductInfo productInfo = productDetailCache.getProductInfo(pid);
                // 三天内上架的商品为新品
                if(productInfo!= null && productInfo.getFirstOnshelfTime()!= null && currentTime - productInfo.getFirstOnshelfTime().getTime() <= days3ByMs){
                    result.add(pid);
                }
            }
        }
    }

    /**
     * 获取商品
     *
     * @param similar3dPrductIdMap 商品池
     * @param preferenceSet       首选
     * @param candidateSet        备选
     * @param result               结果集
     * @param isOld                是否只取老品
     */
    private void getProduct(Map<Long, List<Long>> similar3dPrductIdMap,  Set<Long> preferenceSet,  Set<Long> candidateSet, List<Long> result, boolean isOld) {

        // 从首选preferenceList中选择商品，每个相似三级类目最多选择5个
        getProductFromSimilarSet(similar3dPrductIdMap,preferenceSet,result,isOld);
        //  若选取的商品不足20个时 则从候选集中选择商品
        if(result.size() < PID_NUM_MAX_LIMIT){
            getProductFromSimilarSet(similar3dPrductIdMap,candidateSet,result,isOld);
        }
    }

    private void  getProductFromSimilarSet(Map<Long, List<Long>> similar3dPrductIdMap,  Set<Long> similar3dSet,List<Long> result, boolean isOld){
            if(CollectionUtils.isEmpty(similar3dSet)){
                return;
            }
        long currentTime = System.currentTimeMillis();
        for (Long similar3d : similar3dSet){
            if(result.size() >= PID_NUM_MAX_LIMIT){
                break;
            }
            int temp=0;
            List<Long> list = similar3dPrductIdMap.getOrDefault(similar3d,new ArrayList<>());
            for (Long pid :list){
                if(result.size() >= PID_NUM_MAX_LIMIT || temp >= 5){
                    break;
                }
                ProductInfo productInfo = productDetailCache.getProductInfo(pid);
                if(productInfo != null){
                    if(isOld){
                        // 三天内上架的商品为新品
                        if(currentTime - productInfo.getFirstOnshelfTime().getTime() > days3ByMs){
                            result.add(pid);
                            temp++;
                        }
                    }else {
                        result.add(pid);
                        temp++;
                    }
                }
            }
        }
    }
    /**
     * 过滤流量扶持商品
     * @param validSupportPidList 流量扶持商品集合
     * @param base2ExpPids        base2召回源24小时内对用户曝光的商品
     * @param baseExpList         基础曝光数
     * @param baseRtExpList       实时曝光数
     */
    private void commonFilter(List<Long> validSupportPidList, List<Long> base2ExpPids, List<Integer> baseExpList, List<Integer> baseRtExpList,
                              Integer userSex, String userSeason,String uuid) {

        for (int i = validSupportPidList.size() - 1; i >= 0; i--) {
            Long pid = validSupportPidList.get(i);
            ProductInfo productInfo = productDetailCache.getProductInfo(pid);
            try {
                if (FilterUtil.isCommonFilter(productInfo)) {
                    validSupportPidList.remove(i);
                    continue;
                }
                // 商品基础曝光数 =< 商品实时曝光数的流量扶持商品
                if (baseRtExpList.get(i) >= baseExpList.get(i)) {
                    validSupportPidList.remove(i);
                    continue;
                }
                // 性别过滤
                if (MatchUtil.isFilterBySex(productInfo, userSex)) {
                    validSupportPidList.remove(i);
                    continue;
                }
                // 季节过滤
                if (MatchUtil.isFilterByUserSeason(productSeasonCache.getProductSeasonValue(pid.toString()), MatchUtil.convertSeason2int(userSeason))) {
                    validSupportPidList.remove(i);
                    continue;
                }
                // base2源在24小时内对该用户曝光过的商品
                if (base2ExpPids.contains(pid)) {
                    validSupportPidList.remove(i);
                }
            }catch (Exception e){
                log.error("[严重异常] 过滤流量扶持商品出现异常，UUID:{},异常信息：",uuid,e);
                validSupportPidList.remove(i);
            }
        }
    }

    /**
     * 获取当前小时数
     * @return
     */
    private int getHour() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        return calendar.get(Calendar.HOUR_OF_DAY);
    }

    /**
     * 从Uc中获取数据
     *
     * @param uuid
     * @param uid
     * @return
     */
    private User getDataFromUc(String uuid, Integer uid) {
        List<String> fields = new ArrayList<>();
        fields.add(UserFieldConstants.LEVEL3HOBBY);
        fields.add(UserFieldConstants.NEWSINTEREST);
        fields.add(UserFieldConstants.BSAE2EXPPIDS);
        fields.add(UserFieldConstants.SEASON);
        String uidStr = uid == null ? null : uid.toString();
        return ucRpcService.getData(uuid, uidStr, fields, "mosesmatch");
    }

    /**
     * 获取实时曝光数
     *
     * @param validSupportPidList
     * @return
     */
    private List<Integer> getBaseRtExp(List<Long> validSupportPidList) {

        List<String> baseRtExp = algorithmRedisUtil.hmget(RedisKeyConstant.KEY_MOSES_RT_EXP, validSupportPidList.stream().map(Object::toString).toArray(String[]::new));
        return baseRtExp.stream().map(s -> StringUtil.isBlank(s) ? 0 : Integer.valueOf(s)).collect(Collectors.toList());
    }

    /**
     * 获取基础曝光数
     *
     * @param validSupportPidList
     * @return
     */
    private List<Integer> getBaseExp(List<Long> validSupportPidList) {

        List<String> baseExp = algorithmRedisUtil.hmget(RedisKeyConstant.KEY_MOSES_BASE_EXP, validSupportPidList.stream().map(Object::toString).toArray(String[]::new));

        List<Integer> list = baseExp.stream().map(s -> (StringUtil.isBlank(s) || !StringUtil.isInteger(s)) ? 0 : Integer.valueOf(s)).collect(Collectors.toList());

        // 获取基础曝光数A
        String aBaseNumStr = switchConfigCache.getRecommendContentByConfigId(CommonConstants.A_BASE_NUM);
        int aBaseNum = (StringUtil.isBlank(aBaseNumStr) || !StringUtil.isInteger(aBaseNumStr)) ? 500 : Integer.valueOf(aBaseNumStr);
        // 获取基础曝光数N
        String nBaseNumStr = switchConfigCache.getRecommendContentByConfigId(CommonConstants.N_BASE_NUM);
        int nBaseNum = (StringUtil.isBlank(nBaseNumStr) || !StringUtil.isInteger(nBaseNumStr)) ? 5000 : Integer.valueOf(nBaseNumStr);
        /**
         * a)若B＞A，则基础曝光数为B
         * b)若B<=A，则基础曝光数为A
         * c)基础曝光数最大不超过N，超过N选N
         * i.N在rsm-推荐运营配置-素材配置中进行配置
         * 1.配置id：Nbasenum
         * 2.配置值：运营配置，最小值为0
         * 3.若rsm中未配置或系统异常取不到N，则默认为5000
         */
        return list.stream().map(temp -> Math.min(Math.max(temp, aBaseNum), nBaseNum)).collect(Collectors.toList());
    }

    /**
     * 按照相似三级类目的形式进行聚合 key 为相似三级类目 value 为商品id list
     * @param validSupportPidList
     * @return
     */
    private Map<Long, List<Long>> aggregationProductBySimilar3d(List<Long> validSupportPidList){

        Map<Long, List<Long>> similar3dPrductIdMap = new HashMap<>();
        for (Long pid:validSupportPidList){
            ProductInfo productInfo = productDetailCache.getProductInfo(pid);
            if(productInfo == null || productInfo.getThirdCategoryId()==null){
                continue;
            }
            Long similarCate3Id = similarCategory3IdCache.getSimilarCate3Id(productInfo.getThirdCategoryId());
            if(similar3dPrductIdMap.containsKey(similarCate3Id)){
                similar3dPrductIdMap.get(similarCate3Id).add(pid);
            }else {
                List<Long> pidList=new ArrayList<>();
                pidList.add(pid);
                similar3dPrductIdMap.put(similarCate3Id,pidList);
            }
        }
        return similar3dPrductIdMap;
    }


}
