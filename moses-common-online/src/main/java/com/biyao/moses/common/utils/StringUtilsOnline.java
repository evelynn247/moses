package com.biyao.moses.common.utils;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @program: moses-parent-online
 * @description:
 * @author: changxiaowei
 * @Date: 2022-03-10 19:02
 **/
@Slf4j
public class StringUtilsOnline {

    public static List<Integer> stringArrToIntList(String [] strings){
        List<Integer> result = new ArrayList<>();
        if(strings ==null || strings.length  <= 0){
            return result;
        }
        try {
            for (int i = 0; i < strings.length; i++) {
                if (isInteger(strings[i])) {
                    result.add(Integer.valueOf(strings[i]));
                }
            }
        }catch (Exception e){
            log.error("[严重异常]字符串数组转为int list异常，参数：{}，异常信息：{}", JSONObject.toJSONString(strings),e);
        }
        return result;
    }


    public static boolean isBlank(String str) {
        if ("".equals(str) || str == null) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 判读字符串是否为正整数
     * @param str
     * @return
     */
    public  static  boolean isInteger(String str){

        if(isBlank(str)){
            return false;
        }
        Pattern pattern = Pattern.compile("[0-9]+");
        return pattern.matcher(str).matches();
    }
}
