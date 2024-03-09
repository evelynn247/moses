package com.biyao.moses.service;

import com.alibaba.fastjson.JSON;
import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.common.constant.CacheRedisKeyConstant;
import com.biyao.moses.common.constant.ExpFlagsConstants;
import com.biyao.moses.common.constant.RedisKeyConstant;
import com.biyao.moses.common.constant.UPCUserTypeConstants;
import com.biyao.moses.model.match2.MatchItem2;
import com.biyao.moses.model.rank2.RankItem2;
import com.biyao.moses.params.ProductInfo;
import com.biyao.moses.params.rank2.RankRequest2;
import com.biyao.moses.punishment.impl.ProductExposurePunishmentImpl;
import com.biyao.moses.punishment.impl.ProductSizePunishmentImpl;
import com.biyao.moses.util.CacheRedisUtil;
import com.biyao.moses.util.RedisUtil;
import com.google.common.base.Splitter;
import com.uc.domain.bean.User;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @ClassName CategoryAlgorithmService
 * @Description 获取分类页算法分
 * @Author 肖建凯
 * @Date 2020/12/18 23:19
 * @Version 1.0
 **/
@Component
@Slf4j
public class CategoryService {

    @Autowired
    private AlgorithmRedisAnsyService algorithmRedisAnsyService;

    @Autowired
    private ProductDetailCache productDetailCache;

    @Autowired
    private ProductExposurePunishmentImpl productExposurePunishment;

    @Autowired
    private ProductSizePunishmentImpl productSizePunishment;

    @Autowired
    private CacheRedisUtil cacheRedisUtil;

    @Autowired
    private RedisUtil redisUtil;

    /**
     * 前台类目ID,用于对特定前台类目做分流实验
     */
    @Value("${exp.frontend.categoryid}")
    private String expFrontendCategoryId;

    /**
     * 默认算法排序数据号
     */
    private static final String DEFAULT_DATA_NUM = "0100";

    /**
     * 商品默认分（当未从算法获取到商品分时，则使用该默认分）
     */
    private static final double DEFAULT_PRODUCT_SCORE = 0.0001;

    /**
     * 新品置顶规则中置顶的新品的数量上限
     */
    private static final int NEW_PRODUCT_TOP_PID_NUM_UP_LIMIT = 2;


    /**
     * 通过算法数据对分类页商品进行排序
     * @param rankRequest2
     */
    public List<MatchItem2> dealCategoryAlgorithmSort(RankRequest2 rankRequest2){

        List<MatchItem2> matchItemList = rankRequest2.getMatchItemList();
        String uuid = rankRequest2.getUuid();
        int uid = rankRequest2.getUid() == null ? 0 : rankRequest2.getUid();
        int upcUserType = rankRequest2.getUpcUserType() == null ? UPCUserTypeConstants.NEW_VISITOR: rankRequest2.getUpcUserType();
        String frontendCategoryId = rankRequest2.getFrontendCategoryId();
        String cate3Ids = rankRequest2.getCategoryIds();
        String dataNum = rankRequest2.getDataNum();

        if(CollectionUtils.isEmpty(matchItemList)){
            return matchItemList;
        }

        List<MatchItem2> result;
        try {
            //获取参数
            if (StringUtils.isBlank(dataNum) || ExpFlagsConstants.VALUE_DEFAULT.equals(dataNum)) {
                dataNum = DEFAULT_DATA_NUM;
            }

            String[] dataNumArray = dataNum.trim().split(",");
            String dataNum1 = dataNumArray[0];
            String dataNum2 = dataNumArray[0];
            if (dataNumArray.length > 1) {
                dataNum2 = dataNumArray[1];
            }

            String[] cate3IdArray;
            if (StringUtils.isNotBlank(cate3Ids)) {
                cate3IdArray = cate3Ids.trim().split(",");
            } else {
                cate3IdArray = new String[1];
            }

            Map<String, String> productScore1 = new HashMap<>();
            Map<String, String> productScore2 = new HashMap<>();

            List<String> pidList = matchItemList.stream().map(matchItem2 -> matchItem2.getProductId().toString()).collect(Collectors.toList());
            String[] pidArray = pidList.toArray(new String[0]);

            /****************start*********************/
            //根据前台类目做分流实验
            if (StringUtils.isNotBlank(frontendCategoryId) && StringUtils.isNotEmpty(expFrontendCategoryId) &&
                    Splitter.on(",").trimResults().splitToList(expFrontendCategoryId).contains(frontendCategoryId)) {
                //新品的静态分直接从0200兜底数据获取
                dataNum2 = "0200";
                cate3IdArray = new String[1];
                cate3IdArray[0] = "new_product_category";
            } else {
                if (DEFAULT_DATA_NUM.equals(dataNum1)) {
                    //获取商品分数（redis）
                    Future<HashMap<String, String>> future1 = algorithmRedisAnsyService.redisHmget(RedisKeyConstant.KEY_PREFIEX_MOSES_CR_UP + dataNum1 + "_" + uuid, pidArray);
                    Future<HashMap<String, String>> future2 = algorithmRedisAnsyService.redisHmget(RedisKeyConstant.KEY_PREFIEX_MOSES_CR_P + frontendCategoryId + "_" + dataNum1 + "_" + upcUserType, pidArray);
                    try {
                        productScore1 = future1.get(20, TimeUnit.MILLISECONDS);
                    } catch (Exception e) {
                        log.error("[严重异常]productScore1 redis获取分数超时, uuid {}, uid {}", uuid, uid);
                    }

                    try {
                        productScore2 = future2.get(20, TimeUnit.MILLISECONDS);
                    } catch (Exception e) {
                        log.error("[严重异常]productScore2 redis获取分数超时, uuid {}, uid {}", uuid, uid);
                    }
                } else {
                    //获取商品分数（redis）
                    productScore1 = queryScoreByCate3Id(uuid, uid, dataNum1, cate3IdArray);
                }
            }
            /****************end*********************/

            //全局静态分
            Future<HashMap<String, String>> future3 = algorithmRedisAnsyService.redisHmget(RedisKeyConstant.KEY_PREFIEX_MOSES_RS_P_DR + dataNum2, cate3IdArray);
            Map<String, String> productScore3 = new HashMap<>();
            try {
                Map<String, String> cate3PidScoreMap = future3.get(20, TimeUnit.MILLISECONDS);
                productScore3 = convertToPidScoreMap(cate3PidScoreMap);
            } catch (Exception e) {
                log.error("[严重异常]productScore3 redis获取分数超时, uuid {}, uid {}", uuid, uid);
            }
            //获取不到静态分时，则return
            if ((productScore1 == null || productScore1.size() == 0)
                    && (productScore3 == null || productScore3.size() == 0)
                    && (productScore2 == null || productScore2.size() == 0)) {

                log.error("[严重异常]uuid={},frontendCategoryId={},categoryIds={},dataNum={},获取静态分结果为空", uuid, frontendCategoryId, cate3Ids, dataNum);
            }

            //null值处理
            if (productScore1 == null) {
                productScore1 = new HashMap<>();
            }
            if (productScore2 == null) {
                productScore2 = new HashMap<>();
            }
            if (productScore3 == null) {
                productScore3 = new HashMap<>();
            }

            for (MatchItem2 matchItem2 : matchItemList) {
                matchItem2.setScore(DEFAULT_PRODUCT_SCORE);
                String pid = matchItem2.getProductId().toString();
                String calculateScore = null;
                if (productScore1.containsKey(pid)) {
                    calculateScore = productScore1.get(pid);
                } else if (productScore2.containsKey(pid)) {
                    calculateScore = productScore2.get(pid);
                } else if (productScore3.containsKey(pid)) {
                    calculateScore = productScore3.get(pid);
                }
                if(StringUtils.isBlank(calculateScore)){
                    continue;
                }
                try {
                    matchItem2.setScore(Double.valueOf(calculateScore));
                } catch (Exception e) {
                    log.error("[严重异常]商品分转换失败，pid {},score {}", pid, calculateScore);
                }
            }

            result = matchItemList.stream().sorted(Comparator.comparing(MatchItem2::getScore).reversed())
                    .collect(Collectors.toList());
        }catch (Exception e){
            log.error("[严重异常]处理类目页算法排序规则时出现异常，sid {}， uuid {}, fcateId {} ", rankRequest2.getSid(), uuid, frontendCategoryId, e);
            fillDefaultScore(matchItemList);
            result = matchItemList;
        }
        return result;
    }

    /**
     * 当分数为空时，则填充默认分
     * @param matchItem2List
     */
    private void fillDefaultScore(List<MatchItem2> matchItem2List){
        if(CollectionUtils.isEmpty(matchItem2List)){
            return;
        }

        for(MatchItem2 matchItem2 : matchItem2List){
            if(matchItem2.getScore() == null){
                matchItem2.setScore(DEFAULT_PRODUCT_SCORE);
            }
        }
    }
    /**
     * 通过三级类目ID从redis中获取商品静态分
     */
    private Map<String, String> queryScoreByCate3Id(String uuid, int uid, String dataNum, String[] cate3IdArray){
        Map<String, String> productScore1 = new HashMap<>();
        //key 为后台三级类目ID，value格式为：pid:score
        Map<String, String> cate3PidScoreMap = new HashMap<>();
        //有uid则使用uid获取分值，没有则使用uuid获取分值
        if(uid > 0){
            Future<HashMap<String, String>> future1 = algorithmRedisAnsyService.redisHmget(RedisKeyConstant.KEY_PREFIEX_MOSES_CR_UP + dataNum + "_" + uid, cate3IdArray);
            try {
                cate3PidScoreMap = future1.get(20, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                log.error("[严重异常]redis通过uid获取分数超时, uuid {}, uid {},dateNum {}, cate3IdArray {}", uuid, uid, dataNum, JSON.toJSONString(cate3IdArray), e);
            }
        }else{
            //获取新品静态分
            Future<HashMap<String, String>> future1 = algorithmRedisAnsyService.redisHmget(RedisKeyConstant.KEY_PREFIEX_MOSES_CR_UP + dataNum + "_" + uuid, cate3IdArray);
            try{
                cate3PidScoreMap = future1.get(20, TimeUnit.MILLISECONDS);
            }catch (Exception e) {
                log.error("[严重异常]redis通过uuid获取分数超时, uuid {}, uid {},dateNum {}, cate3IdArray {}", uuid, uid, dataNum, JSON.toJSONString(cate3IdArray), e);
            }
        }
        productScore1 = convertToPidScoreMap(cate3PidScoreMap);
        return productScore1;
    }

    /**
     * 将后台三级类目下的pid:score集合转换为key为pid,value为score的集合
     * @param cate3PidScoreMap
     * @return
     */
    private Map<String, String> convertToPidScoreMap(Map<String, String> cate3PidScoreMap){
        Map<String, String> result = new HashMap<>();
        if(cate3PidScoreMap == null || cate3PidScoreMap.size() == 0){
            return result;
        }
        try {
            for (String cate3pidScoreStr : cate3PidScoreMap.values()) {
                if (StringUtils.isBlank(cate3pidScoreStr)) {
                    continue;
                }
                String[] pidScoreStrArray = cate3pidScoreStr.trim().split(",");
                for(String pidScoreStr : pidScoreStrArray) {
                    if(StringUtils.isBlank(pidScoreStr)){
                        continue;
                    }
                    String[] pidScoreArray = pidScoreStr.trim().split(":");
                    if (pidScoreArray.length != 2) {
                        continue;
                    }
                    String pid = pidScoreArray[0];
                    String score = pidScoreArray[1];
                    result.put(pid, score);
                }
            }
        }catch (Exception e){
            log.error("[严重异常]转换为商品id和分值的map时异常", e);
        }

        return result;
    }

    /**
     * 处理类目页曝光惩罚规则
     * @param matchItem2List
     * @param ucUser
     * @param uuid
     * @return
     */
    public List<MatchItem2> dealExposurePunishmentRule(List<MatchItem2> matchItem2List, User ucUser, String uuid){
        if(CollectionUtils.isEmpty(matchItem2List)){
            return matchItem2List;
        }
        List<MatchItem2> result;
        try {
            //获取曝光商品 格式pid:time
            List<String> fakeExpPids = cacheRedisUtil.lrange(CacheRedisKeyConstant.CATEGORY_FAKE_EXPOSURE_PREFIX + uuid, 0, -1);

            List<String> realExpPids = null;
            if (ucUser != null) {
                realExpPids = ucUser.getExpPids();
            }

            Map<Long, Double> punishmentScoreMap = productExposurePunishment.calculateExposurePunishmentScore(matchItem2List, fakeExpPids, realExpPids, uuid);

            for (MatchItem2 matchItem2 : matchItem2List) {
                double exposurePunishment = punishmentScoreMap.getOrDefault(matchItem2.getProductId(), 1.0);
                matchItem2.setScore(matchItem2.getScore() * exposurePunishment);
            }

            result = matchItem2List.stream().sorted(Comparator.comparing(MatchItem2::getScore).reversed()).collect(Collectors.toList());
        }catch (Exception e){
            log.error("[严重异常]处理类目页曝光惩罚规则时出现异常， uuid {} ", uuid, e);
            result = matchItem2List;
        }
        return result;
    }

    /**
     * 处理类目页新品置顶规则
     * @param matchItem2List
     * @param uuid
     * @param uid
     * @return
     */
    public List<MatchItem2> dealNewProductTopRule(List<MatchItem2> matchItem2List, String uuid, int uid){
        if(CollectionUtils.isEmpty(matchItem2List)){
            return matchItem2List;
        }
        try {
            String lastLoginTime = null;
            if (uid > 0) {
                lastLoginTime = redisUtil.getString(RedisKeyConstant.LAST_LOGIN_TIME + uid); //上次登录时间(天)
            }

            List<MatchItem2> fitNewProductList = new ArrayList<>();
            for (MatchItem2 matchItem2 : matchItem2List) {
                ProductInfo productInfo = productDetailCache.getProductInfo(matchItem2.getProductId());
                if (isFitNewProduct(lastLoginTime, productInfo)) {
                    fitNewProductList.add(matchItem2);
                }
            }

            if (fitNewProductList.size() == 0) {
                return matchItem2List;
            }

            //随机将2个商品置顶
            Collections.shuffle(fitNewProductList);
            List<MatchItem2> fitNewProductFinalList = new ArrayList<>();
            //商品算法最高分
            Double maxScore = matchItem2List.get(0).getScore();
            for(MatchItem2 matchItem2 : fitNewProductList){
                //将置顶新品的分值设置为目前的最高分加默认分
                matchItem2.setScore(maxScore + DEFAULT_PRODUCT_SCORE);
                fitNewProductFinalList.add(matchItem2);
                if(fitNewProductFinalList.size() >= NEW_PRODUCT_TOP_PID_NUM_UP_LIMIT){
                    break;
                }
            }
            matchItem2List.removeAll(fitNewProductFinalList);
            matchItem2List.addAll(0, fitNewProductFinalList);
        }catch (Exception e){
            log.error("[严重异常]处理新品置顶规则时出现异常，uuid {}, ", uuid, e);
        }
        return matchItem2List;
    }

    /**
     * 处理黄金尺码不足置底规则
     * @param matchItem2List
     * @param ucUser
     * @param uuid
     * @return
     */
    public List<MatchItem2> dealGoldSizeNotEnoughBottomRule(List<MatchItem2> matchItem2List, User ucUser, String uuid){
        if(CollectionUtils.isEmpty(matchItem2List)){
            return matchItem2List;
        }
        try {
            Map<Long, Double> goldSizeNotEnoughMap = productSizePunishment.getPunishment(uuid, matchItem2List, ucUser);
            List<MatchItem2> goldSizeNotEnoughList = new ArrayList<>();
            Iterator<MatchItem2> iterator = matchItem2List.iterator();
            while (iterator.hasNext()) {
                MatchItem2 matchItem2 = iterator.next();
                if (!goldSizeNotEnoughMap.containsKey(matchItem2.getProductId())) {
                    continue;
                }
                goldSizeNotEnoughList.add(matchItem2);
                iterator.remove();
            }

            if (goldSizeNotEnoughList.size() > 0) {
                matchItem2List.addAll(goldSizeNotEnoughList);
            }
        }catch (Exception e){
            log.error("[严重异常]处理黄金尺码不足置底规则时出现异常，uuid {}, ", uuid, e);
        }
        return matchItem2List;
    }

    /**
     * 如果该商品是新品，且商品的上新时间在用户上次登录时间之前，则返回true
     * 否则返回false
     * @param lastLoginTimeStr 用户上次登录时间 时间戳ms的字符串
     * @param productInfo 商品信息
     * @return
     */
    private boolean isFitNewProduct(String lastLoginTimeStr, ProductInfo productInfo) {

        if(productInfo == null || productInfo.getFirstOnshelfTime() == null){
            return false;
        }
        long firstOnshelfTime = productInfo.getFirstOnshelfTime().getTime();
        try{
            if(StringUtils.isNotBlank(lastLoginTimeStr)){
                Long lastLoginTime = Long.valueOf(lastLoginTimeStr);
                if(lastLoginTime > firstOnshelfTime){
                    return false;
                }
            }

            int hours = (int) ((System.currentTimeMillis() - firstOnshelfTime) / (1000 * 3600));
            return hours <= 72;
        }
        catch(Exception e){
            log.error("[严重异常]处理新品置顶规则中判断是否满足条件时出现异常，loginTime {}, onshelfTime {}, ", lastLoginTimeStr, firstOnshelfTime, e);
        }
        return false;
    }

    /**
     * 将matchItem2转化成rankItem2
     * @param matchItem2List
     * @return
     */
    public List<RankItem2> convertToRankItem2List(List<MatchItem2> matchItem2List){
        List<RankItem2> result = new ArrayList<>();
        if(CollectionUtils.isEmpty(matchItem2List)){
            return result;
        }
        for(MatchItem2 matchItem2 : matchItem2List){
            if(matchItem2 == null || matchItem2.getProductId() == null){
                continue;
            }
            RankItem2 rankItem2 = new RankItem2();
            rankItem2.setProductId(matchItem2.getProductId());
            rankItem2.setScore(matchItem2.getScore());
            result.add(rankItem2);
        }
        return result;
    }

}
