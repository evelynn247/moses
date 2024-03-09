package com.biyao.moses.model.adapter.impl;

import com.alibaba.fastjson.JSONObject;
import com.biyao.moses.cache.RedisCache;
import com.biyao.moses.cache.SimilarCategory3IdCache;
import com.biyao.moses.common.enums.TemplateTypeEnum;
import com.biyao.moses.constants.CommonConstants;
import com.biyao.moses.context.ByUser;
import com.biyao.moses.model.TemplateNewuserForAdvertTemplateInfo;
import com.biyao.moses.model.TemplateNewuserTemplateInfo;
import com.biyao.moses.model.template.TemplateInfo;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.params.ProductInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.util.Map;

import static com.biyao.moses.constants.CommonConstants.SHOW_TYPE_ADVERT_GLOBAL;

/**
 * @ClassName TemplateNewuserForAdvertAdapter
 * @Description 新手专享商品+广告模板双排样式适配器
 * @Author xiaojiankai
 * @Date 2020/3/6 10:28
 * @Version 1.0
 **/
@Slf4j
@Component("templateNewuserForAdvert")
public class TemplateNewuserForAdvertAdapter extends BaseTemplateAdapter {
    @Override
    public TemplateInfo newTemplateInfoInstance() {
        return new TemplateNewuserForAdvertTemplateInfo();
    }

    @Override
    public Integer getCurTemplateDataNum(int actualSize) {
        return TemplateTypeEnum.TEMPLATE_NEWUSER.getDataSize();
    }

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private SimilarCategory3IdCache similarCategory3IdCache;


    @Override
    public TemplateInfo specialDataHandle(TemplateInfo templateInfo,
                                          TotalTemplateInfo totalData, Integer curDataIndex, ByUser user) {

        TemplateNewuserForAdvertTemplateInfo curTemplateInfo = (TemplateNewuserForAdvertTemplateInfo) templateInfo;
        // 如果是广告则不处理长图和短图
        if(SHOW_TYPE_ADVERT_GLOBAL.contains(totalData.getShowType())){
            return curTemplateInfo;
        }
        curTemplateInfo.setShowType(CommonConstants.SHOW_TYPE_PRODUCT);
        ProductInfo productInfo = productDetailUtil.preSaveProductInfo(totalData.getId(),user);

        curTemplateInfo = (TemplateNewuserForAdvertTemplateInfo) productDetailUtil.buildProductTemplate(curTemplateInfo, productInfo, curDataIndex,user, totalData);

        // 设置短图
        curTemplateInfo.setImage(productInfo.getSquarePortalImg());
        // 设置webp短图
        curTemplateInfo.setImageWebp(productInfo.getSquarePortalImgWebp());

        // 白名单用户制造商背景更换为scm参数，用于观察数据
        if(redisCache.isHomeFeedWhite(user.getUuid()) && !user.isTest()){
            changeSubtitleForWhiteList(curTemplateInfo,productInfo);
        }

        //(填充新手专享价格);
        if (productInfo.getNovicePrice() != null && productInfo.getNovicePrice().signum() == 1) {
            curTemplateInfo.setNovicePrice(String.valueOf(productInfo.getNovicePrice()));
        }

        //替换为sku的新手专享价格
        if(StringUtils.isNotBlank(totalData.getSkuId()) && StringUtils.isNotBlank(totalData.getNovicePrice())
                && Long.valueOf(totalData.getNovicePrice()) > 0 ){
            if(curTemplateInfo.getRouterParams().containsKey("suId")){
                curTemplateInfo.setNovicePrice(totalData.getNovicePrice());
            }
        }

        return curTemplateInfo;
    }


    /**
     * 白名单用户制造商背景更换为scm参数，用于观察数据
     * @param curTemplateInfo
     */
    private void changeSubtitleForWhiteList(TemplateNewuserTemplateInfo curTemplateInfo, ProductInfo productInfo) {
        try {
            Map<String, String> routerParams = curTemplateInfo.getRouterParams();
            if (routerParams == null || routerParams.size() == 0) {
                return;
            }
            String stpStr = routerParams.get(CommonConstants.STP);
            if (StringUtils.isBlank(stpStr)) {
                return;
            }
            String decodeStr = URLDecoder.decode(stpStr, "UTF-8");
            Map<String, String> stpMap = JSONObject.parseObject(decodeStr, Map.class);
            String scmStr = stpMap.get(CommonConstants.SCM);
            if (StringUtils.isBlank(scmStr)) {
                return;
            }
            curTemplateInfo.setSubtitle(scmStr);
            String mainTitle = curTemplateInfo.getMainTitle();
            if(productInfo!=null && productInfo.getThirdCategoryId()!=null){
                Long thirdCategoryId = productInfo.getThirdCategoryId();
                Long similar3CategoryId = similarCategory3IdCache.getSimilarCate3Id(thirdCategoryId);
                if(similar3CategoryId==null){
                    similar3CategoryId = thirdCategoryId;
                }
                mainTitle = similar3CategoryId.toString()+ "_" + mainTitle;
            }
            curTemplateInfo.setMainTitle(mainTitle);
        } catch (Exception e) {
            log.error("为白名单更换scm参数出错",e);
        }
    }
}
