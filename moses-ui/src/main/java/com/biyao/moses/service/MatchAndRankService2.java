package com.biyao.moses.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.cache.SliderProductCache;
import com.biyao.moses.common.constant.BizNameConst;
import com.biyao.moses.common.constant.ErrorCode;
import com.biyao.moses.common.constant.ExpFlagsConstants;
import com.biyao.moses.constants.CommonConstants;
import com.biyao.moses.context.ByUser;
import com.biyao.moses.enums.MosesRuleEnum;
import com.biyao.moses.exp.MosesExpConst;
import com.biyao.moses.model.match.ProductImage;
import com.biyao.moses.model.match2.MatchItem2;
import com.biyao.moses.model.rank2.RankItem2;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.params.ApiResult;
import com.biyao.moses.params.BaseRequest2;
import com.biyao.moses.params.ProductInfo;
import com.biyao.moses.params.UIBaseRequest;
import com.biyao.moses.params.match2.MatchRequest2;
import com.biyao.moses.params.match2.MatchResponse2;
import com.biyao.moses.params.matchOnline.MatchOnlineRequest;
import com.biyao.moses.params.rank2.RankRequest2;
import com.biyao.moses.params.rank2.RankResponse2;
import com.biyao.moses.rules.Rule;
import com.biyao.moses.rules.RuleContext;
import com.biyao.moses.service.imp.AdvertInfoService;
import com.biyao.moses.service.imp.HttpMosesMatchServiceImpl;
import com.biyao.moses.service.imp.HttpMosesRankServiceImpl;
import com.biyao.moses.util.ApplicationContextProvider;
import com.biyao.moses.util.MyBeanUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * @author zhaiweixi@idstaff.com
 * @date 2019/10/12
 * 新版 match rank 服务
 **/
@Component
@Slf4j
public class MatchAndRankService2 {

    @Autowired
    HttpMosesMatchServiceImpl mosesMatchService;

    @Autowired
    HttpMosesRankServiceImpl mosesRankService;

    @Autowired
    private SliderProductCache sliderProductCache;

    @Autowired
    private ProductDetailCache productDetailCache;

    @Autowired
    private AdvertInfoService advertInfoService;

    @Value("${home.feed.pageId}")
    private String homeFeedPageId;

    /**
     * match
     * @param request
     * @return
     */
    public MatchResponse2 match(MatchRequest2 request, ByUser user){
        ApiResult<MatchResponse2> matchResult = mosesMatchService.productMatch(request, user);
        MatchResponse2 matchResponse2 = new MatchResponse2();
        if (ErrorCode.SUCCESS_CODE.equals(matchResult.getSuccess())){
            matchResponse2 = matchResult.getData();
        }
        return matchResponse2;
    }

    /**
     * match
     * @param request
     * @return
     */
    public MatchResponse2 matchOnline(MatchOnlineRequest request, ByUser user){
        ApiResult<MatchResponse2> matchResult = mosesMatchService.productMatchOnline(request, user);
        MatchResponse2 matchResponse2 = new MatchResponse2();
        if (ErrorCode.SUCCESS_CODE.equals(matchResult.getSuccess())){
            matchResponse2 = matchResult.getData();
        }
        return matchResponse2;
    }
    @Async
    public Future<MatchResponse2> asyncMatch(MatchRequest2 request, ByUser user){
        ApiResult<MatchResponse2> matchResult = mosesMatchService.productMatch(request, user);
        MatchResponse2 matchResponse2 = new MatchResponse2();
        if (ErrorCode.SUCCESS_CODE.equals(matchResult.getSuccess())){
            matchResponse2 = matchResult.getData();
        }
        return new AsyncResult<MatchResponse2>(matchResponse2);
    }

    /**
     * rank
     * @param request
     * @return
     */
    public RankResponse2 rank(RankRequest2 request){
        ApiResult<RankResponse2> rankResult = mosesRankService.rank(request);
        RankResponse2 rankResponse2 = new RankResponse2();
        if (ErrorCode.SUCCESS_CODE.equals(rankResult.getSuccess())){
            rankResponse2 = rankResult.getData();
        }else{
            log.error("[严重异常][新rank]新rank报错：response {}, request {}", JSON.toJSONString(rankResult), JSON.toJSONString(request));
            if(CollectionUtils.isEmpty(rankResponse2.getRankItem2List())) {
                rankResponse2.setRankItem2List(convert2RankItemList(request.getMatchItemList()));
            }
        }
        return rankResponse2;
    }

    /**
     *
     * @param baseRequest2
     * @param rankName
     * @param bizName
     * @return
     */
    public List<TotalTemplateInfo> matchAndRank(UIBaseRequest uiBaseRequest, ByUser user, BaseRequest2 baseRequest2,
                                                List<TotalTemplateInfo> waitInsertPidInfoList, String rankName, String bizName){
        List<TotalTemplateInfo> result = new ArrayList<>();
        //日志debug开关
        boolean isDebug = baseRequest2.getDebug() == null ? false : baseRequest2.getDebug();
        String sid = baseRequest2.getSid();
        String uuid = baseRequest2.getUuid();
        boolean isTraceTime = false;
        if(StringUtils.isNotBlank(homeFeedPageId) && homeFeedPageId.equals(uiBaseRequest.getPageId())){
            isTraceTime = true;
        }
        try {
            long startMatchTime = System.currentTimeMillis();
            HashMap<String, String> flags = baseRequest2.getFlags();
            String insertSourceAndWeight = flags.getOrDefault(MosesExpConst.FLAG_INSERT_MATCH_SOURCE_WEIGHT, MosesExpConst.VALUE_DEFAULT_INSERT_MATCH_SOURCE_WEIGHT);
            //如果是feed流横叉业务且没有配置横叉的召回源信息，则直接返回空集合
            if ((StringUtils.isBlank(insertSourceAndWeight) || MosesExpConst.VALUE_DEFAULT.equals(insertSourceAndWeight))
                    && BizNameConst.FEED_INSERT.equals(bizName)) {
                return result;
            }
            MatchRequest2 matchRequest2 = buildMatchRequest2(baseRequest2, flags, bizName);
            if (isDebug) {
                log.error("[DEBUG]开始获取match数据：sid {}， uuid {}， matchRequest2 {}", sid, uuid, JSON.toJSONString(matchRequest2));
            }

            matchRequest2.setPersonalizedRecommendSwitch(user.isPersonalizedRecommendSwitch());
            //调用match2获取召回源信息
            MatchResponse2 matchResponse2 = match(matchRequest2, user);

            List<MatchItem2> matchItem2List = matchResponse2.getMatchItemList();
            //如果未从召回源获取到商品，则返回空集合
            if (CollectionUtils.isEmpty(matchItem2List)) {
                //如果是横插召回源，则可能没有数据，此种情况不需要打日志
                if(BizNameConst.FEED_INSERT.equals(bizName)) {
                    log.error("[一般异常]横插召回源数据为空, sid {}, request {}", sid, JSON.toJSONString(matchRequest2));
                }else{
                    log.error("[严重异常]召回商品数据为空, sid {}, request {},", sid, JSON.toJSONString(matchRequest2));
                }
                return result;
            }
            String matchExpId = matchResponse2.getExpId();
            String rankNameFromMatch = matchResponse2.getRankName();
            if (isDebug) {
                log.error("[DEBUG]match结果：sid {}， uuid {}， matchResponse2 {}", sid, uuid, JSONObject.toJSONString(matchResponse2));
            }

            long startRankTime = System.currentTimeMillis();

            //如果match返回了需要执行的rankName，则执行对应的rankName
            if(StringUtils.isNotBlank(rankNameFromMatch)){
                rankName = rankNameFromMatch;
            }else {
                String rankNameFromMoses = flags.getOrDefault(MosesExpConst.FLAG_RANK_NAME, MosesExpConst.VALUE_DEFAULT_RANK_NAME);
                //如果moses实验参数中有配置rankName，则取该值
                if (StringUtils.isNotBlank(rankNameFromMoses) && !ExpFlagsConstants.VALUE_DEFAULT.equals(rankNameFromMoses)) {
                    rankName = rankNameFromMoses;
                }
            }

            List<RankItem2> rankItem2List = null;
            String rankExpId = null;
            if(StringUtils.isNotBlank(rankName)) {

                //对召回源进行排序rank
                RankRequest2 rankRequest2 = new RankRequest2();
                MyBeanUtil.copyNotNullProperties(baseRequest2, rankRequest2);
                rankRequest2.setRankName(rankName);
                rankRequest2.setBizName(bizName);
                rankRequest2.setMatchItemList(matchItem2List);

                if (isDebug) {
                    log.error("[DEBUG]开始获取rank数据：sid {}， uuid {}， rankRequest2 {}", sid, uuid, JSONObject.toJSONString(rankRequest2));
                }
                //调用rank2
                RankResponse2 rankResponse2 = rank(rankRequest2);

                rankItem2List = rankResponse2.getRankItem2List();
                //如果rank未返回数据，则返回空集合
                if (CollectionUtils.isEmpty(rankItem2List)) {
                    //如果rank发生异常，则直接使用match的排序结果
                    rankItem2List = convertToRankItem2List(matchItem2List);
                }
                rankExpId = rankResponse2.getExpId();
                if (isDebug) {
                    log.error("[DEBUG]rank结果：sid {}， uuid {}，rankName {}， rankResponse2 {}", sid, uuid, rankName, JSONObject.toJSONString(rankResponse2));
                }
            }else{
                rankItem2List = convertToRankItem2List(matchItem2List);
            }

            List<TotalTemplateInfo> allProductList = convert2TotalTemplateInfo(rankItem2List);
            long startRuleTime = System.currentTimeMillis();
            //对排序后的数据进行规则处理
            List<Integer> expIds = baseRequest2.getExpIds();
            String ruleExpId = composeExpId(expIds);
            String ruleNameStr;
            if (BizNameConst.FEED_INSERT.equals(bizName)) {
                ruleNameStr = flags.get(MosesExpConst.FLAG_INSERT_RULE_NAME);
            } else {
                ruleNameStr = flags.get(MosesExpConst.FLAG_RULE_NAME);
            }
            if (StringUtils.isNotEmpty(ruleNameStr) && !MosesExpConst.VALUE_DEFAULT.equals(ruleNameStr)) {
                try {
                    //ruleNameStr格式为ruleName,ruleName...,ruleName
                    String[] ruleNameArray = ruleNameStr.trim().split(",");
                    List<String> ruleNameList = new ArrayList<>();
                    Collections.addAll(ruleNameList, ruleNameArray);
                    allProductList = dealRuleByNames(uiBaseRequest, user, allProductList, waitInsertPidInfoList, ruleNameList, baseRequest2);
                } catch (Exception e) {
                    log.error("[严重异常][隔断规则]机制处理失败，uuid {}, uid {}, ruleExpId {}, ruleName {} ", baseRequest2.getUuid(), baseRequest2.getUid(), ruleExpId, ruleNameStr, e);
                }
            }
            StringBuilder expId = new StringBuilder();
            //如果个性化设置开关关闭，则不需要添加实验id（首页轮播图落地页除外）
            if(user.isPersonalizedRecommendSwitch() || CommonConstants.SLIDER_MIDDLE_PAGE_TOPICID.equals(uiBaseRequest.getTopicId())) {
                if (StringUtils.isNotBlank(matchExpId)) {
                    expId.append(matchExpId).append(CommonConstants.SPLIT_LINE);
                }
                if (StringUtils.isNotBlank(rankExpId)) {
                    expId.append(rankExpId).append(CommonConstants.SPLIT_LINE);
                }
                if (StringUtils.isNotBlank(ruleExpId)) {
                    expId.append(ruleExpId).append(CommonConstants.SPLIT_LINE);
                }
                if (expId.length() > 0) {
                    expId.deleteCharAt(expId.length() - 1);
                }
            }

            result = convert2TotalTemplate(matchItem2List, allProductList, expId.toString(), null);
            if (isDebug) {
                log.error("[DEBUG]转化后结果：sid {}， uuid {}， rankResponse2 {}", sid, uuid, JSONObject.toJSONString(result));
            }
            long endTime = System.currentTimeMillis();
            if(isTraceTime){
                log.info("[耗时统计][首页feed流]uuid {}，sid {}，各阶段耗时统计：match耗时{}ms，rank耗时{}ms，rule耗时{}ms",
                        uuid, sid, startRankTime-startMatchTime, startRuleTime-startRankTime, endTime-startRuleTime);
            }
        }catch (Exception e){
            log.error("[严重异常]matchAndRank2发生错误，sid {}， uuid {}", sid, uuid, e);
        }
        return result;
    }

    /**
     * 使用match的结果作为rank的结果，走rank出现异常或者不需要走rank时，调用此函数转化
     * @param matchItem2List
     * @return
     */
    public List<RankItem2> convertToRankItem2List(List<MatchItem2> matchItem2List){
        List<RankItem2> rankItem2List = new ArrayList<>();
        if(CollectionUtils.isEmpty(matchItem2List)){
            return rankItem2List;
        }
        for(MatchItem2 matchItem2 : matchItem2List){
            RankItem2 rankItem2 = new RankItem2();
            rankItem2.setProductId(matchItem2.getProductId());
            rankItem2.setScore(matchItem2.getScore());
            rankItem2List.add(rankItem2);
        }
        return rankItem2List;
    }
    /**
     * 构造matchRequest2
     * @return
     */
    public MatchRequest2 buildMatchRequest2(BaseRequest2 baseRequest2, Map<String, String> flags, String bizName){
        MatchRequest2 matchRequest2 = new MatchRequest2();
        String insertSourceAndWeight = flags.getOrDefault(MosesExpConst.FLAG_INSERT_MATCH_SOURCE_WEIGHT, MosesExpConst.VALUE_DEFAULT_INSERT_MATCH_SOURCE_WEIGHT);
        String sourceAndWeight = flags.getOrDefault(ExpFlagsConstants.FLAG_SOURCE_AND_WEIGHT, ExpFlagsConstants.VALUE_EMPTY_STRING);
        String sourceDataStrategy = flags.getOrDefault(ExpFlagsConstants.SFLAG_SOURCE_DATA_STRATEGY, ExpFlagsConstants.VALUE_DEFAULT);
        String sourceRedis = flags.getOrDefault(ExpFlagsConstants.SFLAG_SOURCE_REDIS, ExpFlagsConstants.VALUE_DEFAULT);
        String ucbDataNum = flags.getOrDefault(ExpFlagsConstants.SFLAG_UCB_DATA_NUM, ExpFlagsConstants.VALUE_DEFAULT);
        String expNumStr = flags.getOrDefault(ExpFlagsConstants.FLAG_EXPECT_NUM_MAX, ExpFlagsConstants.VALUE_DEFAULT);
        String matchRuleName = flags.getOrDefault(MosesExpConst.FLAG_MATCH_RULE_NAME, ExpFlagsConstants.VALUE_EMPTY_STRING);
        String manualSources=flags.getOrDefault(MosesExpConst.FLAG_MANUAL_SOURCES,ExpFlagsConstants.VALUE_DEFAULT);

        if(!ExpFlagsConstants.VALUE_DEFAULT.equals(manualSources)){
                String[] split = manualSources.split(",");
                List<String> manualSourceList = Arrays.stream(split).collect(Collectors.toList());
                matchRequest2.setManualSourceList(manualSourceList);
        }
        int expNum = 500;
        if(!ExpFlagsConstants.VALUE_DEFAULT.equals(expNumStr)){
            try{
                expNum = Integer.valueOf(expNumStr);
            }catch (Exception e){
                log.error("[严重异常][实验配置]期望的商品数量上限格式错误， expNum {}", expNumStr);
            }
        }
        MyBeanUtil.copyNotNullProperties(baseRequest2, matchRequest2);
        matchRequest2.setBiz(bizName);
        if(BizNameConst.FEED_INSERT.equals(bizName)) {
            matchRequest2.setSourceAndWeight(insertSourceAndWeight);
            expNum = 100;
        }else{
            if(StringUtils.isNotBlank(sourceAndWeight) && !ExpFlagsConstants.VALUE_DEFAULT.equals(sourceAndWeight)){
                matchRequest2.setSourceAndWeight(sourceAndWeight);
            }
        }
        matchRequest2.setExpNum(expNum);
        matchRequest2.setSourceDataStrategy(sourceDataStrategy);
        matchRequest2.setSourceRedis(sourceRedis);
        matchRequest2.setUcbDataNum(ucbDataNum);
        matchRequest2.setRuleName(matchRuleName);
        return matchRequest2;
    }
    /**
     * 根据实验分流返回的实验ID的集合，组装实验ID
     * @param expIds
     * @return
     */
    public String composeExpId(List<Integer> expIds){
        String result = "";
        if(CollectionUtils.isEmpty(expIds)){
            return result;
        }
        int size = expIds.size();
        for(int i = 0; i < size; i++){
            Integer tmpExpId = expIds.get(i);
            if(tmpExpId == null){
                continue;
            }
            if(i == size - 1){
                result += tmpExpId.toString();
            }else {
                result += tmpExpId.toString() + CommonConstants.SPLIT_LINE;
            }
        }
        return result;
    }

    /**
     * 遍历ruleNameList中的每一个ruleName，依次获取对应的bean，执行rulerank方法
     * @param allProductList
     * @param ruleNameList
     * @return
     */
    public List<TotalTemplateInfo> dealRuleByNames(UIBaseRequest uiBaseRequest, ByUser user, List<TotalTemplateInfo> allProductList,
                                                   List<TotalTemplateInfo> waitInsertProductList, List<String> ruleNameList, BaseRequest2 baseRequest2) {
        boolean isDebug = baseRequest2.getDebug() == null ? false : baseRequest2.getDebug();
        String uuid = baseRequest2.getUuid();
        String sid = baseRequest2.getSid();
        List<TotalTemplateInfo> result = new ArrayList<>();
        List<String> sortedRuleNameList = MosesRuleEnum.sort(ruleNameList);
        for (String ruleName : sortedRuleNameList) {
            try {
                Rule ruleImpl = ApplicationContextProvider.getApplicationContext().getBean(ruleName,
                        Rule.class);

                RuleContext ruleContext = RuleContext.builder()
                        .uid(baseRequest2.getUid())
                        .upcUserType(baseRequest2.getUpcUserType())
                        .uuid(baseRequest2.getUuid())
                        .userSex(baseRequest2.getUserSex())
                        .allProductList(allProductList)
                        .advertInfoList(advertInfoService.getAdvertInfoListByRule(baseRequest2.getShowAdvert(), baseRequest2.getUpcUserType(), baseRequest2.getAdvertInfoList(),user,uiBaseRequest.getPagePositionId()))
                        .baseRequest2(baseRequest2)
                        .byUser(user)
                        .uiBaseRequest(uiBaseRequest)
                        .waitInsertProductList(waitInsertProductList)
                        .build();

                result = ruleImpl.ruleRank(ruleContext);

                if(isDebug){
                    log.error("[DEBUG]sid {}, uuid {}, ruleName {}, ruleResult {}", sid, uuid, ruleName, JSON.toJSONString(allProductList));
                }
            } catch (Exception e) {
                log.error("[严重异常][隔断规则]处理规则时发生异常, ruleName {}", ruleName, e);
            }
        }

        return  result;
    }

    /**
     * 转化为List<RankItem2>
     * @param matchItem2List
     * @return
     */
    private List<RankItem2> convert2RankItemList(List<MatchItem2> matchItem2List){
        List<RankItem2> result = new ArrayList<>();
        if(CollectionUtils.isEmpty(matchItem2List)){
            return result;
        }
        matchItem2List.forEach(matchItem2 -> {
            if(matchItem2 != null) {
                RankItem2 rankItem2 = new RankItem2();
                rankItem2.setProductId(matchItem2.getProductId());
                rankItem2.setScore(matchItem2.getScore());
                result.add(rankItem2);
            }
        });
        return result;
    }
    /**
     * 将List<RankItem2> 转化为List<TotalTemplateInfo>
     * @param rankItem2List
     * @return
     */
    public List<TotalTemplateInfo> convert2TotalTemplateInfo(List<RankItem2> rankItem2List){
        List<TotalTemplateInfo> result = new ArrayList<>();
        if(CollectionUtils.isEmpty(rankItem2List)){
            return result;
        }
        rankItem2List.forEach( rankItem2 -> {

            if(rankItem2.getTotalTemplateInfo() != null){
                result.add(rankItem2.getTotalTemplateInfo());
            }else{
                if(rankItem2.getProductId() != null) {
                    TotalTemplateInfo info = new TotalTemplateInfo();
                    info.setId(rankItem2.getProductId().toString());
                    info.setScore(rankItem2.getScore());
                    result.add(info);
                }
            }
        });
        return result;
    }
    /**
     * 将新实验系统返回结构转化为旧实验系统的返回结构
     * @param matchResult
     * @param allProductList
     * @param expId
     * @return
     */
    public List<TotalTemplateInfo> convert2TotalTemplate(List<MatchItem2> matchResult, List<TotalTemplateInfo> allProductList, String expId, Map<String, TotalTemplateInfo> oldMatchResult){
        List<TotalTemplateInfo> result = new ArrayList<>();
        if(CollectionUtils.isEmpty(allProductList) || CollectionUtils.isEmpty(matchResult)){
            return result;
        }
        Map<Long, MatchItem2> matchItem2Map = matchResult.stream().collect(Collectors.toMap(MatchItem2::getProductId, m -> m));
        for(TotalTemplateInfo totalTemplateInfo : allProductList){
            if(totalTemplateInfo == null || StringUtils.isBlank(totalTemplateInfo.getId())){
                continue;
            }
            Long productId = Long.valueOf(totalTemplateInfo.getId());
            // 如果是商品且moses内存中没有 则过滤掉 （非商品的场景不做过滤 如：运营位id 为 -1 ）
            if(StringUtils.isEmpty(totalTemplateInfo.getId()) ||
                    (totalTemplateInfo.getId().length()==10 &&
                            Objects.isNull(productDetailCache.getProductInfo(Long.valueOf(totalTemplateInfo.getId()))))){
                log.info("[一般异常]moses内存中无此商品，请关注此商品的信息。productId:{}",totalTemplateInfo.getId());
                continue;
            }
            totalTemplateInfo.setExpId(expId);
            MatchItem2 matchItem2 = matchItem2Map.get(productId);
            if(matchItem2 != null) {
                totalTemplateInfo.setSource(matchItem2.getSource());
                totalTemplateInfo.setLabelContent(matchItem2.getLabelContent());
            }
            //如果是轮播图返回结构，增加图片信息
            if(MosesExpConst.SLIDER_EXP.equals(expId)){
                ProductImage imageInfo = sliderProductCache.getProductImageById(productId);
                ProductInfo productInfo = productDetailCache.getProductInfo(productId);
                Map<String, String> routerParams = new HashMap<>();
                // 加入参数
                if (productInfo.getSupplierId() != null) {
                    routerParams.put("supplierId", productInfo.getSupplierId().toString());
                }
                if (productInfo.getSuId() != null) {
                    routerParams.put("suId", productInfo.getSuId().toString());
                }
                // 推荐中间页跳转类型
                if (imageInfo.getRouteType() == 6){
                    routerParams.put("priorityProductIds", productInfo.getProductId().toString());
                }

                // 配置中的参数优先
                if (imageInfo.getRouteParams() != null && imageInfo.getRouteParams().size() > 0){
                    routerParams.putAll(imageInfo.getRouteParams());
                }
                totalTemplateInfo.setRouterParams(routerParams);

                totalTemplateInfo.setImage(imageInfo.getImage());
                totalTemplateInfo.setImageWebp(imageInfo.getWebpImage());
                List<String> list = new ArrayList<>();
                List<String> listWebp = new ArrayList<>();
                list.add(imageInfo.getImage());
                listWebp.add(imageInfo.getWebpImage());
                totalTemplateInfo.setLongImages(list);
                totalTemplateInfo.setLongImagesWebp(listWebp);
                totalTemplateInfo.setImages(list);
                totalTemplateInfo.setImagesWebp(listWebp);
                totalTemplateInfo.setRouterType(imageInfo.getRouteType());
            }
            if(oldMatchResult != null && oldMatchResult.size() > 0
                && oldMatchResult.containsKey(totalTemplateInfo.getId())){
                TotalTemplateInfo templateInfo = oldMatchResult.get(totalTemplateInfo.getId());
                if(templateInfo != null){
                    MyBeanUtil.copyNotNullProperties(templateInfo, totalTemplateInfo);
                }
            }
            result.add(totalTemplateInfo);
        }
        return result;
    }
}
