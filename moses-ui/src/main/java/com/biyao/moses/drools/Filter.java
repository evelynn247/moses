package com.biyao.moses.drools;

import com.biyao.moses.model.match2.MatchItem2;

import java.util.List;

/**
 * @program: moses-parent
 * @description: 过滤接口
 * @author: changxiaowei
 * @create: 2021-03-25 11:27
 **/
public interface Filter {



    List<MatchItem2> filter(FilterContext filterContext);
}
