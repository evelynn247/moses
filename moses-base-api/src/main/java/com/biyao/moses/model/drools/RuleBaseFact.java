package com.biyao.moses.model.drools;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @program: moses-parent
 * @description: 基础条件实体类
 * @author: changxiaowei
 * @create: 2021-03-24 17:40
 **/
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RuleBaseFact {

    /**
     * 场景ID
     * 0：首页feeds流
     * 1：个人中心页feeds流
     * 2：购物车页feeds流
     * 3：订单页feeds流
     * 6：加价购
     * 7：津贴活动
     * 8：感兴趣商品集
     * 9：买二返一feeds流
     */
    private  String scene;
    /**
     * 1：老客
     * 2：新访客
     * 3：老访客
     */
    private String utype;

    /**
     * 1：M站
     * 2：小程序
     * 7：ios
     * 9：安卓
     */
    private String siteId;

    private String uuid;
    /**
     * 0：不支持
     * 1：支持
     */
    private Boolean isPersonal=false;

    /**
     * 流量控制
     */
    private Integer bucketMinValue;

    private Integer bucketMaxValue;
    /**
     * uuid 白名单
     */
    private List<String> whiteList;
}
