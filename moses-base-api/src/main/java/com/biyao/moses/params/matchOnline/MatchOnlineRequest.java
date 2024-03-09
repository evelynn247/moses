package com.biyao.moses.params.matchOnline;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * @program: moses-parent-online
 * @description: mosesmatch 请求参数
 * @author: changxiaowei
 * @create: 2021-09-14 10:22
 **/
@Getter
@Setter
public class MatchOnlineRequest {

    /**
     * 服务端生成唯一id
     */
    private  String sid;
    private String uuid;
    private String uid;
    /**
     * 端id
     */
    private Integer siteId;
    /**
     * 机型 如：iphone 13
     */
    private String device="";
    /**
     * 是否为个性化推荐
     */
    private Boolean isPersonal;
    /**
     * 召回源及其策略
     */
    private String sourceAndWeight;
    /**
     * 期望召回数量
     */
    private  int expNum =500;
    /**
     * 中性 2，女性1，男性0，未知 -1
     */
    private Byte userSex= -1;
    /**
     * 春1夏2秋3 冬4四季0
     */
    private Byte userSeason;
    private Long fxCardId;
    /**
     * 用户点击序列（深度浏览+加购+ 购买）
     */
    private List<String> viewPids = new ArrayList<>();
    /**
     * 地理位置
     */
    private String location ="";

    private List<Long> mjCardIds;

    /**
     * 场景id   见：http://wiki.biyao.com/pages/viewpage.action?pageId=60162054
     */
    private String sceneId;
    // 日志追踪
    private boolean debug =false;
    // 后台三级类目集合
    private List<Long> thirdCateGoryIdList;
    // 标签
    private List<Long> tagIdList;
    // 视频流落地页主商品id
    private Long mainPid;
    /**
     * 渠道   必要商城 1  必要分销 2 鸿源分销 3
     */
    private Integer channelType;
    /**
     * 规则id 规则引擎中配置id
     */
    private  String ruleId;
}
