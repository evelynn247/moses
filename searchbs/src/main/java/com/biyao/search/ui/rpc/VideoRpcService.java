package com.biyao.search.ui.rpc;


import com.alibaba.fastjson.JSON;
import com.biyao.product2c.dubbo.client.spu.ISpuToCService;
import com.biyao.product2c.dubbo.param.spu.SpuIdSetQueryParam;
import com.biyao.product2c.dubbo.result.Result;
import com.biyao.search.ui.constant.CommonConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @desc:  由于衍生商品横插商品数设为0，该接口不考虑衍生商品
 * @author: xiafang
 * @date: 2021/7/6
 */
@Slf4j
@Service
public class VideoRpcService {
    @Autowired
    private ISpuToCService spuToCService;
    /**
     * RPC接口查询商品是否展示视频标识
     *
     * @param pidSet
     * @return
     */
    public Map<Long, Boolean> getProductsVideoStatus(Set<Long> pidSet) {

        Map<Long, Boolean> resultMap = new HashMap<>(pidSet.size());
        SpuIdSetQueryParam spuIdSetQueryParam = new SpuIdSetQueryParam();
        spuIdSetQueryParam.setSpuIdSet(pidSet);
        spuIdSetQueryParam.setCaller(CommonConstant.SYSTEM_NAME);
        try {
            //调用RPC接口
            Result<Map<Long, Boolean>> rpcResult = spuToCService.getVideoStatusBySpuIds(spuIdSetQueryParam);

            //判断接口请求数据是否有效
            if (rpcResult == null) {
                log.error("[一般异常]［同步商品视频标识］接口返回null,入参:{}", JSON.toJSONString(spuIdSetQueryParam));
                return resultMap;
            }
            if (!rpcResult.isSuccess()) {
                log.error("[一般异常]［同步商品视频标识］接口调用失败,入参:{},结果：{}", JSON.toJSONString(spuIdSetQueryParam), JSON.toJSONString(rpcResult));
                return resultMap;
            }
            resultMap = rpcResult.getObj();
            if (resultMap == null || resultMap.size() == 0) {
                log.info("[操作日志]同步商品视频标识数据接口调用正常,接口返回数据为空，入参:{}，结果：{}", JSON.toJSONString(spuIdSetQueryParam), JSON.toJSONString(rpcResult));
                return resultMap;
            }
        } catch (Exception e) {
            log.error("[一般异常]［直播管理中心同步直播中商品数据］接口error", e);
        }
        return resultMap;
    }
}
