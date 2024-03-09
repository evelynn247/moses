package com.biyao.moses.match;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * @program: moses-parent-online
 * @description:
 * @author: changxiaowei
 * @create: 2021-09-28 19:38
 **/

@Data
@Builder
public class MatchParam {

    String uuid;
    /**
     * 端id  1 M站 ；2 miniapp ；3 PC ；7 IOS； 9 Android。
     */
    Integer siteId;
    /**
     * 场景id http://wiki.biyao.com/pages/viewpage.action?pageId=60162054
     */
    private Integer sceneId;
    /**
     * 中性 2，女性1，男性0，未知 -1
     */
    private Byte userSex;
    /**
     * 用户季节  春1 夏2 秋3 冬4
     */
    private Byte userSeason;

    private Byte userPaltform;

    private List<Long> mjCardIds;

    private Long fxCardId;
    /**
     * 是否为个性化推荐
     */
    private boolean isPersonal;
    /**
     * 期望召回数量
     */
    private int expNum;
    /**
     * 召回数量权重控制
     */
    private double numWeight;

    /**
     * 召回源
     */
    private String source;
    /**
     * 用户最新点击的商品
     */
    private List<String> viewPids;
    /**
     * 设备
     */
    private String deivce;
    /**
     * 地理位置
     */
    private String location;
    private  boolean debug;
    /**
     * 本次请求唯一标示  moses生成
     */
    private String sid;

    private float [] vector;
    private List<Long> thirdCateGoryId;
    private List<Long> tagId;
    // 必要分销2   必要商城  1 鸿源分销 3
    private Integer channelType = 1;
    // 规则id
    private  String ruleId;
}

