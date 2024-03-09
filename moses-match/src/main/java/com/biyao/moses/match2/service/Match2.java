package com.biyao.moses.match2.service;

import com.biyao.moses.match2.param.MatchParam;
import com.biyao.moses.model.match2.MatchItem2;

import java.util.List;

/**
 * @author zhaiweixi@idstaff.com
 * @date 2019/10/8
 **/
public interface Match2 {
    /**
     * 商品Match接口
     * @return
     */
    List<MatchItem2> match(MatchParam matchParam);
}

