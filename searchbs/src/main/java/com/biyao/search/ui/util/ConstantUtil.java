package com.biyao.search.ui.util;

/**
 * Created by GuoJia on 2018/08/01
 *
 * @Description: appapi全局常量工具类
 */
public class ConstantUtil {

    //public static final String SERVICE_NAME = "APPAPI"; //调用方名称
    //public static final String SERVICE_NAME_S = "appapi"; //调用方名称-小写
    //public static final Integer CALLER_SYSTEM = 1; //调用系统标识1==app
    //public static final String CALLER = "appapi.biyao.com"; //发送消息caller

    public static final String SERVICE_NAME = "SEARCH"; //调用方名称
    public static final String SERVICE_NAME_S = "search"; //调用方名称-小写
    public static final Integer CALLER_SYSTEM = 1; //调用系统标识1==app
    public static final String CALLER = "search.biyao.com"; //发送消息caller
    
    public static final String TOGETHER_GROUP_STR = "一起拼"; //标识
    public static final String PRODUCT_CUSTOMIZATION = "定制"; //标识

    public static final String BIYAO_FLAG = "biyao"; //必要标记

    public static String PLATFORM = ""; //平台信息(分享平台v1.0-获取海报信息使用)
    /**
     * biyao+11位手机号+appapi
     */
    public static final Integer phoneNumberLength = 22;

    /**
     * JsonResult的成功
     */
    public static final int SUCCESS = 1;
    /**
     * JsonResult的错误
     */
    public static final int ERROR = 0;
    /**
     * 兼容andriod获取更多处理逻辑
     */
    public static final String PAGECOUNT_FOR_ANDRIOD = "2";

    public static final String IS_CAN_USE_0 = "0";//否

    public static final String IS_CAN_USE_1 = "1"; //是
}
