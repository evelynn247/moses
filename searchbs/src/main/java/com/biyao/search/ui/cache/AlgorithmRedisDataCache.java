package com.biyao.search.ui.cache;

import com.biyao.search.ui.constant.AlogrithmRedisKeyConsts;
import com.biyao.search.ui.util.AlgorithmRedisUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * @author zhangzhimin
 * @date 2022/2/21
 */
@Component
@Slf4j
public class AlgorithmRedisDataCache {

    /**
     * 商品对应的轮播图视频信息：有无轮播图标识
     */
    @Getter
    private Map<String, String> productVideosMap = new HashMap<>();


    @Autowired
    AlgorithmRedisUtil algorithmRedisUtil;

    @PostConstruct
    public void init(){
        log.info("[任务报告]系统初始化同步算法Redis数据到本地缓存--》start");
        refresh();
        log.info("[任务报告]系统初始化同步算法Redis数据到本地缓存--》end");
    }

    public void refresh(){
        refreshProductLunVideoFlag();
    }

    /**
     * @description 商品对应的轮播图视频信息：有无轮播图标识更新
     * @project 【内容策略V1.0_商品内容视频化】
     * @author 张志敏
     * @date 2022-02-17
     * @return
     */
    private void refreshProductLunVideoFlag() {

        long start = System.currentTimeMillis();

        /**
         1、注入算法工具类对象AlgorithmRedisUtil algorithmRedisUtil；
         2、定义商品轮播图视频标识内存缓存属性：
         Map<商品ID,是否是轮播图视频标识> productVideoFlagMap（只缓存有轮播图视频标识）
         3、新增同步方法：从算法redis获取商品对应含有轮播图视频标识信息 tempMap，其中Redis集群为1007集群，key为product_slide_video，数据类型为hash，具体如下：
         3.1、调用算法工具类的Map<String, String> hscan(String key)方法分批次获取，如果出现异常，则过滤掉，最后将返回结果赋值给tempMap；
         3.2、将tempMap处理成Map<Long,String>  tmpResultMap；
         3.3、最后将tmpResultMap赋值给productVideoFlagMap；
         4、新增根据商品ID获取轮播图视频标识方法getProductVideoFlagByPid（Long pid），方法内部逻辑如下：
         4.1、从本地缓存中根据商品获取含有轮播图视频标识：productVideoFlagMap.get(pid)；
         4.2、若结果为1，则返回true，表示该视频含有轮播图视频；
         4.2、若结果为null或者其他值，则返回false，表示该视频不含轮播图视频；
         */
        Map<String, String> tempMap = algorithmRedisUtil.hscan(AlogrithmRedisKeyConsts.PRODUCT_VIDEO_KEY);

        if(CollectionUtils.isEmpty(tempMap)) {
            log.error("[信息][同步商品含有轮播图视频标识数据]获取商品含有轮播图视频标识数据为空，可能系统出现问题了");
        }
        this.productVideosMap = tempMap;

        log.info("[操作日志]同步商品含有轮播图视频标识数据Key:{}，获取所有映射缓存耗时={}", AlogrithmRedisKeyConsts.PRODUCT_VIDEO_KEY,(System.currentTimeMillis() - start));

    }


    /**
     * 根据商品id判断是否含有轮播图视频，如果有，则返回true，否则返回false
     * @param pid
     * @return
     */
    public Boolean getProductVideoFlagByPid(Long pid){
        String str = productVideosMap.get(String.valueOf(pid));
        if(str != null && AlogrithmRedisKeyConsts.PRODUCT_HAS_VIDEO_CONSTANT.equals(str)){
            return true;
        }
        return false;
    }
}
