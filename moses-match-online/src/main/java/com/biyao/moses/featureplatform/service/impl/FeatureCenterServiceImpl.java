package com.biyao.moses.featureplatform.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.biyao.moses.common.ErrorCode;
import com.biyao.moses.featureplatform.constant.FeatureConstant;
import com.biyao.moses.featureplatform.constant.FeatureEsConstant;
import com.biyao.moses.featureplatform.domain.FeatureRequest;
import com.biyao.moses.featureplatform.domain.FeatureResponse;
import com.biyao.moses.featureplatform.domain.ProductFeatureDTO;
import com.biyao.moses.featureplatform.domain.UserFeatureDTO;
import com.biyao.moses.featureplatform.utils.EsUtils;
import com.biyao.moses.match.ApiResult;
import com.biyao.moses.featureplatform.service.FeatureCenterService;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

import static com.biyao.moses.common.constant.CommonConstant.ZERO;

/**
 * @program: moses-parent-online
 * @description:
 * @author: zzm
 * @create: 2022-03-22 15:00
 **/
@Service
@Slf4j
public class FeatureCenterServiceImpl implements FeatureCenterService {


    @Resource
    RestHighLevelClient restHighLevelClient;

    /**
     * 根据用户特征查询用户群包
     * @param request
     * @return
     */
    @Override
    public ApiResult<FeatureResponse<List<UserFeatureDTO>>> getUserInfoListByFeatures(FeatureRequest request) {

        ApiResult<FeatureResponse<List<UserFeatureDTO>>> result = new ApiResult<>();
        FeatureResponse<List<UserFeatureDTO>> featureResponse = new FeatureResponse<>();
        //入参校验
        if(request.validate()){
            log.error("[严重异常]根据用户特征查询用户群包参数异常，入参request：{}", JSONObject.toJSONString(request));
            result.setSuccess(ErrorCode.PARAM_ERROR_CODE);
            result.setError("必传参数不能为空");
            return result;
        }


        try {
            //构建es检索表达式
            QueryBuilder queryBuilder = EsUtils.transferRuleToQueryBuilder(request.getMatchRule(), FeatureConstant.TRANSFER_RULE_TYPE_CONST_USER);
            //构建searchRequest对象
            SearchRequest searchRequest = EsUtils.buildEsRequest(FeatureEsConstant.USER_FEATURE_INDEX_ALIAS,queryBuilder,request.getPageIndex(),request.getPageSize(), FeatureConstant.TRANSFER_RULE_TYPE_CONST_USER);
            List<UserFeatureDTO> list = new ArrayList<>();
            //调用es的Java api召回商品信息
            SearchResponse response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            //解析结果
            SearchHits searchHits = response.getHits();
            if (response.status() != RestStatus.OK || searchHits == null) {
                log.error("调用方【{}】[严重异常]根据用户特征查询用户群包参数异常,response：{}", request.getCaller(), JSONObject.toJSONString(response));
                result.setSuccess(ErrorCode.SYSTEM_ERROR_CODE);
                result.setError("服务异常");
                return result;
            }
            if (searchHits.getTotalHits().value <= ZERO) {
                log.error("调用方【{}】[严重异常]根据用户特征查询用户群包为空,response：{}", request.getCaller(), JSONObject.toJSONString(response));
                featureResponse.setData(list);
                result.setData(featureResponse);
                return result;
            }
            //获取结果数组
            SearchHit[] hits = searchHits.getHits();

            for (SearchHit hit : hits) {
                //遍历结果数组，将每个json字符串转成ProductFeatureDTO，然后添加到返回list中
                UserFeatureDTO userDto = JSON.parseObject(hit.getSourceAsString(),UserFeatureDTO.class);
                list.add(userDto);
            }
            //封装featureResponse对象，包括返回list，符合条件用户总数量和本次搜索返回用户数量
            featureResponse.setData(list);
            featureResponse.setTotalNum(searchHits.getTotalHits().value);
            featureResponse.setCurrentPageNum(list.size());
            result.setData(featureResponse);
        } catch (Exception e) {
            log.error("调用方【{}】[严重异常]根据用户特征查询用户群包，参数：{}，异常信息：{}", request.getCaller(), JSONObject.toJSONString(request), e);
            result.setSuccess(ErrorCode.SYSTEM_ERROR_CODE);
            result.setError("服务异常");
        }
        return result;
    }


    /**
     * 根据商品特征查询商品信息集合
     * @param request
     * @return
     */
    @Override
    public ApiResult<FeatureResponse<List<ProductFeatureDTO>>> getProductInfoListByFeatures(FeatureRequest request) {
        ApiResult<FeatureResponse<List<ProductFeatureDTO>>> result = new ApiResult<>();
        FeatureResponse<List<ProductFeatureDTO>> featureResponse = new FeatureResponse<>();

        //入参校验
        if(request.validate()){
            log.error("[严重异常]根据商品特征查询商品信息参数异常，入参request：{}", JSONObject.toJSONString(request));
            result.setSuccess(ErrorCode.PARAM_ERROR_CODE);
            result.setError("必传参数不能为空");
            return result;
        }


        try {
            //构建es检索表达式
            QueryBuilder queryBuilder = EsUtils.transferRuleToQueryBuilder(request.getMatchRule(), FeatureConstant.TRANSFER_RULE_TYPE_CONST_PRODUCT);

            //构建searchRequest对象
            SearchRequest searchRequest = EsUtils.buildEsRequest(FeatureEsConstant.PRODUCT_FEATURE_INDEX_ALIAS,queryBuilder,request.getPageIndex(),request.getPageSize(), FeatureConstant.TRANSFER_RULE_TYPE_CONST_PRODUCT);
            List<ProductFeatureDTO> list = new ArrayList<>();
            // 调用es的Java api召回商品信息
            SearchResponse response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            //解析结果
            SearchHits searchHits = response.getHits();
            if (response.status() != RestStatus.OK || searchHits == null) {
                log.error("调用方【{}】[严重异常]根据商品特征查询商品信息异常,response：{}", request.getCaller(), JSONObject.toJSONString(response));
                result.setSuccess(ErrorCode.SYSTEM_ERROR_CODE);
                result.setError("服务异常");
                return result;
            }
            if (searchHits.getTotalHits().value <= ZERO) {
                log.error("调用方【{}】[严重异常]根据商品特征查询商品信息为空,response：{}", request.getCaller(), JSONObject.toJSONString(response));
                featureResponse.setData(list);
                result.setData(featureResponse);
                return result;
            }
            //获取结果数组
            SearchHit[] hits = searchHits.getHits();

            for (SearchHit hit : hits) {
                //遍历结果数组，将每个json字符串转成ProductFeatureDTO，然后添加到返回list中
                ProductFeatureDTO proDto = JSON.parseObject(hit.getSourceAsString(),ProductFeatureDTO.class);
                list.add(proDto);
            }
            //封装featureResponse对象，包括返回list，符合条件商品总数量和本次搜索返回商品数量
            featureResponse.setData(list);
            featureResponse.setTotalNum(searchHits.getTotalHits().value);
            featureResponse.setCurrentPageNum(list.size());
            result.setData(featureResponse);
        } catch (Exception e) {
            log.error("调用方【{}】[严重异常]根据商品特征查询商品信息，参数：{}，异常信息：{}", request.getCaller(), JSONObject.toJSONString(request), e);
            result.setSuccess(ErrorCode.SYSTEM_ERROR_CODE);
            result.setError("服务异常");
        }
        return result;

    }

}
