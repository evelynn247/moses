package com.biyao.moses.match2.service.impl;

import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.cache.ProductSeasonCache;
import com.biyao.moses.match2.constants.MatchStrategyConst;
import com.biyao.moses.match2.param.MatchParam;
import com.biyao.moses.match2.service.Match2;
import com.biyao.moses.match2.util.MatchUtil;
import com.biyao.moses.model.match2.MatchItem2;
import com.biyao.moses.params.ProductInfo;
import com.biyao.moses.rpc.UcRpcService;
import com.biyao.moses.util.FilterUtil;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import com.uc.domain.bean.User;
import com.uc.domain.constant.UserFieldConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 新手专享商品召回源
 */
@Slf4j
@Component(value = MatchStrategyConst.NCHS)
public class NchsMatchImpl implements Match2 {

    @Autowired
    private ProductDetailCache productDetailCache;

    @Autowired
    private UcRpcService ucRpcService;

    private static final int PID_NUM_MAX_LIMIT = 500;

    @Autowired
    private ProductSeasonCache productSeasonCache;

    @BProfiler(key = "NchsMatchImpl.match",
            monitorType = {MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR})
    @Override
    public List<MatchItem2> match(MatchParam matchParam) {

        List<MatchItem2> rtList = new ArrayList<>();

        String uuid = matchParam.getUuid();
        //获取用户季节
        int userSeasonValue = getUserSeasonValue(uuid);

        //一起拼商品结果集
        List<ProductInfo> togetherGroupProductList = new ArrayList<>();

        Map<Long, ProductInfo> productInfoMap = productDetailCache.getProductInfoMap();

        Iterator<Map.Entry<Long, ProductInfo>> iterator = productInfoMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, ProductInfo> next = iterator.next();
            ProductInfo product = next.getValue();

            try {
                //过滤非一起拼（新手专享）商品、下架、定制商品
                if (product == null || product.getIsToggroupProduct() == null ||
                        !product.getIsToggroupProduct().toString().equals("1") ||
                        FilterUtil.isCommonFilter(product)) {
                    continue;
                }

                //进行季节过滤
                int productSeasonValue = productSeasonCache.getProductSeasonValue(product.getProductId().toString());
                if (MatchUtil.isFilterByUserSeason(productSeasonValue, userSeasonValue)) {
                    continue;
                }

                //过滤掉非用户性别商品
                if (MatchUtil.isFilterBySex(product, matchParam.getUserSex())) {
                    continue;
                }
            } catch (Exception e) {
                log.error("[严重异常][召回源]nchs召回源中组装数据异常 pid: {} ", product.getProductId(), e);
                continue;
            }
            togetherGroupProductList.add(product);
        }

        try {
            //根据7日销量倒排
            togetherGroupProductList.sort(Comparator.comparingLong(ProductInfo::getSalesVolume7).reversed());
            //截取销量最大的前500个商品
            if (togetherGroupProductList.size() > PID_NUM_MAX_LIMIT) {
                togetherGroupProductList = togetherGroupProductList.subList(0, PID_NUM_MAX_LIMIT);
            }
            Collections.shuffle(togetherGroupProductList);

            //最多取500个商品，重新计算召回分（500-i）/500, i从0开始;
            int count = 0;
            for (ProductInfo productInfo : togetherGroupProductList) {
                MatchItem2 matchItem2 = new MatchItem2();
                matchItem2.setScore(((double) (PID_NUM_MAX_LIMIT - count)) / PID_NUM_MAX_LIMIT);
                matchItem2.setSource(MatchStrategyConst.NCHS);
                matchItem2.setProductId(productInfo.getProductId());
                rtList.add(matchItem2);
                count += 1;
            }
        } catch (Exception e) {
            log.error("[严重异常][召回源]nchs召回源异常，", e);
        }
        return rtList;
    }

    /**
     * 获取用户季节
     * @param uuid
     * @return
     */
    private int getUserSeasonValue(String uuid) {
        List<String> fields = new ArrayList<>();
        //用户季节
        fields.add(UserFieldConstants.SEASON);
        User user = ucRpcService.getData(uuid, null, fields, "mosesmatch");
        if (user == null) {
            return 0;
        }
        //用户季节
        int userSeasonValue = MatchUtil.convertSeason2int(user.getSeason());
        return userSeasonValue;
    }
}
