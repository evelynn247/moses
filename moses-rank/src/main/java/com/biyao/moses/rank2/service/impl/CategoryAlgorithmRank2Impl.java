package com.biyao.moses.rank2.service.impl;

import com.biyao.moses.common.constant.RankNameConstants;
import com.biyao.moses.model.match2.MatchItem2;
import com.biyao.moses.model.rank2.RankItem2;
import com.biyao.moses.params.rank2.RankRequest2;
import com.biyao.moses.rank2.service.Rank2;
import com.biyao.moses.rpc.UcRpcService;
import com.biyao.moses.service.CategoryService;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import com.uc.domain.bean.User;
import com.uc.domain.constant.UserFieldConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName CategoryAlgorithmRank2Impl
 * @Description 算法类目页新排序
 * @Author xiaojiankai
 * @Date 2020/12/18 11:35
 * @Version 1.0
 **/
@Slf4j
@Component(RankNameConstants.CATEGORY_ALGORITHM_RANK2)
public class CategoryAlgorithmRank2Impl implements Rank2 {

    @Autowired
    private UcRpcService ucRpcService;

    @Autowired
    private CategoryService categoryService;

    @BProfiler(key = "CategoryAlgorithmRank2Impl.rank", monitorType = {
            MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR })
    @Override
    public List<RankItem2> rank(RankRequest2 rankRequest2) {

        List<MatchItem2> matchItem2List = rankRequest2.getMatchItemList();
        if(CollectionUtils.isEmpty(matchItem2List)){
            return new ArrayList<>();
        }

        String uuid = rankRequest2.getUuid();
        int uid = rankRequest2.getUid() == null ? 0 : rankRequest2.getUid();
        try {
            //一次性查询后续需要使用uc中的所有属性
            User ucUser = getUcUser(uuid, uid);
            matchItem2List = categoryService.dealCategoryAlgorithmSort(rankRequest2);

            matchItem2List = categoryService.dealNewProductTopRule(matchItem2List, uuid, uid);

            matchItem2List = categoryService.dealExposurePunishmentRule(matchItem2List, ucUser, uuid);

            matchItem2List = categoryService.dealGoldSizeNotEnoughBottomRule(matchItem2List, ucUser, uuid);
        } catch (Exception e) {
            log.error("[严重异常]CategoryAlgorithmRankImpl#rank未知错误, uuid {}", uuid, e);
        }

        return categoryService.convertToRankItem2List(matchItem2List);
    }

    /**
     * 从uc获取用户个性化尺码、用户浏览商品等用户画像信息
     */
    private User getUcUser(String uuid, int uid){
        List<String> fieldsList = new ArrayList<>();
        //黄金尺码不足置底规则中使用此属性
        fieldsList.add(UserFieldConstants.PERSONALSIZE);
        //曝光降权规则中使用此属性
        fieldsList.add(UserFieldConstants.EXPPIDS);
        String uidStr = null;
        if(uid > 0){
            uidStr = String.valueOf(uid);
        }
        User ucUser = ucRpcService.getData(uuid, uidStr, fieldsList, "mosesrank");
        return ucUser;
    }
}
