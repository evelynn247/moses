package com.biyao.moses.pdc.impl;


import com.biyao.moses.pdc.dao.ISceneRuleRelationDao;
import com.biyao.moses.pdc.domain.SceneRuleRelationPo;
import com.biyao.moses.pdc.mapper.SceneRuleRelationMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * @program: moses-parent-online
 * @description: 商品缓存
 * @author: changxiaowei
 * @create: 2021-09-14 14:29
 **/

@Slf4j
@Repository
public class SceneRuleRelationDaoImpl implements ISceneRuleRelationDao {

    @Autowired
    private SceneRuleRelationMapper sceneRuleRelationMapper;

    @Override
    public List<SceneRuleRelationPo> getAllSceneRule() {
        return sceneRuleRelationMapper.getAllSceneRule();
    }
}
