package com.biyao.moses.service;

import com.alibaba.fastjson.JSONObject;
import com.biyao.moses.Enum.SceneEnum;
import com.biyao.moses.bo.SourceWeightBo;
import com.biyao.moses.constant.CommonConstant;
import com.biyao.moses.match.*;
import com.biyao.moses.service.match.AsyncMatchOnlineService;
import com.biyao.moses.service.rpc.UcRpcService;
import com.biyao.moses.tensorflowModel.TensorflowModelService;
import com.biyao.moses.util.MatchUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.biyao.moses.common.ErrorCode.PARAM_ERROR_CODE;
import static com.biyao.moses.common.ErrorCode.SYSTEM_ERROR_CODE;
import static com.biyao.moses.constant.MatchStrategyConst.PERSONAL_ICF;

/**
 * @program: moses-parent-online
 * @description:
 * @author: changxiaowei
 * @create: 2021-09-23 10:11
 **/
@Component
@Slf4j
public class RecommendMatchContext {

    @Autowired
    AsyncMatchOnlineService asyncMatchOnlineService;
    @Autowired
    UcRpcService ucRpcService;
    @Autowired
    TensorflowModelService tensorflowModelService;

    public ApiResult<MatchResponse2> productMatch(MatchOnlineRequest request) {
        long start = System.currentTimeMillis();
        String sid=request.getSid();
        boolean debug =request.isDebug();
        String uuid =request.getUuid();
        int temp =0;
        //　初始化结果集
        ApiResult<MatchResponse2> apiResult = new ApiResult<>();
        MatchResponse2 matchResponse2 = new MatchResponse2();
        apiResult.setData(matchResponse2);
        // 解析召回源信息
        Map<String, SourceWeightBo> sourceWeightBoMap = MatchUtil.parseSourceAndWeight(request.getSourceAndWeight(), request.getSid());
        if (CollectionUtils.isEmpty(sourceWeightBoMap)) {
            apiResult.setSuccess(PARAM_ERROR_CODE);
            return apiResult;
        }
        if(debug){
            log.info("[debug-检查日志-{}]解析召回源信息结束，sid:{},uuid:{},耗时：{}",temp++,sid,uuid,System.currentTimeMillis()-start);
        }
        // 获取用户的黑名单
        Set<Long> blackList = ucRpcService.getUninterested(request.getUuid());
        if(debug){
            log.info("[debug-检查日志-{}]获取黑名单结束，sid:{},uuid:{},耗时：{}",temp++,sid,uuid,System.currentTimeMillis()-start);
        }
        // 多路召回结果集合初始化
        Map<String, Future<List<MatchItem2>>> resultMap = new HashMap<>();
        // 多路召回开始
        syncMatch(sourceWeightBoMap, request, resultMap);
        if(debug){
            log.info("[debug-检查日志-{}]异步多路召回请求结束，sid:{},uuid:{},耗时：{}",temp++,sid,uuid,System.currentTimeMillis()-start);
        }
        // 获取多路结果集合
        Map<Long, MatchItem2> pidAndMatchItem2Map = getSyncResult(sourceWeightBoMap, resultMap, request, blackList);
        if(debug){
            log.info("[debug-检查日志-{}]多路召回结果返回，sid:{},uuid:{},耗时：{}",temp++,sid,uuid,System.currentTimeMillis()-start);
        }
        // rerank
        List<MatchItem2> matchItem2List = rerank(pidAndMatchItem2Map, request.getExpNum());
        if(debug){
            List<Long> collect = matchItem2List.stream().map(MatchItem2::getProductId).collect(Collectors.toList());
            log.info("[debug-检查日志-{}]重排序结束，sid:{},uuid:{},耗时：{},召回顺序:{}",temp++,sid,uuid,System.currentTimeMillis()-start,JSONObject.toJSONString(collect));
        }
        //结果封装
        if (CollectionUtils.isEmpty(matchItem2List)) {
            apiResult.setSuccess(SYSTEM_ERROR_CODE);
            log.error("[严重异常][邮件告警]在线召回结果为空，请求参数：{}", JSONObject.toJSONString(request));
        }
        matchResponse2.setMatchItemList(matchItem2List);
        if(debug){
            log.info("[debug-检查日志-{}]结果返回结束，sid:{},uuid:{},耗时：{}",temp++,sid,uuid,System.currentTimeMillis()-start);
        }
        return apiResult;
    }


    /**
     * 重排序 粗排
     * @param pidAndMatchItem2Map
     * @param expNumMax
     * @return
     */
    private List<MatchItem2> rerank(Map<Long, MatchItem2> pidAndMatchItem2Map, int expNumMax) {
        List<MatchItem2> resultList = new ArrayList<>();

        if (CollectionUtils.isEmpty(pidAndMatchItem2Map)) {
            return resultList;
        }
        Collection<MatchItem2> values = pidAndMatchItem2Map.values();
        for (MatchItem2 value : values) {
            value.setScore(value.getScore() / value.getMatchNum());
            resultList.add(value);
        }
        List<MatchItem2> matchSorted = values.stream().sorted((m1, m2) -> -m1.getScore().compareTo(m2.getScore())).collect(Collectors.toList());
        return matchSorted.size() > expNumMax ? matchSorted.subList(0, expNumMax) : matchSorted;
    }

    /**
     * @return java.util.Set<com.biyao.moses.match.MatchItem2>
     * @Des 多路结果聚合
     * @Param [resultSet:结果集合 onceResult:单次结果 scoreWeight:召回分权重]
     * @Author changxiaowei
     * @Date 2021/12/20
     */
    private void aggregation(Map<Long, MatchItem2> resultMatchMap, List<MatchItem2> onceResult, double scoreWeight, Set<Long> blackList) {

        if (CollectionUtils.isEmpty(onceResult)) {
            return;
        }
        for (MatchItem2 matchItem2 : onceResult) {
            if (matchItem2 == null || matchItem2.getProductId() == null) {
                continue;
            }
            Long productId = matchItem2.getProductId();
            // 如果已经被召回 则累加召回分、累加数据源、召回次数+1
            if (resultMatchMap.containsKey(productId)) {
                // 黑名单过滤
                if (blackList.contains(productId)) {
                    continue;
                }
                MatchItem2 matchItem = resultMatchMap.get(productId);
                // 召回次数+1
                matchItem.setMatchNum(matchItem2.getMatchNum() + 1);
                // 拼接召回源
                matchItem.setScore(matchItem.getScore() + matchItem2.getScore() * scoreWeight);
                // 累加召回分
                matchItem.setSource(matchItem.getSource() + "," + matchItem2.getSource());
            } else {
                matchItem2.setScore(matchItem2.getScore() * scoreWeight);
                resultMatchMap.put(productId, matchItem2);
            }
        }
    }

    /**
     * 异步召回
     * @param sourceWeightBoMap
     * @param request
     * @param resultMap
     */
    private void syncMatch(Map<String, SourceWeightBo> sourceWeightBoMap, MatchOnlineRequest request, Map<String, Future<List<MatchItem2>>> resultMap) {

        //预测用户向量
        Map<String, float[]> predict = new HashMap<>();
        SceneEnum sceneEnum = SceneEnum.getSceneEnum(request.getSceneId());
        // 如果需要根据主商品找相似商品 则无需预测向量 从redis中获取主商品的icf向量
        if(Objects.nonNull(request. getMainPid())){
            predict =tensorflowModelService.getIcfVectorByMainPid(request.getMainPid());
            // 如果有主商品且成功获取到icf向量  则修改召回源 --只保留icf召回源
            if (!predict.isEmpty()) {
                sourceWeightBoMap.keySet().removeIf((key-> {
                    if(PERSONAL_ICF.equals(key)){
                        sourceWeightBoMap.get(key).setScoreWeight(1);
                        sourceWeightBoMap.get(key).setNumWeight(1);
                        return false;
                    }
                    return true;
                }));
            }
        }

        if((sceneEnum == null || sceneEnum.isPredict()) && predict.isEmpty()){
            predict = tensorflowModelService.predict(request);
        }
        Set<Map.Entry<String, SourceWeightBo>> entries = sourceWeightBoMap.entrySet();
        for (Map.Entry<String, SourceWeightBo> entry : entries) {
            String realSource = entry.getKey();
            if (StringUtils.isEmpty(realSource)) {
                log.error("[严重异常]召回源配置格式错误，跳过该召回召回，召回源配置：{}，sid：{}", request.getSourceAndWeight(), request.getSid());
                continue;
            }
            // 构建match es请求参数
            MatchParam matchParam = MatchParam.builder()
                    .userSex(request.getUserSex())
                    .userSeason(request.getUserSeason())
                    .uuid(request.getUuid())
                    .sceneId(Integer.valueOf(request.getSceneId()))
                    .siteId(request.getSiteId())
                    .userPaltform(request.getSiteId().byteValue())
                    .isPersonal(request.getIsPersonal())
                    .expNum(request.getExpNum())
                    .numWeight(entry.getValue().getNumWeight())
                    .fxCardId(request.getFxCardId())
                    .mjCardIds(request.getMjCardIds())
                    .source(entry.getKey())
                    .deivce(request.getDevice())
                    .viewPids(request.getViewPids())
                    .location(request.getLocation())
                    .debug(request.isDebug())
                    .sid(request.getSid())
                    .thirdCateGoryId(request.getThirdCateGoryIdList())
                    .tagId(request.getTagIdList())
                    .channelType(request.getChannelType())
                    .vector(CollectionUtils.isEmpty(predict)? null:predict.get(realSource))
                    .ruleId(request.getRuleId())
                    .build();
            Future<List<MatchItem2>> asyncMatchResult = asyncMatchOnlineService.executeMatch2(matchParam, realSource);
            resultMap.put(realSource, asyncMatchResult);
        }
    }

    /**
     * 获取异步结果
     *
     * @param sourceWeightBoMap   召回源及其权重
     * @param resultMap 异步结果集合
     * @return
     */
    private Map<Long, MatchItem2> getSyncResult(Map<String, SourceWeightBo> sourceWeightBoMap,
                                                Map<String, Future<List<MatchItem2>>> resultMap, MatchOnlineRequest request, Set<Long> blackList) {

        Map<Long, MatchItem2> pidAndMatchItem2Map = new HashMap<>();
        Set<Map.Entry<String, SourceWeightBo>> entries = sourceWeightBoMap.entrySet();
        // 遍历取出每一个召回源
        for (Map.Entry<String, SourceWeightBo> entry : entries) {
            String realSource = entry.getKey();
            SourceWeightBo sourceWeightBo = entry.getValue();
            if (StringUtils.isEmpty(realSource) || sourceWeightBo == null) {
                continue;
            }
            Future<List<MatchItem2>> asynResult = resultMap.get(realSource);
            try {
                long start = System.currentTimeMillis();
                List<MatchItem2> matchResult = asynResult.get(CommonConstant.MATCH_MAX_WAIT_TIME, TimeUnit.MILLISECONDS);
                if(request.isDebug()){
                    log.info("[debug-检查日志2-1]获取异步召回结果，耗时：{}，source:{}",System.currentTimeMillis()-start,realSource);
                }
                aggregation(pidAndMatchItem2Map, matchResult, sourceWeightBo.getScoreWeight(), blackList);
                if(request.isDebug()){
                    log.info("[debug-检查日志2-2]结果聚合结束，耗时：{}，source:{}",System.currentTimeMillis()-start,realSource);
                }
            } catch (Exception e) {
                log.error("[严重异常][邮件告警]获取召回源商品数据异常，召回源{}，sid {}，超时时间{}ms", realSource, request.getSid(), CommonConstant.MATCH_MAX_WAIT_TIME, e);
            }
        }
        return pidAndMatchItem2Map;
    }


}
