package com.biyao.moses.cache;

import com.biyao.moses.common.constant.MatchRedisKeyConstant;
import com.biyao.moses.params.ProductInfo;
import com.biyao.moses.util.MatchRedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @ClassName BaseSourceProductNoCache
 * @Description 基础召回源候选商品缓存
 * @Author xiaojiankai
 * @Date 2020/7/28 13:34
 * @Version 1.0
 **/
@Slf4j
public class BaseSourceProductNoCache {
    @Autowired
    private MatchRedisUtil matchRedisUtil;

    @Autowired
    private ProductDetailCacheNoCron productDetailCacheNoCron;

    @Autowired
    private SimilarCategory3IdNoCache similarCategory3IdNoCache;
    /**
     * 基础流量召回源候选商品
     * key 为相似后台三级类目，value 为该相似后台三级类目下基础流量候选商品集合
     */
    private Map<Long, List<Long>> baseSourceProductMap = new HashMap<>();

    protected void init(){
        refresh();
    }

    /**
     * 刷新基础流量召回源候选商品
     */
    public void refresh(){
        log.info("[任务进度][基础流量缓存]开始刷新基础流量召回源候选商品缓存");
        long start = System.currentTimeMillis();
        try{
            String baseProductStr = matchRedisUtil.getString(MatchRedisKeyConstant.MOSES_BASE_PRODUCT);
            baseSourceProductMap = parseAndGroupBySimilayCate3(baseProductStr);
        }catch (Exception e){
            log.error("[严重异常][基础流量缓存]刷新基础流量召回源候选商品缓存失败， e ", e);
        }
        log.info("[任务进度][基础流量缓存]结束刷新基础流量召回源候选商品缓存，耗时{}ms，相似三级类目个数{}", System.currentTimeMillis()-start, baseSourceProductMap.size());
    }

    /**
     * 解析并将商品按照相似三级类目分组
     * @param baseProductStr
     * @return
     */
    private Map<Long, List<Long>> parseAndGroupBySimilayCate3(String baseProductStr){
        Map<Long, List<Long>> result = new HashMap<>();
        if(StringUtils.isBlank(baseProductStr)){
            return result;
        }

        String[] baseProductArray = baseProductStr.trim().split(",");
        for(int i = 0; i < baseProductArray.length; i++){
            try{
                Long pid = Long.valueOf(baseProductArray[i]);
                ProductInfo productInfo = productDetailCacheNoCron.getProductInfo(pid);
                if(productInfo == null || productInfo.getThirdCategoryId() == null){
                    continue;
                }
                Long similarCate3Id = similarCategory3IdNoCache.getSimilarCate3Id(productInfo.getThirdCategoryId());
                if(!result.containsKey(similarCate3Id)){
                    List<Long> pidList = new ArrayList<>();
                    pidList.add(pid);
                    result.put(similarCate3Id, pidList);
                }else{
                    List<Long> pidList = result.get(similarCate3Id);
                    if(!pidList.contains(pid)){
                        pidList.add(pid);
                    }
                }
            }catch (Exception e){
                log.error("[严重异常][基础流量缓存]解析基础流量商品信息时出现异常 ", e);
            }
        }
        return result;
    }

    public Map<Long, List<Long>> getBaseSourceProductMap() {
        return baseSourceProductMap;
    }
}
