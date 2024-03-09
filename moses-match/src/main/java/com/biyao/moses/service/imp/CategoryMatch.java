package com.biyao.moses.service.imp;

import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.cache.RedisCache;
import com.biyao.moses.model.exp.MatchDataSourceTypeConf;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.params.ProductInfo;
import com.biyao.moses.rpc.CmsRpcService;
import com.biyao.moses.service.RecommendMatch;
import com.biyao.moses.util.FacetUtil;
import com.biyao.moses.util.FilterUtil;
import com.biyao.moses.util.ProductWeightUtil;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商品类目match
 * 
 * @Description
 * @author zyj
 * @Date 2018年12月21日
 */
@Slf4j
@Component("CM")
public class CategoryMatch implements RecommendMatch {

	@Autowired
	ProductDetailCache productDetailCache;

	@Resource
	CmsRpcService cmsRpcService;

	@Autowired
	RedisCache redisCache;


	@BProfiler(key = "com.biyao.moses.service.imp.CategoryMatch.executeRecommendMatch",
			monitorType = {MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR})
	@Override
	public Map<String, List<TotalTemplateInfo>> executeRecommendMatch(String dataKey, MatchDataSourceTypeConf mdst,
			String uuId) {
		Map<String, List<TotalTemplateInfo>> resultMap = new HashMap<>();
		try {
			String oriKey = dataKey;
		 
			List<TotalTemplateInfo> totalList = new ArrayList<>();

			List<String> category = mdst.getCategoryIds();
			List<String> scmIds = mdst.getScmIds();
			
			if ((category==null||category.size()==0)&&(scmIds==null||scmIds.size()==0)) {
				resultMap.put(oriKey, totalList);
				return resultMap;
			}
//			if (category.size() == 0) {
//				resultMap.put(oriKey, totalList);
//				return resultMap;
//			}
			for (String categoryId : category) {
				List<Long> productIdsByCategoryId = productDetailCache.getProductIdsByCategoryId(Long.valueOf(categoryId));
				if (productIdsByCategoryId==null||productIdsByCategoryId.isEmpty()) {
					continue;
				}
				for (Long spuId : productIdsByCategoryId) {
					fillTotalList(totalList, spuId, mdst);
				}
			}
			
			if (scmIds!=null&&scmIds.size()>0) {
				
				for (String scmId : scmIds) {
					List<Long> productIdsByScmId = productDetailCache.getProductByScmTagId(scmId);
					if (productIdsByScmId==null||productIdsByScmId.isEmpty()) {
						continue;
					}
					for (Long spuId : productIdsByScmId) {
						fillTotalList(totalList, spuId, mdst);
					}

				}
			}

			//平台核心转化提升V1.7新增计算特权金抵扣金额逻辑
			totalList = calctPriDeductAmount(mdst, totalList, uuId);

			//计算权重
			ProductWeightUtil.calculationWeight(mdst.getWeight(), totalList);

			resultMap.put(oriKey, totalList);
		} catch (Exception e) {
			log.error("[严重异常]类目页Match出现异常，uuid {}，frontCategoryId {}, ", uuId, mdst.getFrontendCategoryId(), e);
		}
		return resultMap;

	}

	private void fillTotalList(List<TotalTemplateInfo> totalList, Long spuId, MatchDataSourceTypeConf mdst){
		try {
			ProductInfo productInfo = productDetailCache.getProductInfo(spuId);
			if(mdst.getIsShowCustomProduct() == null) {
				boolean isNotFilterCustomPid = redisCache.isNotFilterCustomPidFcateId(mdst.getFrontendCategoryId());
				//如果需要过滤定制商品，则直接返回
				if (!isNotFilterCustomPid && FilterUtil.isCommonFilter(productInfo)) {
					return;
				}
			}else {
				//如果不展示定制商品 且该商品是定制或者下架商品 则直接返回
				if (!mdst.getIsShowCustomProduct() && FilterUtil.isCommonFilter(productInfo)) {
					return;
				}
			}

			TotalTemplateInfo tti = new TotalTemplateInfo();
			tti.setId(String.valueOf(spuId));
			boolean notFilterFlag = true;
			if (spuId != null && (productInfo) != null) {
				notFilterFlag = FacetUtil.filterAndFillInfoBySelectAtrrs(tti, productInfo, mdst.getSelectedScreenAttrs());
			}
			if (notFilterFlag) {
				totalList.add(tti);
			}
		}catch(Exception e){
			log.error("[严重异常]类目页match填充数据异常，spuId {}， fcateId {}, e", spuId, mdst.getFrontendCategoryId(), e);
		}
	}
	/**
	 * 平台核心转化提升V1.7新增计算特权金抵扣金额逻辑
	 * @param mdst
	 * @param totalList
	 * @return
	 */
	private List<TotalTemplateInfo> calctPriDeductAmount(MatchDataSourceTypeConf mdst, List<TotalTemplateInfo> totalList, String uuid) {

		//如果没有特权金，不计算特权金优惠
		if(mdst.getPriCouponAmountList() == null || mdst.getPriCouponAmountList().trim().isEmpty()){
			return totalList;
		}

		try {
			/*
			 * 解析特权金面额
			 * UI层有格式校验，所以直接拆，不需要验证
			 */

			//新客特权金面额
			int couponNewPriAmount = 0;
			//老客特权金面额
			int couponComPriAmount = 0;

			String[] coupons = mdst.getPriCouponAmountList().split(",");
			for(String copn : coupons){
				String[] temp = copn.split(":");
				if("1".equals(temp[0])){
					couponNewPriAmount = Integer.valueOf(temp[1]);
				}else if("2".equals(temp[0])){
					couponComPriAmount = Integer.valueOf(temp[1]);
				}
			}

			/*
			 * 如果同时有通用特权金和新客特权金，有可能用到cms素材，提前加载一次
			 * 0: 加载失败
			 * 1：一起拼编辑器
			 * 2：普通编辑器
			 */
			int cmsSwitch = 0;
			if(couponComPriAmount > 0 && couponNewPriAmount > 0){
				cmsSwitch = this.cmsRpcService.queryProductEditorSwitch();
			}


			//循环计算每一个商品的特权金优惠
			for(TotalTemplateInfo p : totalList){

				ProductInfo productInfo = productDetailCache.getProductInfo(Long.valueOf(p.getId()));

				//容错
				if(productInfo == null){
					continue;
				}

				/*
				 * 先判断最终应该使用哪种特权金
				 * 如果只有一种特权金，直接使用
				 * 如果同时拥有新客和通用，根据商品池和素材判断
				 */

				//默认使用通用特权金，1新客 2通用
				int couponType = 2;

				if(couponComPriAmount > 0 && couponNewPriAmount > 0){
					/*
					 * 同时拥有新客和通用，根据商品池和素材判断
					 * 	先判断是否仅在一起拼或新客特权金商品池
					 * 		一起拼商品池使用通用特权金
					 * 		新客特权金商品池使用新客特权金
					 * 	如果同时在两个商品池，根据cms素材判断
					 * 		如果是一起拼编辑器使用通用特权金
					 * 		如果是普通编辑器，使用新客特权金
					 */
					if (1 == productInfo.getIsToggroupProduct().intValue() && 1 == productInfo.getNewUserPrivilege()) {

						if(1 == cmsSwitch){
							//一起拼编辑器，通用特权金
							couponType = 2;
						}else if(2 == cmsSwitch){
							//普通编辑器，新手特权金
							couponType = 1;
						}

					} else if (1 == productInfo.getIsToggroupProduct().intValue()) {
						//如果商品仅在一起拼商品池，用通用特权金
						couponType = 2;
					} else if (1 == productInfo.getNewUserPrivilege()) {
						//如果商品仅在新手特权金商品池，用新手特权金
						couponType = 1;
					}


				}else if(couponComPriAmount > 0){
					//只有通用特权金
					couponType = 2;
				}else if(couponNewPriAmount > 0){
					//只有新客特权金
					couponType = 1;
				}

				BigDecimal deductPrice = BigDecimal.ZERO;
				int finalCouponAmount = 0;
				if (couponType == 1) {
					//特权金券类型 1 新客特权金
					deductPrice = productInfo.getNewPrivilateDeduct();
					finalCouponAmount = couponNewPriAmount;

				} else if (couponType == 2) {
					//特权金券类型 2 通用特权金
					finalCouponAmount = couponComPriAmount;
					if (StringUtils.equals("1", mdst.getUserType())) {
						//用户类型 1 新客
						deductPrice = productInfo.getNewPrivilateLimit();

					} else if (StringUtils.equals("2", mdst.getUserType())) {
						//	用户类型 2老客
						deductPrice = productInfo.getOldPrivilateLimit();
					}
				}
				String PriDeductAmount = null;
				if (deductPrice != null && deductPrice.compareTo(BigDecimal.ZERO) == 1) {
					PriDeductAmount = String.valueOf(Math.min(finalCouponAmount, deductPrice.intValue()));
				}
				p.setPriDeductAmount(PriDeductAmount);

			}
		} catch (NumberFormatException e) {
			log.error("[严重异常]类目页match计算特权金优惠金额时发生异常，uuid {}, frontCategoryId {}", uuid, mdst.getFrontendCategoryId(), e);
		}

		//如果是特权金排序，过滤掉没有优惠的商品
		if("3".equals(mdst.getSortType())){
			//过滤掉特权金抵扣金额为0的数据
			totalList = totalList.stream().filter(p -> p.getPriDeductAmount() != null && !"0".equals(p.getPriDeductAmount()) ).collect(Collectors.toList());
		}

		return totalList;
	}

}
