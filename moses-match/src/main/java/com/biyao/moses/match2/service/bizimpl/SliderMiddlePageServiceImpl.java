package com.biyao.moses.match2.service.bizimpl;

import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.common.constant.BizNameConst;
import com.biyao.moses.match2.service.BizService;
import com.biyao.moses.model.match2.MatchItem2;
import com.biyao.moses.params.ProductInfo;
import com.biyao.moses.params.match2.MatchRequest2;
import com.biyao.moses.util.FilterUtil;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;


/**
 * 小蜜蜂轮播图落地页match2
 */
@Slf4j
@Component(value = BizNameConst.SLIDER_MIDDLE_PAGE2)
public class SliderMiddlePageServiceImpl implements BizService {

    @Autowired
    ProductDetailCache productDetailCache;
    @Autowired
    FilterUtil filterUtil;

    //共推出商品目标数
    private final int targetNum = 200;

    // 60*60*24*1000*3 3天
    private final int days3ByMs = 259200000;

    @BProfiler(key = "SliderMiddlePageServiceImpl.match",
            monitorType = {MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR})
    @Override
    public List<MatchItem2> match(MatchRequest2 request) {
        List<MatchItem2> resultList = new ArrayList<>();
        //点击的轮播图商品
        String priorityProductId = request.getPriorityProductId();
        String siteIdStr= request.getSiteId()==null ? null:String.valueOf(request.getSiteId());
        if (StringUtils.isBlank(priorityProductId)) {
            return resultList;
        }

        ProductInfo productInfo = productDetailCache.getProductInfo(Long.valueOf(priorityProductId));
        if (productInfo == null) {
            return resultList;
        }
        //相同商家商品
        List<Long> sameSupplierList = productDetailCache.getProductIdsBySupplierId(productInfo.getSupplierId());
        // 过滤掉不支持用户持有端的相同商家商品
        sameSupplierList = sameSupplierList.stream().filter(productId ->{
            if(filterUtil.isFilteredBySiteId(productId,siteIdStr)){
                return false;
            }
            return true;
        }).collect(Collectors.toList());

        int category2ProductNum = 0;
        if (sameSupplierList.size() < 200) {
            category2ProductNum = targetNum - sameSupplierList.size();
        } else {
            sameSupplierList = sameSupplierList.subList(0, 200);
        }
        Set<Long> allProductSet = new HashSet<>(sameSupplierList);
        //如果相同商家商品不够200个，使用相同2级类目商品补至200
        List<Long> category2ProductList = productDetailCache.
                getCategory2Product(productInfo.getSecondCategoryId());
        if (category2ProductNum > 0 && CollectionUtils.isNotEmpty(category2ProductList)) {
            int j=0;
            for (int i = 0; i < category2ProductList.size(); i++) {
               if (j >= category2ProductNum) {
                    break;
                }
               if(filterUtil.isFilteredBySiteId(category2ProductList.get(i),siteIdStr)){
                   continue;
               }
               allProductSet.add(category2ProductList.get(i));
               j++;
            }
        }

        //拆分商品 焦点图对应商家新品:0 普通新品:1 轮播图对应商家老品:2 其他二级类目老品:3
        List<Long> productList = selectListByTypes(allProductSet, productInfo.getSupplierId());

        //转化为match返回结果
        for (Long pid : productList) {
            MatchItem2 item = new MatchItem2();
            item.setProductId(pid);
            item.setSource(BizNameConst.SLIDER_MIDDLE_PAGE2);
            resultList.add(item);
        }
        return resultList;
    }

    /**
     * 拆分集合为
     * 焦点图对应商家新品 0
     * 普通新品 1
     * 轮播图对应商家老品 2
     * 其他二级类目老品 3
     *
     * @param AllProductSet
     * @return
     */
    private List<Long> selectListByTypes(Set<Long> AllProductSet, Long supplierId) {
        List<Long> number0List = new ArrayList<>(); //焦点图对应商家新品 0
        List<Long> number1List = new ArrayList<>(); //普通新品 1
        List<Long> number2List = new ArrayList<>(); //轮播图对应商家老品 2
        List<Long> number3List = new ArrayList<>(); //其他二级类目老品 3
        long currentTime = System.currentTimeMillis();

        Iterator<Long> iterator = AllProductSet.iterator();
        while (iterator.hasNext()) {
            Long pid = iterator.next();
            ProductInfo productInfo = productDetailCache.getProductInfo(pid);
            try {
                //是72小时内新品
                if (currentTime - productInfo.getFirstOnshelfTime().getTime() <= days3ByMs) {
                    if (productInfo.getSupplierId().equals(supplierId)) {
                        number0List.add(pid);//焦点图对应商家新品 0
                    } else {
                        number1List.add(pid);//普通新品 1
                    }
                } else {
                    if (productInfo.getSupplierId().equals(supplierId)) {
                        number2List.add(pid);//轮播图对应商家老品 2
                    } else {
                        number3List.add(pid);//其他二级类目老品 3
                    }
                }
            } catch (Exception e) {
                log.error("轮播图落地页match2中组装数据失败", e);
                continue;
            }
        }

        //随机化
        Collections.shuffle(number0List);
        Collections.shuffle(number1List);
        Collections.shuffle(number2List);
        Collections.shuffle(number3List);

        number0List.addAll(number1List);
        number0List.addAll(number2List);
        number0List.addAll(number3List);

        return number0List;

    }
}
