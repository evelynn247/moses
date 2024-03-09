package com.biyao.moses.rank2.service;

import com.biyao.moses.model.rank2.RankItem2;
import com.biyao.moses.params.rank2.RankRequest2;

import java.util.List;

/**
 * @author zhaiweixi@idstaff.com
 * @date 2019/10/10
 **/
public interface Rank2 {

    /**
     * rank方法
     * @param rankRequest2
     * @return
     */
    List<RankItem2> rank(RankRequest2 rankRequest2);
}
