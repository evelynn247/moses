package com.biyao.moses.rpc;

import com.alibaba.dubbo.common.utils.CollectionUtils;
import com.alibaba.fastjson.JSON;
import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.model.match.DeepViewProductInfo;
import com.biyao.moses.params.ProductInfo;
import com.biyao.uc.service.UcServerService;
import com.uc.domain.bean.User;
import com.uc.domain.constant.UserFieldConstants;
import com.uc.domain.params.UserRequest;
import com.uc.domain.result.ApiResult;
import com.uc.domain.result.ResultCodeMsg;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @ClassName UcRpcService
 * @Description Uc RPC
 * @Author admin
 * @Date 2020/10/12 17:00
 * @Version 1.0
 **/
@Slf4j
@Component
public class UcRpcService {

    @Autowired
    private UcServerService ucServerService;

    @Autowired
    private ProductDetailCache productDetailCache;

    public User getData(String uuid, String uid, List<String> fields, String caller){
        UserRequest userRequest = new UserRequest();
        userRequest.setUuid(uuid);
        String ucUid = null;
        if(StringUtils.isNotBlank(uid)){
            try{
                int uidTmp = Integer.valueOf(uid);
                if(uidTmp > 0){
                    ucUid = uid;
                }
            }catch (Exception e){
                log.error("[一般异常]传入的uid数据格式不对， uuid {}， uid {}", uuid, uid);
            }
        }
        userRequest.setUid(ucUid);
        userRequest.setCaller(caller);
        userRequest.setFields(fields);
        User result = null;
        ApiResult<User> queryResult = null;
        try{
            queryResult = ucServerService.query(userRequest);
        }catch (Exception e){
            log.error("[严重异常][uc]查询uc出现异常，request {}, e ", JSON.toJSONString(userRequest), e);
            return result;
        }

        if(queryResult != null && ResultCodeMsg.SUCCESS_CODE.equals(queryResult.getCode())
                && queryResult.getData() != null){
            result = queryResult.getData();
        }else{
            log.error("[严重异常][uc]查询uc出现错误，request {}, result {}",
                    JSON.toJSONString(userRequest), queryResult==null ? "null" : JSON.toJSONString(queryResult));
        }
        return result;
    }


    /**
     * 解析格式为 pid:Time
     * @param prasePidAndTime
     * @return  异常情况返回null
     */
    public Long[] prasePidAndTime(String prasePidAndTime) {
        if (StringUtils.isBlank(prasePidAndTime)) {
            return null;
        }
        String[] pidTimeArray = prasePidAndTime.split(":");
        if (pidTimeArray.length != 2
                || StringUtils.isBlank(pidTimeArray[0])
                || StringUtils.isBlank(pidTimeArray[1])) {
            return null;
        }
        String pid = pidTimeArray[0];
        try {
            Long pidL = Long.valueOf(pid);
            Long time = Long.valueOf(pidTimeArray[1]);
            ProductInfo productInfo = productDetailCache.getProductInfo(pidL);
            if (productInfo == null) {
                return null;
            }
            return new Long[]{pidL, time};
        } catch (Exception e) {
            log.error("[严重异常]解析数据异常，入参：{}，异常信息{} ",pidTimeArray, e);
            return null;
        }
    }


    /**
     * 获取用户指定时间范围内深度浏览的商品集合
     * @param uuid
     * @param uid
     * @param caller
     * @param startTime
     * @param endTime
     * @return map  key :pid  value：time
     */
    public List<DeepViewProductInfo> getDeepViewPidsByTime(String uuid, String uid, String caller, long startTime, long endTime){
        List<DeepViewProductInfo> resultList = new ArrayList<>();
        // 获取ucUser
        List<String> viewPids = getDeepViewProduct(uuid, uid, caller);

        if(CollectionUtils.isEmpty(viewPids)){
            return resultList;
        }
        for (String str:viewPids){
            Long[] pidAndTime = prasePidAndTime(str);
            if(pidAndTime==null){
                continue;
            }
            if(pidAndTime[1]<= endTime && pidAndTime[1]>startTime){
                resultList.add(new DeepViewProductInfo(pidAndTime[0],pidAndTime[1]));
            }
        }
        return resultList;
    }

    /**
     * 获取用户多个时间段内的深度浏览商品
     * @param uuid
     * @param uid
     * @param caller
     * @param timesMap  key为时间段的标示   value 为时间段
     * @param
     */
    public Map<String,List<DeepViewProductInfo>> getDeepViewProductMap(String uuid, String uid, String caller, Map<String,long[]> timesMap){

        Map<String,List<DeepViewProductInfo>> resultMap = new HashMap<>();

        List<String> viewPids = getDeepViewProduct(uuid, uid, caller);
        if(CollectionUtils.isEmpty(viewPids)||timesMap.isEmpty()){
            return resultMap;
        }
        for (String str:viewPids) {

            Long[] pidAndTime = prasePidAndTime(str);
            if(pidAndTime==null){
                continue;
            }
            for (String key:timesMap.keySet()){

                if(pidAndTime[1]<= timesMap.get(key)[1] && pidAndTime[1]>timesMap.get(key)[0]){
                    if(resultMap.get(key) == null){
                        List<DeepViewProductInfo> list=new ArrayList<>();
                        list.add(new DeepViewProductInfo(pidAndTime[0],pidAndTime[1]));
                        resultMap.put(key,list);
                    }else {
                        List<DeepViewProductInfo> list=resultMap.get(key);
                        list.add(new DeepViewProductInfo(pidAndTime[0],pidAndTime[1]));
                    }
                }
            }
        }
        return resultMap;
    }

    /**
     * 获取用户近期深度浏览商品
     * @param uuid
     * @param uid
     * @param caller
     * @return
     */
    public List<String> getDeepViewProduct(String uuid, String uid, String caller){
        List<String> resultList = new ArrayList<>();
        User ucUser=null;
        try {
              ucUser = getData(uuid, uid, new ArrayList<String>() {{
                add(UserFieldConstants.VIEWPIDS);
                add(UserFieldConstants.DISINTERESTPIDS);
            }}, caller);
        }catch (Exception e){
            log.error("[严重异常]获取ucUser异常，异常信息：",e);
        }
        if(ucUser != null){
            resultList=ucUser.getViewPids();
        }
        return  resultList;
    }


}
