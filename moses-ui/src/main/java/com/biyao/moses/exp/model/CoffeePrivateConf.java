package com.biyao.moses.exp.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.biyao.productclient.agent.product.IProductionService;
import com.biyao.productclient.dto.common.Result;
import com.biyao.productclient.dto.product.ProductDto;
import com.biyao.productclient.dto.product.ProductionSearchDto;

/**
 * 
 * @Description
 * @author zyj
 * @Date 2018年11月13日
 */
@EnableScheduling
@Slf4j
@Component
public class CoffeePrivateConf {
	@Autowired
	private IProductionService productionService;

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
//	@PostConstruct
	private void init() {
		log.info("初始化私人咖啡商品缓存...");
		refreshCoffeeConf();
	}

	/**
	 * 每2分钟刷新一次
	 */
//	@Scheduled(cron = "0 0/2 * * * ?")
	private void refreshCoffeeConf() {
		
		ProductionSearchDto pd = new ProductionSearchDto();
		pd.setIsDefaultCoffee(1);
		Result<List<ProductDto>> result = productionService.queryProductsByCondition(pd);
		if (result.getSuccess()) {
			List<ProductDto> productList = result.getObj();
			if (productList == null || productList.size() == 0) {
				return;
			}
			coffeeConf = productList.stream()
					.collect(Collectors.toMap(ProductDto::getProductId, i -> i.getProductId()));
		}

	}

}
