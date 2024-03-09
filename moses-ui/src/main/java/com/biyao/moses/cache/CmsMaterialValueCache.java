package com.biyao.moses.cache;

import com.biyao.cms.client.common.bean.ImageDto;
import com.biyao.cms.client.material.dto.*;
import com.biyao.moses.common.constant.UPCUserTypeConstants;
import com.biyao.moses.common.enums.CmsMaterialEnum;
import com.biyao.moses.constants.CommonConstants;
import com.biyao.moses.constants.ERouterType;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.params.AdvertInfo;
import com.biyao.moses.params.UIBaseRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * @ClassName CmsMaterialValueCache
 * @Description Cms素材值缓存
 * @Author admin
 * @Date 2020/3/5 20:22
 * @Version 1.0
 **/
@Slf4j
@Component
@EnableScheduling
public class CmsMaterialValueCache extends CmsMaterialValueCacheNoCron{

    @PostConstruct
    protected void init(){
        super.init();
    }

    //每隔10s刷新一次
    @Scheduled(cron = "2/10 ** * * * ?")
    protected void refresh(){
        super.refresh();
    }

    /**
     * 获取广告信息
     * @param showAdvert
     * @return
     */
    public AdvertInfo getAdvertInfo(String showAdvert, Integer userType, List<AdvertInfo> advertInfoList){
        List<AdvertInfo> advertInfos = new ArrayList<>(advertInfoList);
        AdvertInfo xYuanAdvertInfo = getXYuanAdvertInfo(showAdvert, userType);
        if(xYuanAdvertInfo != null && xYuanAdvertInfo.getTotalTemplateInfo() != null ){
            advertInfos.add(xYuanAdvertInfo);
        }
        if(CollectionUtils.isEmpty(advertInfos)){
            return null;
        }
        Collections.shuffle(advertInfos);

        return advertInfos.get(0);
    }

    /**
     * 获取x元购活动广告信息
     * @param showAdvert
     * @param userType
     * @return
     */
    public AdvertInfo getXYuanAdvertInfo(String showAdvert, Integer userType){
        if(!"1".equals(showAdvert)){
            return null;
        }

        //非新访客、老访客，则直接返回
        if(userType == null ||
                (UPCUserTypeConstants.NEW_VISITOR != userType
                        && UPCUserTypeConstants.OLD_VISITOR != userType)){
            return null;
        }
        AdvertInfo advertInfo = null;
        try {
            MaterialElementTextDTO advertId = (MaterialElementTextDTO)getValue(CmsMaterialEnum.ACTIVITY_ID.getId());
            if(advertId == null || StringUtils.isBlank(advertId.getValue())){
                return null;
            }

            MaterialElementIntegerNumberDto advertPosition = (MaterialElementIntegerNumberDto)getValue(CmsMaterialEnum.ACTIVITY_SHOW_POSITION.getId());
            if(advertPosition == null || advertPosition.getValue() == null
                    || advertPosition.getValue() <= 0){
                return null;
            }
            List<Long> pictureMaterialIdList = new ArrayList<>();
            pictureMaterialIdList.add(CmsMaterialEnum.ACTIVITY_DOUBLE_ROW_PICTURE1.getId());
            pictureMaterialIdList.add(CmsMaterialEnum.ACTIVITY_DOUBLE_ROW_PICTURE2.getId());
            pictureMaterialIdList.add(CmsMaterialEnum.ACTIVITY_DOUBLE_ROW_PICTURE3.getId());
            pictureMaterialIdList.add(CmsMaterialEnum.ACTIVITY_DOUBLE_ROW_PICTURE4.getId());
            pictureMaterialIdList.add(CmsMaterialEnum.ACTIVITY_DOUBLE_ROW_PICTURE5.getId());
            List<ImageDto> imageDtoList = new ArrayList<>();
            for(Long id : pictureMaterialIdList) {
                MaterialElementImageDto advertPicture = (MaterialElementImageDto) getValue(id);
                if (advertPicture != null) {
                    ImageDto value = advertPicture.getValue();
                    if (value != null && StringUtils.isNotBlank(value.getOriginUrl()) && StringUtils.isNotBlank(value.getWebpImageUrl())) {
                        imageDtoList.add(value);
                    }
                }
            }
            if(CollectionUtils.isEmpty(imageDtoList)){
                return null;
            }

            //随机选择一个图片
            Collections.shuffle(imageDtoList);
            Map<String,String> routerParam = new HashMap<>();
            routerParam.put("activityId", advertId.getValue());
            TotalTemplateInfo totalTemplateInfo = new TotalTemplateInfo();
            totalTemplateInfo.setShowType(CommonConstants.SHOW_TYPE_ADVERT);
            totalTemplateInfo.setRouterParams(routerParam);
            totalTemplateInfo.setAdImage(imageDtoList.get(0).getOriginUrl());
            totalTemplateInfo.setAdImageWebp(imageDtoList.get(0).getWebpImageUrl());
            totalTemplateInfo.setId(CommonConstants.INVALID_PRODUCT_ID);
            totalTemplateInfo.setRouterType(ERouterType.ADVERT.getNum());
            advertInfo = new AdvertInfo();
            advertInfo.setPosition(advertPosition.getValue());
            advertInfo.setTotalTemplateInfo(totalTemplateInfo);

        }catch (Exception e){
            log.error("[严重异常]获取x元购物返现活动广告入口信息出现异常");
        }
        return advertInfo;
    }
}
