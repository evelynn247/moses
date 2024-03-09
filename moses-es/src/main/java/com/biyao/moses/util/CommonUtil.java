package com.biyao.moses.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Charsets;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @program: moses-parent-online
 * @description: 通用工具类
 * @author: changxiaowei
 * @Date: 2021-12-15 19:02
 **/
@Slf4j
public class CommonUtil {


    /**
     * 将mqBody中批量商品转成HyPdc接口的参数
     * @param array
     * @return
     */
    public static List<Long> jsonArrTOLongList(JSONArray array){
        List<Long> longList = new ArrayList<>();
        for (int i =0;i < array.size();i++){
            Long pid = array.getLong(i);
            longList.add(pid);
        }
        return longList;
    }

    /**
     * 创建唯一id
     * @return
     */
    public static String createUniqueId(){

        HashFunction hf = Hashing.md5();
        String didMd5 = hf.newHasher().putString(UUID.randomUUID().toString(), Charsets.UTF_8).hash().toString();
        return didMd5.substring(0, 16)+System.currentTimeMillis();
    }
    /**
     * @Des 字符串数组转为byte数组
     * @Param [strings]
     * @return java.lang.Byte[]
     * @Author changxiaowei
     * @Date  2022/2/7
     */
    public static Byte[] StringArrToByte(String  str){
        if(StringUtils.isEmpty(str)){
            return null;
        }
        String[] strings = str.split(",");
        Byte[] result = new  Byte[strings.length];
        try {
            for (int i = 0; i < strings.length; i++) {
                if(StringUtils.isEmpty(strings[i])){
                    continue;
                }
                result[i]=Byte.valueOf(strings[i]);
            }
        }catch (Exception e){
            log.error("[严重异常]字符串转为byte数组异常，参数：{}，异常信息：{}", JSONObject.toJSONString(strings),e);
        }
        return result;
    }
    /**
     * @Des 字符串数组转为long数组
     * @Param [strings]
     * @return java.lang.Long[]
     * @Author changxiaowei
     * @Date  2022/2/7
     */
    public static Long[] StringArrToLong(String [] strings){
        if(strings == null || strings.length  <= 0){
            return null;
        }
        Long[] result = new  Long[strings.length];
        try {
            for (int i = 0; i < strings.length; i++) {
                if(StringUtils.isEmpty(strings[i])){
                    continue;
                }
                result[i] = Long.valueOf(strings[i]);
            }
        }catch (Exception e){
            log.error("[严重异常]字符串数组转为Long数组异常，参数：{}，异常信息：{}", JSONObject.toJSONString(strings),e);
        }
        return result;
    }
}
