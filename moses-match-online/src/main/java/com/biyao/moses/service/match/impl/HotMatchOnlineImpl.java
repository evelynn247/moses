package com.biyao.moses.service.match.impl;

import com.biyao.moses.constant.MatchStrategyConst;
import com.biyao.moses.match.MatchItem2;
import com.biyao.moses.match.MatchParam;
import com.biyao.moses.service.match.IMatchOnline;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.lucene.search.function.CombineFunction;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import static com.biyao.moses.constant.EsFuncConstant.HOT_FUNC;
import static com.biyao.moses.constant.EsFuncConstant.PAINLESS;

/**
 * @program: moses-parent-online
 * @description:
 * @author: changxiaowei
 * @create: 2021-10-09 19:24
 **/
@Slf4j
@Service(value = MatchStrategyConst.HOT)
public class HotMatchOnlineImpl implements IMatchOnline {

    @Autowired
    CommonService commonService;
    @Autowired
    RestHighLevelClient restHighLevelClient;

    @Override
    public List<MatchItem2> match(MatchParam matchParam) {
        long start = System.currentTimeMillis();
        boolean debug = matchParam.isDebug();
        String sid = matchParam.getSid();
        String uuid = matchParam.getUuid();
        // 构建es召回参数
        QueryBuilder boolQuery = commonService.buildQueryBuilder(matchParam);
        QueryBuilder queryBuilder = QueryBuilders.functionScoreQuery(boolQuery, ScoreFunctionBuilders.scriptFunction(
                new Script(ScriptType.INLINE, PAINLESS, HOT_FUNC, new HashMap<>()))).boostMode(CombineFunction.REPLACE);
        // 热门期望数量
        int expNum = (int) Math.round(matchParam.getExpNum() * matchParam.getNumWeight());
        //设置召回属性 （召回字段 查询数量 索引别名）
        SearchRequest searchRequest = commonService.buildSearchRequest(queryBuilder, expNum);
        if(debug){
            log.info("[debug-检查日志-hot]构建es召回参数结束，sid:{},uuid:{},耗时：{}",sid,uuid,System.currentTimeMillis()-start);
        }
        return commonService.search(searchRequest, matchParam);
    }
}
