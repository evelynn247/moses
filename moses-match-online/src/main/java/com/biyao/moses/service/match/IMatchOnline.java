package com.biyao.moses.service.match;

import com.biyao.moses.match.MatchItem2;
import com.biyao.moses.match.MatchParam;

import java.util.List;

/**
 * @program: moses-parent-online
 * @description:
 * @author: changxiaowei
 * @create: 2021-10-09 19:41
 **/
public interface IMatchOnline {

     List<MatchItem2> match(MatchParam matchParam);
}
