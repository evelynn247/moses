package com.biyao.moses.cache;

import com.biyao.moses.params.ProductInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@EnableScheduling
@Slf4j
public class ProductDetailCache extends ProductDetailCacheNoCron{

//	private static final long PID_NUM_MAX_LIMIT = 2000;
	//新手专享销量top 2000商品集合
//	private Set<Long> xszxPidSalesTopSet = new HashSet<>();

	//按后台三级类目ID分组的商品ID集合
	private Map<Long, List<Long>> cate3IdPidMap = new HashMap<>();
	//按scmTagId分组的商品ID集合 key:scm tagId val:商品ID集合
	private Map<String, List<Long>> scmTagIdPidMap = new HashMap<>();
	//按商家ID分组的商品ID集合
	private Map<Long, List<Long>> supplierIdPidMap = new HashMap<>();
	//按前台二级类目ID分组的商品ID集合
	private Map<String, List<Long>> fcate2IdPidMap = new HashMap<>();
	//按后台二级类目ID分组的商品ID集合
	private Map<Long, List<Long>> cate2IdPidMap = new HashMap<>();
	//按是否是小蜜蜂分组的新品商品ID集合
	private Map<String, List<Long>> beePidMap = new HashMap<>();

	@PostConstruct
	protected void init() {
		super.init();
		refreshPidMapCache();
//		refreshXszxTopProduct();
	}

	@Scheduled(cron = "0 0/2 * * * ?")
	@Override
	protected void refreshProductDetailCache() {
		super.refreshProductDetailCache();
		refreshPidMapCache();
	}

	/**
	 * 刷新按不同字段分组的商品ID缓存
	 */
	private void refreshPidMapCache(){
		log.info("[任务进度][商品分组]对所有商品进行分组开始");
		long start = System.currentTimeMillis();
		Map<Long, ProductInfo> productInfoMap = getProductInfoMap();
		if(productInfoMap == null || productInfoMap.size() <= 0){
			log.error("[严重异常][商品分组]所有商品数据为空");
			return;
		}

		Map<Long, List<Long>> cate3IdPidMapTmp = buildCate3IdPidMap(productInfoMap);
		if(cate3IdPidMapTmp != null && cate3IdPidMapTmp.size() > 0){
			cate3IdPidMap = cate3IdPidMapTmp;
			log.info("[任务进度][商品分组]对所有商品按照后台三级类目分组结束，耗时{}ms，分组后个数 {}", System.currentTimeMillis() - start, cate3IdPidMapTmp.size());
		}

		Map<String, List<Long>> scmTagIdPidMapTmp = buildScmTagIdPidMap(productInfoMap);
		if(scmTagIdPidMapTmp != null && scmTagIdPidMapTmp.size() > 0){
			scmTagIdPidMap = scmTagIdPidMapTmp;
			log.info("[任务进度][商品分组]对所有商品按照scmTagId分组结束，耗时{}ms，分组后个数 {}", System.currentTimeMillis() - start, scmTagIdPidMapTmp.size());
		}

		Map<Long, List<Long>> supplierIdPidMapTmp = buildSupplierIdPidMap(productInfoMap);
		if(supplierIdPidMapTmp != null && supplierIdPidMapTmp.size() > 0){
			supplierIdPidMap = supplierIdPidMapTmp;
			log.info("[任务进度][商品分组]对所有商品按照商家id分组结束，耗时{}ms，分组后个数 {}", System.currentTimeMillis() - start, supplierIdPidMapTmp.size());
		}

		Map<String, List<Long>> fcate2IdPidMapTmp = buildFcate2IdPidMap(productInfoMap);
		if(fcate2IdPidMapTmp != null && fcate2IdPidMapTmp.size() > 0){
			fcate2IdPidMap = fcate2IdPidMapTmp;
			log.info("[任务进度][商品分组]对所有商品按照前台二级类目ID分组结束，耗时{}ms，分组后个数 {}", System.currentTimeMillis() - start, fcate2IdPidMapTmp.size());
		}

		Map<Long, List<Long>> cate2IdPidMapTmp = buildCate2IdPidMap(productInfoMap);
		if(cate2IdPidMapTmp != null && cate2IdPidMapTmp.size() > 0){
			cate2IdPidMap = cate2IdPidMapTmp;
			log.info("[任务进度][商品分组]对所有商品按照后台二级类目ID分组结束，耗时{}ms，分组后个数 {}", System.currentTimeMillis() - start, cate2IdPidMapTmp.size());
		}

		Map<String, List<Long>> beePidMapTmp = buildbeePidMap(productInfoMap);
		if(beePidMapTmp != null && beePidMapTmp.size() > 0){
			beePidMap = beePidMapTmp;
			log.info("[任务进度][商品分组]对所有新品按照是否是小蜜蜂新品分组结束，耗时{}ms，小蜜蜂新品个数 {}，非小蜜蜂新品个数 {}", System.currentTimeMillis() - start,
					beePidMapTmp.getOrDefault(IS_BEE_NEW_SPU, new ArrayList<>()).size(),
					beePidMapTmp.getOrDefault(NOT_BEE_NEW_SPU, new ArrayList<>()).size());
		}
	}

	/**
	 * 根据三级类目获取商品ID列表
	 * @param thirdCategoryId
	 * @return
	 */
	public List<Long> getProductIdsByCategoryId(Long thirdCategoryId){
		if (cate3IdPidMap != null) {
			return cate3IdPidMap.getOrDefault(thirdCategoryId, new ArrayList<>());
		}
		return new ArrayList<>();
	}

	/**
	 * 通过scmTagId获取商品集合
	 * @param scmTagId
	 * @return
	 */
	public List<Long> getProductByScmTagId(String scmTagId){
		if(StringUtils.isEmpty(scmTagId)){
			return new ArrayList<>();
		}
		return scmTagIdPidMap.getOrDefault(scmTagId, new ArrayList<>());
	}

	/**
	 * 根据商家获取商品ID列表
	 * @param supplierId
	 * @return
	 */
	public List<Long> getProductIdsBySupplierId(Long supplierId){
		if (supplierIdPidMap != null){
			return supplierIdPidMap.getOrDefault(supplierId, new ArrayList<>());
		}
		return new ArrayList<>();
	}

	/**
	 * 根据前台二级类目获取商品ID列表
	 * @param frontendCategoryId
	 * @return
	 */
	public List<Long> getProductIdsByFrontendCategory2Id(String frontendCategoryId){
		if (fcate2IdPidMap != null){
			return fcate2IdPidMap.getOrDefault(frontendCategoryId, new ArrayList<>());
		}
		return new ArrayList<>();
	}

	/**
	 * 获取后台二级类目所有商品
	 * @param category2
	 * @return
	 */
	public List<Long> getCategory2Product(Long category2){
		if(category2==null){
			return new ArrayList<>();
		}
		return cate2IdPidMap.getOrDefault(category2, new ArrayList<>());
	}

	/**
	 * 获取全部新品商品
	 * @return
	 */
	public List<Long> getNewProductWithoutBeeList(){
		return beePidMap.getOrDefault(NOT_BEE_NEW_SPU, new ArrayList<>());
	}

	/**
	 * 获取全部新品小蜜蜂商品
	 * @return
	 */
	public List<Long> getNewBeeProductList(){

		return beePidMap.getOrDefault(IS_BEE_NEW_SPU, new ArrayList<>());
	}



	/**
	 * 刷新新手专享销量top 2000商品集合
	 */
//	@Scheduled(cron = "0 5/30 * * * ?")
//	public void refreshXszxTopProduct(){
//		Map<Long, ProductInfo> productInfoMap = this.getProductInfoMap();
//		if((productInfoMap == null) || (productInfoMap.isEmpty())){
//			return;
//		}
//
//		List<ProductInfo> allProductInfo = new ArrayList<>(productInfoMap.values());
//		List<ProductInfo> sortedList = allProductInfo.stream().filter(p -> !FilterUtil.isFilteredByBaseXszxCond(p))
//				.sorted((p1, p2) -> {
//					Long v1 = p1.getSalesVolume7() != null ? p1.getSalesVolume7() : 0L;
//					Long v2 = p2.getSalesVolume7() != null ? p2.getSalesVolume7() : 0L;
//					return -v1.compareTo(v2);
//				}).collect(Collectors.toList());
//
//		Set<Long> xszxPidSalesTopTmp = new HashSet<>();
//		int i = 0;
//		for(ProductInfo p : sortedList){
//			if(i >= PID_NUM_MAX_LIMIT){
//				break;
//			}
//			xszxPidSalesTopTmp.add(p.getProductId());
//			i++;
//		}
//
//		if(!xszxPidSalesTopTmp.isEmpty()){
//			xszxPidSalesTopSet = xszxPidSalesTopTmp;
//		}
//	}

	/**
	 * 判断商品是否在新手专享销量前2000中
	 * @param pid
	 * @return
	 */
//	public boolean isXszxPidSalesTop(Long pid){
//		return pid != null && xszxPidSalesTopSet.contains(pid);
//	}
	
}