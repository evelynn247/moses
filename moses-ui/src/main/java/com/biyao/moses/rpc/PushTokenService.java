package com.biyao.moses.rpc;

import com.alibaba.fastjson.JSON;
import com.biyao.msgapi.dubbo.client.pushtoken.IPushTokenDubboService;
import com.biyao.msgapi.dubbo.dto.pushtoken.UserConfigDto;
import com.biyao.msgapi.dubbo.param.pushtoken.GetUserConfigParam;
import com.biyao.msgapi.dubbo.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @ClassName PushTokenService
 * @Description 获取个性化推荐开关设置
 * @Author xiaojiankai
 * @Date 2021/1/7 17:00
 * @Version 1.0
 **/
@Slf4j
@Component
public class PushTokenService {

    @Autowired
    private IPushTokenDubboService pushTokenDubboService;

    public boolean getPersonalizedRecommendSwitch(Long uid, String caller){
        //uid不存在，则返回true
        if(uid == null || uid == 0){
            return true;
        }

        boolean result = true;
        GetUserConfigParam getUserConfigParam = new GetUserConfigParam();
        getUserConfigParam.setUid(uid);
        //type为1 表示查询个性化推荐开关
        getUserConfigParam.setType(1);
        getUserConfigParam.setCaller(caller);
        try{
            Result<UserConfigDto> userConfig = pushTokenDubboService.getUserConfig(getUserConfigParam);
            if(userConfig != null && userConfig.isSuccess()){
                UserConfigDto userConfigDto = userConfig.getObj();
                if(userConfigDto != null){
                    if(userConfigDto.getConfig() == null){
                        //为空 表示未设置，未设置时默认为开启状态
                        result = true;
                    }else {
                        result = (userConfigDto.getConfig() == 0) ? false : true;
                    }
                }else{
                    //异常场景设置为false
                    result = false;
                    log.error("[严重异常][个性化推荐开关]查询个性化推荐开关出现错误，request {}, result {}",
                            JSON.toJSONString(getUserConfigParam), JSON.toJSONString(userConfig));
                }
            }else{
                //异常场景设置为false
                result = false;
                log.error("[严重异常][个性化推荐开关]查询个性化推荐开关出现错误，request {}, result {}",
                        JSON.toJSONString(getUserConfigParam), JSON.toJSONString(userConfig));
            }
        }catch (Exception e){
            log.error("[严重异常][个性化推荐开关]查询个性化推荐开关时出现异常，request {}, e ", JSON.toJSONString(getUserConfigParam), e);
            //异常场景设置为false
            result = false;
        }

        return result;
    }
}
