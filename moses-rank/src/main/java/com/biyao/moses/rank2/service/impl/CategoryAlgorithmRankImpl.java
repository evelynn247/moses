package com.biyao.moses.rank2.service.impl;

import com.alibaba.fastjson.JSON;
import com.biyao.moses.cache.ProductDetailCacheNoCron;
import com.biyao.moses.cache.ProductMustSizeCache;
import com.biyao.moses.common.constant.ExpFlagsConstants;
import com.biyao.moses.common.constant.RankNameConstants;
import com.biyao.moses.common.constant.RedisKeyConstant;
import com.biyao.moses.common.constant.UPCUserTypeConstants;
import com.biyao.moses.model.match2.MatchItem2;
import com.biyao.moses.model.rank2.RankItem2;
import com.biyao.moses.params.ProductInfo;
import com.biyao.moses.params.rank2.RankRequest2;
import com.biyao.moses.punishment.impl.ProductSizePunishmentImpl;
import com.biyao.moses.rank2.service.Rank2;
import com.biyao.moses.rpc.UcRpcService;
import com.biyao.moses.service.AlgorithmRedisAnsyService;
import com.biyao.moses.util.CacheRedisUtil;
import com.biyao.moses.util.RedisUtil;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import com.google.common.base.Splitter;
import com.uc.domain.bean.User;
import com.uc.domain.constant.UserFieldConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @ClassName CategoryAlgorithmRankImpl
 * @Description 算法类目页排序
 * @Author xiaojiankai
 * @Date 2019/12/14 11:35
 * @Version 1.0
 **/
@Slf4j
@Component(RankNameConstants.CATEGORY_ALGORITHM_RANK)
public class CategoryAlgorithmRankImpl implements Rank2 {

    @Resource
    RedisUtil redisUtil;

    @Autowired
    private CacheRedisUtil cacheRedisUtil;

    @Autowired
    AlgorithmRedisAnsyService algorithmRedisAnsyService;

    @Autowired
    ProductDetailCacheNoCron productDetailCacheNoCron;

    @Autowired
    ProductMustSizeCache productMustSizeCache; //黄金尺码是否充足缓存

    @Autowired
    private UcRpcService ucRpcService;

    @Autowired
    private ProductSizePunishmentImpl productSizePunishmentImpl;

    //前台类目ID,用于对特定前台类目做分流实验
    @Value("${exp.frontend.categoryid}")
    private String expFrontendCategoryId;

    private static final String DEFAULT_DATA_NUM = "0100";

    @BProfiler(key = "CategoryAlgorithmRankImpl.rank", monitorType = {
            MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR })
    @Override
    public List<RankItem2> rank(RankRequest2 rankRequest2) {
        List<RankItem2> rankItem2List = new ArrayList<>();

        List<MatchItem2> matchItem2List = rankRequest2.getMatchItemList();

        String uuid = rankRequest2.getUuid();
        int uid = rankRequest2.getUid() == null ? 0 : rankRequest2.getUid();
        try {
            if(CollectionUtils.isEmpty(matchItem2List)){
                return rankItem2List;
            }

            int size = matchItem2List.size();
            List<String> pidList = new ArrayList<>();
            for(int i = 0; i < size; i++){
                MatchItem2 matchItem2 = matchItem2List.get(i);
                if(matchItem2 == null || matchItem2.getProductId() == null){
                    continue;
                }
                RankItem2 rankItem2 = new RankItem2();
                rankItem2.setProductId(matchItem2.getProductId());
                rankItem2.setScore(0D);
                rankItem2List.add(rankItem2);
                pidList.add(matchItem2.getProductId().toString());
            }

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date());
            calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - 3);
            long dayUp3 = calendar.getTime().getTime();

            //获取参数
            String dataNum = rankRequest2.getDataNum();
            if(StringUtils.isBlank(dataNum) || ExpFlagsConstants.VALUE_DEFAULT.equals(dataNum)){
                dataNum = DEFAULT_DATA_NUM;
            }
            String[] dataNumArray = dataNum.trim().split(",");
            String dataNum1 = dataNumArray[0];
            String dataNum2 = dataNumArray[0];
            if(dataNumArray.length > 1){
                dataNum2 = dataNumArray[1];
            }
            int upcUserType = rankRequest2.getUpcUserType() == null ? UPCUserTypeConstants.NEW_VISITOR: rankRequest2.getUpcUserType();
            String frontendCategoryId = rankRequest2.getFrontendCategoryId();
            String cate3Ids = rankRequest2.getCategoryIds();
            String[] cate3IdArray;
            if(StringUtils.isNotBlank(cate3Ids)) {
                cate3IdArray = cate3Ids.trim().split(",");
            }else{
                cate3IdArray = new String[1];
            }


            //获取后置商品集合
            Set<String> postList = new HashSet<>(); //后置集合
            Set<String> exposureSet = cacheRedisUtil.smems("moses:user_category_" + uuid + "_" + frontendCategoryId);//曝光
            List<String> browseList = redisUtil.lrange("moses:user_viewed_products_" + uuid, 0, -1);//浏览

            //UC获取用户浏览、个性化尺码信息
            User ucUser = getUcUser(uuid, uid);

            //循环判断UC获取有效浏览集合中本类目页下的商品
            Set<String> deepBrowseSet = new HashSet<>();
            if (ucUser != null) {
                List<Long> threeDayViewPids = ucUser.getViewPids3d();
                if (CollectionUtils.isNotEmpty(threeDayViewPids)) {
                    for (Long l : threeDayViewPids) {
                        if (pidList.contains(l.toString())) {
                            deepBrowseSet.add(l.toString());
                        }
                    }
                }
            }

            if (exposureSet != null) {
                if (browseList.size() != 0) {
                    Set<String> browseList2 = new HashSet<>();

                    for (String s : browseList) {
                        String id = s.substring(0, s.indexOf(":"));
                        String date = s.substring(s.indexOf(":") + 1);
                        if (isWithin(dayUp3,date)) {
                            browseList2.add(id);
                        }
                        else{
                            break;
                        }
                    }
                    //将已曝光的深度浏览商品加入浏览集合中
                    if(deepBrowseSet.size()!=0){
                        Iterator<String> it = deepBrowseSet.iterator();
                        while(it.hasNext()){
                            String pid = it.next();
                            if(exposureSet.contains(pid)){
                                browseList2.add(pid);
                                it.remove();
                            }
                        }
                    }

                    exposureSet.removeAll(browseList2);
                }
                //获取后置商品集合
                postList = exposureSet;
            }

            //深度浏览商品中随机取得一商品为足迹商品，放入足迹置顶商品集合
            Set<String> topSet = new HashSet<>(); //足迹置顶商品集合
            if(deepBrowseSet.size() != 0){
                Iterator<String> it = deepBrowseSet.iterator();
                while(it.hasNext()){
                    String p = it.next();
                    //log.info("置顶足迹商品的pid："+p);
                    topSet.add(p);//放入一个商品，默认取第一个

                    //获取相似商品
                    String similarList = redisUtil.hget(RedisKeyConstant.MOSES_SIMILAR_PRODUCT,p);
                    if(StringUtils.isNotBlank(similarList)){
                        topSet.add(similarList.substring(0, similarList.indexOf(":")));//放入一个相似商品，取第一个分最高的
                    }
                    break;
                }
            }

            Map<String, String> productScore1 = new HashMap<>();
            Map<String, String> productScore2 = new HashMap<>();

            String[] pidArray = pidList.toArray(new String[pidList.size()]);
            /****************start*********************/
            //根据前台类目做分流实验
            if(StringUtils.isNotBlank(frontendCategoryId) && StringUtils.isNotEmpty(expFrontendCategoryId) &&
                    Splitter.on(",").trimResults().splitToList(expFrontendCategoryId).contains(frontendCategoryId)) {
                //新品的静态分直接从0200兜底数据获取
                dataNum2 = "0200";
                cate3IdArray = new String[1];
                cate3IdArray[0] = "new_product_category";
            } else {
                if(DEFAULT_DATA_NUM.equals(dataNum1)){
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
                }else{
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
            if((productScore1 == null || productScore1.size() == 0)
                    && (productScore3 == null || productScore3.size() == 0)
                    && (productScore2 == null || productScore2.size() == 0)) {

                log.error("[严重异常]uuid={},frontendCategoryId={},categoryIds={},dataNum={},获取静态分结果为空",uuid, frontendCategoryId, rankRequest2.getCategoryIds(),dataNum);
//                return rankItem2List;
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

            //新品保护策略商品(72小时内上新商品)
            Map<RankItem2,Date> NewProtectMap = new HashMap<>();

            //设置商品分&获取新手保护策略商品
            for (RankItem2 rankItem2 : rankItem2List) {
                String pid = rankItem2.getProductId().toString();

                String calculateScore = "0.0";
                if (productScore1.containsKey(pid)) {
                    calculateScore = productScore1.getOrDefault(pid,"0.0");
                }else if(productScore2.containsKey(pid)){
                    calculateScore = productScore2.getOrDefault(pid,"0.0");
                }else if(productScore3.containsKey(pid)){
                    calculateScore = productScore3.getOrDefault(pid,"0.0");
                }

                try{
                    rankItem2.setScore(Double.valueOf(calculateScore));
                    ProductInfo productInfo =  productDetailCacheNoCron.getProductInfo(rankItem2.getProductId());
                    if(productInfo.getFirstOnshelfTime() != null){
                        if(isWithin(dayUp3,String.valueOf(productInfo.getFirstOnshelfTime().getTime()))){
                            NewProtectMap.put(rankItem2,productInfo.getFirstOnshelfTime());
                        }
                    }
                }
                catch(Exception e){
                    log.error("[严重异常]商品分转换失败，失败score："+calculateScore);
                    rankItem2.setScore(0.0); //设置为默认值
                }
            }

            //排序
            Collections.sort(rankItem2List, new Comparator<RankItem2>() {
                @Override
                public int compare(RankItem2 o1, RankItem2 o2) {
                    if (o1.getScore() > o2.getScore()) {
                        return -1;
                    } else if (o1.getScore() < o2.getScore()) {
                        return 1;
                    } else {
                        return 0;
                    }
                }
            });


            //老客新品置顶
            if(rankRequest2.getUpcUserType() == 1 && uid > 0){
                List<RankItem2> setTopList = new ArrayList<>();
                String lastUploadTime = redisUtil.getString(RedisKeyConstant.LAST_LOGIN_TIME +rankRequest2.getUid()); //上次登录时间(天)

                //筛选置顶新品
                if(StringUtils.isNotBlank(lastUploadTime)){
                    //log.info("用户uid："+rankRequest.getUid()+",上次登录时间："+lastUploadTime);
                    for (Map.Entry<RankItem2, Date> entry : NewProtectMap.entrySet()) {
                        if(entry.getValue().getTime()>Long.parseLong(lastUploadTime)){
                            setTopList.add(entry.getKey());
                        }
                    }
                }else{
                    for (Map.Entry<RankItem2, Date> entry : NewProtectMap.entrySet()) {
                        setTopList.add(entry.getKey());
                    }
                }

                //置顶
                rankItem2List.removeAll(setTopList);
                List<RankItem2> tempList = new ArrayList<>();
                tempList.addAll(setTopList);
                tempList.addAll(rankItem2List);
                rankItem2List.clear();
                rankItem2List.addAll(tempList);
            }


            //足迹商品置顶
            if(topSet.size() != 0){
                List<RankItem2> topProductList = new ArrayList<>();
                for (RankItem2 rankItem2:rankItem2List) {
                    if(topSet.contains(rankItem2.getProductId().toString())){
                        topProductList.add(rankItem2);
                    }
                }
                rankItem2List.removeAll(topProductList);
                List<RankItem2> tempList = new ArrayList<>();
                tempList.addAll(topProductList);
                tempList.addAll(rankItem2List);
                rankItem2List.clear();
                rankItem2List.addAll(tempList);

            }

            //曝光未点击后置
            List<RankItem2> setBottomList = new ArrayList<>();
            if (postList.size() != 0) {
                for (RankItem2 rankItem2 : rankItem2List) {
                    if (postList.contains(rankItem2.getProductId().toString())) {
                        setBottomList.add(rankItem2);
                    }
                }

            }
            rankItem2List.removeAll(setBottomList);
            rankItem2List.addAll(setBottomList);

            Map<Long, Double> mustSizeNotFullMap = productSizePunishmentImpl.getPunishment(uuid, matchItem2List, ucUser);
            //置底处理黄金尺码不足商品(新品受保护)
            dealWithMustSizeProduct(rankItem2List, NewProtectMap, mustSizeNotFullMap);
        } catch (Exception e) {
            log.error("[严重异常]CategoryAlgorithmRankImpl#rank未知错误, uuid {}", uuid, e);
        }
        return rankItem2List;
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
     * 置底处理黄金尺码不足商品(新品受保护)
     * @param oriData
     * @param newProtectMap
     */
    private void dealWithMustSizeProduct(List<RankItem2> oriData, Map<RankItem2, Date> newProtectMap, Map<Long, Double> mustSizeNotFullMap) {
        if(mustSizeNotFullMap == null || mustSizeNotFullMap.size() == 0){
            return;
        }
        List<RankItem2> mustSizeNotFullList = new ArrayList<>(); //黄金尺码不足集合
        if(CollectionUtils.isNotEmpty(oriData)){
            Iterator<RankItem2> it = oriData.iterator();
            while (it.hasNext()){
                RankItem2 tti = it.next();
                if(tti == null || tti.getProductId() == null){
                    continue;
                }

                //新品 受保护
                if(newProtectMap.get(tti)!=null){
                    continue;
                }
                //获取商品黄金尺码是否充足
                boolean isNotFull = mustSizeNotFullMap.containsKey(tti.getProductId());
                if(isNotFull){
                    mustSizeNotFullList.add(tti);
                    it.remove();
                }
            }
            oriData.addAll(mustSizeNotFullList);
        }
    }

    /**
     * 从uc获取用户个性化尺码、用户浏览商品等用户画像信息
     */
    private User getUcUser(String uuid, int uid){
        List<String> fieldsList = new ArrayList<>();
        fieldsList.add(UserFieldConstants.PERSONALSIZE);
        fieldsList.add(UserFieldConstants.VIEWPIDS3D);
        String uidStr = null;
        if(uid > 0){
            uidStr = String.valueOf(uid);
        }
        User ucUser = ucRpcService.getData(uuid, uidStr, fieldsList, "mosesrank");
        return ucUser;
    }


    //判断time是否在dayUp 之后
    private boolean isWithin(long dayUp, String time) {

        boolean result = false;
        try{
            Long stamp = new Long(time);
            if (stamp > dayUp) {
                result = true;
            }
        }
        catch(Exception e){
            log.error("[严重异常]浏览商品时间戳转换异常，timeStamp="+time);
        }
        return result;
    }
}
