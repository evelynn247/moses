package com.biyao.moses.common.enums;

import java.util.Arrays;

/**
 * @program: moses-parent
 * @description: 活动页类目场景枚举类
 * @author: changxiaowei
 * @create: 2022-01-26 18:51
 **/

public enum ActCategoryEnum {

    BYZW("27","2,7,9","必要造物频道页");
    /**
     * 活动id
     */
    private String sceneId;

    /**
     * 描述
     */
    private String des;
    /**
     * 支持的分端id
     */
    private String siteIdList;

    ActCategoryEnum(String sceneId,  String siteIds,String des) {
        this.sceneId = sceneId;
        this.des = des;
        this.siteIdList = siteIds;
    }

    /**
     * 判断当前活动是否支持该端
     * @param actCategoryEnum
     * @param siteId
     * @return
     */
   public static boolean isMatchSiteId(ActCategoryEnum actCategoryEnum,String siteId){
       if(actCategoryEnum==null){
           return  false;
       }
       return  Arrays.asList(actCategoryEnum.getSiteIdList().split(",")).contains(siteId);
   }

    public String getSceneId() {
        return sceneId;
    }

    public String getDes() {
        return des;
    }

    public String getSiteIdList() {
        return siteIdList;
    }
}
