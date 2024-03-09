package com.biyao.moses.cache;

import com.biyao.moses.common.constant.RedisKeyConstant;
import com.biyao.moses.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;

@Slf4j
@Component
@EnableScheduling
public class ProductFeatureCache {

    @Autowired
    private RedisUtil redisUtil;
    // 商品向量
    private Map<String, String> productVectorMap;
    // 商品静态分
    private Map<String, String> productStaticScoreMap;

    // 1分钟刷新一次
    @Scheduled(cron = "0 0/30 * * * ?")
    protected void refresh() {
        log.info("[任务进度][商品特征]获取商品特征开始");
        long start = System.currentTimeMillis();
        Map<String, String> tempProductVectorMap = redisUtil.hscan(RedisKeyConstant.MOSES_PRODUCT_VECTOR);
        if (tempProductVectorMap != null && tempProductVectorMap.size()>0){
            productVectorMap = tempProductVectorMap;
            log.info("[任务进度][商品特征]获取商品特征向量结束，耗时{}ms，数量 {}", System.currentTimeMillis()-start, tempProductVectorMap.size());
        }else{
            log.error("[严重异常][商品特征]获取商品特征向量为空");
        }
        Map<String, String> tempProductStaticScoreMap = redisUtil.hscan(RedisKeyConstant.MOSES_PRODUCT_STATIC_SCORE);
        if (tempProductStaticScoreMap != null && tempProductStaticScoreMap.size()>0){
            productStaticScoreMap = tempProductStaticScoreMap;
            log.info("[任务进度][商品特征]获取商品静态分结束，耗时{}ms，数量 {}", System.currentTimeMillis()-start,tempProductStaticScoreMap.size());
        }else{
            log.error("[严重异常][商品特征]获取商品静态分为空");
        }
    }

    @PostConstruct
    protected void init(){
        refresh();
    }

    /**
     * 根据商品ID获取商品向量
     * @param productId
     * @return
     */
    public String getProductVector(String productId){
        if (this.productVectorMap != null){
            return this.productVectorMap.get(productId);
        }
        return null;
    }

    /**
     * 根据商品ID获取商品静态分
     * @param productId
     * @return
     */
    public String getProductStaticScore(String productId){
        if (this.productStaticScoreMap != null){
            return this.productStaticScoreMap.get(productId);
        }
        return null;
    }
}
