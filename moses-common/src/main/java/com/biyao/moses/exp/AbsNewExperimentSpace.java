package com.biyao.moses.exp;

import com.alibaba.fastjson.JSON;
import com.biyao.experiment.ExperimentCondition;
import com.biyao.experiment.ExperimentRequest;
import com.biyao.experiment.ExperimentSpace;
import com.biyao.experiment.ExperimentSpaceBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * @author zhaiweixi@idstaff.com
 * @date 2019/10/12
 **/
@Slf4j
public abstract class AbsNewExperimentSpace {

    protected ExperimentSpace experimentSpace;

    /**
     * 初始化实验空间
     */
    abstract protected void init();

    /**
     * @param layerConfUrl
     * @param expConfUrl
     */
    protected void init(String layerConfUrl, String expConfUrl){
        try {
            ExperimentSpace tempExperimentSpace = new ExperimentSpaceBuilder()
                    .setLayer(layerConfUrl)
                    .setExps(expConfUrl)
                    .build();
            this.experimentSpace = tempExperimentSpace;
        }catch (Exception e){
            log.error("[严重异常][实验配置]新实验空间初始化失败: layerConfUrl={}, expConfUrl={}", layerConfUrl, expConfUrl, e);
        }
    }

    /**
     * 带条件的初始化
     * @param layerConfUrl
     * @param expConfUrl
     * @param conditionMap
     */
    protected void init(String layerConfUrl, String expConfUrl, Map<String, ExperimentCondition> conditionMap){
        if (conditionMap == null || conditionMap.size() == 0){
            init(layerConfUrl, expConfUrl);
            return;
        }
        try {
            ExperimentSpace tempExperimentSpace = new ExperimentSpaceBuilder()
                    .setLayer(layerConfUrl)
                    .setExps(expConfUrl)
                    .setCondition(conditionMap)
                    .build();
            this.experimentSpace = tempExperimentSpace;
        }catch (Exception e){
            log.error("[严重异常][实验配置]新实验空间初始化失败: layerConfUrl={}, expConfUrl={}, conditions={}", layerConfUrl, expConfUrl, JSON.toJSONString(conditionMap.keySet()), e);
        }
    }

    /**
     * 是否命中实验
     * @param flagName  实验层的flag
     * @param expFlagValue 实验的flag对应的value
     * @param request
     * @return
     */
    public boolean hitExperiment(String flagName, String expFlagValue, ExperimentRequest request){
        if (expFlagValue == null || flagName == null || request == null){
            return false;
        }

        if (expFlagValue.equals(request.getStringFlag(flagName))){
            return true;
        }

        return false;
    }

    /**
     * 切分流量
     * @param request
     */
    public void divert(ExperimentRequest request){
        if (this.experimentSpace != null){
            this.experimentSpace.divert(request);
        }
    }
}
