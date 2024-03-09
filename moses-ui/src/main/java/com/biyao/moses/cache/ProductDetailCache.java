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


	//前台三级类目分组的商品ID集合
	private Map<String, List<Long>> fcate3IdPidMap = new HashMap<>();
	//后台三级类目分组的商品ID集合
	private Map<Long, List<Long>> cate3IdPidMap = new HashMap<>();
	//scmTagId分组的商品ID集合 key:scm tagId val:商品ID集合
	private Map<String, List<Long>> scmTagIdPidMap = new HashMap<>();

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

		Map<String, List<Long>> fcate3IdPidMapTmp = buildFcate3IdPidMap(productInfoMap);
		if(fcate3IdPidMapTmp != null && fcate3IdPidMapTmp.size() > 0){
			fcate3IdPidMap = fcate3IdPidMapTmp;
			log.info("[任务进度][商品分组]对所有商品按照前台三级类目分组结束，耗时{}ms，分组后个数 {}", System.currentTimeMillis() - start, fcate3IdPidMapTmp.size());
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
		return scmTagIdPidMap.get(scmTagId);
	}

	/**
	 * 根据三级类目获取商品ID列表
	 * @param thirdCategoryId
	 * @return
	 */
	public List<Long> getProductIdsByCategoryId(Long thirdCategoryId){
		if (cate3IdPidMap != null) {
			return cate3IdPidMap.get(thirdCategoryId);
		}
		return null;
	}

	/**
	 * 获取所有的前台3级类目及其下的商品ID列表
	 * @return
	 */
	public  Map<String, List<Long>> getAllFrontendCategory3ProductId(){
		if (fcate3IdPidMap != null){
			return new HashMap<>(fcate3IdPidMap);
		}
		return null;
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
	 * @return true 在， false 不在
	 */
//	public boolean isXszxPidSalesTop(Long pid){
//		return pid != null && xszxPidSalesTopSet.contains(pid);
//	}
	
	
}
