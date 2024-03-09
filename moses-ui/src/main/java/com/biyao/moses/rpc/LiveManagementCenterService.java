package com.biyao.moses.rpc;

//import com.alibaba.fastjson.JSON;
//import com.biyao.moses.constants.CommonConstants;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
//import com.biyao.vshow.client.product.IRelationProductToCService;
//import com.biyao.vshow.dto.common.Result;
//import com.biyao.vshow.dto.product.ProductDto;
//import com.biyao.vshow.param.product.ProductParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
//import org.springframework.util.StringUtils;
//
//import javax.annotation.Resource;
//import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @ClassName LiveManagementCenterService
 * @Description 直播管理中心RPC
 * @Author xiaojiankai
 * @Date 2020/5/28 16:31
 * @Version 1.0
 **/
@Component
@Slf4j
public class LiveManagementCenterService {
/**
 * 注释直播相关代码
 */
//    @Resource
//    private IRelationProductToCService relationProductToCService;

    /**
     * 批量查询商品的直播状态
     * @return
     */
    public Map<String, String> queryLiveStatus(List<TotalTemplateInfo> totalTemplateInfoList){
        Map<String, String> result = new HashMap<>();
        if(CollectionUtils.isEmpty(totalTemplateInfoList)){
            return result;
        }
//        List<ProductParam> productParamList = new ArrayList<>();
//        try {
//            totalTemplateInfoList.forEach(info -> {
//                if (info != null && info.getId() != null && !info.getId().equals(CommonConstants.INVALID_PRODUCT_ID)) {
//                    ProductParam productParam = new ProductParam();
//                    productParam.setSpuId(info.getId());
//                    productParamList.add(productParam);
//                }
//            });
//            Result<Map<ProductDto, Integer>> lbResult = relationProductToCService.queryShowingProducts(productParamList, "moses");
//            if (lbResult == null || !lbResult.getSuccess().equals(Result.SUCCESS)){
//                log.error("[严重异常][查询商品直播状态]出现异常，入参 {}, 返回结果 {} ", JSON.toJSONString(productParamList),
//                        lbResult == null ? null : JSON.toJSONString(lbResult));
//                return result;
//            }
//            Map<ProductDto, Integer> data = lbResult.getData();
//            if(data == null || data.size() == 0){
//                return result;
//            }
//
//            for(Map.Entry<ProductDto, Integer> entry : data.entrySet()){
//                ProductDto productDto = entry.getKey();
//                Integer liveStatus = entry.getValue();
//                if(productDto == null || StringUtils.isEmpty(productDto.getSpuId())){
//                    continue;
//                }
//                //直播管理中心2表示为直播中
//                if(liveStatus != null && liveStatus == 2){
//                    //推荐返回给网关 “1”表示直播中标签
//                    result.put(productDto.getSpuId(), "1");
//                }
//            }
//
//        }catch (Exception e){
//            log.error("[严重异常][查询商品直播状态]出现异常，入参 {}，", JSON.toJSONString(productParamList), e);
//        }
        return result;
    }
}
