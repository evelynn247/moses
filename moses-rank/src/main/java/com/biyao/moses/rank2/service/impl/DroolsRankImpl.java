package com.biyao.moses.rank2.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.biyao.moses.common.constant.RankNameConstants;
import com.biyao.moses.common.enums.PunishmentEnum;
import com.biyao.moses.model.match2.MatchItem2;
import com.biyao.moses.model.rank2.RankItem2;
import com.biyao.moses.params.rank2.RankRequest2;
import com.biyao.moses.punishment.impl.ProductPriceFactorImpl;
import com.biyao.moses.rank2.service.Rank2;
import com.biyao.moses.rpc.UcRpcService;
import com.biyao.moses.util.ApplicationContextProvider;
import com.biyao.moses.punishment.PunishmentService;
import com.biyao.moses.util.RankUtils;
import com.uc.domain.bean.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * @program: moses-parent
 * @description: 规则引擎rank
 * @author: changxiaowei
 * @create: 2021-03-25 16:47
 **/
@Slf4j
@Component(value = RankNameConstants.DROOLS_RNAK)
public class DroolsRankImpl implements Rank2 {

    @Autowired
    UcRpcService ucRpcService;
    @Autowired
    ProductPriceFactorImpl productPriceFactor;

    @Override
    public List<RankItem2> rank(RankRequest2 rankRequest2) {
        List<RankItem2> rankItem2Lis = new ArrayList<>();
        List<MatchItem2> matchItemList = rankRequest2.getMatchItemList();
        if (CollectionUtils.isEmpty(matchItemList)) {
            return rankItem2Lis;
        }
        try {
            //获取ucUser
            User ucUser = ucRpcService.getUcUserForRank(rankRequest2.getUid(), rankRequest2.getUuid(), "mosesrank");
            // 惩罚因子
            List<Map<Long, Double>> punishmentList = new ArrayList<>();

            // 拆分punishFactor
            String[] punishFactorArray = rankRequest2.getPunishFactor().split(",");
            // 遍历 punishFactor 取出对应的惩罚因子
            for (String punishFactorId : punishFactorArray) {
                PunishmentEnum punishmentEnum = PunishmentEnum.getByPunishmentId(punishFactorId);
                if (punishmentEnum != null) {
                    PunishmentService punishmentService = ApplicationContextProvider.getApplicationContext().getBean(punishmentEnum.getBeanName(), PunishmentService.class);
                    punishmentList.add(punishmentService.getPunishment(rankRequest2, ucUser));
                }
            }
            // 如果使用价格惩罚因子 同理取出价格惩罚因子
            if (rankRequest2.getPriceFactor()) {
                punishmentList.add(productPriceFactor.getPunishment(rankRequest2, ucUser));
            }
            // 遍历商品列表 1 遍历punishmentList  计算惩罚因子之积   2 计算商品最终得分 若不适用召回分 则赋值为1否则使用商品召回分
            for (MatchItem2 matchItem2 : matchItemList) {
                double punish = 1.0;
                for (Map<Long, Double> map : punishmentList) {
                    punish = punish * map.getOrDefault(matchItem2.getProductId(), 1.0);
                }
                // 默认不使用商品召回分排序
                double score = 1.0;
                if (rankRequest2.getRecallPoints()) {
                    score = matchItem2.getScore();
                }
                // 惩罚因子*召回分
                matchItem2.setScore(score * punish);
            }
            // 按照最终得分倒排排序
            matchItemList.sort((o1, o2) -> o2.getScore().compareTo(o1.getScore()));
        }catch (Exception e){
            log.error("[严重异常][规则引擎]排序出错,入参:{}", JSONObject.toJSONString(rankRequest2),e);
        }
        log.info("[检查日志][规则引擎]droolsRank服务,是否使用价格因素:{}，是否使用召回分:{}，惩罚因子:{}，uuid:{}",
                rankRequest2.getPriceFactor(),rankRequest2.getRecallPoints(),rankRequest2.getPunishFactor(),rankRequest2.getUuid());
        //组装结果返回
        return RankUtils.convert(matchItemList);
    }
}
