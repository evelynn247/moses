package com.biyao.moses.match2.service;


import com.biyao.moses.model.match2.MatchItem2;
import com.biyao.moses.params.match2.MatchRequest2;

import java.util.List;

/**
 * match中规则接口
 */
public interface RuleService {

    /**
     * 规则执行通用方法
     * @param list
     * @return
     */
    List<MatchItem2> execute(List<MatchItem2> list,MatchRequest2 request);

}
