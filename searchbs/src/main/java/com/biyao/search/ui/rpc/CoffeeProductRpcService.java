package com.biyao.search.ui.rpc;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.biyao.productclient.agent.product.IProductionService;
import com.biyao.productclient.dto.common.Result;
import com.biyao.productclient.dto.product.ProductDto;
import com.biyao.productclient.dto.product.ProductionSearchDto;

import lombok.extern.slf4j.Slf4j;

/**
 * 同步咖啡商品 RPC service
 * 
 * @author biyao
 *
 */
@Service
@Slf4j
public class CoffeeProductRpcService {

	@Resource
	private IProductionService productionService;

	/**
	 * 
	 * @param pageIndex
	 * @return
	 */
	public List<ProductDto> queryCoffeeProduct() {

		Result<List<ProductDto>> result = null;
		ProductionSearchDto pd = new ProductionSearchDto();
		pd.setIsDefaultCoffee(1);
		try {
			result = productionService.queryProductsByCondition(pd);
		} catch (Exception e) {
			log.error("[严重异常][dubbo]调用product-soa服务获取咖啡商品数据(IProductionService#queryProductsByCondition)时,发生异常,param={}",
					JSON.toJSONString(pd), e);
			throw new ByRpcException("[严重异常][dubbo]调用product-soa服务获取咖啡商品数据(IProductionService#queryProductsByCondition)时,发生异常",
					"param=" + JSON.toJSONString(pd), e);
		}
		if (null == result) {
			log.error("[严重异常][dubbo]调用product-soa服务获取咖啡商品数据(IProductionService#queryProductsByCondition)时,返回结果为null,param={},",
					JSON.toJSONString(pd));
			throw new ByRpcException(					"[严重异常][dubbo]调用product-soa服务获取咖啡商品数据(IProductionService#queryProductsByCondition)时,返回结果为null",
					"param=" + JSON.toJSONString(pd));
		}
		if (! result.getSuccess()) {
			log.error( "[严重异常][dubbo]调用product-soa服务获取咖啡商品数据(IProductionService#queryProductsByCondition)时,返回接口异常,param={},result={}",
					JSON.toJSONString(pd), JSON.toJSONString(result));
			throw new ByRpcException("[严重异常][dubbo]调用product-soa服务获取咖啡商品数据(IProductionService#queryProductsByCondition)时,返回接口异常",
					"param=" + JSON.toJSONString(pd));
		}
		return result.getObj();
	}
}
