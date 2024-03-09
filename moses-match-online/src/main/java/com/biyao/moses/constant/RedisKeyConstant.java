package com.biyao.moses.constant;

/**
 * @program: moses-parent-online
 * @description: redis 相关配置
 * @author: changxiaowei
 * @Date: 2021-12-30 19:03
 **/
public class RedisKeyConstant {

    /**
     * 刷新频率 一天一次
     */
    public  static String user_info="rs_uf_"; // 用户信息前缀
    public  static String is_register= "reg";   // 是否为注册用户
    public  static String regist_date= "regd";  // 注册日期
    public  static String first_order_date= "fod";  //首次下单时间
    public  static String user_cvr_7day= "cvr7";  //用户7天之内的转化率
    public  static String user_paid_num_7day= "pn7"; //用户7天之内购买商品的数目
    public  static String user_gmv_7day= "gmv7";  // 用户7天之内的gmv
    public  static String user_paid_price_avg_7day= "pav7";  // 用户7天之内购买商品的均价
    public  static String user_paid_price_var_7day= "pva7";  // 用户7天之内购买商品价格的方差
    public  static String user_cvr_15day= "cvr15";  //用户15天之内的转化率
    public  static String user_paid_num_15day= "pn15"; //用户15天之内购买商品的数目
    public  static String user_gmv_15day= "gmv15";  // 用户15天之内的gmv
    public  static String user_paid_price_avg_15day= "pav15";  // 用户15天之内购买商品的均价
    public  static String user_paid_price_var_15day= "pva15";  // 用户15天之内购买商品价格的方差
    public  static String user_ctr_7day= "ctr7";  // 用户7天之内的点击率
    public  static String user_ctr_15day= "ctr15";    // 用户15天之内的点击率

}
