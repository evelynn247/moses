package com.biyao.moses.param;

import lombok.Data;
import org.tensorflow.Tensor;

/**
 * @program: moses-parent-online
 * @description:
 * @author: changxiaowei
 * @create: 2021-09-27 14:01
 **/
@Data
public class UserPredictParam {
    Tensor t_hour ;
    Tensor t_week ;
    Tensor t_sex;
    Tensor t_season;
    Tensor  t_siteId ;
    Tensor t_device;
    Tensor t_by_prov ;
    Tensor t_click_pid_10 ;
//    Tensor<?> is_regist ;
//    Tensor<?> regist_date ;
//    Tensor<?> first_order_date ;
//    // 点击率
//    Tensor<?> user_ctr_7day ;
//    // 转化
//    Tensor<?> user_cvr_7day ;
//    // 付款订单数
//    Tensor<?> user_paid_num_7day ;
//    // 付款总金额
//    Tensor<?> user_gmv_7day ;
//    // 均价
//    Tensor<?> user_paid_price_avg_7day ;
//    // 价格方差
//    Tensor<?> user_paid_price_var_7day ;
//    Tensor<?> user_ctr_15day ;
//    Tensor<?> user_cvr_15day ;
//    Tensor<?> user_paid_num_15day ;
//    Tensor<?> user_gmv_15day ;
//    Tensor<?> user_paid_price_avg_15day;
//    Tensor<?> user_paid_price_var_15day ;

    Tensor  user_float;
}
