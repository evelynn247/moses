package com.biyao.moses.rank.impl;

import com.alibaba.fastjson.JSON;
import com.biyao.moses.cache.ProductDetailCacheNoCron;
import com.biyao.moses.cache.ProductMustSizeCache;
import com.biyao.moses.common.constant.RedisKeyConstant;
import com.biyao.moses.common.constant.UPCUserTypeConstants;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.params.ProductInfo;
import com.biyao.moses.rank.RankRequest;
import com.biyao.moses.rank.RecommendRank;
import com.biyao.moses.service.RedisAnsyService;
import com.biyao.moses.util.CacheRedisUtil;
import com.biyao.moses.util.RedisUtil;
import com.biyao.uc.service.UcServerService;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import com.google.common.base.Splitter;
import com.uc.domain.bean.User;
import com.uc.domain.constant.UserFieldConstants;
import com.uc.domain.params.UserRequest;
import com.uc.domain.result.ApiResult;
import com.uc.domain.result.ResultCodeMsg;
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

@Slf4j
@Component("cr")
public class FrontendCategoryRank implements RecommendRank {

    @Resource
    RedisUtil redisUtil;

    @Autowired
    private CacheRedisUtil cacheRedisUtil;

    @Autowired
    RedisAnsyService redisAnsyService;

    @Autowired
    ProductDetailCacheNoCron productDetailCacheNoCron;

    @Autowired
    UcServerService ucServerService;

    @Autowired
    ProductMustSizeCache productMustSizeCache; //黄金尺码是否充足缓存

    //前台类目ID,用于对特定前台类目做分流实验
    @Value("${exp.frontend.categoryid}")
    private String expFrontendCategoryId;

    @BProfiler(key = "FrontendCategoryRank.executeRecommend", monitorType = {
            MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR })
    @Override
    public List<TotalTemplateInfo> executeRecommend(RankRequest rankRequest) {

        List<TotalTemplateInfo> oriData = rankRequest.getOriData();
        try {
            //入参校验，如果待排序的商品为空，则直接返回
            if(CollectionUtils.isEmpty(oriData)){
                log.error("[严重异常]类目页排序-待排序的商品信息为空，rankRequest {}", JSON.toJSONString(rankRequest));
                return oriData;
            }

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date());
            calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - 3);
            long dayUp3 = calendar.getTime().getTime();

            //获取参数
            String dataNum = rankRequest.getDataNum();
            Integer upcUserType = rankRequest.getUpcUserType() == null ? UPCUserTypeConstants.NEW_VISITOR: rankRequest.getUpcUserType();
            String frontendCategoryId = rankRequest.getFrontendCategoryId();
            String uuid = rankRequest.getUuid();

            //获取pid集合
            String[] pids = new String[oriData.size()];
            for (int i = 0; i < oriData.size(); i++) {
                pids[i] = oriData.get(i).getId();
            }
            //获取后置商品集合
            Set<String> postList = new HashSet<>(); //后置集合
            Set<String> exposureSet = cacheRedisUtil.smems("moses:user_category_" + uuid + "_" + frontendCategoryId);//曝光
            List<String> browseList = redisUtil.lrange("moses:user_viewed_products_" + uuid, 0, -1);//浏览

            //UC获取有效浏览集合
            ApiResult<User> result = new ApiResult<>();
            try{
                UserRequest ur = new UserRequest();
                ur.setUuid(uuid);
                ur.setCaller("mosesrank.biyao.com");
                List<String> fields = new ArrayList<>();
                fields.add(UserFieldConstants.VIEWPIDS3D);
                ur.setFields(fields);
                result =ucServerService.query(ur);
            }catch(Exception e){
                log.error("uc获取数据异常："+e.getMessage());
            }
            //循环判断UC获取有效浏览集合中本类目页下的商品
            Set<String> deepBrowseSet = new HashSet<>();
            if (result != null) {
                if (result.getCode() == ResultCodeMsg.SUCCESS_CODE && result.getData() != null) {
                    List<Long> threeDayViewPids = result.getData().getViewPids3d();
                    List<String> pidsList = Arrays.asList(pids);
                    if (CollectionUtils.isNotEmpty(threeDayViewPids)) {
                        for (Long l : threeDayViewPids) {
                            if (pidsList.contains(l.toString())) {
                                deepBrowseSet.add(l.toString());
                            }
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
                    String similarList = redisUtil.hget("moses:similar_product",p);
                    if(StringUtils.isNotBlank(similarList)){
                        topSet.add(similarList.substring(0, similarList.indexOf(":")));//放入一个相似商品，取第一个分最高的
                        //log.info("置顶足迹相似商品的pid："+similarList.substring(0, similarList.indexOf(":")));
                    }
                    break;
                }
            }


            Map<String, String> productScore1 = null;
            Map<String, String> productScore2 = null;

            String realDataNum = dataNum;
            /****************start*********************/
            //根据前台类目做分流实验
            if(StringUtils.isNotBlank(frontendCategoryId) && StringUtils.isNotEmpty(expFrontendCategoryId) &&
            		Splitter.on(",").trimResults().splitToList(expFrontendCategoryId).contains(frontendCategoryId)) {
            	//获取新品静态分
                productScore1 = getProductScore("获取新品静态分", "new_product_category", "new_product_category", String.valueOf(upcUserType), "score");
//                log.error("uuid={},获取es商品静态分数据个数为{} 新品实验",uuid,productScore1.size());
            } else {
                if("0200".equals(dataNum)) {
                    //获取用户个性化商品静态分
                    productScore1 = getProductScore("获取用户个性化商品静态分", "user_score", "sli_score", uuid, rankRequest.getCategoryIds());
//                    log.error("uuid={},获取es商品静态分数据个数为{}", uuid, productScore1.size());
                }else{
                    realDataNum = "0100";
                    //获取商品分数（redis）
                    Future<HashMap<String, String>> future1 = redisAnsyService.redisHmget("moses:cr_up_" + realDataNum + "_" + uuid, pids);
                    Future<HashMap<String, String>> future2 = redisAnsyService.redisHmget("moses:cr_p_" + frontendCategoryId + "_" + realDataNum + "_" + upcUserType, pids);
                    try {
                        productScore1 = future1.get(20, TimeUnit.MILLISECONDS);
                    } catch (Exception e) {
                        log.error("productScore1 redis获取分数超时");
                    }

                    try {
                        productScore2 = future2.get(20, TimeUnit.MILLISECONDS);
                    } catch (Exception e) {
                        log.error("productScore2 redis获取分数超时");
                    }
                }
            } 
            /****************end*********************/
            //全局静态分
            Future<HashMap<String, String>> future3 = redisAnsyService.redisHmget("moses:rs_p_dr_" + realDataNum, pids);
            HashMap<String, String> productScore3 = new HashMap<>();
            log.error("@@*@@分流实验号:dateNum {}, realDataNum {}",dataNum, realDataNum);

            try {
                productScore3 = future3.get(20, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                log.error("productScore3 redis获取分数超时");
            }
            //获取不到静态分时，则return
            if((productScore1 == null || productScore1.size() == 0)
                    && (productScore3 == null || productScore3.size() == 0)
                    && (productScore2 == null || productScore2.size() == 0)) {

            	log.error("uuid={},frontendCategoryId={},categoryIds={},dataNum={},获取静态分结果为空",uuid, frontendCategoryId, rankRequest.getCategoryIds(),dataNum);
            	return oriData;
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
            Map<TotalTemplateInfo,Date> NewProtectMap = new HashMap<>();

            //设置商品分&获取新手保护策略商品
            for (TotalTemplateInfo info : oriData) {
                String calculateScore = "0.0";
                if (productScore1.containsKey(info.getId())) {
                    calculateScore = productScore1.getOrDefault(info.getId(),"0.0");
                }else if(productScore2.containsKey(info.getId())){
                    calculateScore = productScore2.getOrDefault(info.getId(),"0.0");
                }else if(productScore3.containsKey(info.getId())){
                    calculateScore = productScore3.getOrDefault(info.getId(),"0.0");
                }

                try{
                    if(StringUtils.isNotBlank(info.getId())){
                        ProductInfo productInfo =  productDetailCacheNoCron.getProductInfo(Long.valueOf(info.getId()));
                        if(productInfo.getFirstOnshelfTime() != null){
                            if(isWithin(dayUp3,String.valueOf(productInfo.getFirstOnshelfTime().getTime()))){
                                NewProtectMap.put(info,productInfo.getFirstOnshelfTime());
                            }
                        }
                    }
                    info.setScore(Double.valueOf(calculateScore));
                }
                catch(Exception e){
                    log.error("[严重异常]类目页rank，商品分转换失败，失败score："+calculateScore);
                    info.setScore(0.0); //设置为默认值
                }
            }


            //排序
            Collections.sort(oriData, new Comparator<TotalTemplateInfo>() {
                @Override
                public int compare(TotalTemplateInfo o1, TotalTemplateInfo o2) {
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
            if(rankRequest.getUpcUserType() == 1 && StringUtils.isNotBlank(rankRequest.getUid())){
                List<TotalTemplateInfo> setTopList = new ArrayList<>();
                String lastUploadTime = redisUtil.getString(RedisKeyConstant.LAST_LOGIN_TIME +rankRequest.getUid()); //上次登录时间(天)

                //筛选置顶新品
                if(StringUtils.isNotBlank(lastUploadTime)){
                    for (Map.Entry<TotalTemplateInfo, Date> entry : NewProtectMap.entrySet()) {
                        if(entry.getValue().getTime()>Long.parseLong(lastUploadTime)){
                            setTopList.add(entry.getKey());
                        }
                    }
                }else{
                    for (Map.Entry<TotalTemplateInfo, Date> entry : NewProtectMap.entrySet()) {
                        setTopList.add(entry.getKey());
                    }
                }

                //置顶
                oriData.removeAll(setTopList);
                List<TotalTemplateInfo> tempList = new ArrayList<>();
                tempList.addAll(setTopList);
                tempList.addAll(oriData);
                oriData.clear();
                oriData.addAll(tempList);
            }


            //足迹商品置顶
            if(topSet.size() != 0){
                List<TotalTemplateInfo> topProductList = new ArrayList<>();
                for (TotalTemplateInfo t:topProductList) {
                    if(topSet.contains(t.getId())){
                        topProductList.add(t);
                    }
                }
                oriData.removeAll(topProductList);
                List<TotalTemplateInfo> tempList = new ArrayList<>();
                tempList.addAll(topProductList);
                tempList.addAll(oriData);
                oriData.clear();
                oriData.addAll(tempList);

            }

            //曝光未点击后置
            List<TotalTemplateInfo> setBottomList = new ArrayList<>();
            if (postList.size() != 0) {
                for (TotalTemplateInfo info : oriData) {
                    if (postList.contains(info.getId())) {
                        setBottomList.add(info);
                    }
                }

            }
            oriData.removeAll(setBottomList);
            oriData.addAll(setBottomList);

            //置底处理黄金尺码不足商品(新品受保护)
            dealWithMustSizeProduct(oriData, NewProtectMap);
        } catch (Exception e) {
            log.error("[严重异常]类目页rank未知错误，rankRequest {}，", JSON.toJSONString(rankRequest), e);
        }
        return oriData;
    }

    /**
     * 置底处理黄金尺码不足商品(新品受保护)
     * @param oriData
     * @param newProtectMap
     */
    private void dealWithMustSizeProduct(List<TotalTemplateInfo> oriData, Map<TotalTemplateInfo, Date> newProtectMap) {
        List<TotalTemplateInfo> mustSizeNotFullList = new ArrayList<>(); //黄金尺码不足集合
        if(CollectionUtils.isNotEmpty(oriData)){
            Iterator<TotalTemplateInfo> it = oriData.iterator();
            while (it.hasNext()){
                TotalTemplateInfo tti = it.next();
                //新品 受保护
                if(tti!=null && newProtectMap.get(tti)!=null){
                    continue;
                }
                //筛选出黄金尺码不足的
                boolean isFull = productMustSizeCache.isMustSizeFull(Long.parseLong(tti.getId()));
                if(!isFull){
                    mustSizeNotFullList.add(tti);
                    it.remove();
                }
            }
            oriData.addAll(mustSizeNotFullList);
        }
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
            log.error("浏览商品时间戳转换异常，timeStamp="+time);
        }
        return result;
    }

    /**
     * 	es中获取商品分
     * @param esDescLog
     * @param esIndex
     * @param esType
     * @param esId
     * @param fetchSourceIncludes
     * @return
     */
    public Map<String, String> getProductScore(String esDescLog, String esIndex, String esType, String esId, String fetchSourceIncludes){

    	Map<String,  String> result = new HashMap<>();
//    	if (StringUtils.isEmpty(esId) || StringUtils.isEmpty(fetchSourceIncludes)) {
//    		return result;
//		}
//
//    	try {
//
//    		TransportClient transportClient = ESClientConfig.getESClient();
//
//        	GetResponse response = transportClient.prepareGet(esIndex, esType, esId).setFetchSource(fetchSourceIncludes.split(","), null).execute().actionGet(20);
//
//        	//es返回结果判空验证
//        	if(response == null || response.getSource() == null || response.getSource().entrySet() == null ) {
//        		return result;
//        	}
//
//        	for (Map.Entry<String,Object> item :response.getSource().entrySet()){
//        		result.putAll( parseProductStr( item.getValue().toString())  );
//        	}
//		} catch (Exception e) {
//			log.error("{}异常,esId={},categoryIds={}",esDescLog, esId, fetchSourceIncludes, e);
//		}
    	return result;
    }

	/**
	 * products格式为：pid:score,pid:score,...,pid:score; pid存放时是有序的，前面的pid的分值比后面的高。
	 * 若pid有多个相同时，则该pid的分值为第一次出现时的分值。
	 *
	 * @param productStr
	 * @return
	 */
	public static Map<String, String> parseProductStr(String productStr) {
		Map<String, String> result = new HashMap<>();

		String[] pidScores = productStr.split(",");
		for (String pidScore : pidScores) {
			if (StringUtils.isBlank(pidScore)) {
				continue;
			}
			try {
				String[] pidAndScore = pidScore.split(":");
				String productIdStr;
				String scoreStr = null;
				if (pidAndScore.length == 1) {
					productIdStr = pidAndScore[0];
				} else if (pidAndScore.length == 2) {
					productIdStr = pidAndScore[0];
					scoreStr = pidAndScore[1];
				} else {
					log.error("解析商品ID信息时发生错误,pidScore {}", pidScore);
					continue;
				}
				Double score;
				if (StringUtils.isBlank(scoreStr)) {
					score = 0d;
				} else {
					score = Double.valueOf(scoreStr);
				}
				Long productId = Long.valueOf(productIdStr);
				result.put(String.valueOf(productId), score.toString());
			} catch (Exception e) {
				log.error("解析商品ID信息时发生异常,product {}, e {}", pidScore, e);
			}
		}
		return result;
	}
}
