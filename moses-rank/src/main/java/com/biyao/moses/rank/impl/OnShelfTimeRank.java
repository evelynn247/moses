package com.biyao.moses.rank.impl;

import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.common.constant.CacheRedisKeyConstant;
import com.biyao.moses.common.constant.RedisKeyConstant;
import com.biyao.moses.common.enums.SortEnum;
import com.biyao.moses.compare.AscComparator;
import com.biyao.moses.compare.DesComparator;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.params.ProductInfo;
import com.biyao.moses.rank.RankRequest;
import com.biyao.moses.rank.RecommendRank;
import com.biyao.moses.util.CacheRedisUtil;
import com.biyao.moses.util.RedisUtil;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component("OnShelfTimeRank")
public class OnShelfTimeRank  implements RecommendRank {

    @Autowired
    ProductDetailCache productDetailCache;
    @Autowired
    CacheRedisUtil redisUtil;

    @BProfiler(key = "OnShelfTimeRank.executeRecommend", monitorType = {
            MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR })
    @Override
    public List<TotalTemplateInfo> executeRecommend(RankRequest rankRequest) {
        List<TotalTemplateInfo> oriData = rankRequest.getOriData();
        try{


            for (TotalTemplateInfo totalTemplateInfo : oriData) {
                if (StringUtils.isNotBlank(totalTemplateInfo.getId())) {
                    ProductInfo productInfo = productDetailCache.getProductInfo(Long.valueOf(totalTemplateInfo.getId()));
                    if (productInfo!=null) {
                        if(productInfo.getFirstOnshelfTime() !=null){
                            totalTemplateInfo.setScore((double)productInfo.getFirstOnshelfTime().getTime());
                        }else{
                            totalTemplateInfo.setScore(0d);
                        }
                    }else{
                        totalTemplateInfo.setScore(0d);
                    }
                }
            }
            //默认降序
            Collections.sort(oriData,new DesComparator());
        }catch(Exception e){
            log.error("上新排序异常", e);
        }

        try {
            long time = System.currentTimeMillis();
            redisUtil.setString(CacheRedisKeyConstant.NEW_PRODUCT_TAG_TIME + "_"
                    + rankRequest.getUuid() + "_" + rankRequest.getFrontendCategoryId(), String.valueOf(time), 86400);
        } catch (Exception e) {
            log.error("在上新排序中对用户增加访问数据缓存时异常",e);
        }

        return oriData;
    }
}
