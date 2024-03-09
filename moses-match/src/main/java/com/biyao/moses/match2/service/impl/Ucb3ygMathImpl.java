package com.biyao.moses.match2.service.impl;

import com.biyao.moses.cache.ProductSexLabelCache;
import com.biyao.moses.common.constant.CommonConstants;
import com.biyao.moses.match2.constants.MatchStrategyConst;
import com.biyao.moses.match2.param.MatchParam;
import com.biyao.moses.match2.service.Match2;
import com.biyao.moses.model.match2.MatchItem2;
import com.biyao.moses.params.MatchSourceData;
import com.biyao.moses.util.AlgorithmRedisUtil;
import com.biyao.moses.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author changxiaowei@idstaff.com
 * @date 2020/11/18
 * ucb3_yg 必要朋友V2.0 好友已购商品召回源
 **/
@Slf4j
@Component(value = MatchStrategyConst.UCB3YG)
public class Ucb3ygMathImpl implements Match2 {

    @Autowired
    AlgorithmRedisUtil algorithmRedisUtil;
    @Autowired
    private ProductSexLabelCache productSexLabelCache;
    private static final int PID_NUM_MAX_LIMIT = 100;

    @Override
    public List<MatchItem2> match(MatchParam matchParam) {
        //  初始化返回结果
        List<MatchItem2> result = new ArrayList<>();
        String redisKeyPrefix = "moses:" + matchParam.getSource() + "_";
        String[] responseMapKeyList;
        try {
            // 获取responseMapKeys
            String responseMapKeys = matchParam.getResponseMapKeys();
            if (!StringUtil.isBlank(responseMapKeys)) {
                responseMapKeyList = responseMapKeys.split(",");
            } else {
                return result;
            }
            //获取当前用户性别
            String userSex = Objects.isNull(matchParam.getUserSex()) ? CommonConstants.UNKNOWN_SEX : matchParam.getUserSex().toString();
            //当前用户已购买商品集合
            Integer uid = matchParam.getUid();
            String userBoughtString;
            Map<String, MatchSourceData> userBoughtMap = new HashMap<>();
            // 当前用户已购买商品集合
            if (uid != null && uid > 0) {
                String redisKey = redisKeyPrefix + uid;
                userBoughtString = getDataByRedis(redisKey);
                userBoughtMap = StringUtil.parseMatchSourceDataStr(userBoughtString, redisKey);
            }

            // 3 遍历responseMapKeyList
            for (String responseMapKey : responseMapKeyList) {

                //从redis 中获取好友已购商品
                String redisKey = redisKeyPrefix + responseMapKey;
                String friendBoughtString = getDataByRedis(redisKey);
                // 根据当前用户性别 对好友已购商品进行排序
                List<MatchSourceData> friendBoughtList = sortBySex(friendBoughtString, userBoughtMap, userSex);
                //构建返回结果集
                result = buildMatchItem2List(result, friendBoughtList, responseMapKey);
            }
        } catch (Exception e) {
            log.error("[严重异常][召回源]获取ucb3_yg召回源数据异常， uuid {}, uid {}, e ", matchParam.getUuid(), matchParam.getUid(), e);
        }
        return result;
    }


    public List<MatchSourceData> sortByScore(List<MatchSourceData> productScoreInfoList) {

        return productScoreInfoList.stream().sorted(Comparator.comparing(MatchSourceData::getScore).reversed()).collect(Collectors.toList());

    }

    /**
     * 根据商品性别和当前用户性别对商品进行排序/过滤：
     * 排序：
     * 与当前用户性别一致的商品排在前前面，中性商品排在中间，性别相反的商品排在后面。
     * 同一性别的商品按照分数排序
     * 过滤：过滤掉用户已经购买过的商品
     *
     * @param friendBoughtString        好友已购买商品
     * @param userBoughtMap 当前用户已购买商品 用于过滤
     * @param userSex                  当前用户性别
     * @return
     */
    public List<MatchSourceData> sortBySex(String friendBoughtString, Map<String, MatchSourceData> userBoughtMap, String userSex) {

        List<MatchSourceData> result = new ArrayList<>(); // 返回结果集
        List<MatchSourceData> maleList = new ArrayList<>(); // 男性商品结果集
        List<MatchSourceData> femaleList = new ArrayList<>(); // 女性商品结果集
        List<MatchSourceData> unknowSexList = new ArrayList<>(); // 中性商品结果集
        if (StringUtil.isBlank(friendBoughtString)) {
            return result;
        }
        // 解析 productIdAndScore 格式：productId:score,productId:score,
        Map<String, MatchSourceData> matchSourceDataMap = StringUtil.parseMatchSourceDataStr(friendBoughtString, MatchStrategyConst.UCB3YG);
        int index = 0;
        for (Map.Entry<String, MatchSourceData> entry : matchSourceDataMap.entrySet()) {
            // 数量控制
            if (index >= PID_NUM_MAX_LIMIT) {
                break;
            }
            String idStr = entry.getKey();
            // 过滤当前用户已购买商品
            if (userBoughtMap.containsKey(idStr)) {
                continue;
            }
            //中性 = 2， 女性=1，男性=0，未知=-1  衍生商品取不到 默认为无性别
            String productSexLabel = productSexLabelCache.getProductSexLabel(idStr);
            MatchSourceData matchSourceData = new MatchSourceData();
            matchSourceData.setId(idStr);
            matchSourceData.setScore(entry.getValue().getScore());
            if (productSexLabel.equals(CommonConstants.FEMALE_SEX)) {
                femaleList.add(matchSourceData);
            } else if (productSexLabel.equals(CommonConstants.MALE_SEX)) {
                maleList.add(matchSourceData);
            } else {
                unknowSexList.add(matchSourceData);
            }
            index++;
        }
        femaleList = sortByScore(femaleList);
        maleList = sortByScore(maleList);
        unknowSexList = sortByScore(unknowSexList);

        if (userSex.equals(CommonConstants.MALE_SEX)) {
            result.addAll(maleList);
            result.addAll(unknowSexList);
            result.addAll(femaleList);
        } else if (userSex.equals(CommonConstants.FEMALE_SEX)) {
            result.addAll(femaleList);
            result.addAll(unknowSexList);
            result.addAll(maleList);
        } else {
            result.addAll(unknowSexList);
            result.addAll(maleList);
            result.addAll(femaleList);
        }
        return result;
    }

    /**
     * 构建match返回结果
     *
     * @param result               返回结果集
     * @param friendBoughtList 排好序的好友已购商品集合
     * @param ownerId              当前好友id
     * @return
     */
    public List<MatchItem2> buildMatchItem2List(List<MatchItem2> result, List<MatchSourceData> friendBoughtList, String ownerId) {

        if (Objects.isNull(friendBoughtList)) {
            return result;
        }
        for (MatchSourceData matchSourceData : friendBoughtList) {

            MatchItem2 matchItem2 = new MatchItem2();

            matchItem2.setOwnerId(ownerId);
            matchItem2.setId(matchSourceData.getId());
            matchItem2.setScore(matchSourceData.getScore());
            matchItem2.setSource(MatchStrategyConst.UCB3YG);
            result.add(matchItem2);
        }
        return result;

    }


    private String getDataByRedis(String key) {

        String result = algorithmRedisUtil.getString(key);

        return result;
    }
}
