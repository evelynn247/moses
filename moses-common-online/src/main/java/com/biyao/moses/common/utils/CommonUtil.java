package com.biyao.moses.common.utils;

/**
 * @program: moses-parent-online
 * @description: 通用工具类
 * @author: changxiaowei
 * @Date: 2022-02-23 11:07
 **/


import com.alibaba.fastjson.JSONObject;

import lombok.extern.slf4j.Slf4j;



/**
 * @program: moses-parent-online
 * @description: 通用工具类
 * @author: changxiaowei
 * @Date: 2021-12-15 19:02
 **/
@Slf4j
public class CommonUtil {

    /**
     * @Des 字符串数组转为float数组
     * @Param [strings]
     * @return java.lang.Double[]
     * @Author changxiaowei
     * @Date  2022/2/7
     */
    public static float [] StringArrToFloat(String [] strings){
        if(strings ==null || strings.length  <= 0){
            return null;
        }
        float[] result = new  float[strings.length];
        try {
            for (int i = 0; i < strings.length; i++) {
                result[i]=Float.valueOf(strings[i]);
            }
        }catch (Exception e){
            log.error("[严重异常]字符串数组转为Float数组异常，参数：{}，异常信息：{}", JSONObject.toJSONString(strings),e);
        }
        return result;
    }
}
