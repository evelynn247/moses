package com.biyao.moses.cache;

import com.biyao.moses.common.constant.CommonConstants;
import com.biyao.moses.common.constant.MatchRedisKeyConstant;
import com.biyao.moses.params.ProductScoreInfo;
import com.biyao.moses.util.MatchRedisUtil;
import com.biyao.moses.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName HotSaleProductCache
 * @Description 同步热销数据到内存缓存中
 * @Author xiaojiankai
 * @Date 2019/10/12 14:28
 * @Version 1.0
 **/
@Slf4j
public class HotSaleProductCacheNoCron {

    @Autowired
    private MatchRedisUtil redisUtil;

    //商品数量上限，当redis中商品数量大于上限时，只取该上限数据的商品信息
    private static  final int PID_NUM_MAX_LIMIT = 1000;
    //商品数量下限，当redis中商品数量小于下限时，则不更新缓存中的数据
    private static  final int PID_NUM_MIN_LIMIT = 100;

    private List<ProductScoreInfo> hotSaleProductMale = new ArrayList<>();
    private List<ProductScoreInfo> hotSaleProductFemale = new ArrayList<>();
    private List<ProductScoreInfo> hotSaleProductCommon = new ArrayList<>();

    protected void init(){
        refreshHotSaleProductCache();
        if(CollectionUtils.isEmpty(hotSaleProductMale)){
            log.error("[严重异常][hots]初始化时男性用户热销召回源数据为空");
        }
        if(CollectionUtils.isEmpty(hotSaleProductFemale)){
            log.error("[严重异常][hots]初始化时女性用户热销召回源数据为空");
        }
        if(CollectionUtils.isEmpty(hotSaleProductCommon)){
            log.error("[严重异常][hots]初始化时未知性别用户热销召回源数据为空");
        }
    }

    public void refreshHotSaleProductCache() {
        //获取男性用户的热销商品信息
        getProductScoreInfoFromRedis(CommonConstants.MALE_SEX);
        //获取女性用户的热销商品信息
        getProductScoreInfoFromRedis(CommonConstants.FEMALE_SEX);
        //获取未知性别用户的热销商品信息
        getProductScoreInfoFromRedis(CommonConstants.UNKNOWN_SEX);
    }

    /**
     * 通过用户性别获取热销召回商品信息
     * @param sex 用户性别
     * @return
     */
    public List<ProductScoreInfo> getProductScoreInfoBySex(String sex){
        List<ProductScoreInfo> result;
        if(CommonConstants.MALE_SEX.equals(sex)){
            result = hotSaleProductMale;
        }else if(CommonConstants.FEMALE_SEX.equals(sex)){
            result = hotSaleProductFemale;
        }else{
            result = hotSaleProductCommon;
        }
        return result;
    }

    /**
     * 从redis中获取数据并缓存到内存中
     * @param sex
     */
    private void getProductScoreInfoFromRedis(String sex){
        log.info("[任务进度][hots]获取热销召回源商品信息开始，用户性别 {}", sex);
        long start = System.currentTimeMillis();
        String redisKey;
        if(CommonConstants.MALE_SEX.equals(sex)){
            redisKey = MatchRedisKeyConstant.MOSES_HOTS_MALE;
        }else if(CommonConstants.FEMALE_SEX.equals(sex)){
            redisKey = MatchRedisKeyConstant.MOSES_HOTS_FEMALE;
        }else{
            redisKey = MatchRedisKeyConstant.MOSES_HOTS_COMMON;
        }

        String pidScoreStr = redisUtil.getString(redisKey);
        if(StringUtils.isBlank(pidScoreStr)){
            log.error("[严重异常][hots]热销召回源商品信息为空，redis key {}", redisKey);
            return;
        }

        //如果redis中没有数据，则不更新到内存缓存中
        List<ProductScoreInfo> productScoreInfos = StringUtil.parseProductStr(pidScoreStr, PID_NUM_MAX_LIMIT, redisKey);
        if(CollectionUtils.isEmpty(productScoreInfos)){
            log.error("[严重异常][hots]解析不到redis中召回源商品信息，redis key {}", redisKey);
            return;
        }

        //如果redis中商品数量小于下限，则不更新到内存缓存中
        if(productScoreInfos.size() < PID_NUM_MIN_LIMIT){
            log.error("[严重异常][hots]Redis中热销召回源商品数量小于数量下限，redis key {}", redisKey);
            return;
        }

        if(CommonConstants.MALE_SEX.equals(sex)){
            hotSaleProductMale = productScoreInfos;
        }else if(CommonConstants.FEMALE_SEX.equals(sex)){
            hotSaleProductFemale = productScoreInfos;
        }else{
            hotSaleProductCommon = productScoreInfos;
        }
        log.info("[任务进度][hots]获取热销召回源商品信息结束，用户性别 {}，耗时 {}ms，商品数量 {}", sex, System.currentTimeMillis()-start, productScoreInfos.size());
    }

}
