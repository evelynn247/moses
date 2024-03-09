package com.biyao.moses.rank2.exp;

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
 * @date 2019/12/14
 **/
@Component
@Slf4j
public class RankExperimentSpace extends AbsNewExperimentSpace {

    @Autowired
    ApplicationContextProvider applicationContextProvider;

    /**
     * 实验层配置地址
     */
    private final static String LAYER_CONF_URL = "http://conf.nova.biyao.com/nova/mosesranklayer.conf";
    /**
     * 实验配置地址
     */
    private final static String EXP_CONF_URL = "http://conf.nova.biyao.com/nova/mosesrankexp.conf";

    @PostConstruct
    @Override
    protected void init() {
        refresh();
    }

    public void refresh(){
        log.info("刷新新实验配置开始");
        Map<String, ExperimentCondition> conditionMap = ApplicationContextProvider.getApplicationContext().getBeansOfType(ExperimentCondition.class);
        init(LAYER_CONF_URL, EXP_CONF_URL, conditionMap);
        log.info("刷新新实验配置结束");
    }
}
