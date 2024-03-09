package com.biyao.moses.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.biyao.dclog.service.DCLogger;
import com.biyao.moses.cache.ProductDetailCacheNoCron;
import com.biyao.moses.cache.ProductMustSizeCache;
import com.biyao.moses.cache.ProductRetrievalCache;
import com.biyao.moses.cache.SimilarCategory3IdCache;
import com.biyao.moses.common.constant.CommonConstants;
import com.biyao.moses.common.constant.RedisKeyConstant;
import com.biyao.moses.common.enums.MatchStrategyEnum;
import com.biyao.moses.common.enums.PlatformEnum;
import com.biyao.moses.model.filter.UserExtendInfo;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.params.ProductInfo;
import com.biyao.moses.params.match.MatchRequest;
import com.biyao.moses.rpc.UcRpcService;
import com.uc.domain.bean.User;
import com.uc.domain.constant.UserFieldConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class MatchFilterUtil {
    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private CacheRedisUtil cacheRedisUtil;

    @Autowired
    private ProductDetailCacheNoCron productDetailCacheNoCron;

    @Autowired
    private ProductMustSizeCache productMustSizeCache; //黄金尺码是否充足缓存

    @Autowired
    private SimilarCategory3IdCache similarCategory3IdCache;

    @Autowired
    private UcRpcService ucRpcService;

    @Autowired
    private ProductRetrievalCache productRetrievalCache;

    // 首页feed流的topicId
    private static final String HOME_FEED_TOPICID = "10300128";
    // 最新足迹时间限制
    private static final int LAST_VIEW_TIME_LIMIT = 604800000; //60*60*24*7*1000 7天
    // 用户收藏、加车、购买行为 redis key prefix
    private static final String USER_BEHAVIOR_RDK_PREFIX = "moses:user_non_purchased_product_";
    // 用户浏览商品 redis key prefix
    private static final String USER_VIEW_RDK_PREFIX = "moses:user_viewed_products_";
   
    // 用户uid购买行为推荐redis key
    private static final String USER_UID_RDK = "moses:user_buy";   
    // 原首页feed流热销好评推荐同性别major
    public static final String MAJOR_FEED_RCD = "major_feed";
    // 原首页feed流热销好评推荐异性别minor
    private static final String MINOR_FEED_RCD = "minor_feed";
    // 行为推荐[收藏、加车、购买]
    public static final String BEHAVIOR_RCD = "behavior";
    // 上新推荐同性别major
    private static final String MAJOR_NEW_SHELF_RCD = "major_new";
    // 上新推荐异性别minor
    private static final String MINOR_NEW_SHELF_RCD = "minor_new";
    // 低决策推荐同性别major
    public static final String MAJOR_LOW_DECISION = "major_low";
    // 低决策推荐异性别minor
    public static final String MINOR_LOW_DECISION = "minor_low";
    // 基础流量推荐同性别major
    public static final String MAJOR_LOW_EXPOSURE = "major_exposure_low";
    // 基础流量推荐异性别minor
    public static final String MINOR_LOW_EXPOSURE = "minor_exposure_low"; 
    //天气推荐
    public static final String WEATHER_RCD = "weather"; 
    //获取天气URL
    private final static String WEATHER_URL = "http://dcapi.biyao.com/queryweather/v2";
    // 首页feed流白名单推荐的dclogger
    private static DCLogger dcLogger = DCLogger.getLogger("moses_home_feed_whitelist");

    /**
     * zhaiweixi 20190522 获取优先展示的30个商品 领导需求
     *
     * @param expKey            CommonConstants.DEFAULT_PREFIX + topicId + "_" + expId
     * @param totalTemplateList match商品
     * @param matchRequest
     * @return
     */
    public List<TotalTemplateInfo> gpWhitelistFilter(String expKey, List<TotalTemplateInfo> totalTemplateList,
                                                     MatchRequest matchRequest) {
        List<TotalTemplateInfo> result = new ArrayList<>();
        try {
            if (expKey.startsWith(CommonConstants.DEFAULT_PREFIX + HOME_FEED_TOPICID + "_")) {
                // zhaiweixi 20190522 对首页feed流增加白名单商品，老大需求，巍哥设计，此处写死topicId TODO 后人优化吧

                //获取uc不感兴趣商品，加入黑名单
                Set<Long> uninterested = new HashSet<>();
                List<String> filedList = new ArrayList<>();
                filedList.add(UserFieldConstants.DISINTERESTPIDS);
                User ucUser = ucRpcService.getData(matchRequest.getUuId(),null, filedList, "mosesmatch");
                if(ucUser!=null&& CollectionUtils.isNotEmpty(ucUser.getDisinterestPids())){
                    uninterested = ucUser.getDisinterestPids();
                }

                Set<String> priorityProductSet = new HashSet<>();
                // 获取优先显示的X个商品
                //新增经纬度参数 20190606
                List<Long> priorityProductList = getPriorityProductList(matchRequest,uninterested);

                List<Long> filterPriorityProductList = priorityProductList.stream().filter(l -> {
                            ProductInfo productInfo = productDetailCacheNoCron.getProductInfo(l);
                            if (productInfo == null || StringUtil.isBlank(productInfo.getSupportPlatform()) || !productInfo.getSupportPlatform().contains(matchRequest.getSiteId())) {
                                return false;
                            }
                            if (CollectionUtils.isEmpty(productInfo.getSupportChannel()) || !productInfo.getSupportChannel().contains(PlatformEnum.getChannelTypeBySiteId(matchRequest.getSiteId()))) {
                                return false;
                            }
                            return true;
                        }
                ).collect(Collectors.toList());
                // try {
//                log.error("优先展示的前X个商品,  数量={}，商品ID={}", priorityProductList.size(), JSON.toJSONString(priorityProductList));
                // } catch (Exception e) {
                //
                // }
                for (Long productId : filterPriorityProductList) {
                    TotalTemplateInfo totalTemplateInfo = new TotalTemplateInfo();
                    totalTemplateInfo.setId(productId.toString());
                    result.add(totalTemplateInfo);
                    priorityProductSet.add(productId.toString());
                }
                for (TotalTemplateInfo totalTemplateInfo : totalTemplateList) {
                    if (!priorityProductSet.contains(totalTemplateInfo.getId())
                            && !uninterested.contains(Long.parseLong(totalTemplateInfo.getId()))) {
                        result.add(totalTemplateInfo);
                    }
                }
                return result;
            } else {
                return totalTemplateList;
            }
        } catch (Exception e) {
            log.error("优先展示的前X个商品插入失败：", e);
        }
        return totalTemplateList;
    }

    /**
     * zhaiweixi 20190522 获取优先展示的28个商品 zhaiweixi 20190523 又要加入2个足迹相关商品
     *
     * @param matchRequest
     * @return
     */
    public List<Long> getPriorityProductList(MatchRequest matchRequest,Set<Long> uninterested) {
        /**
         * 20190523 领导说要增加取用户的足迹相关，取最近的不同三级类目的2个商品。 20190522 zhaiweixi
         * 前14个从原feed流(热销、好评)取前28个，再从28个中随机取14个。 moses:gpxq_0001
         * 中间10个从剩下的SPU中根据规则排序取前28个，再从28个中随机取10个。 moses:gpxq_0002
         * 最后4个从上新商品中取前28个，再从28个中随机取4个。 moses:gpxq_0003 要求SPU不重复相同三级类目<=2，相同商家<=2。
         */
        List<Long> priorityProductList = new ArrayList<Long>();
        String uuid = matchRequest.getUuId();
        String uid = matchRequest.getUid();
        String lng = matchRequest.getLng();
        String lat = matchRequest.getLat();
        UserExtendInfo userExtendInfo = new UserExtendInfo();
        try {
            // 初始化黑名单商品
            Set<Long> existsProductSet = initProductSet(uuid,matchRequest,uninterested);// 用于判断SPU不重复
            //获取用户性别
            String sex = matchRequest.getSex();
            userExtendInfo.setSex(sex);
            // 热销好评数据：productId:score,productId:score,productId:score
            Map<String, List<Long>> feedRcdProductMap = getFeedRcdProductMap(sex);
            // 取商品共现集合
            List<String> view1ProductRecommendList =  new ArrayList<String>();
            List<String> view3ProductRecommendList =  new ArrayList<String>();
            //获取用户uid行为推荐召回源
            List<Long> userUidRcdProductList = new ArrayList<>();
            if(StringUtils.isNotBlank(uid)){
                userUidRcdProductList = getUserUidRcdProductMap(uid,sex);
            }
            //获取足迹1、足迹3商品共现集合
            getView1and3ProductRecommendList(view1ProductRecommendList,view3ProductRecommendList,uuid,uid);

            /*****************足迹根据时间拆分：需修改点0 start ******************/
            //获取用户行为商品集合
            List<Long> userBehaviorRcdProductList = getUserBehaviorRcdProductList(uid, existsProductSet);

            // 低决策商品推荐，数据由@{翟伟西}提供 redis的value格式：pid1:score1,pid2:score2... 
            List<Long> lowDecisionRcdProductList = productRetrievalCache.getLowDecisionRdkLst();
            commonFilterProduct(lowDecisionRcdProductList);
            //获取兴趣偏好的新品
            Map<String, List<Long>> newShelfRcdProductMap = getNewShelfRcdProductList(uuid, userExtendInfo, sex);
            /*****************根据天气获取推荐商品：需修改点2 start ***finished***************/
            //根据天气获取推荐商品
            List<Long> weatherRcdProductLst = getWeatherRcdProductLst(uuid, lng, lat, userExtendInfo);
            /*****************根据天气获取推荐商品：需修改点2 end ******************/

            /*****************推荐商品：新品基础曝光流量池 ,需修改点3 start *****finished*************/
            //redis获取：新品曝光小于1000次商品，数据格式 pid:score,pid:score,... 
            List<Long> lowExposureRcdProductList = productRetrievalCache.getHomeFeedExposureLessLst();
            commonFilterProduct(lowExposureRcdProductList);
            /*****************推荐商品：新品基础曝光流量池 ,需修改点3 end ******************/

			// 类别推荐商品个数动态调整
			Map<Long, Integer> cateProductCountToday = new HashMap<Long, Integer>();
			Map<Long, Integer> cateProductCount7Day = new HashMap<Long, Integer>();
			if (StringUtils.isNotEmpty(uuid)) {
				String cateProductTodayStr = redisUtil.getString(CommonConstants.CATE_PRODUCT_TODAY_KEY_PREFIEX + uuid);
				String cateProduct7DayStr = redisUtil.getString(CommonConstants.CATE_PRODUCT_7DAY_KEY_PREFIEX + uuid);
				cateProductCountToday = getCateProductCountMap(cateProductTodayStr);
				cateProductCount7Day = getCateProductCountMap(cateProduct7DayStr);
			}
//			log.error("feed流当天类别推荐的商品集合cateProductCountToday={}， uuid={}", JSON.toJSONString(cateProductCountToday),uuid);
//			log.error("feed流7天类别推荐的商品集合cateProductCount7Day={}， uuid={}", JSON.toJSONString(cateProductCount7Day),uuid);

            Map<String, List<Long>> rcdDataMap = new HashMap<>();
            Map<String, List<Long>> lowDecisionRcdProductMap = dealRcdDataMapBySex(sex, lowDecisionRcdProductList, MINOR_LOW_DECISION, MAJOR_LOW_DECISION);
            Map<String, List<Long>> lowExposureRcdProductMap = dealRcdDataMapBySex(sex, lowExposureRcdProductList, MINOR_LOW_EXPOSURE, MAJOR_LOW_EXPOSURE);
            rcdDataMap.putAll(feedRcdProductMap);
            rcdDataMap.putAll(newShelfRcdProductMap);
            rcdDataMap.putAll(lowDecisionRcdProductMap);
            rcdDataMap.putAll(lowExposureRcdProductMap);
            rcdDataMap.put(BEHAVIOR_RCD, userBehaviorRcdProductList);
            rcdDataMap.put(WEATHER_RCD, weatherRcdProductLst);
            Map<String, List<Long>> matchDetailMap = new HashMap<>();
            //推荐200商品处理逻辑
            for (int i = 0; i < 10; i++) {
                //热销商品集合：feedRcdProductList 修改为feedRcdProductMap.get(MAJOR_FEED_RCD)
                List<Long> tempPriorityProductList = dealPriorityProductList(rcdDataMap, view1ProductRecommendList, view3ProductRecommendList,
                        feedRcdProductMap.get(MAJOR_FEED_RCD), uuid, uid, existsProductSet, matchDetailMap, cateProductCountToday, cateProductCount7Day,userUidRcdProductList);
                //推荐结果
                priorityProductList.addAll(tempPriorityProductList);
            }
            sendDCLog(matchRequest, userExtendInfo, matchDetailMap);
//			log.error("首页feed流添加固定200个商品大小：{},priorityProductList内容：{}, uuid={}, uid={}", priorityProductList.size(),
//					JSONObject.toJSONString(priorityProductList), uuid, uid);
        } catch (Exception e) {
            log.error("首页feed流添加固定200个商品失败：{}, uuid={}, uid={}", e.getLocalizedMessage(), uuid, uid);
        }

        return priorityProductList;
    }

    /**
     * 获取兴趣偏好的新品
     * @param uuid
     * @param userExtendInfo
     * @param sex
     * @return
     */
    public Map<String, List<Long>> getNewShelfRcdProductList(String uuid, UserExtendInfo userExtendInfo, String sex) {
        // 上新商品，数据由@{赵亚宁}提供，redis的value格式：pid1:score1,pid2:score2...          
        List<Long> newShelfRcdProductList = productRetrievalCache.getNewShelfRdkLst();                           

        /*****************兴趣偏好的新品加权：需修改点1 start ********finished**********/
        //每日上新按性别改为按三级类目
        Map<String, List<Long>> newShelfRcdProductMap = new HashMap<String, List<Long>>();
        //类目ID:计数,类目ID:计数....
        if (StringUtils.isNotEmpty(uuid) && CollectionUtils.isNotEmpty(newShelfRcdProductList)) {
            String levelHobbyCategoryStr = redisUtil.getString(CommonConstants.LEVEL_HOBBY_PREFIX + uuid);
            if (StringUtils.isNotEmpty(levelHobbyCategoryStr)) {
                userExtendInfo.setUcPrefer(levelHobbyCategoryStr);
                //XXX解析偏好三级类目ID,直接使用该方法getRcdProductList()后续可以独立处理
                List<Long> levelHobbyCategoryList = getRcdProductList(levelHobbyCategoryStr);
                newShelfRcdProductMap = dealRcdDataMapByCategory(sex,levelHobbyCategoryList, newShelfRcdProductList, MINOR_NEW_SHELF_RCD, MAJOR_NEW_SHELF_RCD);
            }
        }
        /*****************有兴趣偏好的新品：需修改点1 end ******************/
        return newShelfRcdProductMap;
    }

    /**
     * 获取用户行为商品集合
     * @param uid
     * @param existsProductSet
     * @return
     */
    public List<Long> getUserBehaviorRcdProductList(String uid, Set<Long> existsProductSet) {
        // 用户足迹行为(加车、收藏、购买等历史行为）获取商品集合。
        //足迹2，userBehaviorValues格式：p1:timestamp,p2:timestamp,...
        List<String> userBehaviorValues = new ArrayList<>();
        if (StringUtils.isNotBlank(uid)) {
            userBehaviorValues = redisUtil.lrange(USER_BEHAVIOR_RDK_PREFIX + uid, 0, 100);
        } else {
            userBehaviorValues = new ArrayList<String>();
        }
        // 用户行为（加购、收藏、购买）逻辑处理
        List<Long> userBehaviorRcdProductList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(userBehaviorValues)) {
            List<String> userBehaviorProductList = new ArrayList<>();
            for (String value : userBehaviorValues) {
                if (StringUtils.isNotBlank(value)) {
                    String[] productValue = value.split(":");
                    userBehaviorProductList.add(productValue[0]);

                }
            }
            // 获取推荐相关商品 结果数据格式：pid1:score1,pid2:score2...
            if (CollectionUtils.isNotEmpty(userBehaviorProductList)) {
                List<String> viewProductRecommendBehaviorList = new ArrayList<String>();
                // 根据用户行为取出全部相似商品，进行推荐
                viewProductRecommendBehaviorList = redisUtil.hmget(CommonConstants.MOSES_GOD_OCCURRENCE,
                        userBehaviorProductList.toArray(new String[userBehaviorProductList.size()]));

                final Set<Long> tmpProductSet = new HashSet<>();
                viewProductRecommendBehaviorList.stream().forEach(productScoreStr -> {
                    getRcdProductList(productScoreStr).stream().filter(pid -> !tmpProductSet.contains(pid))
                            .forEach(pid -> {
                                userBehaviorRcdProductList.add(pid);
                                tmpProductSet.add(pid);
                                //XXX 用户行为（加购、收藏、购买）过的商品需过滤，不推荐 20190611
                                existsProductSet.add(Long.valueOf(pid));
                            });
                });
            }
        }

        //过滤掉不满足通用条件的商品
        commonFilterProduct(userBehaviorRcdProductList);
        return userBehaviorRcdProductList;
    }

    /**
     * 获取足迹1和足迹3商品集合
     * @param view1ProductRecommendList
     * @param view3ProductRecommendList
     * @param uuid
     */
    public void getView1and3ProductRecommendList(List<String> view1ProductRecommendList, List<String> view3ProductRecommendList
            , String uuid,String uid) {

        //足迹1：当日0点之前7X24小时
        List<String> view1ProductList = new ArrayList<>();
        //足迹3：当日0点至当前时间
        List<String> view3ProductList = new ArrayList<>();

        // 用户浏览历史是lpush，此处用lrange取出时，也可以保证最新浏览的在最前面。
        List<String> viewProductValues = redisUtil.lrange(USER_VIEW_RDK_PREFIX + uuid, 0, 100);
//			log.error("用户浏览足迹redis大小：{},viewProductValues内容：{}, uuid={}, uid={}", viewProductValues.size(),
//					JSONObject.toJSONString(viewProductValues), uuid, uid);
        /*****************足迹根据时间拆分：需修改点0 start ****finished**********************/
        //获取当日0点毫秒数
        long currentTime = System.currentTimeMillis();
        long zeroTime = currentTime - (currentTime + TimeZone.getDefault().getRawOffset()) % (1000 * 3600 * 24);

        if (CollectionUtils.isNotEmpty(viewProductValues)) {
            for (String value : viewProductValues) {
                if (StringUtils.isNotBlank(value)) {
                    String[] productValue = value.split(":");
                    String time = productValue[1];
                    if (StringUtils.isEmpty(time)) {
                        continue;
                    }
                    if (zeroTime < Long.valueOf(time)) {
                        view3ProductList.add(productValue[0]);
                        continue;
                    }
                    if (zeroTime - Long.valueOf(time) <= LAST_VIEW_TIME_LIMIT) {
                        view1ProductList.add(productValue[0]);
                        continue;
                    }

                }
            }
        }
        // 取商品共现，格式 ["pid11:score11,pid12:score12...", "pid21:score21,pid22:score22..."]
        List<String> view1List = new ArrayList<>();
        if(view1ProductList.size() > 0 ){
             view1List = redisUtil.hmget(CommonConstants.MOSES_GOD_OCCURRENCE,
                    view1ProductList.toArray(new String[view1ProductList.size()]));
        }
        if (CollectionUtils.isNotEmpty(view1List)) {
            view1ProductRecommendList.addAll(view1List);
        }
        List<String> view3List = new ArrayList<>();
        if(view3ProductList.size() > 0){
             view3List = redisUtil.hmget(CommonConstants.MOSES_GOD_OCCURRENCE,
                    view3ProductList.toArray(new String[view3ProductList.size()]));
        }
        if (CollectionUtils.isNotEmpty(view3List)) {
            view3ProductRecommendList.addAll(view3List);
        }

        /*****************足迹根据时间拆分：需修改点0 start ******************/
    }


    /**
     * 根据性别召回热销商品
     * @param sex
     * @return
     */
    public Map<String, List<Long>> getFeedRcdProductMap(String sex) {
      
        List<Long> feedRcdProductList = productRetrievalCache.getFeedRdkLst();
        return dealRcdDataMapBySex(sex, feedRcdProductList, MINOR_FEED_RCD, MAJOR_FEED_RCD);
    }

    /**
     * 根据uid召回用户购买行为推荐商品
     * @return
     */
    public List<Long> getUserUidRcdProductMap(String uid,String sex) {
        String feedRcdProductScoreStr = redisUtil.getString(USER_UID_RDK+"_"+uid);
        List<Long> userUidRcdProductList = getRcdProductList(feedRcdProductScoreStr);
        if(userUidRcdProductList != null && userUidRcdProductList.size()!=0){
            List<Long> result = dealUserBuyRcdDataMapBySex(userUidRcdProductList,sex);
            return result;
        }else {
            return new ArrayList<>();
        }
    }

    private List<Long> dealUserBuyRcdDataMapBySex(List<Long> userUidRcdProductList,String sex) {
        List<Long> result = new ArrayList<>();

        for (Long productId : userUidRcdProductList) {
            ProductInfo productInfo = productDetailCacheNoCron.getProductInfo(productId);
            if (productInfo != null) {
                String productGender = productInfo.getProductGender() == null ? CommonConstants.UNKNOWN_SEX : productInfo.getProductGender().toString();
                if (CommonConstants.MALE_SEX.equals(sex)) {
                    //用户为男性
                    if (CommonConstants.MALE_SEX.equals(productGender) || CommonConstants.COMMON_SEX.equals(productGender)) {
                        result.add(productId);
                    }
                } else if (CommonConstants.FEMALE_SEX.equals(sex)) {
                    //用户为女性
                    if (CommonConstants.FEMALE_SEX.equals(productGender) || CommonConstants.COMMON_SEX.equals(productGender)) {
                        result.add(productId);
                    }
                } else {
                    //用户为中性
                    result.add(productId);
                }
            }
        }

        return result;
    } 

    /**
	* @Description 获取商品集合类目Id以及商品出现数量
	* @param
	* @return Map<Long,Integer>
	* @version V1.0
	* @auth 邹立强 (zouliqiang@idstaff.com)
	 */
	public Map<Long, Integer> getCateProductCountMap(String cateProductStr) {
		Map<Long, Integer> cateProductCountMap = new HashMap<Long, Integer>();
		if (StringUtils.isNotEmpty(cateProductStr)) {
        	String[] cateProductsplits = cateProductStr.split(",");
        	for (String productCount : cateProductsplits) {
        		String[] productCountSplit = productCount.split(":");
        		String cid=productCountSplit[0];
        		String count=productCountSplit[1];
        		if(StringUtils.isBlank(cid)) {
        			continue;
        		}
        		if(StringUtils.isBlank(count)) {
        			continue;
        		}
        		cateProductCountMap.put(Long.valueOf(cid),Integer.valueOf(count));
			}
        }
		return cateProductCountMap;
	}

    /**
     * 根据天气获取推荐商品
     *
     * @param uuid
     * @param lng  经度
     * @param lat  纬度
     * @return
     * @author huangyq
     * @date 2019年6月6日下午3:58:43
     */
    public List<Long> getWeatherRcdProductLst(String uuid, String lng,
                                               String lat, UserExtendInfo userExtendInfo) {
        List<Long> weatherRcdProductLst = new ArrayList<Long>();
        //判空验证
        if (StringUtils.isEmpty(uuid) || StringUtils.isEmpty(lng) || StringUtils.isEmpty(lat)) {

            return weatherRcdProductLst;
        }
        try {
            //XXX 根据经纬度获取天气,目前获取天气有多个地方在使用，后续该逻辑需要独立出来
            String jsonStr = HttpClientUtil.sendGetRequest(WEATHER_URL + "?uuid=" + uuid + "&lng=" + lng + "&lat=" + lat, 50);

            //第一步：根据经纬度获取天气返回结果为空
            if (StringUtils.isEmpty(jsonStr)) {

                log.error("根据经纬度获取天气返回结果为空, uuid={}", uuid);
                return weatherRcdProductLst;
            }

            //jsonStr返回结果数据结构：{"code":1,"data":"yun","errorCode":"","errorMessage":""}
            JSONObject weatherResultJson = JSON.parseObject(jsonStr);
            String weaData = weatherResultJson.getString("data");

            //第二步：根据经纬度获取天气返回结果code!=1 or data is null
            if (!StringUtils.equals("1", weatherResultJson.getString("code")) || StringUtils.isEmpty(weaData)) {

                log.error("根据经纬度获取天气返回结果异常={}, uuid={}", jsonStr, uuid);
                return weatherRcdProductLst;
            }

            //第三步：获取天气商品集合，数据格式 pid1,pid2...,后续考虑走内存缓存
            // 设置天气
            userExtendInfo.setWeather(weaData);
            weatherRcdProductLst = productRetrievalCache.getWeatherProductLst(weaData);

        } catch (Exception e) {
            log.error("根据天气推荐商品异常, uuid={}", uuid, e);
        }
        commonFilterProduct(weatherRcdProductLst);
        return weatherRcdProductLst;
    }

    /**
     * 通用条件过滤商品
     * @param productList
     */
    public void commonFilterProduct(List<Long> productList){
        if(CollectionUtils.isEmpty(productList)){
            return;
        }
        Iterator<Long> iterator = productList.iterator();
        while(iterator.hasNext()){
            Long productId = iterator.next();
            if(productId == null){
                continue;
            }
            ProductInfo productInfo = productDetailCacheNoCron.getProductInfo(productId);
            if(FilterUtil.isCommonFilter(productInfo)){
                iterator.remove();
            }
        }
    }
    /**
     * @param sex
     * @param list
     * @param minorKey
     * @param majorKey
     * @return Map<String, List < Long>>
     * @Description 根据性别处理热销、基础流量、低决策数据
     * @version V1.1
     * @auth 邹立强 (zouliqiang@idstaff.com)
     */
    public Map<String, List<Long>> dealRcdDataMapBySex(String sex, List<Long> list, String minorKey, String majorKey) {
        Map<String, List<Long>> rcdDataMap = new HashMap<>();
        List<Long> majorRcdProductList = new ArrayList<Long>();
        List<Long> minorRcdProductList = new ArrayList<Long>();
        if (CollectionUtils.isNotEmpty(list)) {
            for (Long productId : list) {
                ProductInfo productInfo = productDetailCacheNoCron.getProductInfo(productId);
                if(FilterUtil.isCommonFilter(productInfo)){
                    continue;
                }

                String productGender = productInfo.getProductGender() == null ? CommonConstants.UNKNOWN_SEX : productInfo.getProductGender().toString();
                // 根据用户性别、商品性别 区分优先推荐集合以及次优先推荐集合
                // 逻辑为：如果用户为男性，则将男性商品和中性商品放入major 女性和未知不要,
                // 如果用户是女性则女性商品和中性商品放入major 男性和未知不要,
                // 如果用户是中性，则把全部商品放入major
                if (CommonConstants.MALE_SEX.equals(sex)) {
                    //用户为男性
                    if (CommonConstants.MALE_SEX.equals(productGender) || CommonConstants.COMMON_SEX.equals(productGender)) {
                        majorRcdProductList.add(productId);
                    }
                } else if (CommonConstants.FEMALE_SEX.equals(sex)) {
                    //用户为女性
                    if (CommonConstants.FEMALE_SEX.equals(productGender) || CommonConstants.COMMON_SEX.equals(productGender)) {
                        majorRcdProductList.add(productId);
                    }
                } else {//用户为中性
                    majorRcdProductList.add(productId);
                }

            }
        }
        rcdDataMap.put(majorKey, majorRcdProductList);
        rcdDataMap.put(minorKey, minorRcdProductList);
        return rcdDataMap;
    }

    /**
     * 根据类目偏好及用户性别，处理每日上新商品
     *@param sex 用户性别
     * @param levelHobbyCategoryList 偏好三级类目集合
     * @param list
     * @param minorKey
     * @param majorKey
     * @return
     * @author huangyq
     * @date 2019年6月6日下午5:19:03
     */
    private Map<String, List<Long>> dealRcdDataMapByCategory(String sex, List<Long> levelHobbyCategoryList, List<Long> list, String minorKey, String majorKey) {
        Map<String, List<Long>> rcdDataMap = new HashMap<>();
        List<Long> majorRcdProductList = new ArrayList<Long>();
        List<Long> minorRcdProductList = new ArrayList<Long>();
        if (CollectionUtils.isNotEmpty(list)) {
            for (Long productId : list) {

                ProductInfo productInfo = productDetailCacheNoCron.getProductInfo(productId);

                if (FilterUtil.isCommonFilter(productInfo)) {
                    continue;
                }

                //三级类目 productInfo.getThirdCategoryId(),眼镜、尿不湿 需用二级类目
                Long category3Id = productInfo.getThirdCategoryId();
                Long category2Id = productInfo.getSecondCategoryId();
                // 二级类目是眼镜的需要特殊处理 后台二级类目是39
                if (CommonConstants.SPECIAL_CATEGORY2_IDS.contains(category2Id)) {
                    category3Id = category2Id;
                }

                if (!CollectionUtils.isEmpty(levelHobbyCategoryList) && levelHobbyCategoryList.contains(category3Id)) {

                    majorRcdProductList.add(productId);
                } else {
                    // 如果用户为男性，则将男性商品和中性商品保留，女性和未知不要,
                    // 如果用户是女性则女性商品和中性商品保留， 男性和未知不要,
                    // 如果用户是中性，则把全部商品保留。
                    String productGender = productInfo.getProductGender() == null ? CommonConstants.UNKNOWN_SEX : productInfo.getProductGender().toString();
                    if (CommonConstants.MALE_SEX.equals(sex)) {
                        //用户为男性
                        if (CommonConstants.MALE_SEX.equals(productGender) || CommonConstants.COMMON_SEX.equals(productGender)) {
                            minorRcdProductList.add(productId);
                        }
                    } else if (CommonConstants.FEMALE_SEX.equals(sex)) {
                        //用户为女性
                        if (CommonConstants.FEMALE_SEX.equals(productGender) || CommonConstants.COMMON_SEX.equals(productGender)) {
                            minorRcdProductList.add(productId);
                        }
                    } else {
                        //用户为中性
                        minorRcdProductList.add(productId);
                    }
                }
            }
        }
        rcdDataMap.put(majorKey, majorRcdProductList);
        rcdDataMap.put(minorKey, minorRcdProductList);
        return rcdDataMap;
    }

    /**
     * 白名单推荐一页数据处理
     *
     * @param rcdDataMap
     * @param view1ProductRecommendList 足迹1、
     * @param view3ProductRecommendList 足迹3
     * @param uuid
     * @param uid
     * @param existsProductSet
     * @param matchDetailMap            记录每种策略召回的内容
     * @return
     */
    public List<Long> dealPriorityProductList(
            Map<String, List<Long>> rcdDataMap,
            List<String> view1ProductRecommendList,
            List<String> view3ProductRecommendList, List<Long> feedProductList,
            String uuid, String uid, Set<Long> existsProductSet,
            Map<String, List<Long>> matchDetailMap, Map<Long,Integer> cateProductCountToday,Map<Long,Integer> cateProductCount7Day,List<Long> userUidRcdProductList) {

        List<Long> result = new ArrayList<>();

        Set<Long> repeatCountSet = new HashSet<Long>(); // 三级类目最多重复次数最多重复3次
        Map<Long, Integer> category3CountMap = new HashMap<Long, Integer>(); // 三级类目计数，用于判断三级类目<=2

        List<Long> feedRcdProductList = new ArrayList<Long>(); // 存放feed流推荐个商品
        List<Long> newRcdProductList = new ArrayList<Long>(); // 存放上新推荐个商品
        List<Long> behaviorViewRcdProductList = new ArrayList<Long>(); // 存放用户行为足迹推荐个商品
        List<Long> lowDecisionsList = new ArrayList<Long>(); // 存放用户行为足迹推荐个商品

        //推荐系统_首页feeds流迭代V1.0
        List<Long> lastView1RcdProductList = new ArrayList<Long>(); // 存放最新足迹1推荐个商品
        List<Long> lastView3RcdProductList = new ArrayList<Long>(); // 存放最新足迹3推荐个商品
        List<Long> weatherRcdProductList = new ArrayList<Long>();//天气推荐商品
        List<Long> lowexposureRcdProductList = new ArrayList<Long>();// 流量池(低曝光的商品)推荐商品


        int feedRcdProductCount = 3;// feed流推荐的商品个数
        int newRcdProductCount = 2;// 兴趣上新推荐的商品个数
        int behaviorViewRcdProductCount = 2;//  用户行为足迹推荐的2个商品个数
        int lowDecisionsCount = 2;// 低决策推荐的商品个数

        //推荐系统_首页feeds流迭代V1.0
        int lastView1RcdProductCount = 3;// 最新足迹1推荐的商品个数
        int lastView3RcdProductCount = 6;// 最新足迹3推荐的商品个数
        int weatherRcdProductCount = 1;// 天气推荐的商品个数
        int lowexposureRcdProductCount = 1;//流量池推荐商品个数


        // 足迹1推荐特殊处理
        lastView1RcdProductList = getLastViewRcdProductList(MatchStrategyEnum.VIEW1.getName(), view1ProductRecommendList, null, feedProductList,
                uuid, uid, existsProductSet, repeatCountSet, category3CountMap,
                lastView1RcdProductList, lastView1RcdProductCount, matchDetailMap, cateProductCount7Day,userUidRcdProductList);
        //足迹3推荐特殊处理
        lastView3RcdProductList = getLastViewRcdProductList(MatchStrategyEnum.VIEW3.getName(), view3ProductRecommendList, view1ProductRecommendList, feedProductList,
                uuid, uid, existsProductSet, repeatCountSet, category3CountMap,
                lastView3RcdProductList, lastView3RcdProductCount, matchDetailMap, cateProductCountToday,userUidRcdProductList);

        // 热销召回
        feedRcdProductList = getRcdProductList(rcdDataMap.get(MAJOR_FEED_RCD), category3CountMap,
                existsProductSet, feedRcdProductCount, repeatCountSet, null);
        int feedRcdProductFillNum = feedRcdProductCount - feedRcdProductList.size();
        if (feedRcdProductFillNum > 0) {
            feedRcdProductList.addAll(getRcdProductList(rcdDataMap.get(MINOR_FEED_RCD), category3CountMap,
                    existsProductSet, feedRcdProductFillNum, repeatCountSet, null));
        }
        // 热销召回加入到matchDetailMap中
        addMatchDetailMap(matchDetailMap, MatchStrategyEnum.HS.getName(), feedRcdProductList);
        //用户行为
        behaviorViewRcdProductList = getRcdProductList(rcdDataMap.get(BEHAVIOR_RCD), category3CountMap,
                existsProductSet, behaviorViewRcdProductCount, repeatCountSet, null);
        // 足迹2加入到matchDetailMap中
        addMatchDetailMap(matchDetailMap, MatchStrategyEnum.ZJ2.getName(), behaviorViewRcdProductList);
        //足迹2不足先用用户uid购买行为推荐商品进行兜底
        int behaviorRcdFillNum = behaviorViewRcdProductCount - behaviorViewRcdProductList.size();
        if(behaviorRcdFillNum > 0 && userUidRcdProductList.size() > 0 ){
            List<Long> userBuyRcdProductList = getRcdProductList(userUidRcdProductList, category3CountMap,
                    existsProductSet, behaviorRcdFillNum, repeatCountSet, null);
            behaviorViewRcdProductList.addAll(userBuyRcdProductList);
            //用户购买行为推荐加入到match详细中
            addMatchDetailMap(matchDetailMap, MatchStrategyEnum.USER_BUY.getName(), userBuyRcdProductList);
        }
        behaviorRcdFillNum = behaviorViewRcdProductCount - behaviorViewRcdProductList.size();
        if (behaviorRcdFillNum > 0) {
            List<Long> fillRcdProductList = getRcdProductList(feedProductList, category3CountMap,
                    existsProductSet, behaviorRcdFillNum, repeatCountSet, null);
            behaviorViewRcdProductList.addAll(fillRcdProductList);
            // 热销托底加入到matchDetailMap
            addMatchDetailMap(matchDetailMap, MatchStrategyEnum.HS.getName(), fillRcdProductList);
        }
        //低决策
        lowDecisionsList = getRcdProductList(rcdDataMap.get(MAJOR_LOW_DECISION), category3CountMap,
                existsProductSet, lowDecisionsCount, repeatCountSet, null);
        int lowDecisionRcdFillNum = lowDecisionsCount - lowDecisionsList.size();
        if (lowDecisionRcdFillNum > 0) {
            lowDecisionsList.addAll(getRcdProductList(rcdDataMap.get(MINOR_LOW_DECISION), category3CountMap,
                    existsProductSet, lowDecisionRcdFillNum, repeatCountSet, null));
        }
        // 低决策召回加入到matchDetailMap
        addMatchDetailMap(matchDetailMap, MatchStrategyEnum.LD.getName(), lowDecisionsList);
        // 低决策热销托底
        lowDecisionRcdFillNum = lowDecisionsCount - lowDecisionsList.size();
        if (lowDecisionRcdFillNum > 0) {
            List<Long> fillRcdProductList = getRcdProductList(feedProductList, category3CountMap,
                    existsProductSet, lowDecisionRcdFillNum, repeatCountSet, null);
            lowDecisionsList.addAll(fillRcdProductList);
            // 热销托底加入到matchDetailMap
            addMatchDetailMap(matchDetailMap, MatchStrategyEnum.HS.getName(), fillRcdProductList);
        }
        // 兴趣新品
        newRcdProductList = getRcdProductList(rcdDataMap.get(MAJOR_NEW_SHELF_RCD), category3CountMap,
                existsProductSet, newRcdProductCount, repeatCountSet, null);
        int newShelfRcdFillNum = newRcdProductCount - newRcdProductList.size();
        if (newShelfRcdFillNum > 0) {
            newRcdProductList.addAll(getRcdProductList(rcdDataMap.get(MINOR_NEW_SHELF_RCD), category3CountMap,
                    existsProductSet, newShelfRcdFillNum, repeatCountSet, null));
        }
        // 兴趣新品召回加入到matchDetailMap
        addMatchDetailMap(matchDetailMap, MatchStrategyEnum.NEW_CATE.getName(), newRcdProductList);
        newShelfRcdFillNum = newRcdProductCount - newRcdProductList.size();
        if (newShelfRcdFillNum > 0) {
            List<Long> fillRcdProductList = getRcdProductList(feedProductList, category3CountMap,
                    existsProductSet, newShelfRcdFillNum, repeatCountSet, null);
            newRcdProductList.addAll(fillRcdProductList);
            // 热销托底加入到matchDetailMap
            addMatchDetailMap(matchDetailMap, MatchStrategyEnum.HS.getName(), fillRcdProductList);
        }
        // 天气
        weatherRcdProductList = getRcdProductList(rcdDataMap.get(WEATHER_RCD), category3CountMap,
                existsProductSet, weatherRcdProductCount, repeatCountSet, null);
        // 天气召回加入到matchDetailMap
        addMatchDetailMap(matchDetailMap, MatchStrategyEnum.WEA.getName(), weatherRcdProductList);
        int weatherRcdFillNum = weatherRcdProductCount - weatherRcdProductList.size();
        if (weatherRcdFillNum > 0) {
            List<Long> fillRcdProductList = getRcdProductList(feedProductList, category3CountMap,
                    existsProductSet, weatherRcdFillNum, repeatCountSet, null);
            weatherRcdProductList.addAll(fillRcdProductList);
            // 热销托底加入到matchDetailMap
            addMatchDetailMap(matchDetailMap, MatchStrategyEnum.HS.getName(), fillRcdProductList);
        }
        // 流量池(低曝光的商品)推荐商品
        lowexposureRcdProductList = getRcdProductList(rcdDataMap.get(MAJOR_LOW_EXPOSURE), category3CountMap,
                existsProductSet, lowexposureRcdProductCount, repeatCountSet, null);
        int lowexposureRcdFillNum = lowexposureRcdProductCount - lowexposureRcdProductList.size();
        if (lowexposureRcdFillNum > 0) {
            lowexposureRcdProductList.addAll(getRcdProductList(rcdDataMap.get(MINOR_LOW_EXPOSURE), category3CountMap,
                    existsProductSet, lowexposureRcdFillNum, repeatCountSet, null));
        }

        // 天气召回加入到matchDetailMap
        addMatchDetailMap(matchDetailMap, MatchStrategyEnum.NEW_LOW.getName(), lowexposureRcdProductList);

        lowexposureRcdFillNum = lowexposureRcdProductCount - lowexposureRcdProductList.size();
        if (lowexposureRcdFillNum > 0) {
            List<Long> fillRcdProductList = getRcdProductList(feedProductList, category3CountMap,
                    existsProductSet, lowexposureRcdFillNum, repeatCountSet, null);
            lowexposureRcdProductList.addAll(fillRcdProductList);
            // 热销托底加入到matchDetailMap
            addMatchDetailMap(matchDetailMap, MatchStrategyEnum.HS.getName(), fillRcdProductList);
        }
        int lastView1RcdProductListSize = lastView1RcdProductList.size();
        int behaviorViewRcdProductListSize = behaviorViewRcdProductList.size();
        int feedRcdProductListSize = feedRcdProductList.size();
        int newRcdProductListSize = newRcdProductList.size();
        int lowDecisionsListSize = lowDecisionsList.size();

        int lastView3RcdProductListSize = lastView3RcdProductList.size();
        int weatherRcdProductListSize = weatherRcdProductList.size();
        int lowexposureRcdProductListSize = lowexposureRcdProductList.size();

        int maxSize = Math.max(lastView1RcdProductListSize, behaviorViewRcdProductListSize);
        maxSize = Math.max(maxSize, feedRcdProductListSize);
        maxSize = Math.max(maxSize, newRcdProductListSize);
        maxSize = Math.max(maxSize, lowDecisionsListSize);

        maxSize = Math.max(maxSize, lastView3RcdProductListSize);
        maxSize = Math.max(maxSize, weatherRcdProductListSize);
        maxSize = Math.max(maxSize, lowexposureRcdProductListSize);

        //推荐系统_首页feeds流迭代V1.0版本规则：取最大的数据集进行循环「足迹3、足迹2、热销、足迹1、天气、兴趣新品、低决策、基础流量新品」
        for (int i = 0; i < maxSize; i++) {
            if (i < lastView3RcdProductListSize) {
                result.add(lastView3RcdProductList.get(i));
            }
            if (i < behaviorViewRcdProductListSize) {
                result.add(behaviorViewRcdProductList.get(i));
            }
            if (i < feedRcdProductListSize) {
                result.add(feedRcdProductList.get(i));
            }
            if (i < lastView1RcdProductListSize) {
                result.add(lastView1RcdProductList.get(i));
            }
            if (i < weatherRcdProductListSize) {
                result.add(weatherRcdProductList.get(i));
            }
            if (i < newRcdProductListSize) {
                result.add(newRcdProductList.get(i));
            }
            if (i < lowDecisionsListSize) {
                result.add(lowDecisionsList.get(i));
            }
            if (i < lowexposureRcdProductListSize) {
                result.add(lowexposureRcdProductList.get(i));
            }
        }
        return result;
    }

    /**
     * 获取足迹推荐商品
     *
     * @param viewType                  view1：足迹1 | view3：足迹3
     * @param viewProductRecommendList  当前需要推荐足迹商品原数据
     * @param view1ProductRecommendList 足迹1商品原数据
     * @param feedProductList
     * @param uuid
     * @param uid
     * @param existsProductSet
     * @param repeatCountSet
     * @param category3CountMap
     * @param lastViewRcdProductList
     * @param lastViewRcdProductCount
     * @return
     * @author huangyq
     * @date 2019年6月6日下午6:05:16
     */
    private List<Long> getLastViewRcdProductList(String viewType,
                                                 List<String> viewProductRecommendList, List<String> view1ProductRecommendList, List<Long> feedProductList,
                                                 String uuid, String uid, Set<Long> existsProductSet,
                                                 Set<Long> repeatCountSet, Map<Long, Integer> category3CountMap,
                                                 List<Long> lastViewRcdProductList, int lastViewRcdProductCount,
                                                 Map<String, List<Long>> matchDetailMap ,Map<Long, Integer> cateProductCountMap,List<Long> userUidRcdProductList) {
        if (CollectionUtils.isNotEmpty(viewProductRecommendList)) {
            // 获取推荐相关商品 结果数据格式：pid1:score1,pid2:score2...
            for (String productScoreStr : viewProductRecommendList) {
                List<Long> tmpCandidateProductList = getRcdProductList(productScoreStr);
                List<Long> tmpLastViewRcdProductList = getRcdProductList(tmpCandidateProductList, category3CountMap,
                        existsProductSet, 1, repeatCountSet, cateProductCountMap);

                if (tmpLastViewRcdProductList != null && tmpLastViewRcdProductList.size() > 0) {
                    lastViewRcdProductList.add(tmpLastViewRcdProductList.get(0));
                    // 足迹推荐加入到match详细中
                    addMatchDetailMap(matchDetailMap, viewType, tmpLastViewRcdProductList.get(0));
                }

                if (lastViewRcdProductList.size() >= lastViewRcdProductCount) {
                    break;
                }
            }
        }
        int lastViewRcdFillNum = lastViewRcdProductCount - lastViewRcdProductList.size();
        //足迹3商品不足时,优先获取足迹1商品，足迹1也不足时获取热销商品
        if (lastViewRcdFillNum > 0 && StringUtils.equalsIgnoreCase(MatchStrategyEnum.VIEW3.getName(), viewType) && CollectionUtils.isNotEmpty(view1ProductRecommendList)) {
            // 获取推荐相关商品 结果数据格式：pid1:score1,pid2:score2...
            for (String productScoreStr : view1ProductRecommendList) {
                List<Long> tmpCandidateProductList = getRcdProductList(productScoreStr);
                List<Long> tmpLastViewRcdProductList = getRcdProductList(tmpCandidateProductList, category3CountMap,
                        existsProductSet, 1, repeatCountSet, cateProductCountMap);

                if (tmpLastViewRcdProductList != null && tmpLastViewRcdProductList.size() > 0) {
                    lastViewRcdProductList.add(tmpLastViewRcdProductList.get(0));
                    // 足迹1加入到match详细中
                    addMatchDetailMap(matchDetailMap, MatchStrategyEnum.VIEW1.getName(), tmpLastViewRcdProductList.get(0));
                }

                if (lastViewRcdProductList.size() >= lastViewRcdProductCount) {
                    break;
                }
            }
        }
        //先用用户uid购买行为推荐商品进行兜底
        lastViewRcdFillNum = lastViewRcdProductCount - lastViewRcdProductList.size();
        if(lastViewRcdFillNum > 0 && userUidRcdProductList.size() > 0){
            List<Long> userBuyRcdProductList = getRcdProductList(userUidRcdProductList, category3CountMap,
                    existsProductSet, lastViewRcdFillNum, repeatCountSet, cateProductCountMap);
            lastViewRcdProductList.addAll(userBuyRcdProductList);
            //用户购买行为推荐加入到match详细中
            addMatchDetailMap(matchDetailMap, MatchStrategyEnum.USER_BUY.getName(), userBuyRcdProductList);
        }

        // 足迹兜底处理
        lastViewRcdFillNum = lastViewRcdProductCount - lastViewRcdProductList.size();
        if (lastViewRcdFillNum > 0) {
            List<Long> fillRcdProductList = getRcdProductList(feedProductList, category3CountMap,
                    existsProductSet, lastViewRcdFillNum, repeatCountSet, cateProductCountMap);
            lastViewRcdProductList.addAll(fillRcdProductList);
            // 热销加入到match详细中
            addMatchDetailMap(matchDetailMap, MatchStrategyEnum.HS.getName(), fillRcdProductList);
        }

        return lastViewRcdProductList;
    }

    /**
     * @param uuid
     * @return Set<Long>
     * @Description 初始化黑名单
     * @version V1.0
     * @auth 邹立强 (zouliqiang@idstaff.com)
     */
    public Set<Long> initProductSet(String uuid, MatchRequest matchRequest,Set<Long> uninterested) {
        Set<Long> existsProductSet = new HashSet<Long>(); // 用于判断SPU不重复
        // 过滤楼层显示的商品
        String prefix = RedisKeyConstant.MOSES_BLACK_UUID_TOPICID_KEY_PREFIX;
        String uuid2topicidKey = prefix + "_" + uuid + "_" + HOME_FEED_TOPICID;
        // 结果格式：productId,productId,productId
        String redisResult = redisUtil.getString(uuid2topicidKey);
        if (StringUtils.isNotEmpty(redisResult)) {
            String[] split = redisResult.split(",");
            for (int i = 0; i < split.length; i++) {
                existsProductSet.add(Long.valueOf(split[i]));
            }
        } else {
            uuid2topicidKey = prefix + "_default_" + HOME_FEED_TOPICID;
            String defaultResult = redisUtil.getString(uuid2topicidKey);
            if (StringUtils.isNotEmpty(defaultResult)) {
                String[] split = defaultResult.split(",");
                for (int i = 0; i < split.length; i++) {
                    existsProductSet.add(Long.valueOf(split[i]));
                }
            }
        }
        //首页feed流曝光集合
        Set<String> exposureSet = cacheRedisUtil.smems(CommonConstants.PAGE_CACHE_PREFIX + matchRequest.getPvid() + "_" + matchRequest.getPageId() + "_" + matchRequest.getDataSourceType() + "_"
				+ CommonConstants.HOME_FEED_CACHE_SUFFIX);
        //将feed流曝光集合加入黑名单
        if(CollectionUtils.isNotEmpty(exposureSet)) {
        	for (String pid : exposureSet) {
				if(StringUtils.isNotBlank(pid)) {
		        	existsProductSet.add(Long.valueOf(pid));
				}
			}
        }
        //黄金尺码不足的加入黑名单
        Map<String, String> mustSizeMap = productMustSizeCache.getMustSizeMap();
        if (mustSizeMap != null && mustSizeMap.size() > 0) {
            Iterator<Map.Entry<String, String>> it = mustSizeMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, String> item = it.next();
                //黄金尺码不足
                if (StringUtils.isNotBlank(item.getValue()) && "0".equals(item.getValue())) {
                    existsProductSet.add(Long.parseLong(item.getKey()));
                }
            }
        }
        if(CollectionUtils.isNotEmpty(uninterested)){
            existsProductSet.addAll(uninterested);
        }
        return existsProductSet;
    }

    /**
     * 获取推荐商品ID列表
     *
     * @param productScoreStr 数据格式 pid1:score1,pid2:score2...或者pid1,pid2...
     * @return
     */
    public List<Long> getRcdProductList(String productScoreStr) {
        if (StringUtils.isBlank(productScoreStr)) {
            return new ArrayList<>();
        }
        // todo lambda表达式会影响效率
        List<Long> rcdProductList = Arrays.stream(productScoreStr.split(",")).map(productScore -> {
            return Long.valueOf(productScore.split(":")[0]);
        }).collect(Collectors.toList());

        commonFilterProduct(rcdProductList);
        return rcdProductList;
    }

    /**
     * 按规则推荐商品
     *
     * @param candidateProductList 原始商品集合
     * @param category3CountMap    类目计数
     * @param existsProductSet     去重商品集合
     * @param targetNum            推荐个数
     * @param repeatCountSet       已推荐两个商品类目计数
     * @return
     */
    private List<Long> getRcdProductList(List<Long> candidateProductList, Map<Long, Integer> category3CountMap,
                                         Set<Long> existsProductSet, int targetNum, Set<Long> repeatCountSet,Map<Long, Integer> cateProductCountMap) {
        List<Long> rcdProductList = new ArrayList<>();
        if (candidateProductList == null || candidateProductList.size() == 0) {
            return rcdProductList;
        }
        Collections.shuffle(candidateProductList);
        for (Long productId : candidateProductList) {
            ProductInfo productInfo = productDetailCacheNoCron.getProductInfo(productId);
            if (productInfo == null) {
                continue;
            }
            if (productInfo.getShelfStatus() == null || !productInfo.getShelfStatus().toString().equals("1")) {
                continue;
            }
            if (existsProductSet.contains(productId)) {
                continue;
            }
            Long category3Id = productInfo.getThirdCategoryId();
            Long category2Id = productInfo.getSecondCategoryId();
            if (CommonConstants.SPECIAL_CATEGORY2_IDS.contains(category2Id)) { // 二级类目是眼镜的需要特殊处理 后台二级类目是39
                category3Id = category2Id;
            }
            //相似三级类目替换
            category3Id = similarCategory3IdCache.getSimilarCate3Id(category3Id);

            if (!category3CountMap.containsKey(category3Id)) {
                category3CountMap.put(category3Id, 0);
            }
            // todo 类目判断需要修改，需要判断最多出现X个三级类目(允许最多有3个三级类目出现2个商品)
            Integer c3Num = category3CountMap.get(category3Id);
            // 如果该三级类目已有n个推荐商品，则跳过、只处理足迹1和3
            if(cateProductCountMap==null) {
                if (c3Num >= 2) {
                    continue;
                }
            }else {
                Integer cateProductCount = cateProductCountMap.get(category3Id);
                if(cateProductCount==null) {
                	cateProductCount=0;
                }
                if (c3Num >= cateProductCount) {
                    continue;
                }
            }
            rcdProductList.add(productId);
            existsProductSet.add(productId);
            category3CountMap.put(category3Id, c3Num + 1); // 已推荐的商品的三级类目加入到三级类目计数集合中
            // 推荐足够个商品后，退出
            if (rcdProductList.size() >= targetNum) {
                break;
            }
        }
        return rcdProductList;
    }

    /**
     * matchDetailMap添加单个商品
     *
     * @param matchDetailMap
     * @param key
     * @param productId
     */
    private void addMatchDetailMap(Map<String, List<Long>> matchDetailMap, String key, Long productId) {
        if (!matchDetailMap.containsKey(key)) {
            matchDetailMap.put(key, new ArrayList<>());
        }
        matchDetailMap.get(key).add(productId);
    }

    /**
     * matchDetailMap添加多个商品
     *
     * @param matchDetailMap
     * @param key
     * @param productIdList
     */
    private void addMatchDetailMap(Map<String, List<Long>> matchDetailMap, String key, List<Long> productIdList) {
        if (!matchDetailMap.containsKey(key)) {
            matchDetailMap.put(key, new ArrayList<>());
        }
        matchDetailMap.get(key).addAll(productIdList);
    }

    /**
     * 打印dclog
     *
     * @param matchRequest
     * @param userExtendInfo
     * @param matchDetailMap
     */
    private void sendDCLog(MatchRequest matchRequest, UserExtendInfo userExtendInfo, Map<String, List<Long>> matchDetailMap) {
        StringBuffer logStr = new StringBuffer();
        logStr.append("lt=moses_home_feed_whitelist").append("\t");
        logStr.append("lv=1.0\t");
        String uu = !StringUtils.isEmpty(matchRequest.getUuId()) ? matchRequest.getUuId() : "";
        logStr.append("uu=").append(uu).append("\t");
        String uid = !StringUtils.isEmpty(matchRequest.getUid()) ? matchRequest.getUid() : "";
        logStr.append("u=").append(uid).append("\t");
        String siteId = !StringUtils.isEmpty((matchRequest.getSiteId())) ? matchRequest.getSiteId() : "";
        logStr.append("stid=" + siteId + "\t");
        Date d = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        logStr.append("st=" + sdf.format(d)).append("\t");
        // weather
        logStr.append("wea=").append(StringUtils.isBlank(userExtendInfo.getWeather()) ? "" : userExtendInfo.getWeather()).append("\t");
        // sex
        logStr.append("sex=").append(StringUtils.isBlank(userExtendInfo.getSex()) ? "" : userExtendInfo.getSex()).append("\t");
        // ucprefer
        logStr.append("ucprefer=").append(StringUtils.isBlank(userExtendInfo.getUcPrefer()) ? "" : userExtendInfo.getUcPrefer()).append("\t");
        // mixmatch
        Map<String, String> tmpMap = new HashMap<>();
        for (Map.Entry<String, List<Long>> entry : matchDetailMap.entrySet()) {
            if (entry.getValue() == null || entry.getValue().size() == 0) {
                continue;
            }
            tmpMap.put(entry.getKey(), StringUtils.join(entry.getValue(), ","));
        }
        logStr.append("mixmatch=").append(JSON.toJSONString(tmpMap));
        String logString = logStr.toString();
        dcLogger.printDCLog(logString);
    }
}
