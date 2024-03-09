package com.biyao.moses.featureplatform.constant;

import com.biyao.moses.common.constant.EsIndexConstant;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;


/**
 * @program: moses-parent-online
 * @description:
 * @author: zzm
 * @create: 2022-03-22 15:00
 **/
public class FeatureConstant {


    /**
     * 用户特征es字段
     */
    public static final List<String> USER_FEATURE_FIELD_LIST_CONST = Stream.of(
            "uuid", "uid", "by_prov", "by_city", "by_district", "site_id", "is_regist", "appv", "new_user_type", "is_old_cust", "device",
            "mobile_brand", "regist_date", "first_order_time", "gender", "friends_num", "friends_num_in_by", "is_upload_addressbook",
            "order_num", "return_times", "exchange_times", "good_comment_num", "bad_comment_num", "hist_paid_order_count", "hist_paid_gmv",
            "order_count", "un_paid_order_count", "paid_order_count", "paid_gmv", "click_p_count", "deep_p_count", "expo_p_count", "search_count",
            "origin_array", "search_click_count", "search_paid_order_count", "search_paid_gmv", "search_expo_count", "search_order_count",
            "is_active", "active_num_3day", "active_num_7day", "active_num_14day", "active_num_30day"
    ).collect(toList());


    /**
     * 商品特征es字段
     */
    public static final List<String> PRODUCT_FEATURE_FIELD_LIST_CONST = Stream.of(
            "product_id", "short_title", "category1_id", "category2_id", "category3_id", "shelf_status", "first_onshelf_time",
            "price", "operation_deduction", "technology_deduction", "supplier_id", "supplier_name", "sex", "season", "sale_status",
            "supplier_discount", "stock", "video_type", "in_type", "in_sea_type", "bee_product", "product_source", "act_stock_type",
            "act_stock", "video_score", "pv_click", "pv_deep_view", "pv_exp", "pv_order", "pv_buy", "pv_comment", "pv_good_comment",
            "pv_bad_comment", "pv_return", "pv_change", "uv_click", "uv_deep_view", "uv_exp", "uv_order", "uv_buy", "search_exp",
            "search_click", "search_deep_view", "search_order", "search_buy"
    ).collect(toList());


    /**
     * 解析规则类型-0-用户
     */
    public static final Integer TRANSFER_RULE_TYPE_CONST_USER = 0;

    /**
     * 解析规则类型-1-商品
     */
    public static final Integer TRANSFER_RULE_TYPE_CONST_PRODUCT = 1;


    public static final String SYMBOL_EQUAL_CONS = "=";

    public static final String SYMBOL_NOT_EQUAL_CONS = "!=";

    public static final String SYMBOL_RANGE_CONS = "<>";

    public static final String SYMBOL_COMMA_CONS = ",";


    /**
     * 分隔元规则之后的数组数量，正常为2
     */
    public static final Integer SPLIT_BASIC_RULE_SIZE_CONST = 2;


    /**
     * 用户特征召回字段
     */
    public static final String [] fetchUserSource ={"uuid","uid"};

    /**
     * 商品特征召回字段
     */
    public static final String [] fetchProductSource ={"product_id"};



}
