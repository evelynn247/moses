package com.biyao.moses.Enum;

/**
 * @program: moses-parent-online
 * @description: 活动场景
 * @author: changxiaowei
 * @create: 2021-09-28 19:30
 **/
public enum SceneEnum {
    BYFX(10,"必要分销",true),
    MJQ(18,"满减券热门",true),
    CTK(20,"参团卡活动热门",true),
    LJJ(21,"立减金",true),
    FX(22,"返现",true),
    BYZW(27,"必要造物频道页",true),
    TQJXF(28,"特权金下发页",true),
    BYZW_CAR(2701,"必要造物频道页类目商品召回",false),
    NEWV_VIDEO(2901,"新访客新手专享视频流推荐",true),
    OLDV_VIDEO(2902,"老访客访客新手专享视频流推荐",true),
    TQJXF_VIDEO(2903,"特权金下发页视频流推荐",true),
    COMMON_VIDEO(29,"通用视频流推荐",true);
    SceneEnum(Integer sceneId, String des,boolean isPredict) {
        this.sceneId = sceneId;
        this.des = des;
        this.isPredict =isPredict;
    }

    public static SceneEnum getSceneEnum(String sceneId){
        for (SceneEnum value : SceneEnum.values()) {
            if(sceneId.equals(value.getSceneId().toString())){
                return value;
            }
        }
        return null;
    }
    // 场景id
    private Integer sceneId;
    private String des;

    public boolean isPredict() {
        return isPredict;
    }
    /**
     * 是否需要预测用户向量
     */
    private boolean isPredict;
    public Integer getSceneId() {
        return sceneId;
    }
}
