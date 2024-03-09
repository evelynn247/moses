package com.biyao.moses.util;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 处理特征中函数工具类
 */
@Slf4j
public class OnlineParseUtil {
    private static Double DEFAULT_DOUBLE_VALUE = -999999999.0;

    /**
     * @description: 划分阈值,返回下标,出错时返回null
     * @param feaKey 当前feature名称
     * @param value	当前feature原始值
     * @return
     * @author: luozhuo
     * @date: 2017年7月26日 下午7:02:36
     */
    public static Integer split(String paramName,Double originalValue, List<Double> threshold) {
        if(threshold == null) {
            return null;
        }

        if(DEFAULT_DOUBLE_VALUE.compareTo(originalValue) == 0) {
            return null;
        }

        int i = 0;
        for(; i < threshold.size(); i++) {
            double value = threshold.get(i);
            if(originalValue.compareTo(value) <= 0) {
                return i;
            }
        }
        return i;

    }

    public static Double countIsin(String key, JSONObject jsonObject) throws Exception {
        if (jsonObject == null) {
            throw new Exception("countIsin value为空， A key：" + key);
        }

        Double result = jsonObject.getDouble(key);
        return result;
    }

    public static Double clkOrderRate(Double denominator, Double numerator) {
        if(DEFAULT_DOUBLE_VALUE.compareTo(denominator) == 0 || denominator.intValue() < 20) {
            return null;
        }else if(DEFAULT_DOUBLE_VALUE.compareTo(numerator) == 0) {
            return 0.0;
        }else {
            return numerator/denominator;
        }
    }

    public static Double log1Norm(Double x) {
        if( x == null || x < 0 ) {
            return 0.0;
        }

        return Math.log(x+1)/Math.log(2);
    }
}