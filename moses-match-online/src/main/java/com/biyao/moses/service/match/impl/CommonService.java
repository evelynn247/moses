package com.biyao.moses.service.match.impl;

import com.alibaba.fastjson.JSONObject;
import com.biyao.moses.Enum.PlatformEnum;
import com.biyao.moses.Enum.SceneEnum;
import com.biyao.moses.cache.SceneRuleRelationCache;
import com.biyao.moses.common.CommonConstant;
import com.biyao.moses.constant.MatchStrategyConst;
import com.biyao.moses.match.MatchItem2;
import com.biyao.moses.match.MatchParam;
import com.biyao.moses.util.MatchUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static com.biyao.moses.common.constant.CommonConstant.*;
import static com.biyao.moses.common.constant.EsIndexConstant.*;
import static com.biyao.moses.constant.CommonConstant.*;

/**
 * @program: moses-parent-online
 * @description:
 * @author: changxiaowei
 * @create: 2021-10-09 19:20
 **/
@Component
@Slf4j
public class CommonService {

    @Value("${env.name}")
    private String envName;
    @Autowired
    RestHighLevelClient restHighLevelClient;
    @Autowired
    SceneRuleRelationCache sceneRuleRelationCache;

    /**
     * 设置召回属性 （召回字段 查询数量 索引别名）
     *
     * @param queryBuilder
     * @param maxNum
     * @return
     */
    public SearchRequest buildSearchRequest(QueryBuilder queryBuilder, Integer maxNum) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        // 1 构建es查询参数 BoolQueryBuilder  buildBoolQueryBuilder()
        searchSourceBuilder.query(queryBuilder);
        // 召回字段设置
        searchSourceBuilder.fetchSource(fetchSource, null);
        // 控制查询数量
        searchSourceBuilder.from(ZERO);
        searchSourceBuilder.size(maxNum);
//        // 排序
//        searchSourceBuilder.sort(new ScoreSortBuilder().order(SortOrder.DESC));
        SearchRequest searchRequest = new SearchRequest();
        //索引名
        searchRequest.indices(PRODUCT_INDEX_ALIAS + envName.toLowerCase());
        searchRequest.source(searchSourceBuilder);
        return searchRequest;
    }

    /**
     * 构建es查询参数
     * 处理 分端 上下架 是否展示 性别  季节 场景id等
     *
     * @param matchParam
     * @return
     */
    public QueryBuilder buildQueryBuilder(MatchParam matchParam) {
        // 构建es查询参数
        BoolQueryBuilder mustQueryBuilder = QueryBuilders.boolQuery();
        // 先处理无需动态配置的参数
        dealNoDynamic(mustQueryBuilder,matchParam);
        //可配置的规则参数 如果内存中含有此规则id 对应的query表达式 则取内存中的
        QueryBuilder queryBuilderCache = sceneRuleRelationCache.getQueryBuilderByRuleId(matchParam.getRuleId());
        if(queryBuilderCache != null){
            return mustQueryBuilder.must(queryBuilderCache);
        }
        // 如果内存中没有此规则id对应的配置 则走固定的配置
        dealDynamic(mustQueryBuilder,matchParam);
        return mustQueryBuilder;
    }
    /**
     * 处理动态配置的属性
     * @param queryBuilder
     * @param matchParam
     */
    private void dealDynamic( BoolQueryBuilder queryBuilder ,MatchParam matchParam){
        Integer sceneId = matchParam.getSceneId();
        // 必要造物
        if (SceneEnum.BYZW.getSceneId().equals(sceneId) || SceneEnum.BYZW_CAR.getSceneId().equals(sceneId)) {
            queryBuilder.must(QueryBuilders.termQuery(IS_CREATOR, ONE));
            // 定制商品过滤 supportTexture=1   定制咖啡除外 supportTexture=2
            queryBuilder.mustNot(QueryBuilders.termQuery(SUPPORT_TEXTURE, ONE));
        }
        // 特权金下发页
        if (SceneEnum.TQJXF.getSceneId().equals(sceneId)) {
            queryBuilder.must(QueryBuilders.termQuery(NEW_PRIVILEGE, ONE));
            // 特权金抵扣金额大于0
            queryBuilder.must(QueryBuilders.rangeQuery(NEW_PRIVILATE_DEDUCT).gt(ZERO));
            // 定制商品过滤 supportTexture=1   定制咖啡除外 supportTexture=2
            queryBuilder.mustNot(QueryBuilders.termQuery(SUPPORT_TEXTURE, ONE));
        }
        // 必要分销
        if (SceneEnum.BYFX.getSceneId().equals(sceneId)) {
        queryBuilder.must(QueryBuilders.termQuery(SUPPORT_ACT, ONE));
    }
}
    /**
     * 处理无需动态配置的参数
     * @param queryBuilder
     * @param matchParam
     */
    private void dealNoDynamic( BoolQueryBuilder queryBuilder ,MatchParam matchParam){
        // 上下架状态
        queryBuilder.must(QueryBuilders.termQuery(SHELF_STATUS, ONE));
        // 是否展示
        queryBuilder.must(QueryBuilders.termQuery(SHOW_STATUS, ZERO));
        // 分端过滤
        if (matchParam.getUserPaltform() != ZERO && !NO_SITE_FILTER.contains(matchParam.getUserPaltform())){
            queryBuilder.must(QueryBuilders.termQuery(SUPPORT_PLATFORM, matchParam.getUserPaltform()));
        }
        // 性别过滤 季节过滤  类目下商品召回无需性别和季节过滤
        if (!MatchStrategyConst.CATEGORY.equals(matchParam.getSource())) {
            if (CommonConstant.SEX_FEMALE.equals(matchParam.getUserSex())) {
                queryBuilder.mustNot(QueryBuilders.termQuery(SEX, CommonConstant.SEX_MALE.byteValue()));
            }
            if (CommonConstant.SEX_MALE.equals(matchParam.getUserSex())) {
                queryBuilder.mustNot(QueryBuilders.termQuery(SEX, CommonConstant.SEX_FEMALE.byteValue()));
            }
            // 季节用户季节为空 则不做处理
            if (matchParam.getUserSeason() != null) {
                BoolQueryBuilder shouldQueryBuilder = QueryBuilders.boolQuery();
                shouldQueryBuilder.should(QueryBuilders.termsQuery(SEASON, MatchUtil.conver2SeasonListByUserSeason(matchParam.getUserSeason())));
                queryBuilder.must(shouldQueryBuilder);
            }
        }
        // 满减券场景
        if (SceneEnum.MJQ.getSceneId().equals(matchParam.getSceneId())) {
            BoolQueryBuilder shouldQueryBuilder = QueryBuilders.boolQuery();
            shouldQueryBuilder.should(QueryBuilders.termsQuery(SUPPORT_MJQ, matchParam.getMjCardIds()));
            queryBuilder.must(shouldQueryBuilder);
        }
        //返现
        if (SceneEnum.FX.getSceneId().equals(matchParam.getSceneId())) {
            queryBuilder.must(QueryBuilders.termQuery(SUPPORT_FX, matchParam.getFxCardId()));
        }
        // 类目页场景
        BoolQueryBuilder shouldQueryBuilder = QueryBuilders.boolQuery();
        if (CollectionUtils.isNotEmpty(matchParam.getThirdCateGoryId())) {
            shouldQueryBuilder.should(QueryBuilders.termsQuery(CATEGORY3ID, matchParam.getThirdCateGoryId()));
        }
        if (CollectionUtils.isNotEmpty(matchParam.getTagId())) {
            shouldQueryBuilder.should(QueryBuilders.termsQuery(TAGSID, matchParam.getTagId()));
        }
        queryBuilder.must(shouldQueryBuilder);
        // 渠道
        if (VIDEO_SCENDIDS.contains(matchParam.getSceneId())) {
            if(2 == matchParam.getChannelType()){
                queryBuilder.must(QueryBuilders.termQuery(SUPPORT_ACT, ONE));
            }
            queryBuilder.must(QueryBuilders.termQuery(VID_SUPPORT_PALTFORM,matchParam.getChannelType()));
        }
        //可售渠道
        queryBuilder.must(QueryBuilders.termQuery(SUPPORT_CHANNEL, PlatformEnum.getChannelTypeBySiteId(matchParam.getSiteId())));
        //如果是个性化召回  召回的商品一定是有向量的  否则 召回函数调用时会报错
        if (MatchStrategyConst.PERSONAL_FM.equals(matchParam.getSource())) {
            queryBuilder.must(QueryBuilders.existsQuery(FM_VECTOR));
        }
        if (MatchStrategyConst.PERSONAL_ICF.equals(matchParam.getSource())) {
            queryBuilder.must(QueryBuilders.existsQuery(ICF_VECTOR));
        }
    }

    /**
     * 查询es api 召回
     *
     * @param searchRequest
     * @param matchParam
     * @return
     */
    public List<MatchItem2> search(SearchRequest searchRequest, MatchParam matchParam) {
        long start = System.currentTimeMillis();
        boolean debug = matchParam.isDebug();
        String sid = matchParam.getSid();
        String uuid = matchParam.getUuid();
        List<MatchItem2> resultList = new ArrayList<>();
        try {
            // 召回
            SearchResponse response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            SearchHits searchHits = response.getHits();
            if (response.status() != RestStatus.OK || searchHits == null) {
                log.error("[严重异常]es召回商品数量失败,response：{}", JSONObject.toJSONString(response));
                return resultList;
            }
            if (searchHits.getTotalHits().value <= ZERO) {
                log.error("[严重异常]es召回商品数量为空,response：{}", JSONObject.toJSONString(response));
                return resultList;
            }
            // 返回召回字段
            //List<Map<String, Object>> collect = Arrays.stream(searchHits.getHits()).map(SearchHit::getSourceAsMap).collect(Collectors.toList());
            // 返回id 和召回分
            Map<String, Float> pidScoreMap = Arrays.stream(searchHits.getHits()).collect(Collectors.toMap(SearchHit::getId, SearchHit::getScore));
            // 填充
            Set<Map.Entry<String, Float>> entries = pidScoreMap.entrySet();
            for (Map.Entry<String, Float> entry : entries) {
                MatchItem2 matchItem2 = new MatchItem2();
                matchItem2.setProductId(Long.valueOf(entry.getKey()));
                matchItem2.setScore(Double.valueOf(entry.getValue()));
                matchItem2.setSource(matchParam.getSource());
                resultList.add(matchItem2);
            }
        } catch (Exception e) {
            log.error("[严重异常]个性化召回异常，参数：{}，异常信息：{}", JSONObject.toJSONString(matchParam), e);
        }
        if (debug) {
            log.info("[debug-检查日志-{}]es召回结束，sid:{},uuid:{},耗时：{}", matchParam.getSource(), sid, uuid, System.currentTimeMillis() - start);
        }
        return resultList;
    }

}
