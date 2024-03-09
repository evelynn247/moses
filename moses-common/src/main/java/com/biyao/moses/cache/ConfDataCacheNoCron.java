package com.biyao.moses.cache;

import com.biyao.client.model.ExperimentConfig;
import com.biyao.client.service.IExperimentConfigDubboService;
import com.biyao.moses.common.enums.MosesConfTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @ClassName ConfDataCacheNoCron
 * @Description TODO
 * @Author admin
 * @Date 2019/8/16 15:07
 * @Version 1.0
 **/
@Slf4j
public class ConfDataCacheNoCron {

    @Autowired
    IExperimentConfigDubboService experimentConfigDubboService;

    //页面配置集合
    private Map<String, String> pageConfMap = new ConcurrentHashMap<>();
    //实验配置集合
    private Map<String, String> expConfMap = new ConcurrentHashMap<>();
    //开关配置集合
    private Map<String, String> switchConfMap = new ConcurrentHashMap<>();
    //版本控制集合
    private Map<String, String> versionConfMap = new ConcurrentHashMap<>();

    /**
     * 初始化缓存
     */
    protected void init(){
        buildConfMap();
    }

    private void buildConfMap(){
        try {
            List<ExperimentConfig> allExperimentConfig = experimentConfigDubboService.getAllExperimentConfig();
            if (CollectionUtils.isEmpty(allExperimentConfig)) {
                return;
            }
            for (ExperimentConfig experimentConfig : allExperimentConfig) {
                int type = experimentConfig.getType().intValue();
                if (type == MosesConfTypeEnum.PageConf.getType().intValue()) {
                    pageConfMap.put(experimentConfig.getKey(), experimentConfig.getValue());
                } else if (type == MosesConfTypeEnum.ExpConf.getType().intValue()) {
                    expConfMap.put(experimentConfig.getKey(), experimentConfig.getValue());
                } else if (type == MosesConfTypeEnum.SwitchConf.getType().intValue()) {
                    switchConfMap.put(experimentConfig.getKey(), experimentConfig.getValue());
                } else if (type == MosesConfTypeEnum.VersionConf.getType().intValue()) {
                    versionConfMap.put(experimentConfig.getKey(), experimentConfig.getValue());
                }
            }
        }catch(Exception e){
            log.error("实验配置从数据库刷新到内存缓存异常， e {}", e);
        }
    }

    public Map<String, String> getPageConfMap() {
        return pageConfMap;
    }

    public Map<String, String> getExpConfMap() {
        return expConfMap;
    }

    public Map<String, String> getSwitchConfMap() {
        return switchConfMap;
    }

    public Map<String, String> getVersionConfMap() {
        return versionConfMap;
    }
}
