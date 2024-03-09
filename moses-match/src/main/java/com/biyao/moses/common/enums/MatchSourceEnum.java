package com.biyao.moses.common.enums;

import com.biyao.moses.match2.constants.MatchStrategyConst;
import org.apache.commons.lang3.StringUtils;

/**
 * match召回源的固定信息
 *
 */
public enum MatchSourceEnum {
    /**
     * action based recommendation（基于操作的推荐）
     */
    ACREC(MatchStrategyConst.ACREC, true, true),
    /**
     * 双向编码推荐召回源
     */
    BERT(MatchStrategyConst.BERT, true, true),
    /**
     * bert2召回源，用于加价购
     */
    BERT2(MatchStrategyConst.BERT2, true, true),
    /**
     * 基于商品内容的协同过滤召回源
     */
    IBCF(MatchStrategyConst.IBCF, true, true),
    IBCF2(MatchStrategyConst.IBCF2, true, true),

    /**
     * 基于标签的召回源
     */
    TAG(MatchStrategyConst.TAG, true, true),
    /**
     * 基于实时浏览商品的召回源
     */
    RTCBR(MatchStrategyConst.RTCBR, true, true),
    RTBR2(MatchStrategyConst.RTBR2, true, true),
    /**
     * 实时兴趣探索召回源
     */
    RTEE(MatchStrategyConst.RTEE, true, true),
    /**
     * 多臂赌博机召回源
     */
    UCB(MatchStrategyConst.UCB, true, true),
    UCB2(MatchStrategyConst.UCB2, true, true),
    /**
     * slot1实验槽source名称
     */
    SLOT1(MatchStrategyConst.SLOT1, true, true),

    /**
     * slot2实验槽source名称
     */
    SLOT2(MatchStrategyConst.SLOT2, true, true),

    /**
     * 新手专享兜底召回源
     */
    NCHS(MatchStrategyConst.NCHS, true, true),

    /**
     * 热销兜底召回源
     */
    HOTS(MatchStrategyConst.HOTS, true, true),

    /**
     * 天气召回源
     */
    WEATHER(MatchStrategyConst.WEATHER, true, true),

    /**
     * 基础流量召回源
     */
    BASE(MatchStrategyConst.BASE, true, true),

    /**
     * 梦工厂聚合页店铺个性化召回源
     */
    DWS(MatchStrategyConst.DWS, false, false),

    /**
     * 梦工厂聚合页店铺通用召回源
     */
    DWSCOMM(MatchStrategyConst.DWSCOMM, false, false),

    /**
     * 梦工厂聚合页商品个性化召回源
     */
    DWP(MatchStrategyConst.DWP, false, false),

    /**
     * 梦工厂聚合页商品性别通用召回源
     */
    DWPSEX(MatchStrategyConst.DWPSEX, false, false),
    /**
     * 必要朋友V2.0-feed流内容丰富及展示优化 好友已购商品 召回源
     */
    UCB3YG(MatchStrategyConst.UCB3YG, true, true),
    /**
     * 必要朋友V2.0-feed流内容丰富及展示优化 精选内容 个性化评论召回源
     */
    COM(MatchStrategyConst.COM, false, false),
    /**
     * 必要朋友V2.0-feed流内容丰富及展示优化  精选内容 热度商品评论召回源
     */
    COMHOTS(MatchStrategyConst.COMHOTS, false, false),
    /**
     * 必要朋友V2.0-feed流内容丰富及展示优化  精选内容 个性化主动发布内容召回源
     */
    VIEWS(MatchStrategyConst.VIEWS, false, false),
    /**
     * 必要朋友V2.0-feed流内容丰富及展示优化  精选内容 热度主动发布召回源
     */
    VIEWHOTS(MatchStrategyConst.VIEWHOTS, false, false),

    /**
     * 到站老客转化升级V1.0-精准活动推荐 个性化推荐活动召回源
     */
    DZLKACT(MatchStrategyConst.DZLKACT,false,false)
    ,
    GXQSP(MatchStrategyConst.GXQSP_MATCH, true, true),
    BASE2(MatchStrategyConst.BASE2, true, true)
    ;

    /**
     * 召回源名称
     */
    private String sourceName;
    /**
     * 是否是普通商品，
     * true: 是普通商品
     * false: 不是普通商品，例如衍生商品、店铺ID
     */
    private boolean isNormalProduct;

    /**
     * 该召回源是否有单独的召回bean，默认为false，表示没有单独的召回bean
     */
    private boolean isHadExclusiveBean;

    MatchSourceEnum(String sourceName, boolean isNormalProduct, boolean isHadExclusiveBean) {
        this.sourceName = sourceName;
        this.isNormalProduct = isNormalProduct;
        this.isHadExclusiveBean = isHadExclusiveBean;
    }

    public String getSourceName() {
        return sourceName;
    }

    public boolean isNormalProduct() {
        return isNormalProduct;
    }

    public boolean isHadExclusiveBean() {
        return isHadExclusiveBean;
    }

    /**
     * 根据match召回源名称获取枚举类

     * @return
     */
    public static MatchSourceEnum getMatchSourceEnum(String source){
        MatchSourceEnum result = null;
        if(StringUtils.isBlank(source)){
            return null;
        }
        for(MatchSourceEnum matchSourceEnum : MatchSourceEnum.values()){
            if(matchSourceEnum.getSourceName().equals(source)){
                result = matchSourceEnum;
                break;
            }
        }
        return result;
    }

    /**
     * 判断该source 是否有单独私有的java bean, 默认是没有私有的java bean
     * @param source
     * @return
     */
    public static boolean isHadExclusiveBean(String source){
        MatchSourceEnum matchSourceEnum = getMatchSourceEnum(source);
        if(matchSourceEnum == null || !matchSourceEnum.isHadExclusiveBean()){
            return false;
        }
        return true;
    }

    /**
     * 判断该source 是否是普通商品召回源
     * @param source
     * @return
     */
    public static boolean isNotNormalProduct(String source) {
        MatchSourceEnum matchSourceEnum = getMatchSourceEnum(source);
        //默认都是普通商品
        if(matchSourceEnum == null || matchSourceEnum.isNormalProduct()){
            return false;
        }
        return true;
    }
}

