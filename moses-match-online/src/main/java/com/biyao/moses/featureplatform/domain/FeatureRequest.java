package com.biyao.moses.featureplatform.domain;


import com.alibaba.dubbo.common.utils.StringUtils;
import lombok.Data;


/**
 * @program: moses-parent-online
 * @description: 特征平台 请求参数
 * @author: zhangzhimin
 * @create: 2022-03-25 10:22
 **/

@Data
public class FeatureRequest {


    /**
     * 调用方服务名
     */
    private String caller;


    /**
     * 特征集字符串
     */
    private String matchRule;


    /**
     * 当前页码，从0开始
     */
    private Integer pageIndex;


    /**
     * 每页返回数量，默认1000，最大2000
     */
    private Integer pageSize=1000;


    /**
     * 对请求参数进行非空校验，校验不通过返回true，并对参数进行处理
     * @return
     */
    public Boolean validate(){
        if(StringUtils.isEmpty(caller) || StringUtils.isEmpty(matchRule) || pageIndex==null){
            return true;
        }

        //每页最多返回2000条
        pageSize = pageSize>2000?2000:pageSize;

        //特征条件符号转换（中-英）
        matchRule = matchRule.replaceAll("《","<").replaceAll("》",">")
                .replaceAll("！","!").replaceAll("（","(")
                .replaceAll("）",")").replaceAll("，",",");

        return false;
    }
}
