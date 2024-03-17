package com.biyao.search.ui.cache;

import com.alibaba.dubbo.common.utils.StringUtils;
import com.biyao.client.model.Product;
import com.biyao.client.model.SuProduct;
import com.biyao.search.ui.model.SearchProductInfo;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author zhaiweixi@idstaff.com
 * @date 2019/9/5
 **/
@Component
@Slf4j
public class ProductCache {

    @Autowired
    private com.biyao.search.ui.rpc.ProductRpcService ProductRpcService;

    /**
     * 大运河V1.1项目要求搜索过滤所有定制商品，常量CUSTOMIZE = 1表示定制商品
     */
    private static final Byte CUSTOMIZE = 1;

    private Map<Long, SearchProductInfo> searchProductInfoMap;

    @PostConstruct
    public void init(){
    	log.info("[任务报告]同步商品数据，系统启动时初始化到本地缓存--》start");
        refresh();
        log.info("[任务报告]同步商品数据，系统启动时初始化到本地缓存--》end");    }

    public void refresh(){
        try{
            List<SearchProductInfo> searchProductInfoList = getProductList();
            Map<Long, SearchProductInfo> tempMap = new HashMap<>();
            if (searchProductInfoList != null && searchProductInfoList.size() > 0){
                searchProductInfoList.stream().forEach(item -> {tempMap.put(item.getProductId(), item);});
            }
            //同步商品数据时，如果本次同步商品数据为空，则不更新内存中数据
            if(CollectionUtils.isEmpty(tempMap)) {
            	log.error("[严重异常][同步商品数据异常]获取商品数据为空，可能系统出现问题了，需要立即处理");
            }else {            	
            	this.searchProductInfoMap = tempMap;
            }
         }catch (Exception e){
        	 log.error("[严重异常][同步商品数据异常]影响搜索商品数据准确性，需要立即处理，异常描述：", e );
        }
    }


    /**
     * 从pdc拉取全量商品
     * @return
     */
    private List<SearchProductInfo> getProductList(){
        List<SearchProductInfo> res = new LinkedList<>();
        List<com.biyao.client.model.Product> temp = new LinkedList<>();
        int pageIndex = 1;

        while (true) {
        	

        	try {			
        		List<Product> productLst = ProductRpcService.queryProductByPage(pageIndex);
        		if(CollectionUtils.isEmpty(productLst)) {
        			break;
        		}
        		 pageIndex++;
        		temp.addAll(productLst);
			} catch (Exception e) {
				log.error("[严重异常]同步商品数据rpc接口异常，本次同步失败，原有内存缓存数据将被清空", e);
				return null;
			}   
        }

        if (temp.size() > 0) {

            for (Product pdcProduct : temp) {
                if (CUSTOMIZE.equals(pdcProduct.getSupportTexture())) {
                    continue;
                }
                SearchProductInfo searchProductInfo = SearchProductInfo.builder()
                        .productId(pdcProduct.getProductId())
                        .supplierId(pdcProduct.getSupplierId())
                        .firstCategoryId(pdcProduct.getFirstCategoryId())
                        .firstCategoryName(pdcProduct.getFirstCategoryName())
                        .secondCategoryId(pdcProduct.getSecondCategoryId())
                        .secondCategoryName(pdcProduct.getSecondCategoryName())
                        .thirdCategoryId(pdcProduct.getThirdCategoryId())
                        .thirdCategoryName(pdcProduct.getThirdCategoryName())
                        .firstOnShelfTime(pdcProduct.getFirstOnshelfTime())
                        .squarePortalImg(pdcProduct.getSquarePortalImg())
                        .squarePortalImgWebp(pdcProduct.getSquarePortalImgWebp())
                        .rectPortalImg(pdcProduct.getRectPortalImg())
                        .rectPortalImgWebp(pdcProduct.getRectPortalImgWebp())
                        .salePoint(pdcProduct.getSalePoint())
                        .shortTitle(pdcProduct.getShortTitle())
                        .title(pdcProduct.getTitle())
                        .shelfStatus(pdcProduct.getShelfStatus())
                        .supplierBackground(pdcProduct.getSupplierBackground())
                        .supplierName(pdcProduct.getSupplierName())
                        .storeName(pdcProduct.getStoreName())
                        .suId(pdcProduct.getSuId())
                        .minDuration(pdcProduct.getMinDuration())
                        .price(pdcProduct.getPrice())
                        .isToggroupProduct(pdcProduct.getIsToggroupProduct())
                        .commentNum(pdcProduct.getCommentNum())
                        .salesVolume7(pdcProduct.getSalesVolume7())
                        .salesVolume(pdcProduct.getSalesVolume())
                        .allTogether(pdcProduct.getAllTogether())
                        .supportPlatform(pdcProduct.getSupportPlatform())
                        .supportCarve(pdcProduct.getSupportCarve())
                        .rasterType(pdcProduct.getRasterType())
                        .isLaddergroupProduct(pdcProduct.getIsLaddergroupProduct())
                        .groupPrice(pdcProduct.getGroupPrice())
                        .positiveComment(pdcProduct.getPositiveComment())
                        .newPrivilateLimit(pdcProduct.getNewPrivilateLimit())
                        .oldPrivilateLimit(pdcProduct.getOldPrivilateLimit())
                        .supportTexture(pdcProduct.getSupportTexture())
                        .newUserPrivilege(pdcProduct.getNewUserPrivilege())
                        .oldUserPrivilege(pdcProduct.getOldUserPrivilege())
                        .goodCommentToAll(pdcProduct.getGoodCommentToAll())
                        .isSetGoldenSize(pdcProduct.getIsSetGoldenSize())
                        .goldenSizeSu(pdcProduct.getGoldenSizeSu())
                        .suProductList(pdcProduct.getSuProductList())
                        .goldenSizeSet(pdcProduct.getGoldenSizeSet())
                        .productFacet(pdcProduct.getProductFacet())
                        .isAllowance(pdcProduct.getIsAllowance())
                        .allowancePrice(pdcProduct.getAllowancePrice())
                        .isCreation(pdcProduct.getIsCreator())
                        .build();
                // 前台类目特殊处理
                if (StringUtils.isNotEmpty(pdcProduct.getfCategory1Ids()) && StringUtils.isNotEmpty(pdcProduct.getfCategory1Names())){
                    searchProductInfo.setFCategory1Ids(Arrays.stream(pdcProduct.getfCategory1Ids().split(",")).collect(Collectors.toList()));
                    searchProductInfo.setFCategory1Names(Arrays.stream(pdcProduct.getfCategory1Names().split(",")).collect(Collectors.toList()));
                }

                if (StringUtils.isNotEmpty(pdcProduct.getfCategory2Ids()) && StringUtils.isNotEmpty(pdcProduct.getfCategory2Names())){
                    searchProductInfo.setFCategory2Ids(Arrays.stream(pdcProduct.getfCategory2Ids().split(",")).collect(Collectors.toList()));
                    searchProductInfo.setFCategory1Names(Arrays.stream(pdcProduct.getfCategory2Names().split(",")).collect(Collectors.toList()));
                }

                if (StringUtils.isNotEmpty(pdcProduct.getfCategory3Ids()) && StringUtils.isNotEmpty(pdcProduct.getfCategory3Names())){
                    searchProductInfo.setFCategory3Ids(Arrays.stream(pdcProduct.getfCategory3Ids().split(",")).collect(Collectors.toList()));
                    searchProductInfo.setFCategory3Names(Arrays.stream(pdcProduct.getfCategory3Names().split(",")).collect(Collectors.toList()));
                }

                // 搜索标签特殊处理
                if (StringUtils.isNotEmpty(pdcProduct.getSearchLabels())){
                    searchProductInfo.setSearchLabels(Arrays.stream(pdcProduct.getSearchLabels().split(",")).collect(Collectors.toList()));
                }

                //设置造物价
                setCreatorPrice(searchProductInfo);

                res.add(searchProductInfo);
            }
        }

        return res;
    }

    /**
     * 填充造物价
     * @param searchProductInfo
     * @return
     */
    public void setCreatorPrice(SearchProductInfo searchProductInfo){


        try{

            List<SuProduct> suProductList = searchProductInfo.getSuProductList();
            if(suProductList == null || suProductList.size()<=0){
                return;
            }
            SuProduct suProduct = null;
            for (SuProduct su:suProductList) {
                if(su.getSuId() != null && searchProductInfo.getSuId() != null &&  searchProductInfo.getSuId().compareTo(su.getSuId())==0){
                    suProduct = su;
                    break;
                }

            }
            if(suProduct == null){
                return;
            }

            Long price = suProduct.getPrice();//原价
            Long platformDiscountPrice = suProduct.getPlatformDiscountPrice();//获取平台折扣价
            Long supplierDiscountPrice = suProduct.getSupplierDiscountPrice();//获取商家折扣价

            if(price!=null && platformDiscountPrice!=null && supplierDiscountPrice!= null){
                //造物价 = 原价-平台折扣价-商家折扣价
                Long creationPrice = (suProduct.getPrice() - suProduct.getPlatformDiscountPrice() - suProduct.getSupplierDiscountPrice());
                if(creationPrice > 0 && creationPrice <= price){

                    BigDecimal creationPriceDec = new BigDecimal(creationPrice.toString());
                    BigDecimal hundred = new BigDecimal("100");
                    String  creationPriceStr = creationPriceDec.divide(hundred).stripTrailingZeros().toPlainString();
                    //creationPrice = creationPrice/100;//上述三个价格的单位都是分，需要转成元，所以除以100
                    suProduct.setCreatorPrice(creationPriceStr);//设置SKU造物价
                    searchProductInfo.setCreationPriceStr(creationPriceStr);//设置SPU造物价
                }
            }
        }catch (Exception e){
            log.error("[严重异常]缓存商品造物价异常", e);
        }

    }




    /**
     * 从缓存中获取商品信息(by pid)
     * @param productId
     * @return
     */
    public SearchProductInfo getSearchProductInfo(Long productId){
        if (this.searchProductInfoMap != null){
            return this.searchProductInfoMap.get(productId);
        }
        return null;
    }


    /**
     * 从缓存中获取商品信息(all)
     * @return
     */
    public Map<Long,SearchProductInfo> getAllSearchProductInfo(){
        if (this.searchProductInfoMap != null){
            return this.searchProductInfoMap;
        }
        return null;
    }
}
