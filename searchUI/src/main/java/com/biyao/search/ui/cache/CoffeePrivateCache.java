package com.biyao.search.ui.cache;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.biyao.productclient.dto.product.ProductDto;
import com.biyao.search.ui.rpc.CoffeeProductRpcService;

import lombok.extern.slf4j.Slf4j;

/**
 * 咖啡商品信息缓存
 * @Description
 * @author zyj
 * @Date 2018年11月13日
 */
@EnableScheduling
@Component
@Slf4j
public class CoffeePrivateCache {

	@Autowired
	private CoffeeProductRpcService coffeeProductRpcService;

	private static Map<Long, Object> coffeeConf = new HashMap<Long, Object>();

	public static boolean checkCoffeePid(Long pid) {
		if (coffeeConf.containsKey(pid)) {
			return true;
		}
		return false;
	}

	/**
	 * 初始化
	 */
	@PostConstruct
	private void init() {
    	log.info("[任务报告]初始化私人咖啡商品数据，系统启动时初始化到本地缓存--》start");
		refreshCoffeeConf();
    	log.info("[任务报告]初始化私人咖啡商品数据，系统启动时初始化到本地缓存--》end");
	}
 
	public void refreshCoffeeConf() {
		
		try {
			List<ProductDto> coffeeProductDtoLst = coffeeProductRpcService.queryCoffeeProduct();
			if(! CollectionUtils.isEmpty(coffeeProductDtoLst)) {
				coffeeConf = coffeeProductDtoLst.stream()
						.collect(Collectors.toMap(ProductDto::getProductId, i -> i.getProductId()));
			}
		} catch (Exception e) {
            log.error("[严重异常]初始化私人咖啡商品数据异常", e);
		}
	}
}
