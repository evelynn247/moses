package com.biyao.moses.cache;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.biyao.client.model.PDCResponse;
import com.biyao.client.model.Product;
import com.biyao.client.model.SuProduct;
import com.biyao.client.service.IProductDubboService;
import com.biyao.moses.params.ProductInfo;
import com.biyao.moses.params.SuProductInfo;
import com.biyao.moses.util.FilterUtil;
import com.biyao.moses.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.*;

@Slf4j
public class ProductDetailCacheNoCron {

	private Map<Long, ProductInfo> productInfoMap = new HashMap<>();

	// 60*60*24*1000*3 3天毫秒数
	private final int days3ByMs = 259200000;

	private static final int PAGE_SIZE = 100;

	protected static final String IS_BEE_NEW_SPU = "isBee";
	protected static final String NOT_BEE_NEW_SPU = "notBee";
	 
	@Resource
	IProductDubboService productDubboService;
	
	@Autowired
	private ProductSexlabelCacheNoCron productSexlabelCacheNoCron;

	public ProductInfo getProductInfo(Long productId){
		if (productInfoMap!=null) {
			return productInfoMap.get(productId);
		}
		return null;
	}

	/**
	 * 初始化
	 */
	protected void init() {
		refreshProductDetailCache();
	}

	/**
	 * 每2分钟刷新一次
	 */
	protected void refreshProductDetailCache() {
		List<ProductInfo> productInfos = null;
		try {
			productInfos = getProductList();
		} catch (Exception e) {
			log.error("[严重异常][所有商品缓存]获取商品信息失败，",e);
			return;
		}
		if (CollectionUtils.isEmpty(productInfos)) {
			log.error("[严重异常][所有商品缓存]获取商品信息为空");
			return;
		}

		Map<Long, ProductInfo> temp = new HashMap<>();
		for (ProductInfo productInfo : productInfos) {
			temp.put(productInfo.getProductId(), productInfo);
		}
		productInfoMap = temp;
	}

	private List<ProductInfo> getProductList() throws Exception{
		long start = System.currentTimeMillis();
		List<ProductInfo> res = new LinkedList<>();
		List<Product> temp = new LinkedList<>();
		int pageIndex = 1;
		log.info("[任务进度][所有商品缓存]获取所有商品信息开始");
		while (true) {
			PDCResponse<List<Product>> response = productDubboService.listProductByPage(pageIndex, PAGE_SIZE);
			if (null == response || response.getCode() != 1) {
				log.error("[严重异常][所有商品缓存]调用IProductDubboService.listProductByPage方法报错，pageIndex {}， response {}",
						pageIndex ,(null == response ? "null" : JSON.toJSONString(response)));
				return null;
			}

			if (null == response.getData() || response.getData().size() == 0) {
				break;
			}
			temp.addAll(response.getData());
			pageIndex++;
		}

		if (temp.size() > 0) {
			for (Product p : temp) {
				ProductInfo productInfo = parseProduct(p);
				if (null == productInfo) {
					return null;
				}
				res.add(productInfo);
			}
		}
		long end = System.currentTimeMillis();
		log.info("[任务进度][所有商品缓存]获取所有商品信息结束，耗时={}，商品个数 {}", end - start, res.size());

		return res;
	}

	private ProductInfo parseProduct(com.biyao.client.model.Product p) {
		try {
			ProductInfo res = new ProductInfo();

			res.setProductId(p.getProductId());
			res.setSupplierId(p.getSupplierId());
			res.setFirstCategoryId(p.getFirstCategoryId());
			res.setFirstCategoryName(p.getFirstCategoryName());
			res.setSecondCategoryId(p.getSecondCategoryId());
			res.setSecondCategoryName(p.getSecondCategoryName());
			res.setThirdCategoryId(p.getThirdCategoryId());
			res.setThirdCategoryName(p.getThirdCategoryName());
			res.setFirstOnshelfTime(p.getFirstOnshelfTime());
			res.setSquarePortalImg(p.getSquarePortalImg());
			res.setSquarePortalImgWebp(p.getSquarePortalImgWebp());
			res.setRectPortalImg(p.getRectPortalImg());
			res.setRectPortalImgWebp(p.getRectPortalImgWebp());
			res.setSalePoint(p.getSalePoint());
			res.setShortTitle(p.getShortTitle());
			res.setTitle(p.getTitle());
			res.setShelfStatus(p.getShelfStatus());
			res.setSupplierBackground(p.getSupplierBackground());
			res.setRasterProduct(p.getRasterProduct());
			res.setProductPool(p.getProductPool());
//			res.setSupplierName(p.getSupplierName());
//			res.setStoreName(p.getStoreName());
			res.setSuId(p.getSuId());
			res.setMinDuration(p.getMinDuration());
			res.setPrice(p.getPrice());
//			res.setIsGroupProduct(p.getIsGroupProduct());
			res.setGroupPrice(p.getGroupPrice());
			res.setIsToggroupProduct(p.getIsToggroupProduct());
			res.setCommentNum(p.getCommentNum());
			res.setImgCount(p.getImgCount());
			res.setNegativeComment(p.getNegativeComment());
			res.setNeutralComment(p.getNeutralComment());
			res.setPositiveComment(p.getPositiveComment());
			res.setProductScore(p.getProductScore());
//			res.setHashValue(p.getHashValue());
			res.setSalesVolume7(p.getSalesVolume7());

//			res.setBaseAttribute(parseMap(p.getBaseAttribute()));
//			res.setFilterAttribute(parseMap(p.getFilterAttribute()));
//			res.setProductSpec(parseMap(p.getProductSpec()));

			res.setFCategory1Names(parseList(p.getfCategory1Names()));
			res.setFCategory1Ids(parseList(p.getfCategory1Ids()));
			res.setFCategory2Names(parseList(p.getfCategory2Names()));
			res.setFCategory2Ids(parseList(p.getfCategory2Ids()));
			res.setFCategory3Names(parseList(p.getfCategory3Names()));
			res.setFCategory3Ids(parseList(p.getfCategory3Ids()));
//			res.setFCategory3SalePoint(parseList(p.getfCategory3SalePoint()));
			res.setSearchLabels(parseList(p.getSearchLabels()));
			// 支持特权金
			res.setIsLaddergroupProduct(p.getIsLaddergroupProduct());
//			res.setSupportPrivilegeAmount(p.getSupportPrivilegeAmount());
			res.setNewUserPrivilege(p.getNewUserPrivilege());
			res.setOldUserPrivilege(p.getOldUserPrivilege());
			res.setSupportPlatform(p.getSupportPlatform());
			// 支持定制贴图 zhaiweixi 20190418
			res.setSupportTexture(p.getSupportTexture());
			//平台核心转化提升V1.7新增新手特权金抵扣金额
			res.setNewPrivilateDeduct(p.getNewPrivilateDeduct());
			res.setNewPrivilateLimit(p.getNewPrivilateLimit());
			res.setOldPrivilateLimit(p.getOldPrivilateLimit());
			// 判断商品性别并处理
			res.setProductGender(new Byte(productSexlabelCacheNoCron.getProductSexLabel(p.getProductId().toString())));
			// 如果为空或者中性
//			if (StringUtils.isBlank(sex) || MIDDLE_SEX.equals(sex)) {
//				res.setProductGender(new Byte(MIDDLE_SEX));
//				// 商品性别判断
//				String title = res.getTitle(); // 优先根据类目判断商品性别。如男装、男鞋之类的
//				if (title.indexOf("男") > 0) {
//					if (!title.contains("女")) {
//						res.setProductGender(new Byte(MALE_SEX));
//					}
//				} else if (!title.contains("男")) {
//					if (title.contains("女") || title.contains("文胸") || title.contains("唇膏") || title.contains("卫生巾")
//							|| title.contains("面膜") || title.contains("精华")) {
//						res.setProductGender(new Byte(FEMALE_SEX));
//					}
//				}
//			} else {
//				res.setProductGender(new Byte(sex));
//			}

			res.setNovicePrice(p.getNovicePrice());
//			Map<String, Map<String,String>> skuStdSaleAttrs = new HashMap<>();
//			Map<String, Set<String>> spuStdSaleAttrs = new HashMap<>();
//			parseMapAndMap(p.getSkuStdSaleAttrs(), skuStdSaleAttrs, spuStdSaleAttrs);
//			res.setSkuStdSaleAttrs(skuStdSaleAttrs);
//			res.setSpuStdSaleAttrs(spuStdSaleAttrs);
			//log.error("同步标准销售属性：原始值{}，解析后的值{}", p.getSkuStdSaleAttrs(), JSON.toJSONString(res.getSkuStdSaleAttrs()));

			//是否支持签名(0 不支持 1 支持)
			res.setSupportCarve(p.getSupportCarve());
			//低模商品类型：0-普通低模商品；1-眼镜低模商品
			res.setRasterType(p.getRasterType());
			//设置对所有人可见的好评数
			res.setGoodCommentAll(p.getGoodCommentToAll());

			res.setSalesVolume7(p.getSalesVolume7());
			res.setSalesVolume(p.getSalesVolume());
			res.setIsSetGoldenSize(p.getIsSetGoldenSize());
			res.setGoldenSizeSet(p.getGoldenSizeSet());
			res.setGoldenSizeSu(convertSuProductInfo(p.getGoldenSizeSu()));
			res.setSuProductList(convertSuProductInfo(p.getSuProductList()));
			res.setProductFacet(p.getProductFacet());
			res.setIsBee(p.getIsBee());
			res.setTagsId(p.getTagsId());
			res.setSpuStdSaleAttrs(aggrSkuAttr2Spu(res.getSuProductList()));
			//商品退货退款率
			res.setReturnRefundRate(p.getRefundRate());
			res.setIsCreator(p.getIsCreator());
			res.setSupportChannel(StringUtil.strConverToSet(p.getSupportChannel()));
			return res;
		} catch (Exception e) {
			log.error("[严重异常][所有商品缓存]解析商品信息出现异常 product {},",JSON.toJSONString(p), e);
		}

		return null;
	}

	private List<SuProductInfo> convertSuProductInfo(List<SuProduct> suList) {
		List<SuProductInfo> SuInfoList = new ArrayList<>();
		if (CollectionUtils.isEmpty(suList)) {
			return SuInfoList;
		} else {
			for (SuProduct su : suList) {
				SuProductInfo suInfo = new SuProductInfo();
				BeanUtils.copyProperties(su, suInfo);
				SuInfoList.add(suInfo);
			}
		}
		return SuInfoList;
	}

	private List<String> parseList(String str) {
		try {
			if (null == str || str.trim().length() == 0)
				return null;

			List<String> res = new ArrayList<>();
			Collections.addAll(res, str.split(","));
			return res;
		} catch (Exception e) {
			log.error("[严重异常][所有商品缓存]字符串解析为List集合出现异常，str {}，", str, e);
		}

		return null;
	}

	private Map<String, List<String>> parseMap(String str) {
		try {
			if (null == str || str.trim().length() == 0)
				return null;

			JSONObject json = JSONObject.parseObject(str);
			Map<String, List<String>> map = new HashMap<>();
			for (String k : json.keySet()) {
				String val = json.getString(k);
				List<String> list = map.computeIfAbsent(k,
						k1 -> new ArrayList<>());
				Collections.addAll(list, val.split(","));
			}

			return map;
		} catch (Exception e) {
			log.error("ProductRpcService parseMap error {}, str {}", e, str);
		}

		return null;
	}

	/**
	 * 格式为：{1300476856010200001:
	 * 			{"color":"粉红色","size":"2","price":"10.00"},
	 * 		  1300476856010500001:
	 * 		    {"color":"粉红色","size":"100","price":"12.00"},
	 * 		  1300476856010300001:
	 * 		    {"color":"粉红色","size":"3","price":"13.00"},
	 * 		  1300476856010400001:
	 * 		    {"color":"粉红色","size":"95"},"price":"14.00"}
	 * @param str
	 * @param skuStdSaleAttrs
	 * @param spuStdSaleAttrs
	 */
	private void parseMapAndMap(String str, Map<String, Map<String, String>> skuStdSaleAttrs, Map<String, Set<String>> spuStdSaleAttrs) {
		try {
			if (null == str || str.trim().length() == 0) {
				return;
			}
			JSONObject json = JSONObject.parseObject(str);
			for (String k : json.keySet()) {
				String val = json.getString(k);
				if(val == null || val.trim().length()==0) {
					continue;
				}
				JSONObject json1 = JSONObject.parseObject(val);
				Map<String, String> map1 = new HashMap<>();
				for(String k1 : json1.keySet()){
					map1.put(k1,json1.getString(k1));
					if(!spuStdSaleAttrs.containsKey(k1)){
						Set<String> set1 = new HashSet<>();
						spuStdSaleAttrs.put(k1, set1);
					}
					spuStdSaleAttrs.get(k1).add(json1.getString(k1));
				}
				skuStdSaleAttrs.put(k, map1);
			}
		} catch (Exception e) {
			log.error("ProductRpcService parseMap error {}, str {}", e, str);
		}

		return;
	}

	/**
	 * 将sku上的销售属性聚合到spu上
	 * @param suProductInfoList
	 * @return
	 */
	private Map<String, Set<String>> aggrSkuAttr2Spu(List<SuProductInfo> suProductInfoList){
		Map<String, Set<String>> spuAttrMap = new HashMap<>();
		if(CollectionUtils.isEmpty(suProductInfoList)){
			return spuAttrMap;
		}

		for(SuProductInfo suProductInfo : suProductInfoList){
			try {
				if (suProductInfo == null || StringUtils.isBlank(suProductInfo.getFacet())) {
					continue;
				}
				Map<String, String> skuAttrMap = JSONObject.parseObject(suProductInfo.getFacet(), Map.class);
				if (skuAttrMap == null || skuAttrMap.size() <= 0) {
					continue;
				}
				for(Map.Entry<String, String> entry : skuAttrMap.entrySet()){
					String attrName = entry.getKey();
					String attrValue = entry.getValue();
					if(!spuAttrMap.containsKey(attrName)){
						Set<String> attrValueSet = new HashSet<>();
						spuAttrMap.put(attrName, attrValueSet);
					}
					spuAttrMap.get(attrName).add(attrValue);
				}
			}catch (Exception e){
				log.error("[一般异常]将sku的属性聚合到spu时发生错误, suId {}",suProductInfo.getSuId(), e);
			}
		}

		return spuAttrMap;
	}

	/**
	 * 获取全量spu缓存
	 * @return
	 */
	public Map<Long, ProductInfo> getProductInfoMap() {
		return productInfoMap;
	}

	/**
	 * 根据后台三级类目将商品分组
	 * @param productInfoMap
	 * @return
	 */
	protected Map<Long, List<Long>> buildCate3IdPidMap(Map<Long, ProductInfo> productInfoMap){
		Map<Long, List<Long>> cate3IdPidMap = new HashMap<>();
		if(CollectionUtils.isEmpty(productInfoMap)){
			return cate3IdPidMap;
		}
		try {
			for (ProductInfo productInfo : productInfoMap.values()) {
				Long thirdCategoryId = productInfo.getThirdCategoryId();
				if (thirdCategoryId == null) {
					continue;
				}
				if (cate3IdPidMap.containsKey(thirdCategoryId)) {
					List<Long> list = cate3IdPidMap.get(thirdCategoryId);
					list.add(productInfo.getProductId());
				} else {
					ArrayList<Long> arrayList = new ArrayList<>();
					arrayList.add(productInfo.getProductId());
					cate3IdPidMap.put(thirdCategoryId, arrayList);
				}
			}
		}catch (Exception e){
			log.error("[严重异常]将商品按照后台三级类目分组失败，", e);
		}
		return cate3IdPidMap;
	}

	/**
	 * 根据scmTagId将商品ID分组
	 * @param productInfoMap
	 * @return
	 */
	protected  Map<String,List<Long>> buildScmTagIdPidMap(Map<Long, ProductInfo> productInfoMap) {

		Map<String, List<Long>> scmTagIdPidMap = new HashMap<>();
		if (CollectionUtils.isEmpty(productInfoMap)) {
			return scmTagIdPidMap;
		}
		try {
			for (ProductInfo productInfo : productInfoMap.values()) {
				String scmIds = productInfo.getTagsId();
				if (StringUtils.isNotEmpty(scmIds)) {
					String[] scmSplit = scmIds.split(",");
					if (scmSplit != null && scmSplit.length > 0) {
						// 此商品有scm标签
						for (int i = 0; i < scmSplit.length; i++) {
							String scmId = scmSplit[i];
							if (scmTagIdPidMap.containsKey(scmId)) {
								List<Long> list = scmTagIdPidMap.get(scmId);
								list.add(productInfo.getProductId());
							} else {
								ArrayList<Long> arrayList = new ArrayList<>();
								arrayList.add(productInfo.getProductId());
								scmTagIdPidMap.put(scmId, arrayList);
							}
						}
					}
				}
			}
		}catch (Exception e){
			log.error("[严重异常]将商品按照scmTagId分组失败，", e);
		}
		return scmTagIdPidMap;
	}

	/**
	 * 根据supplierId将商品ID分组
	 * @param productInfoMap
	 * @return
	 */
	protected Map<Long, List<Long>> buildSupplierIdPidMap(Map<Long, ProductInfo> productInfoMap){
		Map<Long, List<Long>> supplierIdPidMap = new HashMap<>();
		if (CollectionUtils.isEmpty(productInfoMap)) {
			return supplierIdPidMap;
		}
		try {
			for (ProductInfo productInfo : productInfoMap.values()) {
				// 商家商品ID列表填充
				Long supplierId = productInfo.getSupplierId();
				if (supplierId != null && !FilterUtil.isCommonFilter(productInfo)) {
					if (!supplierIdPidMap.containsKey(supplierId)) {
						supplierIdPidMap.put(supplierId, new ArrayList<>());
					}
					supplierIdPidMap.get(supplierId).add(productInfo.getProductId());
				}
			}
		}catch (Exception e){
			log.error("[严重异常]将商品按照supplierId分组失败，", e);
		}
		return supplierIdPidMap;
	}

	/**
	 * 根据后台二级类目将商品ID分组
	 * @param productInfoMap
	 * @return
	 */
	protected Map<Long, List<Long>> buildCate2IdPidMap(Map<Long, ProductInfo> productInfoMap){
		Map<Long, List<Long>> cate2IdPidMap = new HashMap<>();
		if (CollectionUtils.isEmpty(productInfoMap)) {
			return cate2IdPidMap;
		}
		try {
			for (ProductInfo productInfo : productInfoMap.values()) {
				//后台二级类目商品Id填充
				Long secondCategoryId = productInfo.getSecondCategoryId();
				if (secondCategoryId != null && !FilterUtil.isCommonFilter(productInfo)) {
					if (cate2IdPidMap.containsKey(secondCategoryId)) {
						List<Long> pidList = cate2IdPidMap.get(secondCategoryId);
						pidList.add(productInfo.getProductId());
					} else {
						List<Long> valList = new ArrayList<>();
						valList.add(productInfo.getProductId());
						cate2IdPidMap.put(secondCategoryId, valList);
					}

				}
			}
		}catch (Exception e){
			log.error("[严重异常]将商品按照前台二级类目分组失败，", e);
		}
		return cate2IdPidMap;
	}

	/**
	 * 根据前台二级类目将商品ID分组
	 * @param productInfoMap
	 * @return
	 */
	protected Map<String, List<Long>> buildFcate2IdPidMap(Map<Long, ProductInfo> productInfoMap){
		Map<String, List<Long>> fcate2IdPidMap = new HashMap<>();
		if (CollectionUtils.isEmpty(productInfoMap)) {
			return fcate2IdPidMap;
		}
		try {
			for (ProductInfo productInfo : productInfoMap.values()) {
				// 前台二级类目商品ID填充
				List<String> fCategory2IdList = productInfo.getFCategory2Ids();
				if (fCategory2IdList != null && fCategory2IdList.size() > 0) {
					for (String fCategory2Id : fCategory2IdList) {
						if (!fcate2IdPidMap.containsKey(fCategory2Id)) {
							fcate2IdPidMap.put(fCategory2Id, new ArrayList<>());
						}
						fcate2IdPidMap.get(fCategory2Id).add(productInfo.getProductId());
					}
				}
			}
		}catch (Exception e){
			log.error("[严重异常]将商品按照前台二级类目分组失败，", e);
		}
		return fcate2IdPidMap;
	}

	/**
	 * 根据前台三级类目将商品ID分组
	 * @param productInfoMap
	 * @return
	 */
	protected Map<String, List<Long>> buildFcate3IdPidMap(Map<Long, ProductInfo> productInfoMap){
		Map<String, List<Long>> fcate3IdPidMap = new HashMap<>();
		if (CollectionUtils.isEmpty(productInfoMap)) {
			return fcate3IdPidMap;
		}
		try {
			for (ProductInfo productInfo : productInfoMap.values()) {
				// 前台三级类目商品ID填充
				List<String> fCategory3IdList = productInfo.getFCategory3Ids();
				if (fCategory3IdList != null && fCategory3IdList.size() > 0) {
					for (String fCategory3Id : fCategory3IdList) {
						if (!fcate3IdPidMap.containsKey(fCategory3Id)) {
							fcate3IdPidMap.put(fCategory3Id, new ArrayList<>());
						}
						fcate3IdPidMap.get(fCategory3Id).add(productInfo.getProductId());
					}
				}
			}
		}catch (Exception e){
			log.error("[严重异常]将商品按照前台三级类目分组失败，", e);
		}
		return fcate3IdPidMap;
	}

	/**
	 * 根据是否是小蜜蜂新品将商品ID分组
	 * @param productInfoMap
	 * @return
	 */
	protected Map<String, List<Long>> buildbeePidMap(Map<Long, ProductInfo> productInfoMap){
		Map<String, List<Long>> beenPidMap = new HashMap<>();
		if (CollectionUtils.isEmpty(productInfoMap)) {
			return beenPidMap;
		}
		try {
			beenPidMap.put(IS_BEE_NEW_SPU, new ArrayList<>());
			beenPidMap.put(NOT_BEE_NEW_SPU, new ArrayList<>());
			// 当前毫秒数
			long currentTime = System.currentTimeMillis();
			for (ProductInfo productInfo : productInfoMap.values()) {
				if(productInfo.getFirstOnshelfTime()!=null){
					//是72小时内新品
					if (currentTime - productInfo.getFirstOnshelfTime().getTime() <= days3ByMs
							&&!FilterUtil.isCommonFilter(productInfo)) {
						//是小蜜蜂商品
						if (productInfo.getIsBee().equals(1)) {
							beenPidMap.get(IS_BEE_NEW_SPU).add(productInfo.getProductId());
						} else {
							beenPidMap.get(NOT_BEE_NEW_SPU).add(productInfo.getProductId());
						}
					}
				}
			}
		}catch (Exception e){
			log.error("[严重异常]将商品按照是否是小蜜蜂分组失败，", e);
		}
		return beenPidMap;
	}

}