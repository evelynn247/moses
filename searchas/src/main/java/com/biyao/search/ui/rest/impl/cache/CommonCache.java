package com.biyao.search.ui.rest.impl.cache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.biyao.rank.client.product.dto.ProductDto;
import com.biyao.rank.client.product.dto.QuerySalesProductRankListParam;
import com.biyao.rank.client.product.service.IProductRankListService;
import com.biyao.rank.common.bean.PageInfo;
import com.biyao.rank.common.bean.Result;
import com.biyao.search.common.model.ASProduct;
import com.biyao.search.common.model.SearchFallback;
import com.biyao.search.ui.config.LocalConfig;
import com.biyao.search.ui.constant.CommonConstant;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * @author biyao
 */
@Service
public class CommonCache {

    @Autowired
    private IProductRankListService productRankService;

    public Logger logger = LoggerFactory.getLogger(CommonCache.class);
    public LocalConfig localConfig = LocalConfig.getInstance();

    /**
     * 数据刷新线程池
     */
    protected ListeningExecutorService refreshPool = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(1));

    private String FALLBACK = "fallback";

    private LoadingCache<String, Object> commonCache = CacheBuilder
            // 设置并发级别为2，并发级别是指可以同时写缓存的线程数
            .newBuilder().concurrencyLevel(2)
            // 设置写缓存后过期时间 永不过期
            .expireAfterWrite(1000, TimeUnit.DAYS)
            // 2小时刷新一次
            .refreshAfterWrite(120, TimeUnit.SECONDS)
            // 设置缓存容器的初始容量
            .initialCapacity(2)
            // 设置缓存最大容量为100，超过100之后就会按照LRU最近虽少使用算法来移除缓存项
            .maximumSize(100)
            // 设置要统计缓存的命中率
            .recordStats()
            // 自动加载
            .build(new CacheLoader<String, Object>() {
                @Override
                public Object load(String key) throws Exception {
                    // System.out.println("loading " + key);
                    SearchFallback searchFallback = initSearchFallback();
                    if (searchFallback == null) {
                        searchFallback = new SearchFallback();
                    }
                    return searchFallback;
                }

                @Override
                public ListenableFuture<Object> reload(final String key,
                                                       Object oldValue) throws Exception {
                    return refreshPool.submit(new Callable<Object>() {
                        public Object call() throws Exception {
                            SearchFallback searchFallback = initSearchFallback();
                            if (searchFallback == null) {
                                throw new Exception("刷新搜索托底数据时，调用排行服务出错");
                            }
                            return searchFallback;
                        }
                    });
                }
            });

    /**
     * 获取托底数据
     *
     * @return 成功返回SearchFallback对象，失败返回null
     */
    public SearchFallback getSearchFallback() {
        return (SearchFallback) get(FALLBACK);
    }

    /**
     * 获取缓存值
     */
    private Object get(String key) {
        try {
            return commonCache.get(key);
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取搜索托底数据
     */
    public SearchFallback initSearchFallback() {
        QuerySalesProductRankListParam param = new QuerySalesProductRankListParam();
        param.setPageIndex(1);
        param.setPageSize(50);
        Result<PageInfo<ProductDto>> rpcResult = null;
        try {
            rpcResult = productRankService.getWeeklySalesProductRankList(param);
        } catch (Exception e) {
            logger.error("[严重异常]调用排行服务发生异常", e);
            return null;
        }

        if (!rpcResult.isSuccess()) {
            logger.error("[严重异常]调用排行服务接口返回失败");
            return null;
        }

        // 将排行服务的结果转换为searchAs的商品结果形式
        List<ASProduct> fallbackProducts = new ArrayList<ASProduct>();
        for (ProductDto item : rpcResult.getData().getList()) {
            ASProduct product = new ASProduct();
            product.setImage(item.getSquarePortalImageUrl());
            product.setPrice(item.getPrice().floatValue());
            product.setSuId(item.getSuId().toString());
            product.setShortTitle(item.getTitle());
            // 这里排行服务和datacenter里的sale_mode枚举值恰好一样，后续如继续添加枚举值，请注意此处
            product.setSaleMode(item.getSaleMode());
            if (item.getActivityType() != null && (item.getActivityType() == 1 || item.getActivityType() == 3)) {
                // 一起拼的商品 1-同事一起拼
                product.setActivities(Lists.newArrayList(CommonConstant.YIQIPIN_ACTIVITY));
            } else {
                product.setActivities(Lists.newArrayList());
            }

            fallbackProducts.add(product);
        }

        SearchFallback searchFallback = new SearchFallback();
        searchFallback.setProducts(fallbackProducts);

        return searchFallback;
    }

}
