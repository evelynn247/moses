package com.biyao.moses.util;

import com.biyao.moses.model.match2.MatchItem2;
import com.biyao.moses.model.rank2.RankItem2;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zhaiweixi@idstaff.com
 * @date 2019/10/12
 **/
public class RankUtils {

    /**
     * 将matchItemList转成rankItemList
     *
     * @param matchItemList
     * @return
     */
    public static List<RankItem2> convert(List<MatchItem2> matchItemList) {
        List<RankItem2> result = new ArrayList<>();
        if (matchItemList != null) {
            matchItemList.forEach(matchItem -> {
                RankItem2 rankItem = new RankItem2();
                rankItem.setProductId(matchItem.getProductId());
                rankItem.setScore(matchItem.getScore());
                result.add(rankItem);
            });
        }

        return result;
    }

    /**
     * 将matchItem转成rankItem
     * @param matchItem
     * @return
     */
    public static RankItem2 convert(MatchItem2 matchItem) {
        if (matchItem == null) {
            return null;
        }
        RankItem2 rankItem = new RankItem2();
        rankItem.setProductId(matchItem.getProductId());
        rankItem.setScore(matchItem.getScore());
        return rankItem;

    }
}
