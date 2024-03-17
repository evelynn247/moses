package com.biyao.search.ui.cache;

import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.fastjson.JSON;
import com.biyao.client.model.DeriveProduct;
import com.biyao.css.dubbo.dto.sync.SyncCelebrityDto;
import com.biyao.search.ui.model.DeriveProductInfo;
import com.biyao.search.ui.model.SaleAgent;
import com.biyao.search.ui.rpc.DeriveProductRpcService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * @author zj
 * @version 1.0
 * @date 2020/1/21 14:21
 * @description
 */
@Slf4j
@Component
@EnableScheduling
public class DeriveProductDetailCache {


	@Autowired
	private DeriveProductRpcService deriveProductRpcService;

    @Autowired
    SyncVDataCache syncVDataCache;

    private static final Byte ON_SALE = 1;

    //大V类型
    private static final int V_TYPE = 2;

    //企业定制号类型 此处后端服务返回和前端约定映射不一致，因此此处特殊转换
    private static final int ENTERPRISE_TYPE = 6;

    private Map<String,DeriveProductInfo> onSaleSearchDeriveProductMap = new HashMap<>();


    /**
     * 从缓存中获取商品信息(by pid)
     * @param productId
     * @return
     */
    public DeriveProductInfo getDeriveProductInfo(String productId){
        if (this.onSaleSearchDeriveProductMap != null){
            return this.onSaleSearchDeriveProductMap.get(productId);
        }
        return null;
    }


    /**
     * 初始化
     */
    @PostConstruct
    public void init() {
        log.info("[任务报告]同步衍生商品数据，系统启动时初始化到本地缓存--》start");
        refreshProductDetailCache();
        log.info("[任务报告]同步衍生商品数据，系统启动时初始化到本地缓存--》end");
    }

    public void refreshProductDetailCache() {
    	try {
    		 
            List<DeriveProductInfo> tempSearchProductList = filterAndCompleteIdentityInfo(getProductList());
            Map<String,DeriveProductInfo> tempMap = new HashMap<>();
            if(tempSearchProductList != null){
                tempSearchProductList.forEach(item -> {tempMap.put(item.getProductId(), item);});
            }

            this.onSaleSearchDeriveProductMap = tempMap; 
		} catch (Exception e) {
			 log.error("[严重异常][同步衍生商品数据异常]影响搜索衍生商品数据准确性，需要立即处理，异常描述：", e );
		}
    }
      
    /**
     * 从pdc获取衍生(一次定制)商品信息
     * @return
     */
    private List<DeriveProductInfo> getProductList(){

        long start = System.currentTimeMillis();
        List<DeriveProductInfo> res = new LinkedList<>();
        List<com.biyao.client.model.DeriveProduct> temp = new LinkedList<>();
        int pageIndex = 1;

        // pdcservice中获取全量衍生商品数据
        while (true) {
        	try {			
        		List<DeriveProduct> deriveProductLst = deriveProductRpcService.queryDeriveProductByPage(pageIndex);
        		if(CollectionUtils.isEmpty(deriveProductLst)) {
        			break;
        		}
        		 pageIndex++;
        		temp.addAll(deriveProductLst);
			} catch (Exception e) {
				log.error("[严重异常]同步衍生商品数据rpc接口异常，本次同步失败，原有内存缓存数据将被清空", e);
				return null;
			}  
        }
        //衍生商品信息DTO--》VO转换
        if (temp.size() > 0) {
            for (DeriveProduct p : temp) {
                if (!ON_SALE.equals(p.getShelfStatus())) {
                    continue;
                }
                DeriveProductInfo searchProduct = parseProduct(p);
                if (null == searchProduct) {
                    continue;
                }
                res.add(searchProduct);
            }
        }
        log.info("[操作日志]同步衍生商品数据，获取所有商品缓存耗时={}", (System.currentTimeMillis() - start));

        return res;

    }

    private DeriveProductInfo parseProduct(DeriveProduct p) {

        DeriveProductInfo sp = new DeriveProductInfo();
        try{
            sp.setProductId(p.getProductId());
            sp.setSuId(p.getSuId());
            sp.setSupplierId(p.getSupplierId());
            sp.setStoreName(p.getStoreName());
            sp.setMinDuration(p.getMinDuration());
            sp.setTitle(p.getTitle());
            sp.setShortTitle(p.getShortTitle());
            sp.setFirstOnShelfTime(p.getFirstOnShelfTime());
            sp.setSalePoint(p.getSalePoint());
            sp.setFCategory3SalePoint(p.getfCategory3SalePoint());
            sp.setSquarePortalImg(p.getSquarePortalImg());
            sp.setSquarePortalImgWebp(p.getSquarePortalImgWebp());
            sp.setFCategory1Ids(p.getfCategory1Ids());
            sp.setFCategory2Ids(p.getfCategory2Ids());
            sp.setFCategory3Ids(p.getfCategory3Ids());
            sp.setFCategory1Names(p.getfCategory1Names());
            sp.setFCategory2Names(p.getfCategory2Names());
            sp.setFCategory3Names(p.getfCategory3Names());
            sp.setFirstCategoryId(p.getFirstCategoryId());
            sp.setFirstCategoryName(p.getFirstCategoryName());
            sp.setSecondCategoryId(p.getSecondCategoryId());
            sp.setSecondCategoryName(p.getSecondCategoryName());
            sp.setThirdCategoryName(p.getThirdCategoryName());
            sp.setThirdCategoryId(p.getThirdCategoryId());
            sp.setSupplierBackground(p.getSupplierBackground());
            sp.setPrice(p.getPrice());
            sp.setGroupPrice(p.getGroupPrice());
            sp.setDeriveAddvalTotalPrice(p.getDeriveAddvalTotalPrice());
            sp.setIsToggroupProduct(p.getIsToggroupProduct());
            sp.setSalesVolume7(p.getSalesVolume7());
            sp.setSalesVolume(p.getSalesVolume());
            sp.setSupportPlatform(p.getSupportPlatform());
            sp.setCreateUid(p.getCreateUid());

        }catch(Exception e){
            log.error("[严重异常]同步衍生商品数据数据转换异常，该商品数据同步失败，DeriveProductId={},DeriveProduct={}", p.getProductId(), JSON.toJSONString(p), e);
            return null;
        }

        return sp;
    }

    /**
     * 过滤并补全身份信息，如果创建用户身份不是大V或者企业定制用户，则过滤掉。
     * @param tempSearchProductList
     */
    private List<DeriveProductInfo> filterAndCompleteIdentityInfo(List<DeriveProductInfo> tempSearchProductList) {
        if(tempSearchProductList == null || tempSearchProductList.size() == 0){
            return new ArrayList<>();
        }
        List<DeriveProductInfo> result = new ArrayList<>();

        for (DeriveProductInfo deriveProductInfo:tempSearchProductList) {
            SyncCelebrityDto vDto = syncVDataCache.getVMapById(deriveProductInfo.getCreateUid());
            SyncCelebrityDto enterpriseDto = syncVDataCache.getEnterpriseMapById(deriveProductInfo.getCreateUid());

            if(vDto == null && enterpriseDto==null){
                continue;
            }
            if(vDto != null){
                SaleAgent saleAgent = new SaleAgent();
                saleAgent.setIdentityType(String.valueOf(V_TYPE));
                saleAgent.setNickName(StringUtils.isBlank(vDto.getRealName())?vDto.getNickname():vDto.getRealName());
                saleAgent.setPortrait(vDto.getAvatarUrl());
                deriveProductInfo.setSaleAgent(saleAgent);
            }
            if(enterpriseDto != null){
                SaleAgent saleAgent = new SaleAgent();
                saleAgent.setIdentityType(String.valueOf(ENTERPRISE_TYPE));
                saleAgent.setNickName(StringUtils.isBlank(enterpriseDto.getRealName())?enterpriseDto.getNickname():enterpriseDto.getRealName());
                saleAgent.setPortrait(enterpriseDto.getAvatarUrl());
                deriveProductInfo.setSaleAgent(saleAgent);
            }

            result.add(deriveProductInfo);

        }
        return result;

    }
}
