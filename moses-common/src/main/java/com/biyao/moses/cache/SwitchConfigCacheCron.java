package com.biyao.moses.cache;
import com.alibaba.fastjson.JSON;
import com.biyao.client.model.MaterialConfig;
import com.biyao.client.model.PDCResponse;
import com.biyao.client.service.ISwitchConfigDubboService;
import com.biyao.moses.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


@Slf4j
public class SwitchConfigCacheCron {
    @Resource
    ISwitchConfigDubboService switchConfigDubboService;

    private static final String DEFAULT_VALUE_ZERO = "0";
    private Map<String, MaterialConfig> switchConfigMap = new HashMap<>();

    protected void init(){
        refreshProductDetailCache();
    }

    protected void refreshProductDetailCache() {
        try {
            switchConfigMap = getAllSwitchConfigMap();
        } catch (Exception e) {
            log.error("[严重异常][所有商品缓存]获取商品信息失败，",e);
        }
    }
    private Map<String, MaterialConfig> getAllSwitchConfigMap(){
        Map<String, MaterialConfig> resultMap =new HashMap<>();
        try {
            PDCResponse<Map<String, MaterialConfig>> response = switchConfigDubboService.getAllSwitchConfig();
            if (null == response || response.getCode() != 1) {
                log.error("[严重异常][开关配置缓存]调用ISwitchConfigDubboService.switchConfigDubboService方法报错，response:{}",
                        JSON.toJSONString(response));
                return resultMap;
            }
            resultMap= response.getData();
        }catch (Exception e){
            log.error("[严重异常][dubbo异常]获取全量推荐配置开关异常(接口ISwitchConfigDubboService.getAllSwitchConfig)接口时发生异常,异常堆栈信息：",e);
        }
       return resultMap;
    }

    /**
     * 获取推荐配置内容
     * @param configId  唯一配置id
     * 对于开关类配置 仅启用状态的配置且配置的内容为0时为关  其余情况默认为开 如：app1.5.1中推荐个性化话活动开关
     * 对于配置不为开关的属性 为启用状态配置的值  否则返回空字符串 走默认值  如：推荐V2.23中感兴趣商品集活动召回商品数量下限的配置 N
     * @return
     */
    public String getRecommendContentByConfigId(String configId){
        String result="";
        if(!CollectionUtils.isEmpty(switchConfigMap) && Objects.nonNull(switchConfigMap.get(configId))){
            result=switchConfigMap.get(configId).getConfigContent();
        }
        return result;
    }

    /**
     * 获取素材配置中的开关属性
     * @param configId  业务唯一标识
     * @return false 表示关
     */
    public Boolean getRecommendSwitchByConfigId(String configId){
        return !DEFAULT_VALUE_ZERO.equals(getRecommendContentByConfigId(configId));
    }

    /**
     * 获取rsm 配置的默认int值
     * @param configId 配置id
     * @param defaultValue 默认值
     * @return
     */
    public int getRsmIntValue(String configId,int defaultValue,int maxValue,int minValue){
        String intValue = getRecommendContentByConfigId(configId);
        if(!StringUtil.isInteger(intValue)){
            return defaultValue;
        }
       int value = Integer.valueOf(intValue);
        //  没有取值范围的  maxValue = minValue
       if(maxValue != minValue && (value > maxValue || value<minValue)){
           return  defaultValue;
       }
       return Integer.valueOf(intValue);
    }

    /**
     * 获取rsm 配置的默认float值
     * @param configId 配置id
     * @param defaultValue 默认值
     * @return
     */
    public float getRsmFloatValue(String configId,float defaultValue,int maxValue,int minValue){
        String floatValue = getRecommendContentByConfigId(configId);
        try {
            float value = Float.valueOf(floatValue);
            if(maxValue != minValue && (value > maxValue || value<minValue)){
                return  defaultValue;
            }
            return  value;
        }catch (Exception e){
            log.error("[严重异常]rsm配置的值转化成浮点数异常，取默认值，配置id：{},默认值：{}",configId,defaultValue);
        }
        return defaultValue;
    }

    public float getRsmLongValue(String configId,long defaultValue,int maxValue,int minValue){
        String longValue = getRecommendContentByConfigId(configId);
        try {
            long value = Long.valueOf(longValue);
            if(maxValue != minValue && (value > maxValue || value<minValue)){
                return  defaultValue;
            }
            return  value;
        }catch (Exception e){
            log.error("[严重异常]rsm配置的值转化成Long类型异常，取默认值，配置id：{},默认值：{}",configId,defaultValue);
        }
        return defaultValue;
    }
}
