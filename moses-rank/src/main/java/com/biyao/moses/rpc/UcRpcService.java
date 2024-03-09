package com.biyao.moses.rpc;

import com.alibaba.fastjson.JSON;
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
import java.util.List;

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


    public User getUcUserForRank(Integer uid,String uuid,String caller){

        List<String> fieldsList = new ArrayList<>();
        fieldsList.add(UserFieldConstants.PERSONALSIZE);
        fieldsList.add(UserFieldConstants.EXPPIDS);
        String uidStr = null;
        if(uid != null && uid > 0){
            uidStr = uid.toString();
        }
        User ucUser = getData(uuid, uidStr, fieldsList, caller);

        return ucUser;
    }
}
