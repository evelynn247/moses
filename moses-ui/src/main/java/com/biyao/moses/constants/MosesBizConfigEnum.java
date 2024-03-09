package com.biyao.moses.constants;

import org.apache.commons.lang3.StringUtils;

/**
 * moses接入外部业务的配置信息
 */
public enum MosesBizConfigEnum {
    /**
     * 加价购商品列表页-商品列表区域中推荐数据源商品
     * 复购提升专项V2.0-加价购及相关优化新增
     */
    JJG("jjg","bert2,1",null,null,null,null,null,100, true, ""),
    /**
     * 购物津贴页热门中推荐数据源商品
     * 复购提升专项V2.1-买二返一流程引导优化新
     */
    ALLOWANCE_PAGE("allowancePage", "bert3,1",null,"bert3,2","bert3,2",null,null,100, true, ""),
    /**
     * 猜你喜欢页推荐数据源商品
     * 搜索V2.5-底纹词和搜索无结果优化
     */
    GUESSYOULIKE_PAGE("guessYouLike", "fsr,1.0|ucb3,0.01|hots,0.001","ucb3,1",null,null,null,null,100, true, ""),
    /**
     * 梦工厂V1.1.1 梦工厂聚合页店铺推荐
     */
    DREAM_WORKS_STORE("dreamWorksStore", "dws,1|dwscomm,0.01","dwscomm,1","dws,1|dwscomm,-common","dwscomm,-common",null,null,10, false, "mgc"),
    /**
     * 梦工厂V1.1.1 梦工厂聚合页衍生商品推荐
     */
    DREAM_WORKS_PRODUCT("dreamWorksProduct", "dwp,1|dwpsex,0.001","dwpsex,1","dwp,1|dwpsex,-sex","dwpsex,-sex",null,null,200, false, "mgc"),
    /**
     * 新手专享V1.5 N折商品推荐
     */
    DISCOUNTN("discountN","ucbnz,1|ucbnzsex,0.001","ucbnzsex,1","ucbnzsex,-sex","ucbnzsex,-sex",null,null,500, true, "nzzq"),
    /**
     * 新手专享V1.5 新人必买榜单推荐商品
     */
    NEWUSER_MUST_BUY("newUserMustBuy","ucbnb,1","ucbnb,1","ucbnb,-sex","ucbnb,-sex",null,null,100, true, "xrbm"),
    /**
     * 必要朋友V2.0-feed流内容丰富及展示优化项目新增 --精选内容
     */
    BYFRIEND_JXCONTENT("excellentContent","com,1|comhots,1|views,1|viewhots,1","comhots,1|viewhots,1","com,1|comhots,1-common|views,1|viewhots,1-common","comhots,-common|viewhots,-common",null,null,1000,false,"jxnr"),
    /**
     * 必要朋友V2.0-feed流内容丰富及展示优化项目新增 --好友已购
     */
    BYFRIEND_HYYG("friendsBought","ucb3_yg,1",null,"ucb3_yg,1","ucb3_yg,1",null,null,100,true,"hyyg"),
    /**
     * 到站老客转化升级V1.0-精准活动推荐 个性化推荐活动
     */
    DZLK_ACTIVITY("dzlkActivity","act,1",null,"act,1",null,null,null,50,false,"hd"),
    /**
     * 到站老客转化升级V1.0-精准活动推荐  专题页商品推荐
     */
    DZLK_SPECIAL_PAGE("dzlkSpecialPage","spe,1",null,"spe,1-common",null,null,null,200,true,"zty")
    ;
    /**
     * 业务名称
     */
    private String bizName;
    /**
     * 召回源及其权重
     */
    private String sourceWeight;
    /**
     * 非个性化召回源及其权重，若为null 则说明没有非个性化数据
     */
    private String impersonalSourceWeight;
    /**
     * 召回源及其数据获取策略
     */
    private String sourceDataStrategy;

    /**
     * 非个性化召回源及其数据获取策略
     */
    private String impersonalSourceDataStrategy;
    /**
     * 召回源及其redis信息
     */
    private String sourceRedis;
    /**
     * ucb召回源的数据号
     */
    private String ucbDataNum;
    /**
     * 期望召回的商品上限
     */
    private int expNum;

    /**
     * 是否是普通商品推荐
     */
    private boolean isNormalProduct;
    /**
     * 默认使用ID，用于scm埋点
     */
    private String defalutExpId;

    MosesBizConfigEnum(String bizName, String sourceWeight,String impersonalSourceWeight, String sourceDataStrategy,String impersonalSourceDataStrategy,
                       String sourceRedis, String ucbDataNum, int expNum, boolean isNormalProduct, String defalutExpId) {
        this.bizName = bizName;
        this.sourceWeight = sourceWeight;
        this.impersonalSourceWeight=impersonalSourceWeight;
        this.sourceDataStrategy = sourceDataStrategy;
        this.impersonalSourceDataStrategy=impersonalSourceDataStrategy;
        this.sourceRedis = sourceRedis;
        this.ucbDataNum = ucbDataNum;
        this.expNum = expNum;
        this.isNormalProduct = isNormalProduct;
        this.defalutExpId = defalutExpId;
    }

    public String getBizName() {
        return bizName;
    }

    public String getSourceWeight() {
        return sourceWeight;
    }

    public String getImpersonalSourceWeight() {
        return impersonalSourceWeight;
    }

    public String getImpersonalSourceDataStrategy() {
        return impersonalSourceDataStrategy;
    }

    public String getSourceDataStrategy() {
        return sourceDataStrategy;
    }

    public String getSourceRedis() {
        return sourceRedis;
    }

    public String getUcbDataNum() {
        return ucbDataNum;
    }

    public int getExpNum() {
        return expNum;
    }

    public boolean isNormalProduct() {
        return isNormalProduct;
    }

    public String getDefalutExpId() {
        return defalutExpId;
    }

    public static MosesBizConfigEnum getByBizName(String bizName){
        if(StringUtils.isBlank(bizName)){
            return null;
        }
        MosesBizConfigEnum[] enums = values();
        for(MosesBizConfigEnum mosesBizConfigEnum : enums){
            if(bizName.equals(mosesBizConfigEnum.getBizName())){
                return mosesBizConfigEnum;
            }
        }
        return null;
    }
}
