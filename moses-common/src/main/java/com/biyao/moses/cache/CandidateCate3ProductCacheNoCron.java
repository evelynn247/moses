package com.biyao.moses.cache;

import com.biyao.moses.common.constant.CommonConstants;
import com.biyao.moses.common.constant.MatchRedisKeyConstant;
import com.biyao.moses.common.constant.UPCUserTypeConstants;
import com.biyao.moses.common.enums.SeasonEnum;
import com.biyao.moses.util.MatchRedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * @ClassName CandidateCate3ProductCacheNoCron
 * @Description 不同用户类型候选三级类目组及其候选商品缓存
 * @Author xiaojiankai
 * @Date 2019/11/29 18:05
 * @Version 1.0
 **/
@Slf4j
public class CandidateCate3ProductCacheNoCron {
    //老客、老访客、新访客的候选后台三级类目组集合和候选商品集合Map
    private Map<String, List<Long>> customerMap = new LinkedHashMap<>();
    private Map<String, List<Long>> oldVisitorMap = new LinkedHashMap<>();
    private Map<String, List<Long>> newVisitorMap = new LinkedHashMap<>();

    @Autowired
    private MatchRedisUtil matchRedisUtil;

    protected void init(){
      log.info("初始化候选三级类目ID组集合及其下的候选商品集合开始");
      refreshCache();
      log.info("初始化候选三级类目ID组集合及其下的候选商品集合结束");
    }

    public void refreshCache(){
        log.info("刷新候选三级类目ID组集合及其下的候选商品集合开始");
        Map<String, List<Long>> customerMapTmp = refreshByRedisKey(MatchRedisKeyConstant.CANDIDATE_CATE3_PID_CUSTOMER);
        if(customerMapTmp != null && customerMapTmp.size() > 0){
            customerMap = customerMapTmp;
        }
        Map<String, List<Long>> oldVisitorMapTmp = refreshByRedisKey(MatchRedisKeyConstant.CANDIDATE_CATE3_PID_OLD_VISITOR);
        if(oldVisitorMapTmp != null && oldVisitorMapTmp.size() > 0){
            oldVisitorMap = oldVisitorMapTmp;
        }
        Map<String, List<Long>> newVisitorMapTmp = refreshByRedisKey(MatchRedisKeyConstant.CANDIDATE_CATE3_PID_NEW_VISITOR);
        if(newVisitorMapTmp != null && newVisitorMapTmp.size() > 0){
            newVisitorMap = newVisitorMapTmp;
        }
    }

    /**
     * 通过redis key刷新候选类目集合及商品集合
     * @param redisKey
     */
    private Map<String, List<Long>> refreshByRedisKey(String redisKey){
        Map<String, List<Long>> result = new LinkedHashMap<>();
        try {
            String candidateCate3ProductStr = matchRedisUtil.getString(redisKey);
            result = parseCandidateCate3ProductStr(candidateCate3ProductStr);
            if (CollectionUtils.isEmpty(result)) {
                log.error("候选三级类目ID组集合及其下的候选商品集合数据为空，redisKey {}", redisKey);
            }
            log.info("刷新候选三级类目ID组集合及其下的候选商品集合结束, redisKey {}", redisKey);

        }catch(Exception e){
            log.info("[严重异常]刷新候选三级类目ID组集合及其下的候选商品集合异常, redisKey {}", redisKey);
        }
        return result;
    }

    /**
     * 通过用户类型获取候选三级类目集合和候选商品集合信息
     * @param upcUserType
     * @return
     */
    public Map<String, List<Long>> getCacheMap(Integer upcUserType){
        if(upcUserType == null){
            return null;
        }
        if(UPCUserTypeConstants.CUSTOMER == upcUserType){
            return customerMap;
        }else if(UPCUserTypeConstants.OLD_VISITOR == upcUserType){
            return oldVisitorMap;
        }else if(UPCUserTypeConstants.NEW_VISITOR == upcUserType){
            return newVisitorMap;
        }
        return null;
    }

    /**
     * 格式为：category3:productid,productid…|category3:productid,productid,productid,productid
     * @param candidateCate3ProductStr
     * @return
     */
    private Map<String, List<Long>> parseCandidateCate3ProductStr(String candidateCate3ProductStr){
        Map<String, List<Long>> result = new LinkedHashMap<>();
        if(StringUtils.isBlank(candidateCate3ProductStr)){
            return result;
        }

        boolean isCheckError = false;
        String[] candidateCate3ProductArray = candidateCate3ProductStr.trim().split("\\|");
        for(String candidateCate3Product : candidateCate3ProductArray){
            String[] cate3AndProductStr = candidateCate3Product.trim().split(":");
            //如果长度不为2，则过滤掉该数据
            if(cate3AndProductStr.length != 2){
                isCheckError = true;
                continue;
            }
            String candidateCate3 = cate3AndProductStr[0].trim();
            String productIdsStr = cate3AndProductStr[1].trim();
            String[] productIdArray = productIdsStr.split(",");
            List<Long> productIdList = new ArrayList<>();
            for(String productIdStr : productIdArray){
                if(StringUtils.isBlank(productIdsStr)){
                    isCheckError = true;
                    continue;
                }
                try{
                    Long productId = Long.valueOf(productIdStr);
                    productIdList.add(productId);
                }catch(Exception e){
                    isCheckError = true;
                }
            }
            result.put(candidateCate3, productIdList);
        }

        if(isCheckError){
            log.error("[严重异常]候选后台三级类目组及其候选商品数据格式不符合要求");
        }
        return result;
    }
}
