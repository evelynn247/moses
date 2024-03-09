package com.biyao.moses.cache;

import com.biyao.moses.common.constant.AlgorithmRedisKeyConstants;
import com.biyao.moses.common.constant.CommonConstants;
import com.biyao.moses.params.ProductScoreInfo;
import com.biyao.moses.util.AlgorithmRedisUtil;
import com.biyao.moses.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName UcbProductCacheNoCron
 * @Description 同步ucb召回源数据到内存缓存中
 * @Author xiaojiankai
 * @Date 2019/12/23 19:28
 * @Version 1.0
 **/
@Slf4j
public class UcbProductCacheNoCron {

    @Autowired
    private AlgorithmRedisUtil algorithmRedisUtil;

    //商品数量上限，当redis中商品数量大于上限时，只取该上限数据的商品信息
    private static  final int PID_NUM_MAX_LIMIT = 500;
    //商品数量下限，当redis中商品数量小于下限时，记录日志
    private static  final int PID_NUM_MIN_LIMIT = 100;

    private List<ProductScoreInfo> ucbMaleProductList = new ArrayList<>();
    private List<ProductScoreInfo> ucbFemaleProductList = new ArrayList<>();
    private List<ProductScoreInfo> ucbCommonProductList = new ArrayList<>();

    private List<ProductScoreInfo> ucb2MaleProductList = new ArrayList<>();
    private List<ProductScoreInfo> ucb2FemaleProductList = new ArrayList<>();
    private List<ProductScoreInfo> ucb2CommonProductList = new ArrayList<>();

    protected void init(){
        refresh();
    }

    public void refresh() {
        log.info("[任务进度][ucb召回源]获取ucb召回源商品信息开始");
        List<ProductScoreInfo> ucbMaleProductListTmp = getProductScoreInfoFromRedis(AlgorithmRedisKeyConstants.MOSES_MAB_UCB_MALE);
        if(CollectionUtils.isNotEmpty(ucbMaleProductListTmp)){
            ucbMaleProductList = ucbMaleProductListTmp;
        }

        List<ProductScoreInfo> ucbFemaleProductListTmp = getProductScoreInfoFromRedis(AlgorithmRedisKeyConstants.MOSES_MAB_UCB_FEMALE);
        if(CollectionUtils.isNotEmpty(ucbFemaleProductListTmp)){
            ucbFemaleProductList = ucbFemaleProductListTmp;
        }

        List<ProductScoreInfo> ucbCommonProductListTmp = getProductScoreInfoFromRedis(AlgorithmRedisKeyConstants.MOSES_MAB_UCB_COMMON);
        if(CollectionUtils.isNotEmpty(ucbCommonProductListTmp)){
            ucbCommonProductList = ucbCommonProductListTmp;
        }

        List<ProductScoreInfo> ucb2MaleProductListTmp = getProductScoreInfoFromRedis(AlgorithmRedisKeyConstants.MOSES_MAB_UCB2_MALE);
        if(CollectionUtils.isNotEmpty(ucb2MaleProductListTmp)){
            ucb2MaleProductList = ucb2MaleProductListTmp;
        }

        List<ProductScoreInfo> ucb2FemaleProductListTmp = getProductScoreInfoFromRedis(AlgorithmRedisKeyConstants.MOSES_MAB_UCB2_FEMALE);
        if(CollectionUtils.isNotEmpty(ucb2FemaleProductListTmp)){
            ucb2FemaleProductList = ucb2FemaleProductListTmp;
        }

        List<ProductScoreInfo> ucb2CommonProductListTmp = getProductScoreInfoFromRedis(AlgorithmRedisKeyConstants.MOSES_MAB_UCB2_COMMON);
        if(CollectionUtils.isNotEmpty(ucb2CommonProductListTmp)){
            ucb2CommonProductList = ucb2CommonProductListTmp;
        }
        log.info("[任务进度][ucb召回源]获取ucb召回源商品信息结束， 男性商品数量 ucb {} ucb2 {}， 女性商品数量 ucb {} ucb2 {}，通用性别商品数量 ucb {} ucb2 {}",
                ucbMaleProductList.size(), ucb2MaleProductList.size(), ucbFemaleProductList.size(),
                ucb2FemaleProductList.size(), ucbCommonProductList.size(), ucb2CommonProductList.size());
    }

    /**
     * 从redis中获取数据
     * @param redisKey
     */
    private List<ProductScoreInfo> getProductScoreInfoFromRedis(String redisKey){
        List<ProductScoreInfo> result = new ArrayList<>();
        try {
            String pidScoreStr = algorithmRedisUtil.getString(redisKey);
            if (StringUtils.isBlank(pidScoreStr)) {
                log.error("[严重异常][ucb召回源]获取ucb召回源商品信息为空，redis key {}", redisKey);
                return result;
            }

            //如果redis中没有数据，则不更新到内存缓存中
            result = StringUtil.parseProductStr(pidScoreStr, PID_NUM_MAX_LIMIT, redisKey);
            if (CollectionUtils.isEmpty(result)) {
                log.error("[严重异常][ucb召回源]解析ucb召回源商品信息为空，redis key {}", redisKey);
                return result;
            }

            //如果redis中商品数量小于下限，则不更新到内存缓存中
            if (result.size() < PID_NUM_MIN_LIMIT) {
                log.error("[严重异常][ucb召回源]ucb召回源商品数量小于数量下限100，redis key {}, count {}", redisKey, result.size());
            }
        }catch (Exception e){
            log.error("[严重异常][ucb召回源]获取ucb召回源商品信息出现异常，redis key {} ", redisKey, e);
        }

        return result;
    }

    /**
     * 通过用户性别获取ucb召回商品信息
     * @param sex 用户性别
     * @return
     */
    public List<ProductScoreInfo> getProductScoreInfoBySex(String sex){
        List<ProductScoreInfo> result;
        if(CommonConstants.MALE_SEX.equals(sex)){
            result = ucbMaleProductList;
        }else if(CommonConstants.FEMALE_SEX.equals(sex)){
            result = ucbFemaleProductList;
        }else{
            result = ucbCommonProductList;
        }
        return result;
    }

    /**
     * 通过用户性别获取ucb2召回商品信息
     * @param sex 用户性别
     * @return
     */
    public List<ProductScoreInfo> getProductScoreInfoForUcb2BySex(String sex){
        List<ProductScoreInfo> result;
        if(CommonConstants.MALE_SEX.equals(sex)){
            result = ucb2MaleProductList;
        }else if(CommonConstants.FEMALE_SEX.equals(sex)){
            result = ucb2FemaleProductList;
        }else{
            result = ucb2CommonProductList;
        }
        return result;
    }

}
