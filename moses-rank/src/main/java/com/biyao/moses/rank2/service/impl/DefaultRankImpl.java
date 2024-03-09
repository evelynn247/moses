package com.biyao.moses.rank2.service.impl;

import com.alibaba.fastjson.JSON;
import com.biyao.moses.common.constant.RankNameConstants;
import com.biyao.moses.model.match2.MatchItem2;
import com.biyao.moses.model.rank2.RankItem2;
import com.biyao.moses.params.rank2.RankRequest2;
import com.biyao.moses.rank2.service.Rank2;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zhaiweixi@idstaff.com
 * @date 2019/10/11
 * 新版本Rank默认排序，根据match分排序
 **/
@Slf4j
@Component(value = RankNameConstants.DEFAULT_RANK2)
public class DefaultRankImpl implements Rank2{

    @Override
    public List<RankItem2> rank(RankRequest2 rankRequest2) {
        List<RankItem2> result = new ArrayList<>();
        try {
            List<MatchItem2> matchItemList = rankRequest2.getMatchItemList();
            if (CollectionUtils.isEmpty(matchItemList)) {
                return result;
            }

            for (MatchItem2 matchItem2 : matchItemList) {
                if (matchItem2 == null) {
                    continue;
                }
                RankItem2 rankItem2 = new RankItem2();
                rankItem2.setProductId(matchItem2.getProductId());
                rankItem2.setScore(matchItem2.getScore());
                result.add(rankItem2);
            }
        }catch (Exception e){
            log.error("[严重异常]DefaultRankImpl rankRequest2 {} ", JSON.toJSONString(rankRequest2), e);
            result = new ArrayList<>();
        }
        return result;
    }
}
