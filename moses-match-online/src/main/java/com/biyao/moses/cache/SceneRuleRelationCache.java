package com.biyao.moses.cache;
import com.biyao.moses.pdc.domain.SceneRuleRelationPo;
import com.biyao.moses.pdc.impl.SceneRuleRelationDaoImpl;
import com.biyao.moses.util.StringUtilMatchOnline;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.elasticsearch.index.query.QueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * @program: moses-parent-online
 * @description: 规则场景关系缓存
 * @author: changxiaowei
 * @Date: 2022-03-10 16:28
 **/
@Slf4j
@Service
@EnableScheduling
public class SceneRuleRelationCache {


    @Autowired
    SceneRuleRelationDaoImpl sceneRuleRelationDao;

    private Map<String, QueryBuilder> ruleQueryBuilderMap = new HashMap<>();

    @PostConstruct
    public void init(){
        refreshRuleQueryBuilderCache();
    }

    public void refreshRuleQueryBuilderCache() {
        Map<String, QueryBuilder> ruleQueryBuilderMapTemp = new HashMap<>();
        try {
            List<SceneRuleRelationPo> allSceneRule = sceneRuleRelationDao.getAllSceneRule();
            if (CollectionUtils.isEmpty(allSceneRule)) {
                return;
            }
            for (SceneRuleRelationPo sceneRuleRelationPo : allSceneRule) {
                QueryBuilder queryBuilder = StringUtilMatchOnline.stringToQureyBuilder(sceneRuleRelationPo.getMatchRule());
                if(queryBuilder != null){
                    ruleQueryBuilderMapTemp.put(sceneRuleRelationPo.getRuleId(), queryBuilder);
                }
            }
        }catch (Exception e){
            log.error("[严重异常]刷新召回规则缓存时异常，异常信息：",e);
        }
        if (!ruleQueryBuilderMapTemp.isEmpty()) {
            ruleQueryBuilderMap = ruleQueryBuilderMapTemp;
        }
    }
    public QueryBuilder getQueryBuilderByRuleId(String ruleId){
        return ruleQueryBuilderMap.get(ruleId);
    }
}
