package com.biyao.moses.cache;

import com.biyao.moses.common.constant.MatchRedisKeyConstant;
import com.biyao.moses.util.MatchRedisUtil;
import com.biyao.moses.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @ClassName SearchProductLabelNoCron
 * @Description 缓存商品标签签配置
 * @Author xiaojiankai
 * @Date 2019/8/16 15:07
 * @Version 1.0
 **/
@Slf4j
public class SearchProductLabelNoCron {

    @Autowired
    private MatchRedisUtil matchRedisUtil;

    //页面配置集合
    private String searchProductLabel = null;

    /**
     * 初始化缓存
     */
    protected void init(){
        refreshSearchProductLabel();
    }

    protected void refreshSearchProductLabel(){
        log.info("[任务进度][商品标签]获取商品标签信息开始");
        try {
            String searchProductLabelTmp = matchRedisUtil.getString(MatchRedisKeyConstant.PRODUCT_LABEL_INFO_CONFIG);
            if (StringUtils.isNotBlank(searchProductLabelTmp)) {
                searchProductLabel = searchProductLabelTmp;
                log.info("[任务进度][商品标签]获取商品标签信息结束，标签信息长度 {}",searchProductLabelTmp.length());
            } else {
                log.error("[严重异常][商品标签]获取商品标签信息为空，不更新缓存");
            }
        }catch(Exception e){
            log.error("[严重异常][商品标签]获取商品标签时出现异常，", e);
        }
    }

    public String getSearchProductLabel() {
        return searchProductLabel;
    }
}
