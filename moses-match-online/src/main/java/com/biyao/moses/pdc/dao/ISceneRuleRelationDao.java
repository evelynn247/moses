package com.biyao.moses.pdc.dao;
import com.biyao.moses.pdc.domain.SceneRuleRelationPo;
import java.util.List;

/**
 * @program: moses-parent-online
 * @description: 场景规则关系dao
 * @author: changxiaowei
 * @create: 2021-12-02 20:19
 **/
public interface ISceneRuleRelationDao {

    /**
     * 查询所有的
     * @return
     */
    List<SceneRuleRelationPo> getAllSceneRule();
}