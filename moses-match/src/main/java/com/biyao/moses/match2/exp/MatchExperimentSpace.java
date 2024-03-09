package com.biyao.moses.match2.exp;

import com.biyao.experiment.ExperimentCondition;
import com.biyao.moses.exp.AbsNewExperimentSpace;
import com.biyao.moses.util.ApplicationContextProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;

/**
 * @author xiaojiankai@idstaff.com
 * @date 2019/11/20
 **/
@Component
@Slf4j
public class MatchExperimentSpace extends AbsNewExperimentSpace {

    @Autowired
    ApplicationContextProvider applicationContextProvider;

    /**
     * 实验层配置地址
     */
    private final static String LAYER_CONF_URL = "http://conf.nova.biyao.com/nova/mosesmatchlayer.conf";
    /**
     * 实验配置地址
     */
    private final static String EXP_CONF_URL = "http://conf.nova.biyao.com/nova/mosesmatchexp.conf";

    @PostConstruct
    @Override
    protected void init() {
        refresh();
    }

    public void refresh(){
        log.info("[任务进度][实验配置]获取新实验配置开始");
        long start = System.currentTimeMillis();
        Map<String, ExperimentCondition> conditionMap = ApplicationContextProvider.getApplicationContext().getBeansOfType(ExperimentCondition.class);
        init(LAYER_CONF_URL, EXP_CONF_URL, conditionMap);
        log.info("[任务进度][实验配置]获取新实验配置结束，耗时{}ms",System.currentTimeMillis()-start);
    }
}
