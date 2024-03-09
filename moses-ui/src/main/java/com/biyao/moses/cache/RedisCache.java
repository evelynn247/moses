package com.biyao.moses.cache;

import com.biyao.moses.common.constant.MatchRedisKeyConstant;
import com.biyao.moses.model.match.ProductImage;
import com.biyao.moses.util.MatchRedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;

@Slf4j
@Component
@EnableScheduling
public class RedisCache {

    @Autowired
    MatchRedisUtil matchRedisUtil;

    //新实验首页feed流前端界面显示scm的白名单uuid信息
    private Set<String> homeFeedWhiteSet;

    //置顶的轮播图地址配置信息
    private List<ProductImage> topSliderPicListConfig = new ArrayList<>();

    //10分钟刷新一次
    @Scheduled(cron = "20 0/10 * * * ?")
    protected void refresh() {
        refreshHomeFeedWhite();
        refreshTopSliderPicListConfig();
    }

    @PostConstruct
    protected void init(){
        refresh();
    }

    /**
     * 刷新SCM白名单配置信息
     */
    private void refreshHomeFeedWhite(){
        try {
            log.info("[任务进度][白名单]获取scm白名单信息开始");
            Set<String> homeFeedWhiteSetTmp = new HashSet<>();
            String whiteStr = matchRedisUtil.getString(MatchRedisKeyConstant.HOME_FEED_WHITE);
            if (StringUtils.isNotBlank(whiteStr)) {
                String[] whiteArray = whiteStr.split(",");
                for (String white : whiteArray) {
                    if (StringUtils.isNotBlank(white)) {
                        homeFeedWhiteSetTmp.add(white);
                    }
                }
            }
            homeFeedWhiteSet = homeFeedWhiteSetTmp;
            log.info("[任务进度][白名单]获取scm白名单信息结束, 个数：{}", homeFeedWhiteSet.size());
        }catch (Exception e){
            log.error("[严重异常][白名单]获取scm白名单信息出现异常", e);
        }
    }

    /**
     * 刷新置顶的轮播图信息
     */
    private void refreshTopSliderPicListConfig(){
        log.info("[任务进度][轮播图]获取置顶的轮播图信息开始");
        try {
            Boolean[] isExceptionArray = new Boolean[]{false};
            String topSliderPicListStr = matchRedisUtil.getString(MatchRedisKeyConstant.MOSES_CONFIG_TOP_SLIDER_PIC, isExceptionArray);
            //如果查询时未出现异常，则更新缓存；否则不更新缓存
            if (!isExceptionArray[0]) {
                topSliderPicListConfig = parseTopSliderPic(topSliderPicListStr);
                log.info("[任务进度][轮播图]获取置顶的轮播图信息结束，数目 {}", topSliderPicListConfig.size());
            }else{
                log.info("[严重异常][轮播图]获取置顶的轮播图信息时出现错误，访问redis异常");
            }
        }catch (Exception e){
            log.error("[严重异常][轮播图]获取置顶的轮播图出现错误，", e);
        }
    }

    /**
     * 解析置顶的轮播图配置信息
     * 格式为：轮播图1非webp图,轮播图1webp图|轮播图2非webp图,轮播图2webp图
     * @param topSliderPicListStr
     */
    private List<ProductImage> parseTopSliderPic(String topSliderPicListStr){
        List<ProductImage> result = new ArrayList<>();
        if(StringUtils.isBlank(topSliderPicListStr)){
            return result;
        }

        boolean isCheckError = false;
        String[] topSliderPicStrArray = topSliderPicListStr.trim().split("\\|");
        for(String topSliderPicStr : topSliderPicStrArray){
            if(StringUtils.isBlank(topSliderPicStr)){
                isCheckError = true;
                continue;
            }

            String[] sliderPicAddrArray = topSliderPicStr.trim().split(",");
            if(sliderPicAddrArray.length != 2){
                isCheckError = true;
                continue;
            }
            ProductImage productImage = new ProductImage();
            productImage.setImage(sliderPicAddrArray[0].trim());
            productImage.setWebpImage(sliderPicAddrArray[1].trim());
            result.add(productImage);
        }

        if(isCheckError){
            log.error("[严重异常]解析置顶的轮播图配置信息出现错误， {}", topSliderPicListStr);
        }

        return result;
    }

    /**
     * 判断uuid是否在首页feed流前端界面显示scm信息的白名单中
     * @param uuid
     * @return
     */
    public boolean isHomeFeedWhite(String uuid){
        if(CollectionUtils.isEmpty(homeFeedWhiteSet)){
            return false;
        }
        if(homeFeedWhiteSet.contains(uuid)){
            return true;
        }
        return false;
    }

    /**
     * 查询置顶的轮播图集合配置
     * @return
     */
    public List<ProductImage> getTopSliderPicList(){
        return topSliderPicListConfig;
    }
}


