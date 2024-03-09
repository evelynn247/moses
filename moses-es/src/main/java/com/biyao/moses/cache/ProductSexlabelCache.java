package com.biyao.moses.cache;

import com.biyao.moses.config.MatchRedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

import static com.biyao.moses.constant.RedisKeyConstant.*;

/**
 * @program: moses-parent-online
 * @description:
 * @author: changxiaowei
 * @Date: 2021-12-21 16:23
 **/
@Slf4j
@Service
@EnableScheduling
public class ProductSexlabelCache {

    @Autowired
    private MatchRedisUtil matchRedisUtil;

    private Map<String, String> productSexMap = new HashMap<>();

    @PostConstruct
    @Scheduled(cron = "0 0 0/1 * * ?")
    protected void init() {
        refreshProductSexlabelCache();
    }

    /**
     * 	定时刷新商品性别缓存
     */
    protected void refreshProductSexlabelCache() {
        log.info("[任务进度][商品性别]获取商品性别信息开始");
        long start = System.currentTimeMillis();
        try {
            Map<String, String> redisProductSexMap = matchRedisUtil.hscan(MOSES_PRODUCT_SEX);

            if(CollectionUtils.isEmpty(redisProductSexMap)) {
                log.error("[严重异常][商品性别]获取商品性别为空");
                return;
            }
            productSexMap = redisProductSexMap;
            log.info("[任务进度][商品性别]获取商品性别信息结束，耗时{}ms，商品个数 {}", System.currentTimeMillis()-start, redisProductSexMap.size());
        } catch (Exception e) {
            log.error("[严重异常][商品性别]获取商品性别出现异常 ",e);
        }
    }
    /**
     * 根据商品ID获取商品性别
     * @param productId
     * @return
     */
    public String getProductSexLabel(String productId) {
        return productSexMap.getOrDefault(productId, "-1");
    }

}
