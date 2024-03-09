package com.biyao.moses.util;

import com.alibaba.fastjson.JSON;
import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.cache.ProductFeatureCache;
import com.biyao.moses.common.constant.CacheRedisKeyConstant;
import com.biyao.moses.common.constant.CommonConstants;
import com.biyao.moses.model.feature.UserFeature;
import com.biyao.moses.params.ProductInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 商品曝光方法
 *
 * @Description
 * @Date 2019年6月5日
 */
@Component
@Slf4j
public class ProductExposureUtil {

    //
    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private CacheRedisUtil cacheRedisUtil;

    @Autowired
    private ProductDetailCache productDetailCache;

    @Autowired
    private ProductFeatureCache productFeatureCache;

    /**
     * 添加用户已曝光商品接口
     *
     * @param uuid
     * @param pids
     */
    public void setViewedSlideProducts(String uuid, List<String> pids) {

        String redisKey = CacheRedisKeyConstant.MOSES_EXPOSURE_TO_UUID_PREFIX + uuid;
        String viewedString = cacheRedisUtil.getString(redisKey);
        String stamp =String.valueOf(System.currentTimeMillis());
        List<String> results = new ArrayList<>();
        for (String pid : pids) {
            results.add(pid +":"+stamp);
        }
        if (StringUtils.isNotBlank(viewedString)) {
            String[] viewedStringArr = viewedString.split(",");
            for (String pidTimestampStr : viewedStringArr){
                String[] pidTimestampArr = pidTimestampStr.split(":");
                // pid:timestamp格式 直接add
                if (pidTimestampArr.length == 2){
                    results.add(pidTimestampStr);
                }else if (pidTimestampArr.length == 1){ // pid,pid...格式
                    results.add(pidTimestampStr +":"+stamp);
                }
                if (results.size() >= 100){
                    break;
                }
            }
        }
        cacheRedisUtil.setString(redisKey, StringUtils.join(results, ','), 60 * 60 * 24 * 7);
    }

    /**
     * 获取对用户已曝光的商品（去时间戳）
     * @param uuid
     * @param
     * @return
     */
    public Set<String> getViewedSlidePids(String uuid) {
        Set<String>  result = new HashSet<String>();
        String viewedString = cacheRedisUtil.getString(CacheRedisKeyConstant.MOSES_EXPOSURE_TO_UUID_PREFIX + uuid);
        if( StringUtils.isNotEmpty(viewedString)){
            for(String pidStamp : viewedString.split(",") ) {
                if(pidStamp.contains(":")){
                    result.add(pidStamp.substring(0, pidStamp.indexOf(":")));
                }else{
                    result.add(pidStamp);
                }
            }
        }
        return result;
    }

    /**
     * 获取对用户已曝光的商品（带时间戳）
     * @param uuid
     * @param
     * @return
     */
    public Set<String> getViewedSlidePidsWithStamp(String uuid) {
        Set<String>  result = new HashSet<String>();
        String viewedString = cacheRedisUtil.getString(CacheRedisKeyConstant.MOSES_EXPOSURE_TO_UUID_PREFIX + uuid);
        if( StringUtils.isNotEmpty(viewedString)){
            for(String pid : viewedString.split(",") ) {
                result.add(pid);
            }
        }
        return result;
    }

    /**
     * 根据三级类目兴趣排序
     *
     * @return
     */
    public List<String> sortByUserPreferCate3(List<String> userPreferCate3Ids, List<String> sourcePidList, String sex) {
        //根据三级类目偏好 收集一个高优先级集合
        List<String> majorList = new ArrayList<>();
        //低优先级集合 存放剔除偏好后的高转化
        List<String> minorList = new ArrayList<>();
        Iterator<String> iterator = sourcePidList.iterator();
        while (iterator.hasNext()) {
            String next = iterator.next();
            ProductInfo productInfo = productDetailCache.getProductInfo(Long.parseLong(next));
            if (productInfo == null) {
                continue;
            }
            if (productInfo.getShelfStatus() == null || !productInfo.getShelfStatus().toString().equals("1")) {
                continue;
            }
            Long secondCategoryId = productInfo.getSecondCategoryId();
            Long thirdCategoryId = productInfo.getThirdCategoryId();
            if (userPreferCate3Ids.contains(thirdCategoryId) || userPreferCate3Ids.contains(secondCategoryId)) {
                majorList.add(next);
            } else {
                String productGender = productInfo.getProductGender() == null ? CommonConstants.UNKNOWN_SEX : productInfo.getProductGender().toString();
                //非兴趣相关的集合中加入同性别、中性商品 否则丢弃掉
                if (StringUtils.isBlank(sex) || CommonConstants.UNKNOWN_SEX.equals(sex) ||
                        (CommonConstants.MALE_SEX.equals(sex) && (CommonConstants.MALE_SEX.equals(productGender) || CommonConstants.COMMON_SEX.equals(productGender))) ||
                        (CommonConstants.FEMALE_SEX.equals(sex) && (CommonConstants.FEMALE_SEX.equals(productGender) || CommonConstants.COMMON_SEX.equals(productGender)))) {
                    minorList.add(next);
                }
            }
        }
        majorList.addAll(minorList);
        return majorList;
    }

    /**
     * 根据商品静态分以及用户兴趣向量来排序
     */
    public List<String> sortByVector(List<String> sourcePidList, String uuid) {
        List<Map.Entry<String, Double>> list = roughSort(sourcePidList, uuid);
        return sortByHistory(list, uuid,40);
    }

    private List<Map.Entry<String, Double>> roughSort(List<String> sourcePidList, String uuid){
        Map<String,Double> sortMap = new HashMap<>();

        //根据uuid获取用户长期兴趣向量和短期兴趣向量
        String longInterest = redisUtil.getString("moses:u_ll_"+uuid);
        String shortInterest = redisUtil.getString("moses:u_sl_"+uuid);

//        if(longInterest!= null){
//            log.info("uuid:"+uuid+",长期兴趣向量："+longInterest);
//        }
//        if(shortInterest!= null){
//            log.info("uuid:"+uuid+",短期兴趣向量："+shortInterest);
//        }
        String sScore;
        String pVector;
        //循环获取计算商品分
        Double staticScore;
        Double long_weight = 0.2;
        Double short_weight = 0.8;
        for (String pid:sourcePidList) {
            try{
                sScore = productFeatureCache.getProductStaticScore(pid);
                pVector = productFeatureCache.getProductVector(pid);
                staticScore = 0.01;
                if (StringUtils.isNotBlank(sScore)){
                    staticScore = Double.valueOf(sScore);
                }
                double longInterestScore = 0.0, shortInterestScore = 0.0;
                if (StringUtils.isNotBlank(pVector)){
                    if (StringUtils.isNotBlank(longInterest)){
                        longInterestScore = getConceptScore(pVector.split(","), longInterest.split(","))*long_weight;
                    }
                    if (StringUtils.isNotBlank(shortInterest)){
                        shortInterestScore = getConceptScore(pVector.split(","), shortInterest.split(","))*short_weight;
                    }
                }
                // 如果有动态分大于0，则使用动态分*静态分，否则只使用静态分
                if (longInterestScore + shortInterestScore > 0.0){
                    sortMap.put(pid, (longInterestScore + shortInterestScore)*staticScore);
                }else{
                    sortMap.put(pid, staticScore);
                }

            }catch(Exception e){
                log.error("计算商品质量分异常，异常原因为"+e.getMessage());
            }

        }
        //根据商品动态分对商品进行排序
        List<Map.Entry<String, Double>> list = new ArrayList<>(sortMap.entrySet()); //转换为list
        list.sort((Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) -> o2.getValue().compareTo(o1.getValue()));
//        Collections.sort(list, new Comparator<Map.Entry<String, Double>>() {
//            @Override
//            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
//                return o2.getValue().compareTo(o1.getValue());
//            }
//        });
//        log.info("首页轮播图-轮播图粗排分数: uuid={}, pidScore={}", uuid, JSON.toJSONString((list != null && list.size() > 40) ? list.subList(0, 39) : list));
        return list;
    }

    /**
     * 根据浏览历史加权精排
     * ( 1 - e**(-0.1*viewedHour) ) * (e**(-0.35*viewedCount))
     */
    private List<String> sortByHistory(List<Map.Entry<String, Double>> sourcePidList, String uuid, int target) {
        List<String> majorList = new ArrayList<>();
        Map<String,Double> result = new HashMap<>();
        Set<String> pidsWithStamp = getViewedSlidePidsWithStamp(uuid);

        // 已曝光的轮播图商品 Map
        Map<String, UserExposureProduct> userExposureProductMap = new HashMap<>();
        for (String pidTimeStampStr : pidsWithStamp){
            String[] pidTimeStampArr = pidTimeStampStr.split(":");
            if (pidTimeStampArr.length != 2){
                continue;
            }
            try {
                String pid = pidTimeStampArr[0];
                long timeStamp = Long.valueOf(pidTimeStampArr[1]);
                int exposureNum = 1;
                if (userExposureProductMap.containsKey(pid)) {
                    UserExposureProduct userExposureProduct = userExposureProductMap.get(pid);
                    exposureNum = userExposureProduct.exposureNum + 1;
                    if (timeStamp < userExposureProduct.exposureTimestamp){
                        timeStamp = userExposureProduct.exposureTimestamp;
                    }
                }
                UserExposureProduct userExposureProduct = new UserExposureProduct();
                userExposureProduct.exposureTimestamp = timeStamp;
                userExposureProduct.exposureNum = exposureNum;
                userExposureProductMap.put(pid, userExposureProduct);
            }catch (Exception e){
                log.error("首页轮播图-用户曝光信息解析失败：uu={}, pidTimeStampStr={}", uuid, pidTimeStampStr);
            }
        }

        Double historyScore;
        if(sourcePidList.size()<target){
            target = sourcePidList.size();
        }
        long nowStamp = System.currentTimeMillis();
        for (int i = 0;i <target && i < sourcePidList.size(); i++){
            // 默认值
            long viewedCount = 0;
            double viewedHour = 48;

            // 计算轮播图商品曝光次数及最近一次曝光距离现在的小时。
            String productId = sourcePidList.get(i).getKey();
            if (userExposureProductMap.containsKey(productId)){
                viewedCount = userExposureProductMap.get(productId).exposureNum;
                viewedHour = (double) (nowStamp - userExposureProductMap.get(productId).exposureTimestamp)/3600000;
                if (viewedHour < 0){
                    viewedHour = 0;
                }
            }

            //f = ( 1 - e**(-0.1*viewedHour) ) * (e**(-0.35*viewedCount))
            historyScore = ( 1 - Math.pow(Math.E,(-0.1)*viewedHour) ) * (Math.pow(Math.E,(-0.35)*viewedCount));
            result.put(sourcePidList.get(i).getKey(),historyScore*sourcePidList.get(i).getValue());
        }

        //排序
        List<Map.Entry<String, Double>> list = new ArrayList<>(result.entrySet()); //转换为list
        list.sort((Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) -> o2.getValue().compareTo(o1.getValue()));
//        Collections.sort(list, new Comparator<Map.Entry<String, Double>>() {
//            @Override
//            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
//                return o2.getValue().compareTo(o1.getValue());
//            }
//        });
        log.info("首页轮播图-轮播图精排分数: uu={}, pidScore={}", uuid, JSON.toJSONString((list != null && list.size() > 40) ? list.subList(0, 39) : list));
        for (Map.Entry<String, Double> mapping : list){
            majorList.add(mapping.getKey());
        }

        return majorList;

    }

    /**
     * 获取推荐商品ID列表
     *
     * @param productScoreStr 数据格式 pid1:score1,pid2:score2...或者pid1,pid2...
     * @return
     */
    public List<String> splitIdAndScore(String productScoreStr) {
        if (org.apache.commons.lang3.StringUtils.isBlank(productScoreStr)) {
            return new ArrayList<>();
        }
        List<String> rcdProductList = Arrays.stream(productScoreStr.split(",")).map(productScore -> {
            return productScore.split(":")[0];
        }).collect(Collectors.toList());
        return rcdProductList;
    }

    //向量概念分计算
    private Double getConceptScore(String[] vector1,String[] vector2){
        if(vector1.length != vector2.length){
            return 0.00;
        }
        Double num1 = 0.00;
        Double num2 = 0.00;
        Double num3 = 0.00;

        for (int i=0 ;i <vector1.length; i++ ){
            num1 = num1+(Double.valueOf(vector1[i])*Double.valueOf(vector2[i]));
            num2 = num2+ Double.valueOf(vector1[i])*Double.valueOf(vector1[i]);
            num3 = num3+ Double.valueOf(vector2[i])*Double.valueOf(vector2[i]);
        }
        if(num2==0){
            return 0.00;
        }
        DecimalFormat df=new DecimalFormat("0.00");
        return Double.valueOf(df.format((double)num1/(Math.sqrt(num2)*Math.sqrt(num3))));

    }

    /**
     * 内部类，定义用户曝光商品
     */
    class UserExposureProduct{
        int exposureNum = 0;
        long exposureTimestamp = 0L;
    }

    /**
     * 先按用户兴趣、性别分组，再按兴趣分排序
     * @param candidateProductList
     * @param userFeature
     * @return
     */
    public List<String> sortByUserFeature(List<String> candidateProductList, UserFeature userFeature){
        String uuid = userFeature.getUuid();
        String sex = userFeature.getSex();
        List<String> userPreferCategoryList = userFeature.getPreferCategoryList();

        //根据三级类目偏好 收集一个高优先级集合
        List<String> majorList = new ArrayList<>();
        //低优先级集合 存放剔除偏好后的高转化
//        List<String> minorList = new ArrayList<>();
//        Iterator<String> iterator = candidateProductList.iterator();
        for (String productId : candidateProductList){
            ProductInfo productInfo = productDetailCache.getProductInfo(Long.parseLong(productId));
            if (productInfo == null) {
                continue;
            }
            // 过滤下架商品
            if (productInfo.getShelfStatus() == null || productInfo.getShelfStatus() != 1) {
                continue;
            }
            // 过滤定制商品
            if (productInfo.getSupportTexture() != null && productInfo.getSupportTexture() == 1){
                continue;
            }

            Long secondCategoryId = productInfo.getSecondCategoryId();
            Long thirdCategoryId = productInfo.getThirdCategoryId();
            if (CommonConstants.SPECIAL_CATEGORY2_IDS.contains(secondCategoryId)){
                thirdCategoryId = secondCategoryId;
            }
            if (userPreferCategoryList.contains(thirdCategoryId.toString())) {
                majorList.add(productId);
            } else {
                String productGender = productInfo.getProductGender() == null ? CommonConstants.UNKNOWN_SEX : productInfo.getProductGender().toString();
                // 非兴趣相关的集合中加入同性别、中性商品 否则丢弃掉
                if (StringUtils.isBlank(sex) || CommonConstants.UNKNOWN_SEX.equals(sex) ||
                        (CommonConstants.MALE_SEX.equals(sex) && (CommonConstants.MALE_SEX.equals(productGender) || CommonConstants.COMMON_SEX.equals(productGender))) ||
                        (CommonConstants.FEMALE_SEX.equals(sex) && (CommonConstants.FEMALE_SEX.equals(productGender) || CommonConstants.COMMON_SEX.equals(productGender)))) {
                    majorList.add(productId);
                }
            }
        }
        List<Map.Entry<String, Double>> roughMajorList = roughSort(majorList, uuid);
        majorList = sortByHistory(roughMajorList, uuid, roughMajorList.size());
//        if (majorList != null && majorList.size() > 40) {
//            return majorList;
//        }else{
//            if (majorList == null){
//                majorList = new ArrayList<>();
//            }
//            List<Map.Entry<String, Double>> roughMinorList = roughSort(minorList, uuid);
//            minorList = sortByHistory(roughMinorList, uuid, 40);
//            majorList.addAll(minorList);
//        }
        return majorList;
    }
}
