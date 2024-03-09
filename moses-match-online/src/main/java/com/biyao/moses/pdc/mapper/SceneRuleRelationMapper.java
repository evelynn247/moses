package com.biyao.moses.pdc.mapper;

import com.biyao.moses.pdc.domain.SceneRuleRelationPo;
import java.util.List;

/**
 * @Des
 * @Param
 * @return
 * @Author changxiaowei
 * @Date  2022/3/10
 */
public interface SceneRuleRelationMapper {
    /**
     * 查询所有的场景规则映射关系
     * @return
     */
    List<SceneRuleRelationPo> getAllSceneRule();
}
