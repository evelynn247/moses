package com.biyao.moses.rpc;

import com.alibaba.fastjson.JSON;
import com.biyao.uc.service.UcServerService;
import com.uc.domain.bean.User;
import com.uc.domain.constant.UserFieldConstants;
import com.uc.domain.params.UserRequest;
import com.uc.domain.result.ApiResult;
import com.uc.domain.result.ResultCodeMsg;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
     * 获取用户30天已购买的商品
     * @param uid
     * @return
     */
    public Set<Long> getOrderPids30d(Integer uid){

        List<String> list = new ArrayList<>();
        list.add(UserFieldConstants.ORDERPIDS30D);
        User user = getData(null, uid.toString(), list, "moses");
        if (user == null) {
            return null;
        }
        return user.getOrderPids30d();
    }

    /**
     * 获取用户近期购买的商品
     * @param uid
     * @return
     */
    public List<String> getOrderPids(Integer uid){
        List<String> list = new ArrayList<>();
        list.add(UserFieldConstants.ORDERPIDS);
        User user = getData(null, uid.toString(), list, "moses");
        if (user == null) {
            return null;
        }
        return user.getOrderPids();
    }

    public String getUserSeason(String uuid){
            List<String> fields = new ArrayList<>();
            //用户季节
            fields.add(UserFieldConstants.SEASON);
            User user = getData(uuid, null, fields, "mosesmatch");
            if (user == null) {
                return "";
            }
            return user.getSeason();
        }


    public String getUserSexFromUc(String uid,String uuid) {
        String result = com.biyao.moses.common.constant.CommonConstants.UNKNOWN_SEX;
        if("0".equals(uid) && StringUtils.isBlank(uuid)){
            log.error("[严重异常]headerFilter获取用户性别，uuid 和 uid都为空");
            return result;
        }
        try {
            List<String> fields = new ArrayList<>();
            fields.add(UserFieldConstants.SEX);
            String uidParam = "0".equals(uid) ? null : uid;
            User user = getData(uuid, uidParam, fields, "moses");
            if (user != null && user.getSex() != null) {
                result = user.getSex().toString();
            }
        }catch (Exception e){
            log.error("[严重异常]获取用户性别异常， uuid {}, uid {}, e ", uuid, uid, e);
        }
        return result;
    }

    /**
     * 获取用户近两分钟内深度浏览的商品
     * @param uuid
     * @param uid
     * @return
     */
   public Long getUser2minViewPid(String uuid,String uid){
        List<String> fields = new ArrayList<>();
        fields.add(UserFieldConstants.VIEWPIDS0D);
        User ucUser = getData(uuid, uid, fields, "moses");
        if(ucUser != null && !CollectionUtils.isEmpty(ucUser.getViewPids())) {
            List<String> viewPid = ucUser.getViewPids();
            try {
                String pidTime = viewPid.get(viewPid.size() - 1);
                String[] pidTimeArr = pidTime.split(":");
                if(System.currentTimeMillis()-120000 < Long.valueOf(pidTimeArr[1])){
                    return Long.valueOf(pidTimeArr[0]);
                }
            }catch (Exception e){
                log.error("[严重异常]获取uc用户近两分钟深度浏览的商品异常，uuid:{},异常信息:",uuid,e);
                return null;
            }
        }
        return null;
    }
}
