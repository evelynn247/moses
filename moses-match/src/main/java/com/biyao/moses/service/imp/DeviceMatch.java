package com.biyao.moses.service.imp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.biyao.moses.common.constant.CommonConstants;
import com.biyao.moses.model.exp.MatchDataSourceTypeConf;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.service.RecommendMatch;
import com.biyao.moses.util.ProductWeightUtil;
import com.biyao.moses.util.RedisUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * 商品类目match
 *
 * @Description
 * @author yl
 * @Date 2019年4月4日
 */
@Slf4j
@Component("Device")
public class DeviceMatch implements RecommendMatch {

    @Autowired
    DefaultMatch defaultMatch;
    @Autowired
    RedisUtil redisUtil;

    @BProfiler(key = "com.biyao.moses.service.imp.DeviceMatch.executeRecommendMatch",
            monitorType = {MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR})
    @Override
    public Map<String, List<TotalTemplateInfo>> executeRecommendMatch(String dataKey, MatchDataSourceTypeConf mdst, String uuId) {

        Map<String, List<TotalTemplateInfo>> resultMap = new HashMap<>();
        try{
            String device = mdst.getDevice();
            device = this.processDevice(device);
           
            String products = redisUtil.hget(dataKey, device);
            if(!mdst.isPersonalizedRecommendActSwitch()|| StringUtils.isBlank(products)){
                // 查询device无数据，执行兜底方法
                products = redisUtil.hget(dataKey,CommonConstants.DEVICE_DEFAULT_NAME);
            }
            List<TotalTemplateInfo> totalList = new ArrayList<>();
            String[] productList = products.split(",");
            for(String productAndScore: productList){
                TotalTemplateInfo totalTemplateInfo = new TotalTemplateInfo();
                totalTemplateInfo.setId(productAndScore.split(":")[0]);
                totalTemplateInfo.setScore(Double.parseDouble(StringUtils.isEmpty(productAndScore.split(":")[1])?"0":productAndScore.split(":")[1]));
                totalList.add(totalTemplateInfo);
            }
        	//计算权重
			ProductWeightUtil.calculationWeight(mdst.getWeight(), totalList);
            resultMap.put(dataKey, totalList);
        }catch (Exception e){
            log.error("[严重异常]DeviceMatch获取机型数据出现异常，uuid {} ", uuId, e);
        }
        return resultMap;
    }

    /**
     * 设备型号标准化
     * @param device
     * @return
     */
    private String processDevice(String device){
        String result = device.replace("%20", "")
                .replace("-", "")
                .replace("_", "")
                .replace(",", "")
                .replace("%2C", "")
                .replace(" ", "").toLowerCase();
        String preBracket = "(";
        String lessThan = "<";
        if (result.indexOf(preBracket) > 0){
            int pos = result.indexOf(preBracket);
            result = result.substring(0, pos);
        }

        if (result.indexOf(lessThan) > 0){
            int pos = result.indexOf(lessThan);
            result = result.substring(0, pos);
        }
        return result;
    }
}
