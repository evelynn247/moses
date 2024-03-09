package com.biyao.moses.match2.util;

import com.alibaba.fastjson.JSON;
import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.common.constant.CacheRedisKeyConstant;
import com.biyao.moses.common.constant.CommonConstants;
import com.biyao.moses.common.enums.MatchSourceEnum;
import com.biyao.moses.common.enums.SeasonEnum;
import com.biyao.moses.match2.param.MatchParam;
import com.biyao.moses.match2.service.Match2;
import com.biyao.moses.model.match2.MatchItem2;
import com.biyao.moses.params.ProductInfo;
import com.biyao.moses.util.ApplicationContextProvider;
import com.biyao.moses.util.CacheRedisUtil;
import com.biyao.moses.util.FilterUtil;
import com.uc.domain.constant.UserFieldConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * @ClassName MatchUtil
 * @Description match通用方法
 * @Author admin
 * @Date 2019/10/10 20:23
 * @Version 1.0
 **/
@Slf4j
@Component
public class MatchUtil {

    @Autowired
    ProductDetailCache productDetailCache;
    @Autowired
    FilterUtil filterUtil;

    //最多类目限制
    private final int MAX_CATEGROY_NUM = 30;
    //单个类目最多商品数
    private final int CATEGROY_PRODUCTS_NUM = 10;

    /**
     * 在原有数据源的基础上集合新的数据源数据，如果商品id相同则：最终的分值为分值+新召回源分值*权重。召回源为：现有召回源+“_”+新召回源
     *
     * @param matchItemList
     * @param weight
     * @param matchItemMap
     */
    public  void aggrMatchItem(List<MatchItem2> matchItemList, Double weight, Map<String, MatchItem2> matchItemMap,Integer siteId) {
        if (matchItemMap == null || CollectionUtils.isEmpty(matchItemList)) {
            return;
        }
        String siteIdStr = siteId == null ? null :String.valueOf(siteId);
        for (MatchItem2 matchItem : matchItemList) {
            if (matchItem == null) {
                continue;
            }
            //增加权重后的分值为：分值*权重
            matchItem.setScore(matchItem.getScore() * weight);
            // 过滤不支持用户所持有端的普通商品 只对普通商品过滤 productId为空说明不是普通商品
            if(matchItem.getProductId() !=null && filterUtil.isFilteredBySiteId(matchItem.getProductId(),siteIdStr)){
                continue;
            }
            String id = matchItem.getProductId() != null ? matchItem.getProductId().toString() : matchItem.getId();

            if (matchItemMap.containsKey(id)) {
                MatchItem2 matchItem1 = matchItemMap.get(id);
                matchItem1.setScore(matchItem1.getScore() + matchItem.getScore());
                matchItem1.setSource(matchItem1.getSource() + "_" + matchItem.getSource());
                if(StringUtils.isNotBlank(matchItem.getLabelContent())){
                    matchItem1.setLabelContent(matchItem.getLabelContent());
                }
            } else {
                matchItemMap.put(id, matchItem);
            }
        }
    }

    /**
     * 根据基础召回源bean名称同步获取召回源信息
     * @param matchBeanName
     * @return
     */
    public static List<MatchItem2> executeMatch2(MatchParam matchParam, String matchBeanName){
        List<MatchItem2> result = new ArrayList<>();
        try {
            Match2 match = ApplicationContextProvider.getBean(matchBeanName, Match2.class);
            result = match.match(matchParam);
        }catch (Exception e){
            log.error("[严重异常]通过bean名称获取召回源{}数据异常",matchBeanName, e);
        }
        return result;
    }

    /**
     * 使用指定的商品数据补齐
     * @param matchItem2List
     * @param maxProductNum
     * @param matchMap
     * @param isFilterRepeatPid true:需要过滤重复的pid，false:不需要过滤重复的pid
     */
    public  List<MatchItem2> getFillProduct(List<MatchItem2> matchItem2List, int maxProductNum, Map<String, MatchItem2> matchMap, boolean isFilterRepeatPid, Integer siteId){

        if(CollectionUtils.isEmpty(matchItem2List)){
            return  matchItem2List;
        }
        List<MatchItem2> result = new ArrayList<>();
        String siteIdStr = siteId == null ? null :String.valueOf(siteId);
        try {
            int productNum = matchMap.size();
            if(productNum >= maxProductNum){
                return result;
            }
            int fillProductNum =  maxProductNum - productNum;

            for (MatchItem2 matchItem2 : matchItem2List) {
                if (fillProductNum <= 0) {
                    break;
                }
                // 过滤不支持用户所持有端的普通商品   productId为空说明不是普通商品
                if(matchItem2.getProductId() != null && filterUtil.isFilteredBySiteId(matchItem2.getProductId(),siteIdStr)){
                    continue;
                }
                String id = matchItem2.getProductId() != null ? matchItem2.getProductId().toString() : matchItem2.getId();
                //如果该商品已经召回，则直接继续获取下一个
                if (matchMap.containsKey(id)) {
                    if(!isFilterRepeatPid){
                        result.add(matchItem2);
                    }
                    continue;
                }
                result.add(matchItem2);
                fillProductNum--;
            }
        }catch(Exception e){
            log.error("[严重异常]热销召回源补齐发生错误", e);
        }
        return result;
    }

    /**
     * 根据用户类目兴趣选出商品
     *
     * @param productList
     * @param level3HobbyMap
     * @return
     */
    public List<MatchItem2> getHobbyCategory3Products(List<Long> productList, Map<String, BigDecimal> level3HobbyMap, String source, int targetNum) {

        List<MatchItem2> resultList = new ArrayList<>();

        if (level3HobbyMap == null || level3HobbyMap.size() <= 0 || CollectionUtils.isEmpty(productList)) {
            return resultList;
        }
        List<Long> mergeList = new ArrayList<>();

        //组装商品候选集group by 后台三级类目
        //key:后台三级类目 val:商品id集合
        Map<Long, List<Long>> categroyProductsMap = new HashMap<>();

        for (Long pid : productList) {
            ProductInfo productInfo = productDetailCache.getProductInfo(pid);
            if (productInfo == null || productInfo.getThirdCategoryId() == null) {
                continue;
            }
            List<Long> pids = categroyProductsMap.get(productInfo.getThirdCategoryId());
            if (CollectionUtils.isEmpty(pids)) {
                List<Long> pidsList = new ArrayList<>();
                pidsList.add(pid);
                categroyProductsMap.put(productInfo.getThirdCategoryId(), pidsList);
                continue;
            }  
            pids.add(pid);
             
        }
        //key:后台三级类目 val:商品id集合
        LinkedHashMap<Long, List<Long>> categroyScoreDescMap = new LinkedHashMap<>();
        //uc获取的类目兴趣分倒排的map
        Iterator<Map.Entry<String, BigDecimal>> level3HobbyIterator = level3HobbyMap.entrySet().iterator();
        while (level3HobbyIterator.hasNext()) {
            Map.Entry<String, BigDecimal> next = level3HobbyIterator.next();
            if (next.getKey() == null) {
                continue;
            }
            long key = Long.parseLong(next.getKey());
            List<Long> pids = categroyProductsMap.get(key);
            if (CollectionUtils.isEmpty(pids)) {
                continue;
            }
            if (pids.size() > CATEGROY_PRODUCTS_NUM) {
            	pids = pids.subList(0, CATEGROY_PRODUCTS_NUM-1);
            }
            Collections.shuffle(pids);
            categroyScoreDescMap.put(key, pids);
            if (categroyScoreDescMap.size() >= MAX_CATEGROY_NUM) {
                break;
            }
        }

        //合并结果
        Iterator<Map.Entry<Long, List<Long>>> resultIterator = categroyScoreDescMap.entrySet().iterator();
        while (resultIterator.hasNext()) {
            Map.Entry<Long, List<Long>> next = resultIterator.next();
            List<Long> valueList = next.getValue();
            mergeList.addAll(valueList);
            if (mergeList.size() >= targetNum) {
                break;
            }
        }

        for (Long pid : mergeList) {
            MatchItem2 item = new MatchItem2();
            ProductInfo productInfo = productDetailCache.getProductInfo(pid);
            if (productInfo == null || productInfo.getThirdCategoryId() == null) {
                continue;
            }
            item.setProductId(productInfo.getProductId());
            item.setSource(source);
            BigDecimal score = level3HobbyMap.get(productInfo.getThirdCategoryId().toString());
            item.setScore(score.doubleValue());
            resultList.add(item);
            if (resultList.size() >= targetNum) {
                break;
            }
        }

        return resultList;
    }

    /**
     * 将季节（春、夏、秋、冬）转化为对应的数值
     * 如果季节为春或秋时，则对应的季节ID为春ID+秋ID
     * @param season
     * @return
     */
    public static int convertSeason2int(String season){
        int seasonValue = 0;
        if(StringUtils.isBlank(season)){
            return seasonValue;
        }
        //转化成season对应的id，如果季节为春或秋时，对应的季节ID为春ID+秋ID
        if (SeasonEnum.SPRING.getName().equals(season)) {
            seasonValue = SeasonEnum.SPRING.getId() + SeasonEnum.AUTUMN.getId();
        } else if (SeasonEnum.SUMMER.getName().equals(season)) {
            seasonValue = SeasonEnum.SUMMER.getId();
        } else if (SeasonEnum.AUTUMN.getName().equals(season)) {
            seasonValue = SeasonEnum.AUTUMN.getId() + SeasonEnum.SPRING.getId();
        } else if (SeasonEnum.WINTER.getName().equals(season)) {
            seasonValue = SeasonEnum.WINTER.getId();
        }
        return seasonValue;
    }

    /**
     * 已知性别与商品性别相反，则返回true，认为需要被过滤
     * @param productInfo
     * @param sex
     * @return true 表示不满足已知性别，false 表示满足已知性别
     */
    public static boolean isFilterBySex(ProductInfo productInfo, Integer sex){
        Byte productSex = productInfo.getProductGender();
        if(productSex == null || sex == null){
            return false;
        }
        boolean result = false;
        String productSexStr = productSex.toString();
        String userSexStr = sex.toString();
        if(CommonConstants.FEMALE_SEX.equals(productSexStr) && CommonConstants.MALE_SEX.equals(userSexStr)){
            result = true;
        }else if(CommonConstants.MALE_SEX.equals(productSexStr) && CommonConstants.FEMALE_SEX.equals(userSexStr)){
            result = true;
        }

        return result;
    }

    /**
     * 过滤掉非用户季节商品，过滤规则如下：
     * 如果用户季节为空 则不过滤
     * 如果用户季节不为空，但商品季节为空，则过滤
     * 如果用户季节春（1），商品适用季节包含春或秋或四季，则不过滤
     * 如果用户季节秋（4），商品适用季节包含春或秋或四季，则不过滤
     * 如果用户季节夏（2），商品适用季节包含夏或四季，则不过滤
     * 如果用户季节冬（8），商品适用季节包含冬或四季，则不过滤
     * @param productSeasonValue 商品季节对应的值
     * @param userSeasonValue 用户季节对应的值
     * @return ture 表示不满足用户季节，false 表示满足用户季节
     */
    public static boolean isFilterByUserSeason(int productSeasonValue, int userSeasonValue){
        //用户季节为空，则不需要过滤
        if (userSeasonValue == 0) {
            return false;
        }
        //如果用户季节不为空，但商品季节为空，则需要过滤
        if(productSeasonValue == 0)
        {
            return true;
        }
        //用户季节和商品季节无交集，则需要过滤
        if ((productSeasonValue & userSeasonValue) == 0) {
            return true;
        }
        return false;
    }

    /**
     * 季节过滤  两个季节入参 季节相反是过滤  相反季节：夏天 <--->冬天
     * @param productSeasonValue1
     * @param productSeasonValue2
     * @return
     */
    public static boolean isFilterBySeason(int productSeasonValue1, int productSeasonValue2){

       if( SeasonEnum.SUMMER.getId() == productSeasonValue1 && SeasonEnum.WINTER.getId()==productSeasonValue2 ){
           return  true;
       }
       if( SeasonEnum.SUMMER.getId() == productSeasonValue2 && SeasonEnum.WINTER.getId()==productSeasonValue1 ){
            return  true;
        }
       return  false;
    }
    /**
     * 从用户曝光信息中获取指定召回源和指定小时内的曝光商品ID
     * @param uuid 用户uuid
     * @param source  召回源名称
     * @param cacheRedisUtil cache Redis工具类
     * @param timeSet 指定的时间集合，单位ms
     * @return
     */
    public static Map<Long, List<Long>> getSourceExposurePid(String uuid, String source, CacheRedisUtil cacheRedisUtil, Set<Long> timeSet){
        Map<Long, List<Long>> result = new HashMap<>();
        if(StringUtils.isBlank(uuid) || StringUtils.isBlank(source)
            || cacheRedisUtil == null || CollectionUtils.isEmpty(timeSet)){
            return result;
        }
        //获取曝光商品 格式pid:time:source
        List<String> expPidInfoList = cacheRedisUtil.lrange(CacheRedisKeyConstant.NEW_HOME_FEED_EXPOSURE + uuid, 0, -1);
        if(CollectionUtils.isEmpty(expPidInfoList)){
            return result;
        }

        //将指定时间内的数据设置为空集合
        for(Long time : timeSet){
            result.put(time, new ArrayList<>());
        }

        long currentTime = System.currentTimeMillis();
        for(String expPidInfoStr : expPidInfoList){
            try {
                if(StringUtils.isBlank(expPidInfoStr)){
                    continue;
                }
                String[] pidInfoArray = expPidInfoStr.split(":");
                if(pidInfoArray.length < 3){
                    continue;
                }
                long expPid = Long.valueOf(pidInfoArray[0]);
                long expTime = Long.valueOf(pidInfoArray[1]);
                String expSource = pidInfoArray[2];
                if(StringUtils.isBlank(expSource)){
                    continue;
                }
                Set<String> sourceSet = new HashSet<>(Arrays.asList(expSource.split("_")));
                if(!sourceSet.contains(source)){
                    continue;
                }
                //遍历所有期望时间，填充期望时间内的曝光商品
                long diffTime = currentTime - expTime;
                for(Long time : timeSet){
                    if(diffTime <= time){
                        List<Long> expPidList = result.get(time);
                        expPidList.add(expPid);
                    }
                }
            }catch (Exception e){
                log.error("[严重异常]解析用户曝光信息发生异常 uuid {}， 召回源 {} ， 期望时间 {}， expInfo {}， e ", uuid, source, JSON.toJSONString(timeSet), expPidInfoStr, e);
            }
        }
        return result;
    }

    /**
     * 根据召回源信息获取需要从UC中查询的字段
     * @param sourceSet
     * @return
     */
    public Set<String> getUcFieldBySource(Set<String> sourceSet){
        Set<String> ucFieldSet = new HashSet<>();
        if(CollectionUtils.isEmpty(sourceSet)){
            return ucFieldSet;
        }
        for(String source : sourceSet){
            if(StringUtils.isBlank(source)){
                continue;
            }
            if(MatchSourceEnum.TAG.getSourceName().equals(source)){
                ucFieldSet.add(UserFieldConstants.SEASON);
            }else if(MatchSourceEnum.WEATHER.getSourceName().equals(source)){
                ucFieldSet.add(UserFieldConstants.SEASON);
                //添加天气field
                ucFieldSet.add(UserFieldConstants.WEATHERSEVENDAY);
            }else if(MatchSourceEnum.BASE.getSourceName().equals(source)){
                ucFieldSet.add(UserFieldConstants.SEASON);
                //添加用户感兴趣类目
                ucFieldSet.add(UserFieldConstants.LEVEL3HOBBY);
            }
        }
        return ucFieldSet;
    }

    /**
     * 根据排名计算商品分（商品期望总数-排名）/(商品期望总数)，并聚合（相同商品，分数相加，召回源信息聚合）
     * @param matchItem2List
     * @param totalNum
     */
    public void calculateAndFillScore(List<MatchItem2> matchItem2List, int totalNum, Map<String, Double> allSourceAndWeight){
        Map<Long, MatchItem2> matchItem2Map = new HashMap<>();
        if(CollectionUtils.isEmpty(matchItem2List)){
            return;
        }

        int sortedIndex = 0;
        Iterator<MatchItem2> iterator = matchItem2List.iterator();
        while(iterator.hasNext()){
            MatchItem2 matchItem2 = iterator.next();
            if(matchItem2 == null || matchItem2.getProductId() == null){
                iterator.remove();
                continue;
            }
            Long productId = matchItem2.getProductId();
            double weight = allSourceAndWeight.getOrDefault(matchItem2.getSource(), 1.0);
            double score = (totalNum - sortedIndex) / (double)totalNum * weight;
            sortedIndex = sortedIndex + 1;
            if(matchItem2Map.containsKey(productId)){
                MatchItem2 matchItem2Tmp = matchItem2Map.get(productId);
                matchItem2Tmp.setScore(score + matchItem2Tmp.getScore());
                matchItem2Tmp.setSource(matchItem2Tmp.getSource() + "_" + matchItem2.getSource());
                iterator.remove();
                continue;
            }
            matchItem2.setScore(score);
            matchItem2Map.put(productId, matchItem2);
        }
    }

}
