package com.biyao.moses.service.match.impl;

import com.biyao.moses.constant.EsFuncConstant;
import com.biyao.moses.constant.MatchStrategyConst;
import com.biyao.moses.match.MatchItem2;
import com.biyao.moses.match.MatchParam;
import com.biyao.moses.service.match.IMatchOnline;
import com.biyao.moses.util.ApplicationContextProvider;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.common.lucene.search.function.CombineFunction;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static com.biyao.moses.constant.EsFuncConstant.PAINLESS;

/**
 * @program: moses-parent-online
 * @description:
 * @author: changxiaowei
 * @create: 2021-09-22 11:22
 **/
@Slf4j
@Service(value = MatchStrategyConst.PERSONAL)
public class PersonalMatchOnlineImpl implements IMatchOnline {
    @Autowired
    CommonService commonService;

    public List<MatchItem2> match(MatchParam matchParam) {
        long start = System.currentTimeMillis();
        boolean debug = matchParam.isDebug();
        String sid= matchParam.getSid();
        String uuid = matchParam.getUuid();
        String source = matchParam.getSource();
        Integer sceneId = matchParam.getSceneId();
        // 构建es召回参数
        QueryBuilder boolQuery = commonService.buildQueryBuilder(matchParam);
        // 预测用户向量
        Map<String, Object> params = new HashMap<>();
        params.put("query_vector", matchParam.getVector());
        // 个性化召回函数
        String func = EsFuncConstant.getFuncBySceneAndoSource(source,sceneId);
        QueryBuilder queryBuilder= QueryBuilders.functionScoreQuery(boolQuery, ScoreFunctionBuilders.scriptFunction(
                new Script(ScriptType.INLINE, PAINLESS, func, params))).boostMode(CombineFunction.REPLACE);
        // 热门期望数量
        int expNum = (int)Math.round(matchParam.getExpNum() * matchParam.getNumWeight());
        SearchRequest searchRequest = commonService.buildSearchRequest(queryBuilder, expNum);
        if(debug){
            log.info("[debug-检查日志-per]es召回参数准备结束，sid:{},uuid:{},耗时：{}",sid,uuid,System.currentTimeMillis()-start);
        }
        List<MatchItem2> search = commonService.search(searchRequest, matchParam);
        // 如果个性化召回失败 则重新走一次热门召回
        if(CollectionUtils.isEmpty(search)){
            matchParam.setSource(MatchStrategyConst.HOT);
            IMatchOnline match = ApplicationContextProvider.getBean(MatchStrategyConst.HOT, IMatchOnline.class);
            search = match.match(matchParam);
            if(debug){
                log.info("[debug-检查日志-per]个性化召回失败，热门召回结束，sid:{},uuid:{},耗时：{}",sid,uuid,System.currentTimeMillis()-start);
            }
        }
        return search;
    }
}
