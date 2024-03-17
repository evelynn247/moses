package com.biyao.search.ui.rest.impl.module;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.biyao.search.ui.util.RedisUtil;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.biyao.search.as.service.ASSearchService;
import com.biyao.search.as.service.enums.PlatformEnum;
import com.biyao.search.as.service.model.request.ASSearchRequest;
import com.biyao.search.as.service.model.response.ASSearchResponse;
import com.biyao.search.common.constant.SearchLimit;
import com.biyao.search.common.constant.SearchStatus;
import com.biyao.search.common.enums.SearchOrderByEnum;
import com.biyao.search.common.model.ASProduct;
import com.biyao.search.common.model.RPCResult;
import com.biyao.search.common.model.Status;
import com.biyao.search.ui.model.RequestBlock;
import com.google.common.collect.Lists;

/**
 * 从AS请求搜索结果
 */
public class ModRequestAS implements UIModule {

    @Autowired
    private  ASSearchService asSearchService;
    @Autowired
	private RedisUtil redisUtil;
    
    private static final String SEARCH_RESULT_CACHE_PREFIX = "search_page_result_";
    private static final int CACHE_TIME = 30 * 60; // 半小时
    
    private static ExecutorService threadPool = Executors.newFixedThreadPool(20);

    /**
     * 初始化操作
     * 程序启动时执行一次
     */
    public void init(){}
    
    /**
     * 清理操作
     * 启动关闭时执行一次
     */
    public void destroy(){}

    /**
     * 处理请求
     */
    public Status run(RequestBlock request ) {
        
        /**
         *  超出条数限制
         */
        if( (request.getPageIndex()-1) * request.getPageSize() >= SearchLimit.MAX_HIT_COUNT ) {
            ASSearchResponse<ASProduct> asResponse= new ASSearchResponse<ASProduct>();
            asResponse.setHitTotal( SearchLimit.MAX_HIT_COUNT );
            request.setAsResponse( asResponse );
            return SearchStatus.OK;
        }
        
        ASSearchResponse<ASProduct> asResponse = null;
        if (request.getPageIndex() == 1) { // 首页请求
        	RPCResult<ASSearchResponse<ASProduct>> asResult = requestAs(request);
            if( asResult == null ) {
                return SearchStatus.AS.UNKNOWN;
            }
            if( ! SearchStatus.OK.equals( asResult.getStatus() ) ) {
            	return asResult.getStatus();
            }
            
            asResponse = asResult.getData();
            
            // 根据用户传入的排序方式进行重排
            asResponse.setResult(rerankASProducts(asResponse.getResult(), request));
            asResponse.setOrderBy(request.getOrderBy());
            
            asResponse = cache2redisAndReturnFirstPage(request, asResponse);
        } else { //　翻页请求
        	String cacheKey = SEARCH_RESULT_CACHE_PREFIX + request.getSid();
        	asResponse = (ASSearchResponse<ASProduct>) redisUtil.hget(cacheKey, request.getPageIndex().toString());
        	if (asResponse == null) {
        		asResponse = new ASSearchResponse<>();
        	}
        	
        	// 删除此页缓存
        	redisUtil.hdel(cacheKey, request.getPageIndex().toString());
        }
        
       
        /**
         *  返回AS的返回结果
         */
        if( asResponse.getHitTotal() > SearchLimit.MAX_HIT_COUNT ) {
            asResponse.setHitTotal( SearchLimit.MAX_HIT_COUNT );
        }
        
        request.setAsResponse( asResponse );
        return SearchStatus.OK;
    }

	private List<ASProduct> rerankASProducts(List<ASProduct> asProducts,
			RequestBlock request) {
		if (asProducts == null) {
			return new ArrayList<ASProduct>();
		}
		if (asProducts.size() == 0) {
			return asProducts;
		}
		
		switch (request.getOrderBy()) {
		case PRICE_ASC:
			asProducts.sort((a, b) -> a.getPrice().compareTo(b.getPrice()));
			break;
		case PRICE_DESC:
			asProducts.sort((a, b) -> b.getPrice().compareTo(a.getPrice()));
			break;
		case SALE_QUANTITY:
			asProducts.sort((a, b) -> b.getWeekSaleNum().compareTo(a.getWeekSaleNum()));
			break;
		default:
			break;
		}
		
		return asProducts;
	}

	private ASSearchResponse<ASProduct> cache2redisAndReturnFirstPage(
			RequestBlock request,
			ASSearchResponse<ASProduct> asResponse) {
		List<ASProduct> allProducts = asResponse.getResult();
		
		if (allProducts.size() == 0) {
			return asResponse;
		}
		
		// 对结果进行拷贝，用于立即返回。原有结果用于异步缓存
		ASSearchResponse<ASProduct> copyOfasResponse = null;
		try {
			copyOfasResponse = (ASSearchResponse<ASProduct>) BeanUtils.cloneBean(asResponse);
		} catch (Exception e) {
			e.printStackTrace();
			asResponse.setResult(Lists.newArrayList(allProducts.subList(0, allProducts.size() > request.getPageSize() ?
					request.getPageSize() : allProducts.size())));
			return asResponse;
		}

		
		// 第二页开始的结果进行缓存
		int pageCount = allProducts.size() % request.getPageSize() == 0 ? 
				allProducts.size() / request.getPageSize() : allProducts.size() / request.getPageSize() + 1;
		threadPool.submit(new Runnable() {
			@Override
			public void run() {
				String cacheKey = SEARCH_RESULT_CACHE_PREFIX + request.getSid();
				for (int i = 1; i < pageCount; i++) {
					asResponse.setResult(Lists.newArrayList(allProducts.subList(i * request.getPageSize(), 
							allProducts.size() > (i + 1) * request.getPageSize() ? (i + 1) * request.getPageSize() : allProducts.size())));
					redisUtil.hset(cacheKey, i + 1 + "", asResponse, CACHE_TIME);
				}
			}
		});
		
		// 返回首页结果
		copyOfasResponse.setResult(Lists.newArrayList(allProducts.subList(0, allProducts.size() > request.getPageSize() ?
				request.getPageSize() : allProducts.size())));
		return copyOfasResponse;
	}

	private RPCResult<ASSearchResponse<ASProduct>> requestAs(
			RequestBlock request) {
		/**
         *  向AS发起请求
         */
        ASSearchRequest asRequest = new ASSearchRequest();
        asRequest.setPageIndex(1);
        asRequest.setPageSize(SearchLimit.MAX_HIT_COUNT);
        asRequest.setQuery( request.getQuery() );
        asRequest.setPlatform(PlatformEnum.getByName(request.getPlatform()));
        asRequest.setUuid(request.getUuid());
        asRequest.setSid(request.getSid());
        asRequest.setOrderBy(SearchOrderByEnum.NORMAL); // TODO 只按照NORMAL排序即可，其他排序在ui里做
        
        RPCResult<ASSearchResponse<ASProduct>> asResult = null;
        
        long start = System.currentTimeMillis();
        try {
            asResult = asSearchService.search(asRequest);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // AS请求消耗的时间
        request.setAsTookTime( (int)(System.currentTimeMillis()-start) );
        
        return asResult;
	}
}
