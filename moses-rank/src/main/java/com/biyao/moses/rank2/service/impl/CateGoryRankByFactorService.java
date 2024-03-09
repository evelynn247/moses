package com.biyao.moses.rank2.service.impl;

import com.biyao.moses.model.rank2.RankItem2;
import com.biyao.moses.params.rank2.RankRequest2;
import com.biyao.moses.punishment.impl.ProductVideoFactorImpl;
import com.uc.domain.bean.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * @program: moses-parent
 * @description: 类目下商品排序 考虑惩罚因子
 * @author: changxiaowei
 * @Date: 2022-03-01 14:11
 **/
@Slf4j
@Service
public class CateGoryRankByFactorService {
    @Autowired
    ProductVideoFactorImpl productVideoFactor;

        public List<RankItem2> rank(List<RankItem2> rankItem2s, RankRequest2 rankRequest){
            //计算惩罚因子
            Map<Long, Double> punishment = productVideoFactor.getPunishment(rankRequest, new User());
            // 重新计算排序分
            for (RankItem2 rankItem2 : rankItem2s) {
                Long productId = rankItem2.getProductId();
                Double punishmentFactor = punishment.getOrDefault(productId, 1D);
                rankItem2.setScore(rankItem2.getScore() * punishmentFactor);
            }
            // 重排序
            rankItem2s.sort((o1, o2) -> o2.getScore().compareTo(o1.getScore()));
            return rankItem2s;
        }
}
