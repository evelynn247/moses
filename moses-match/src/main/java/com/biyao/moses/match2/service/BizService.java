package com.biyao.moses.match2.service;


import com.biyao.moses.model.match2.MatchItem2;
import com.biyao.moses.params.match2.MatchRequest2;

import java.util.List;

/**
 * @author zhaiweixi@idstaff.com
 * @date 2019/10/8
 **/
public interface BizService {

    /**
     * 业务召回
     * @param request
     * @return
     */
    List<MatchItem2> match(MatchRequest2 request);
}