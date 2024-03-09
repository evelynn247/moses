package com.biyao.moses;

import com.biyao.moses.cache.SceneRuleRelationCache;
import com.biyao.moses.pdc.domain.SceneRuleRelationPo;
import com.biyao.moses.pdc.impl.SceneRuleRelationDaoImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * @program: moses-parent-online
 * @description:
 * @author: changxiaowei
 * @Date: 2022-03-10 16:09
 **/
@SpringBootTest
public class CommonTest {

    @Autowired
    SceneRuleRelationDaoImpl sceneRuleRelationDao;
    @Autowired
    SceneRuleRelationCache sceneRuleRelationCache;
    @Test
    public void test1(){
        sceneRuleRelationCache.refreshRuleQueryBuilderCache();
    }

}
