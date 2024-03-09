package com.biyao.moses.params;

/**
 * @ClassName AdvertParam
 * @Description 可以展示的活动广告列表
 * @Author admin
 * @Date 2020/3/26 18:04
 * @Version 1.0
 **/

import lombok.*;

/**
 * 广告位参数
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdvertParam {
    /**
     * 活动广告ID
     */
    private String id;
    /**
     * 活动广告展示位置
     */
    private String position;
    /**
     * 活动广告单排模板入口图
     */
    private String imageSingle;
    /**
     * 活动广告单排模板入口图webp格式
     */
    private String imageWebpSingle;
    /**
     * 活动广告双排模板入口图
     */
    private String imageDouble;
    /**
     * 活动广告双排模板入口图webp格式
     */
    private String imageWebpDouble;
    /**
     * 活动广告路由
     */
    private String router;
    /**
     * 是否为视频 1 ==true
     */
    private String isVideo;
    /**
     * 配置的运营位自带的商品id
     */
    private Long productId;
}
