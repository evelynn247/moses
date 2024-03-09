package com.biyao.moses.punishment;


import com.biyao.moses.model.match2.MatchItem2;
import com.biyao.moses.params.rank2.RankRequest2;
import com.uc.domain.bean.User;

import java.util.List;
import java.util.Map;

/**
 * 惩罚机制接口
 */
public interface PunishmentService {

    /**
     * 对一批商品计算各种惩罚分
     * 目前存在 1、商品曝光惩罚因子计算
     * @param uuid
     * @param matchItemList
     * @return
     */
    Map<Long, Double> getPunishment(String uuid, List<MatchItem2> matchItemList, User user);


    /**
     * 对一批商品计算各种惩罚分
     * @return
     */
    Map<Long, Double> getPunishment(RankRequest2 rankRequest2, User user);
}
