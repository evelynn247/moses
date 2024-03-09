package com.biyao.moses.service.rpc;

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
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @program: moses-parent-online
 * @description:
 * @author: changxiaowei
 * @create: 2021-09-23 16:32
 **/
@Service
@Slf4j
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
     * 获取用户不感兴趣的商品
     */
    public Set<Long> getUninterested(String uuid){
        Set<Long> uninterested = new HashSet<>();
        List<String> filedList = new ArrayList<>();
        filedList.add(UserFieldConstants.DISINTERESTPIDS);
        User ucUser = getData(uuid,null, filedList, "mosesmatchonline");
        if(ucUser!=null && CollectionUtils.isNotEmpty(ucUser.getDisinterestPids())){
            uninterested = ucUser.getDisinterestPids();
        }
        return uninterested;
    }
}
