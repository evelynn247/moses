package com.biyao.moses.rpc;

import com.alibaba.fastjson.JSON;
import com.biyao.magic.dubbo.client.common.Result;
import com.biyao.magic.dubbo.client.toc.repurchase.service.GroupProductToCDubboService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @program: moses-parent
 * @description: 商品组服务
 * @author: changxiaowei
 * @create: 2021-07-12 15:11
 **/
@Slf4j
@Component
public class ProductGroupRpcService {

    @Resource
    GroupProductToCDubboService groupProductToCDubboService;

     public Map<Long,Long> getProductGroupInfo(List<Long> pidList,String uuid){

        try {
            Result<Map<Long, Long>> mapResult = groupProductToCDubboService.queryEffectiveGroupBySpuIds(pidList);

            if(mapResult == null || !mapResult.success){
                log.error("[严重异常][查询商品组服务]出现异常，入参 {}, 返回结果 {} ，uuid={}", JSON.toJSONString(pidList),
                        mapResult == null ? null : JSON.toJSONString(mapResult),uuid);
                return new HashMap<>();
            }
            if(CollectionUtils.isEmpty(mapResult.getData())){
                log.error("[严重异常][查询商品组服务]结果为空，入参 {}, 返回结果 {} ，uuid={}", JSON.toJSONString(pidList),
                         JSON.toJSONString(mapResult),uuid);
            }
            return  mapResult.getData();

        }catch (Exception e){
            log.error("[严重异常][查询商品组服务]出现异常，入参 {},uuid={}，", JSON.toJSONString(pidList),uuid, e);
        }
        return new HashMap<>();
     }

}
