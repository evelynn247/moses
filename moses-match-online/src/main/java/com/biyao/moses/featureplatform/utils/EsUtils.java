package com.biyao.moses.featureplatform.utils;


import com.biyao.moses.featureplatform.common.IsEsFieldExist;
import com.biyao.moses.featureplatform.constant.FeatureConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;



import java.util.Arrays;
import java.util.List;


/**
 * @program: moses-parent-online
 * @description:
 * @author: zzm
 * @create: 2022-03-22 15:00
 **/

@Slf4j
public class EsUtils {


    /**
     * 将matchRule转成es检索表达式QueryBuilder
     * @param matchRule
     * @return
     */
    public static QueryBuilder transferRuleToQueryBuilder(String matchRule, Integer type){
        BoolQueryBuilder mustQueryBuilder;
        //如果matchRule为空，则返回null
        if (StringUtils.isBlank(matchRule)) {
            return null;
        }
        mustQueryBuilder = QueryBuilders.boolQuery();
        String[] rules = matchRule.split("&&");
        try {
            for (int i = 0; i < rules.length; i++) {
                String keyValue = rules[i];
                // 先处理括号中的解析类型
                if (keyValue.startsWith("(") && keyValue.endsWith(")")) {
                    BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
                    String shouldRule = keyValue.replaceAll("\\(","").replaceAll("\\)","");
                    String[] shouldRuleArr = shouldRule.split("\\|\\|");
                    for (int i1 = 0; i1 < shouldRuleArr.length; i1++) {
                        //括号里面的每一个||条件
                        String basicRule = shouldRuleArr[i1];
                        QueryBuilder basicQueryBuilder = buildBasicQueryBuilder(basicRule, type);
                        if (basicQueryBuilder == null) {
                            continue;
                        }
                        queryBuilder.should(basicQueryBuilder);
                    }
                    mustQueryBuilder.must(queryBuilder);
                }else{
                    //没有括号时候是单个的元规则
                    QueryBuilder basicQueryBuilder = buildBasicQueryBuilder(keyValue, type);
                    if (basicQueryBuilder == null) {
                        continue;
                    }
                    if(keyValue.contains(FeatureConstant.SYMBOL_NOT_EQUAL_CONS)){
                        //!= 不等于的时候特殊处理，需要mustNot
                        mustQueryBuilder.mustNot(basicQueryBuilder);
                    }else{
                        //= <> 其他情况都是must
                        mustQueryBuilder.must(basicQueryBuilder);
                    }
                }
            }
        } catch (Exception e) {
            log.error("[严重异常]字符串转成queryBuilder异常，参数：{}，异常信息：{}", matchRule, e);
        }
        return mustQueryBuilder;
    }

    /**
     * 对元规则的处理：field，符号，值
     * @param basicRule
     * @return
     */
    private static QueryBuilder buildBasicQueryBuilder(String basicRule, Integer type) {

        //元规则非空校验
        if (StringUtils.isEmpty(basicRule)) {
            return null;
        }


        //处理范围规则 <>  range  price<>200,500
        if(basicRule.contains(FeatureConstant.SYMBOL_RANGE_CONS)){
            //用<>分隔
            String[] keyValue2 = basicRule.split(FeatureConstant.SYMBOL_RANGE_CONS);
            //校验分隔后的字符串组长度是否为2 当前特征是否存在于索引field中（用户和商品特征分开）
            if (keyValue2.length != FeatureConstant.SPLIT_BASIC_RULE_SIZE_CONST ||
                    (FeatureConstant.TRANSFER_RULE_TYPE_CONST_USER.equals(type) && !IsEsFieldExist.isHaveFieldInUser(keyValue2[0])) ||
                    (FeatureConstant.TRANSFER_RULE_TYPE_CONST_PRODUCT.equals(type) && !IsEsFieldExist.isHaveFieldInProduct(keyValue2[0]))) {
                return null;
            }
            //直接用rangeQuery  200,500  用逗号分隔
            String[] valueArr = keyValue2[1].split(FeatureConstant.SYMBOL_COMMA_CONS);
            if (valueArr.length>0) {
                RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery(keyValue2[0]);
                //范围下限
                if(StringUtils.isNotEmpty(valueArr[0])){
                    rangeQueryBuilder.gte(valueArr[0]);
                }
                //范围上限
                if(valueArr.length>1 && StringUtils.isNotEmpty(valueArr[1])){
                    rangeQueryBuilder.lte(valueArr[1]);
                }
                return rangeQueryBuilder;
            }
        }
        //处理 =  != 条件  判断是否含有=
        if(basicRule.contains(FeatureConstant.SYMBOL_EQUAL_CONS)){
            //先以!=分隔，分隔失败按照=分隔，校验分隔后的数组长度为2为符合条件，构建表达式，否则返回null
            String[] keyValue2 = basicRule.split(FeatureConstant.SYMBOL_NOT_EQUAL_CONS);
            if (keyValue2.length != FeatureConstant.SPLIT_BASIC_RULE_SIZE_CONST) {
                keyValue2 = basicRule.split(FeatureConstant.SYMBOL_EQUAL_CONS);
                if (keyValue2.length != FeatureConstant.SPLIT_BASIC_RULE_SIZE_CONST) {
                    return null;
                }
            }
            //直接用termsQuery 用逗号分隔
            List<String> valueList = Arrays.asList(keyValue2[1].split(FeatureConstant.SYMBOL_COMMA_CONS));
            if (!CollectionUtils.isEmpty(valueList)) {
                return QueryBuilders.termsQuery(keyValue2[0], valueList);
            }
        }
        return null;
    }


    /**
     * 构建searchRequest
     * @param index
     * @param queryBuilder
     * @param from
     * @param size
     * @return
     */
    public static SearchRequest buildEsRequest(String index, QueryBuilder queryBuilder,Integer from,Integer size, Integer type){


        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        // 1 构建es查询参数 BoolQueryBuilder  buildBoolQueryBuilder()
        searchSourceBuilder.query(queryBuilder);
        // 召回字段设置
        searchSourceBuilder.fetchSource(FeatureConstant.TRANSFER_RULE_TYPE_CONST_USER.equals(type)?FeatureConstant.fetchUserSource:FeatureConstant.fetchProductSource, null);
        // 分页信息
        searchSourceBuilder.from(from*size);
        searchSourceBuilder.size(size);
        searchSourceBuilder.trackTotalHits(true);

        SearchRequest searchRequest = new SearchRequest();
        //索引名
        searchRequest.indices(index);
        searchRequest.source(searchSourceBuilder);
        return searchRequest;

    }


}
