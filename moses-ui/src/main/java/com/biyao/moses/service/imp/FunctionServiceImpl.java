package com.biyao.moses.service.imp;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.cache.ProductVIdeoRelationCache;
import com.biyao.moses.cache.RecommendManualSourceConfigCache;
import com.biyao.moses.cache.SwitchConfigCache;
import com.biyao.moses.cache.drools.RuleConfigCache;
import com.biyao.moses.config.drools.KiaSessionConfig;
import com.biyao.moses.constants.CommonConstants;
import com.biyao.moses.constants.MosesBizConfigEnum;
import com.biyao.moses.context.ByUser;
import com.biyao.moses.drools.DroolsCommonUtil;
import com.biyao.moses.drools.DroolsService;
import com.biyao.moses.model.drools.BuildBaseFactParam;
import com.biyao.moses.model.drools.RuleBaseFact;
import com.biyao.moses.model.drools.RuleFact;
import com.biyao.moses.model.match2.MatchItem2;
import com.biyao.moses.model.rank2.RankItem2;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.params.*;
import com.biyao.moses.params.match2.MatchRequest2;
import com.biyao.moses.params.match2.MatchResponse2;
import com.biyao.moses.params.matchOnline.MatchOnlineRequest;
import com.biyao.moses.params.rank2.RankRequest2;
import com.biyao.moses.params.rank2.RankResponse2;
import com.biyao.moses.rpc.PushTokenService;
import com.biyao.moses.rpc.UcRpcService;
import com.biyao.moses.rules.RuleContext;
import com.biyao.moses.service.IFunctionService;
import com.biyao.moses.service.MatchAndRankService2;
import com.biyao.moses.util.*;
import com.uc.domain.bean.User;
import com.uc.domain.constant.UserFieldConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static com.biyao.moses.common.constant.AlgorithmRedisKeyConstants.PRODUICT_VEDIO_REDIS;
import static com.biyao.moses.common.constant.AlgorithmRedisKeyConstants.VIODE_SIMILAR_PID;
import static com.biyao.moses.common.constant.CacheRedisKeyConstant.VIDEO_EXPO_CACHE_PREFIX;
import static com.biyao.moses.common.constant.CacheRedisKeyConstant.VIDEO_PAGE_CACHE_PREFIX;
import static com.biyao.moses.common.constant.CommonConstants.*;
import static com.biyao.moses.constants.CommonConstants.INVALID_PRODUCT_ID;

/**
 * @ClassName FunctionServiceImpl
 * @Description service
 * @Author xiaojiankai
 * @Date 2020/3/30 15:09
 * @Version 1.0
 **/
@Component
@Slf4j
public class FunctionServiceImpl implements IFunctionService {
    // 默认获取全量推荐商品的pid数量的上限
    private static final Integer EXP_NUMBER_UPPER= 500;

    // 默认获取全量推荐商品的pid数量的下限
    private static final Integer EXP_NUMBER_LOWER= 100;

    @Autowired
    private MatchAndRankService2 matchAndRankService2;
    @Autowired
    private  MatchAndRankAnsyService matchAndRankAnsyService;

    @Autowired
    private ProductDetailCache productDetailCache;
    @Autowired
    ProductServiceImpl productService;

    @Autowired
    private UcRpcService ucRpcService;
    @Autowired
    FilterUtil filterUtil;

    @Autowired
    private PushTokenService pushTokenService;

    @Autowired
    SwitchConfigCache switchConfigCache;

    @Autowired
    RuleConfigCache ruleConfigCache;

    @Autowired
    DroolsService droolsService;

    @Autowired
    KiaSessionConfig kiaSessionConfig;

    @Autowired
    DroolsCommonUtil droolsCommonUtil;

    @Autowired
    RecommendManualSourceConfigCache recommendManualSourceConfigCache;
    @Autowired
    CommonService commonService;
    @Autowired
    AdvertInfoService advertInfoService;
    @Autowired
    CacheRedisUtil cacheRedisUtil;
    @Autowired
    AlgorithmRedisUtil algorithmRedisUtil;
    @Autowired
    ProductVIdeoRelationCache productVIdeoRelationCache;



    /**
     * 调用mosesmatch 获取召回源数据
     * @param request
     * @param bodyRequest
     * @return
     */
    private MatchResponse2 match(RecommendAllRequest request, RecommendAllBodyRequest bodyRequest){
        MatchResponse2 matchResponse2 = new MatchResponse2();
        if(MosesBizConfigEnum.getByBizName(request.getBiz()) != null) {
            MatchRequest2 matchRequest2 = buildRequest2(request, bodyRequest,null);
            matchResponse2 = matchAndRankService2.match(matchRequest2, new ByUser());
            if(matchResponse2 == null || CollectionUtils.isEmpty(matchResponse2.getMatchItemList())){
                log.error("[一般异常]获取全量推荐信息为空，request {}", JSON.toJSONString(request));
            }
        }else{
            log.error("[严重异常]获取全量推荐信息失败，缺少对应业务{}的配置信息", request.getBiz());
        }
        return matchResponse2;
    }

    /**
     * 从UC中获取用户性别
     * @param uid
     * @param uuid
     * @return
     */
    private int getUserSexFromUc(String uid,String uuid) {
        int result = Integer.parseInt(CommonConstants.UNKNOWN_SEX);
        if("0".equals(uid) && StringUtils.isBlank(uuid)){
            log.error("[严重异常]FunctionServiceImpl中获取用户性别，uuid 和 uid都为空");
            return result;
        }
        try {
            List<String> fields = new ArrayList<>();
            fields.add(UserFieldConstants.SEX);
            String uidParam = "0".equals(uid) ? null : uid;
            User user = ucRpcService.getData(uuid, uidParam, fields, "moses");
            if (user != null && user.getSex() != null) {
                result = user.getSex();
            }
        }catch (Exception e){
            log.error("[严重异常]获取用户性别异常， uuid {}, uid {}, e ", uuid, uid, e);
        }
        return result;
    }

    @Override
    public RecommendPidsResponse getRecommendPids(RecommendAllRequest request, RecommendAllBodyRequest bodyRequest) {
        // 先默认走规则引擎 异常或没有获取到数据 则走原始逻辑
        MatchResponse2 matchResponse2;
        matchResponse2 = matchAndRankForDrools(request, bodyRequest);
        if(CollectionUtils.isEmpty(matchResponse2.getMatchItemList())){
            matchResponse2 = match(request, bodyRequest);
        }
        return convert2PidsResponse(matchResponse2);
    }

    @Override
    public RecommendAllResponse getAllRecommendInfo(RecommendAllRequest request, RecommendAllBodyRequest bodyRequest) {
        // 先默认走规则引擎 异常或没有获取到数据 则走原始逻辑
        MatchResponse2 matchResponse2;
        if(VIDEO_SCENDIDS.contains(request.getSceneId())){
            return dealVideoScene( request,  bodyRequest);
        }
        matchResponse2 = matchAndRankForDrools(request, bodyRequest);
        if(CollectionUtils.isEmpty(matchResponse2.getMatchItemList())){
            matchResponse2 = match(request, bodyRequest);
        }
        // 如果结果为空  兜底处理
        if( CollectionUtils.isEmpty(matchResponse2.getMatchItemList())){
            matchResponse2.setMatchItemList(commonService.fillCacheData(request));
        }
        // 如果 ruleId不为空 则说明规则引擎成功获取的数据 SCM的实验i取自ruleId
        return convert2RecommendAllResponse(matchResponse2, request);
    }


    /**
     * 处理视频流落地页场景
     * @return
     */
    private RecommendAllResponse dealVideoScene(RecommendAllRequest request, RecommendAllBodyRequest bodyRequest){
        //初始化结果
        RecommendAllResponse recommendAllResponse = new RecommendAllResponse();
        RecommendInfo [] recommendInfos = new RecommendInfo[20];
        if(request.getChannelType() == null){
            log.error("[严重异常]获取视频流落地页时渠道类型为空，参数：{}",JSONObject.toJSONString(request));
            return recommendAllResponse;
        }
//        List<RecommendInfo> recommendInfoList = new ArrayList<>(20);
        String redisPostKey = request.getUuid()+SPLIT_COLON+ request.getPageId()+SPLIT_COLON +request.getEntryVideoId();
        String cacheRedisKey = VIDEO_PAGE_CACHE_PREFIX + redisPostKey;
        String expoRedisKey = VIDEO_EXPO_CACHE_PREFIX + redisPostKey;
        Integer pageIndex = request.getPageIndex();
        // 当前页面人工插入视频的个数和位置
        List<AdvertParam> videoInfo = advertInfoService.getAdverInfoByFeedIndex(bodyRequest.getVideoInfo(), pageIndex, 20);
        // 记录已经曝光的商品  人工运营配置的主商品id  用与去重
        Set<Long> exposureSet = videoInfo.stream().map(AdvertParam::getProductId).collect(Collectors.toSet());
        //填充置顶视频id 运营配置的视频id
        fillTopAndCmsVid(pageIndex,recommendInfos,request.getEntryVideoId(),request.getEntryProductId(),videoInfo,exposureSet);
        // 记录需要推荐填充的商品
        List<Long> candidateSet =new ArrayList<>();
        int candidateNum = 20 - videoInfo.size();
        if(pageIndex==1 && !StringUtil.isBlank(request.getEntryVideoId())){
            candidateNum =candidateNum -1;
        }
        //　一 获取视频主商品
        if(pageIndex==1){
            dealVideoSceneForFirstPage(candidateNum,candidateSet,cacheRedisKey,expoRedisKey,request,exposureSet);
        }else {
            // 1 获取用户近2分钟深度浏览的商品，根据商品查询相似商品
            Long viewPid = ucRpcService.getUser2minViewPid(request.getUuid(), request.getUid());
            if(viewPid != null){
                String similarRedisKey = VIODE_SIMILAR_PID +request.getChannelType()+CommonConstants.SPLIT_COLON+request.getSceneId()+CommonConstants.SPLIT_COLON+viewPid;
                String similarResult = algorithmRedisUtil.getString(similarRedisKey);
                if(!StringUtil.isBlank(similarResult)){
                    for (String pid : similarResult.split(",")) {
                        if(StringUtil.isInteger(pid)){
                            //2 去除当前页面已经对该用户曝光的商品（hexists）
                            if(cacheRedisUtil.hexists(expoRedisKey,pid)){
                                continue;
                            }
                            Long pidLong =  Long.valueOf(pid);
                            if (exposureSet.contains(pidLong)) {
                                continue;
                            }
                            exposureSet.add(pidLong);
                            candidateSet.add(pidLong);
                            candidateNum--;
                            if(candidateNum <= 0){
                                break;
                            }
                        }
                    }
                }
            }
            // 相似商品填充后 仍不足20个 则用翻页缓存补齐
            if( candidateNum > 0 && cacheRedisUtil.llen(cacheRedisKey)<= 0 ){
                dealVideoSceneForFirstPage(candidateNum,candidateSet,cacheRedisKey,expoRedisKey,request,exposureSet);
            }

            while (candidateNum > 0){
                String pidStr = cacheRedisUtil.rpop(cacheRedisKey);
                // 如果取到最后一位
                if(INVALID_PRODUCT_ID.equals(pidStr)){
                    cacheRedisUtil.rpush(cacheRedisKey,INVALID_PRODUCT_ID);
                    break;
                }
                // 异常情况下直接退出
                if(StringUtil.isBlank(pidStr)){
                    break;
                }
                if(cacheRedisUtil.hexists(expoRedisKey,pidStr)){
                    continue;
                }
                Long pid =Long.valueOf(pidStr);
                if (exposureSet.contains(pid)) {
                    continue;
                }
                exposureSet.add(pid);
                candidateSet.add(pid);
                candidateNum--;
            }
        }

        int expNum = candidateSet.size();
        int i = 0;
        //　2 根据主商品(候选集合中的主商品)获取视频
        if(!CollectionUtils.isEmpty(candidateSet)){
            String[] pids =StringUtils.join(candidateSet,",").split(",");
            List<String> videoInfoList = algorithmRedisUtil.hmget(PRODUICT_VEDIO_REDIS,pids);
            if(!CollectionUtils.isEmpty(videoInfoList)){
                for (String videoInfos : videoInfoList) {
                    Integer vid = advertInfoService.selectOptimalVid(videoInfos,request.getChannelType());
                    if(vid==null ){
                        continue;
                    }
                    RecommendInfo recommendInfo = new RecommendInfo();
                    recommendInfo.setId(vid.toString());
                    while (expNum > 0 && i < recommendInfos.length){
                        if(recommendInfos[i] == null){
                            recommendInfos[i] =recommendInfo;
                            i++;
                            expNum--;
                            break;
                        }
                        i++;
                    }
                }
            }
            //如果填充的不足 则用热门分补齐
            if(expNum >0 ){
                Map<Long, Integer> randomDateFromCache = productVIdeoRelationCache.getRandomDateFromCache(expNum,request.getChannelType());
                for (Map.Entry<Long, Integer> entry : randomDateFromCache.entrySet()) {
                    RecommendInfo recommendInfo = new RecommendInfo();
                    recommendInfo.setId(entry.getValue().toString());
                    while (expNum > 0 && i < recommendInfos.length){
                        if(recommendInfos[i] ==null){
                            recommendInfos[i] =recommendInfo;
                            exposureSet.add(entry.getKey());
                            expNum--;
                            i++;
                            break;
                        }
                        i++;
                    }
                }
            }
        }
        // 3 将最终返回视频对应商品记录到已经曝光的reids中
        Map<String, String> exposureMap = new HashMap<>();
        for (Long pid : exposureSet) {
            if(pid!=null){
                exposureMap.put(pid.toString(),INVALID_PRODUCT_ID)  ;
            }
        }
        cacheRedisUtil.hmset(expoRedisKey,exposureMap);
        List<RecommendInfo> recommendInfoList = new ArrayList<>();
        for (int i1 = 0; i1 < recommendInfos.length; i1++) {
            if(recommendInfos[i1] != null){
                recommendInfoList.add(recommendInfos[i1]);
            }
        }
        recommendAllResponse.setRecommendInfoList(recommendInfoList);
        // 4 结果返回
        return recommendAllResponse;
    }


    /**
     * 填充置顶视频id 和cms配置的视频id
     * @param recommendInfos
     * @param topVid
     * @param topPid
     * @param videoInfo
     * @param exposureSet
     */
    private void fillTopAndCmsVid(int pageindex,RecommendInfo [] recommendInfos,String topVid,Long topPid,List<AdvertParam> videoInfo,Set<Long> exposureSet ){
        if(pageindex==1 && !StringUtil.isBlank(topVid)){
            RecommendInfo recommendInfoFirst = new RecommendInfo();
            recommendInfoFirst.setId(topVid);
            recommendInfos[0] = recommendInfoFirst;
        }
        if(topPid != null){
            exposureSet.add(topPid);
        }
        for (AdvertParam advertParam : videoInfo) {
            RecommendInfo recommendInfo = new RecommendInfo();
            recommendInfo.setId(advertParam.getId());
            if(advertParam.getProductId() != null){
                exposureSet.add(advertParam.getProductId());
            }
            int position = Integer.valueOf(advertParam.getPosition());
            recommendInfos[position-1] =recommendInfo;
        }
    }

    private void  dealVideoSceneForFirstPage(int expNum,List<Long> candidateSet,String cacheRedisKey,String expoRedisKey,RecommendAllRequest request,Set<Long> exposureList){
        // 清空已经曝光的记录 和 翻页缓存 重新召回
        cacheRedisUtil.del(cacheRedisKey);
        cacheRedisUtil.del(expoRedisKey);
        // 查询feed流
        List<Long> matchPidList = getFeedInfo(request);
        if(CollectionUtils.isEmpty(matchPidList)){
            log.error("[严重异常]视频流落地页商品召回结果为空.兜底处理，入参:request：{}",JSONObject.toJSONString(request));
            // 兜底处理
            matchPidList = productVIdeoRelationCache.fillCache(request.getChannelType());
        }
//        String[] pids =StringUtils.join(matchPidSet,",").split(",");
////        productService.createTestData(pids);
        Iterator<Long> iterator = matchPidList.iterator();
        while (iterator.hasNext()) {
            if(expNum <= 0){
                break;
            }
            Long pid = iterator.next();
            if (exposureList.contains(pid)) {
                iterator.remove();
                continue;
            }
            exposureList.add(pid);
            candidateSet.add(pid);
            expNum --;
        }
        String[] cachePid = new String[matchPidList.size()+1];
        int i =0;
        for (Long pid : matchPidList) {
            cachePid[i++] = pid.toString();
        }
        cachePid[i]= INVALID_PRODUCT_ID ;
        // 将剩余的商品集合记录到redis中
        cacheRedisUtil.lpush(cacheRedisKey,cachePid);
        cacheRedisUtil.expire(cacheRedisKey,ONE_HOUR_SECOND);
    }


    private List<Long> getFeedInfo(RecommendAllRequest request){
        List<Long> pidList =new ArrayList<>();
        RuleFact ruleFact = getRuleFact(request);
        if(ruleFact == null){
            return  pidList;
        }
        // 一 召回
        MatchOnlineRequest matchRequest = droolsCommonUtil.buildMatchOnlineRequest(request,ruleFact);
        MatchResponse2 matchResponse2 = matchAndRankService2.matchOnline(matchRequest, new ByUser());
        if(matchResponse2 == null ||CollectionUtils.isEmpty(matchResponse2.getMatchItemList())){
            return pidList;
        }
        pidList = matchResponse2.getMatchItemList().stream().map(MatchItem2::getProductId).collect(Collectors.toList());
        if(request.isDebug()){
            log.info("[检查日志]视频流落地页召回结果：{}，入参：{}",JSONObject.toJSONString(pidList),JSONObject.toJSONString(request));
        }
        return pidList;
    }

    @Override
    public RecommendInfoMapResponse getAllRecommendInfoMap(RecommendAllRequest request, RecommendAllBodyRequest bodyRequest) {

        // 先默认走规则引擎 异常或没有获取到数据 则走原始逻辑
        MatchResponse2 matchResponse2;
        matchResponse2 = matchAndRankForDrools(request, bodyRequest);
        if(CollectionUtils.isEmpty(matchResponse2.getMatchItemList())){
            matchResponse2 = match(request, bodyRequest);
        }
        return convert2RecommendInfoMapResponse(matchResponse2, request);
    }

    @Override
    public RuleFact getRuleFact(RecommendAllRequest request){
        // 构建 RuleBaseFact
        RuleBaseFact ruleBaseFact = droolsCommonUtil.buildRuleBaseFact(BuildBaseFactParam.builder()
                .uuid(request.getUuid())
                .uid(request.getUid())
                .siteId(request.getSiteId())
                .sceneId(request.getSceneId())
                .biz(request.getBiz())
                .build());
        // 获取命中的规则
        return ruleConfigCache.getRuleFactByCondition(ruleBaseFact,"layer_mosesui_feed");
    }
    private  MatchResponse2 matchAndRankForDrools(RecommendAllRequest request, RecommendAllBodyRequest bodyRequest){
        try {
            // 获取drools工作内存对象
            RuleFact ruleFact = getRuleFact(request);
            if(ruleFact==null){
                return  new MatchResponse2();
            }
            // 一 召回
            MatchResponse2 matchResponse2;
            // 1 构建matchRequest2参数  buildmatchRequest2
            if(ruleFact.getMatchType().intValue()==1) {
                // 构建在线召回match 请求参数;
                MatchOnlineRequest matchRequest = droolsCommonUtil.buildMatchOnlineRequest(request,ruleFact);
                matchResponse2 =matchAndRankService2.matchOnline(matchRequest, new ByUser());
            }else {
                MatchRequest2 matchRequest2 = buildRequest2(request,bodyRequest,ruleFact);
                matchRequest2.setDrools(true);
                matchResponse2 = matchAndRankService2.match(matchRequest2, new ByUser());
            }
            if (matchResponse2 == null || CollectionUtils.isEmpty(matchResponse2.getMatchItemList())) {
                log.error("[一般异常]规则引擎召回商品数据为空,参数：{}", JSONObject.toJSONString(request));
                return matchResponse2;
            }
            // 3 过滤 向工作内存中注入fact对象
            KieSession kieSession = kiaSessionConfig.kieSession();
            FactHandle insert = kieSession.insert(ruleFact);
            try {
                List<MatchItem2> filterResult = droolsService.filter(kieSession, ruleFact, matchResponse2.getMatchItemList(),request.getUid(),request.getUuid());
                if (CollectionUtils.isEmpty(filterResult)) {
                    log.error("[一般异常]执行规则引擎中过滤规则后商品数量为空");
                    return new MatchResponse2();
                }
                // 二 排序rank
                // 构建rank参数
                RankRequest2 rankRequest2 = droolsCommonUtil.buildRankRequestForDrools(filterResult, ruleFact);
                rankRequest2.setUuid(request.getUuid());
                rankRequest2.setUid(Integer.valueOf(request.getUid()));
                rankRequest2.setSiteId(Integer.valueOf(request.getSiteId()));
                rankRequest2.setSid(IdCalculateUtil.createUniqueId());
                rankRequest2.setTqjMaxLimit(request.getTqjMaxLimit());
                RankResponse2 rankResult = droolsService.rank(kieSession, rankRequest2);
                List<RankItem2> rankItem2List;
                if (rankResult == null || CollectionUtils.isEmpty(rankResult.getRankItem2List())) {
                    log.error("[一般异常]执行规则引擎rank规则后商品数量为空（FunctionServiceImpl）");
                    rankItem2List = matchAndRankService2.convertToRankItem2List(filterResult);
                } else {
                    rankItem2List = rankResult.getRankItem2List();
                }
                //三  机制
                // 构建类目机制参数
                UIBaseRequest uiBaseRequest = new UIBaseRequest();
                uiBaseRequest.setPageIndex("1");
                uiBaseRequest.setPageSize(20);
                uiBaseRequest.setPagePositionId(request.getSceneId());
                ByUser user=new ByUser();
                MyBeanUtil.copyNotNullProperties(request,user);
                user.setUpcUserType(getUserSexFromUc(request.getUid(),request.getUuid()));
                user.setSex(String.valueOf(getUserSexFromUc(request.getUid(),request.getUuid())));
                user.setAdvertInfoList(new ArrayList<>());
                RuleContext ruleContext = matchAndRankAnsyService.buildRuleContext(null, rankItem2List, user, uiBaseRequest);
                List<TotalTemplateInfo> categoryResult = droolsService.dealCategory(kieSession,ruleContext);
                // 结果转换 MatchResponse2
                return convertToMatchResponse2(filterResult, categoryResult, ruleFact.getRuleId());
            }catch (Exception e) {
                log.error("[严重异常]执行规则引擎逻辑出现异常,异常信息",e);
                return  new MatchResponse2();
            } finally {
                kieSession.delete(insert);
                kieSession.dispose();
            }
        }catch (Exception e){
            log.error("[严重异常]规则引擎未知异常,异常信息",e);
        }
        return  new MatchResponse2();
    }


    private MatchResponse2 convertToMatchResponse2(List<MatchItem2> matchItem2List,List<TotalTemplateInfo> totalTemplateInfoList,String ruleId){

        MatchResponse2 matchResponse2=new MatchResponse2();

        if(CollectionUtils.isEmpty(totalTemplateInfoList) || CollectionUtils.isEmpty(matchItem2List)){
            return matchResponse2;
        }
        List<MatchItem2> matchItemList=new ArrayList<>();
        matchResponse2.setMatchItemList(matchItemList);
        matchResponse2.setRuleId(ruleId);
        Map<Long, MatchItem2> matchItem2Map = matchItem2List.stream().collect(Collectors.toMap(MatchItem2::getProductId, m -> m));
        totalTemplateInfoList.forEach(totalTemplateInfo -> {
            Long productId=  Long.valueOf(totalTemplateInfo.getId());
            MatchItem2 matchItem2 = new MatchItem2();
            matchItem2.setScore(totalTemplateInfo.getScore());
            matchItem2.setProductId(productId);
            MatchItem2 matchItem=matchItem2Map.get(productId);
            if(matchItem != null){
                matchItem2.setSource(matchItem.getSource());
                matchItem2.setOwnerId(matchItem.getOwnerId());
            }
            matchItemList.add(matchItem2);
        });
        return  matchResponse2;
    }
    /**
     * 构造MatchRequest2
     * @param request
     * @param bodyRequest
     * @return
     */
    private MatchRequest2 buildRequest2(RecommendAllRequest request, RecommendAllBodyRequest bodyRequest,RuleFact ruleFact){
        MatchRequest2 matchRequest2 = new MatchRequest2();
        // 本次请求的唯一id
        String sid = IdCalculateUtil.createUniqueId();
        matchRequest2.setSid(sid);
        matchRequest2.setUuid(request.getUuid());
        matchRequest2.setResponseMapKeys(request.getResponseMapKeys());
        matchRequest2.setIsFilterByZw(request.getIsFilterByZw());
        if(StringUtils.isNotBlank(request.getSiteId())){
            try {
                Integer siteId= Integer.valueOf(request.getSiteId());
                matchRequest2.setSiteId(siteId);
            }catch (Exception e){
                log.error("[严重异常]siteId 入参格式不对, request {}", JSON.toJSONString(request), e);
            }
        }
        long uid = 0;
        if(StringUtils.isNotBlank(request.getUid())){
            try{
                uid = Long.valueOf(request.getUid());
            }catch (Exception e){
                log.error("[严重异常]uid 入参格式不对, request {}", JSON.toJSONString(request), e);
            }
            if(uid > 0){
                //从活动中心获取该用户的个性化推荐开关设置状态
                request.setPersonalizedRecommendSwitch(pushTokenService.getPersonalizedRecommendSwitch(uid, "moses.biyao.com"));
            }
        }
        matchRequest2.setUid((int)uid);
        matchRequest2.setUserSex(getUserSexFromUc(String.valueOf(uid), request.getUuid()));
        //必须有配置信息，调用方需校验
        MosesBizConfigEnum bizConfigEnum = MosesBizConfigEnum.getByBizName(request.getBiz());

        if(ruleFact != null){
            matchRequest2.setSourceDataStrategy(ruleFact.getSourceDataStrategy());
            matchRequest2.setSourceAndWeight(ruleFact.getSourceAndWeight());
            matchRequest2.setExpNum(1000);

            List<String> maunalSourceList=new ArrayList<>();
            Set<String> recommendManualSource = recommendManualSourceConfigCache.getRecommendManualSourceMapKey();
            if(CollectionUtils.isNotEmpty(recommendManualSource)){
                //拆分出召回源 本期先这么做  后期产品在规则文件中配置哪些召回源是人工召回源
                try {
                    String[] sourceAndWeightLayerArray = ruleFact.getSourceAndWeight().split(";");
                    for(String sourceAndWeightLayer :sourceAndWeightLayerArray){
                        if(StringUtils.isBlank(sourceAndWeightLayer)){
                            continue;
                        }
                        String[] sourceAndWeightArray = sourceAndWeightLayer.split("\\|");
                        for (String str2:sourceAndWeightArray){
                            String[] sourceAndWeightStr = str2.split(",");
                            if(sourceAndWeightStr.length!=2){
                                continue;
                            }
                            if(recommendManualSource.contains(sourceAndWeightStr[0])){
                                maunalSourceList.add(sourceAndWeightStr[0]);
                            }
                        }
                    }
                }catch (Exception e){
                    log.error("[严重异常]召回源{}参数解析出错",ruleFact.getSourceAndWeight());
                }
            }
            matchRequest2.setManualSourceList(maunalSourceList);

        }else {
            if(StringUtils.isNotBlank(bizConfigEnum.getSourceDataStrategy())){
                matchRequest2.setSourceDataStrategy(bizConfigEnum.getSourceDataStrategy());
            }
            if(StringUtils.isNotBlank(bizConfigEnum.getSourceWeight())){
                matchRequest2.setSourceAndWeight(bizConfigEnum.getSourceWeight());
                //如果个性化推荐开关设置为关闭且为猜你喜欢业务时，则将召回源及权重信息修改为ucb3,1.0
                if(!request.isPersonalizedRecommendSwitch()
                        && bizConfigEnum.getBizName().equals(MosesBizConfigEnum.GUESSYOULIKE_PAGE.getBizName())){
                    matchRequest2.setSourceAndWeight(bizConfigEnum.getImpersonalSourceWeight());
                    matchRequest2.setUserSex(Integer.valueOf(CommonConstants.UNKNOWN_SEX));
                }
                // 若活动个性化开关为关且不是猜你喜欢业务时
                if(!switchConfigCache.getRecommendSwitchByConfigId(CommonConstants.PERSONALIZE_RECOMMEND_OFF_CONFIG_ID) &&
                        !bizConfigEnum.getBizName().equals(MosesBizConfigEnum.GUESSYOULIKE_PAGE.getBizName()) ){
                    matchRequest2.setSourceAndWeight(bizConfigEnum.getImpersonalSourceWeight());
                    matchRequest2.setSourceDataStrategy(bizConfigEnum.getImpersonalSourceDataStrategy());
                    matchRequest2.setUserSex(Integer.valueOf(CommonConstants.UNKNOWN_SEX));
                }
            }
            if(StringUtils.isNotBlank(bizConfigEnum.getSourceRedis())){
                matchRequest2.setSourceRedis(bizConfigEnum.getSourceRedis());
            }
            if(StringUtils.isNotBlank(bizConfigEnum.getUcbDataNum())){
                matchRequest2.setUcbDataNum(bizConfigEnum.getUcbDataNum());
            }
            if(bizConfigEnum.getExpNum() > 0){
                matchRequest2.setExpNum(bizConfigEnum.getExpNum());
            }
            if (StringUtils.isNotBlank(request.getResponseMapKeys()) && bizConfigEnum.getExpNum() > 0) {
                int length = request.getResponseMapKeys().split(",").length;
                matchRequest2.setExpNum(bizConfigEnum.getExpNum() * length);
            }
        }


        if(StringUtils.isNotBlank(request.getExpNum())){
            try {
                Integer productNum = Integer.valueOf(request.getExpNum());
                matchRequest2.setExpNum(productNum);
            }catch (Exception e){
                log.error("[严重异常]expNum 入参格式不对, request {}", JSON.toJSONString(request), e);
            }
        }

        if (bodyRequest != null) {
            List<String> pids = bodyRequest.getPids();
            //最多根据100个商品，找相似商品
            if (CollectionUtils.isNotEmpty(pids) && pids.size() > 100) {
                pids = pids.subList(0, 100);
            }
            matchRequest2.setPidList(pids);
        }
        return matchRequest2;
    }

    /**
     * 将MatchResponse2装换为RecommendPidsResponse
     * @param matchResponse2
     * @return
     */
    private RecommendPidsResponse convert2PidsResponse(MatchResponse2 matchResponse2){
        RecommendPidsResponse response = new RecommendPidsResponse();
        List<String> pids = new ArrayList<>();
        response.setPids(pids);
        if(matchResponse2 == null || CollectionUtils.isEmpty(matchResponse2.getMatchItemList())){
            return response;
        }

        List<MatchItem2> matchItemList = matchResponse2.getMatchItemList();
        for(MatchItem2 matchItem2 : matchItemList){
            if(matchItem2 == null || matchItem2.getProductId() == null){
                continue;
            }
            ProductInfo productInfo = productDetailCache.getProductInfo(matchItem2.getProductId());
            if(FilterUtil.isCommonFilter(productInfo)){
                continue;
            }
            pids.add(matchItem2.getProductId().toString());
        }

        return response;
    }

    /**
     * 将MatchResponse2装换为RecommendAllResponse
     * @param matchResponse2
     * @return
     */
    private RecommendAllResponse convert2RecommendAllResponse(MatchResponse2 matchResponse2, RecommendAllRequest request){

        RecommendAllResponse response = new RecommendAllResponse();
        List<RecommendInfo> recommendInfoList = new ArrayList<>();
        response.setRecommendInfoList(recommendInfoList);
        MosesBizConfigEnum mosesBizConfigEnum = getMosesBizConfigEnum(matchResponse2, request);
            if (StringUtils.isBlank(matchResponse2.getRuleId()) && mosesBizConfigEnum == null  ){
                return  response;
            }
        // matchResponse2校验 & 获取mosesBizConfigEnum

        List<MatchItem2> matchItemList = matchResponse2.getMatchItemList();
        for(MatchItem2 matchItem2 : matchItemList){
            if(matchItem2 == null){
                continue;
            }

            if(StringUtils.isBlank(matchResponse2.getRuleId()) && mosesBizConfigEnum.isNormalProduct()){
                ProductInfo productInfo = productDetailCache.getProductInfo(matchItem2.getProductId());
                if (FilterUtil.isCommonFilter(productInfo)) {
                    continue;
                }
            }
            //初始化 recommendInfo
            RecommendInfo recommendInfo = initRecommendInfo(matchItem2, matchResponse2,mosesBizConfigEnum);
            recommendInfoList.add(recommendInfo);
        }
        return response;
    }


    /**
     * 将MatchResponse2装换为RecommendProductCardsResponse
     * @param matchResponse2 request
     * @return
     */
    private RecommendInfoMapResponse convert2RecommendInfoMapResponse(MatchResponse2 matchResponse2, RecommendAllRequest request) {
        // 初始化结果集
        RecommendInfoMapResponse recommendPidsMapResponse = new RecommendInfoMapResponse();
        Map<String, List<RecommendInfo>> recommendInfoMap = new HashMap<>();
        recommendPidsMapResponse.setRecommendInfoMap(recommendInfoMap);
        // matchResponse2校验 & 获取mosesBizConfigEnum
        MosesBizConfigEnum mosesBizConfigEnum = getMosesBizConfigEnum(matchResponse2, request);

        if (StringUtils.isBlank(matchResponse2.getRuleId()) && mosesBizConfigEnum == null) {
            return recommendPidsMapResponse;
        }
        List<MatchItem2> matchItemList = matchResponse2.getMatchItemList();

        for (MatchItem2 matchItem2 : matchItemList) {
            // id长度大于10 默认为衍生商品 对衍生商品不过滤
            if (matchItem2.getId().length() < 12 && filterUtil.isFilteredBySiteId(Long.valueOf(matchItem2.getId()), request.getSiteId())) {
                continue;
            }
            //  初始化 recommendInfo
            RecommendInfo recommendInfo = initRecommendInfo(matchItem2, matchResponse2, mosesBizConfigEnum);
            if(!request.isPersonalizedRecommendSwitch()
                    && MosesBizConfigEnum.GUESSYOULIKE_PAGE.getBizName().equals(request.getBiz())) {
                recommendInfo.setScm("");
            }
            List<RecommendInfo> recommendInfoList = recommendInfoMap.get(matchItem2.getOwnerId());
            // recommendInfoMap 中没有此id的信息
            if (recommendInfoList == null) {
                recommendInfoList = new ArrayList<>();
                recommendInfoMap.put(matchItem2.getOwnerId(), recommendInfoList);
            }
            recommendInfoList.add(recommendInfo);
        }
        return recommendPidsMapResponse;
    }


    public MosesBizConfigEnum getMosesBizConfigEnum(MatchResponse2 matchResponse2, RecommendAllRequest request) {
        if (matchResponse2 == null || CollectionUtils.isEmpty(matchResponse2.getMatchItemList())) {
            return null;
        }
        MosesBizConfigEnum mosesBizConfigEnum = MosesBizConfigEnum.getByBizName(request.getBiz());
        return mosesBizConfigEnum;
    }

    /**
     * 初始化  RecommendInfo
     *
     * @param
     * @return
     */
    public RecommendInfo initRecommendInfo(MatchItem2 matchItem2, MatchResponse2 matchResponse2, MosesBizConfigEnum mosesBizConfigEnum) {
        boolean isNormalProductBiz=true;
        String expId=matchResponse2.getRuleId();
        if(StringUtils.isBlank(expId)){
            isNormalProductBiz = mosesBizConfigEnum.isNormalProduct();
            expId = StringUtils.isBlank(matchResponse2.getExpId()) ? mosesBizConfigEnum.getDefalutExpId() : matchResponse2.getExpId();
        }
        RecommendInfo recommendInfo = new RecommendInfo();
        if (isNormalProductBiz) {
            String id = matchItem2.getProductId() == null ? matchItem2.getId() : matchItem2.getProductId().toString();
            recommendInfo.setId(id);
        } else {
            recommendInfo.setId(matchItem2.getId());
        }
        String scm = "moses." + matchItem2.getSource() + "." + expId + ".";
        recommendInfo.setScm(scm);
        return recommendInfo;
    }
}
