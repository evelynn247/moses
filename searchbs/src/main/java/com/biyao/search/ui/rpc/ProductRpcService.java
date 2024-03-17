package com.biyao.search.ui.rpc;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.biyao.client.model.PDCResponse;
import com.biyao.client.model.Product;
import com.biyao.client.service.IProductDubboService;

import lombok.extern.slf4j.Slf4j;

/**
 * 同步商品数据 RPC service 
 * @author biyao
 *
 */
@Service
@Slf4j
public class ProductRpcService {

	private static final int PAGE_SIZE = 100;
	
	 @Resource
	 private IProductDubboService pdcDubboService;
	 
	/**
	 * 分页获取商品数据
	 * @param pageIndex
	 * @return
	 */
	 public List<Product> queryProductByPage(int pageIndex){
		 
		 PDCResponse<List<Product>> response = null;
		 try {
			 response = pdcDubboService.listProductByPage(pageIndex, PAGE_SIZE);
		} catch (Exception e) {
			log.error("[严重异常][dubbo]调用pdcservice服务获取商品数据(IProductDubboService#listProductByPage)时,发生异常,pageIndex={}", pageIndex, e);
			throw new ByRpcException("[严重异常][dubbo]调用pdcservice服务获取商品数据(IProductDubboService#listProductByPage)时,发生异常", "pageIndex="+pageIndex, e);
		}
		 if (null == response ) {
             log.error("[严重异常][dubbo]调用pdcservice服务获取商品数据(IProductDubboService#listProductByPage)时,返回结果为null,pageIndex={},", pageIndex);
             throw new ByRpcException("[严重异常][dubbo]调用pdcservice服务获取商品数据(IProductDubboService#listProductByPage)时,返回结果为null", "pageIndex="+pageIndex);
         }
		 if(response.getCode() != 1) {
			 log.error("[严重异常][dubbo]调用pdcservice服务获取商品数据(IProductDubboService#listProductByPage)时,返回接口异常,pageIndex={},response={}", 
					 pageIndex, JSON.toJSONString(response));
			 throw new ByRpcException("[严重异常][dubbo]调用pdcservice服务获取商品数据(IProductDubboService#listProductByPage)时,返回接口异常", "pageIndex="+pageIndex);
		 }
		 return response.getData();
	 }
}
