package com.biyao.search.ui.cache;

import com.alibaba.fastjson.JSONObject;
import com.biyao.mag.dubbo.client.common.PageInfo;
import com.biyao.mag.dubbo.client.common.Result;
import com.biyao.mag.dubbo.client.tob.IDreamWorksShopCommonService;
import com.biyao.mag.dubbo.client.tob.dto.WorksShopDto;
import com.biyao.mag.dubbo.client.tob.dto.WorksShopParamDto;
import com.biyao.search.ui.rpc.VStoreRpcService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zj
 * @version 1.0
 * @date 2020/6/29 10:03
 * @description 梦工厂店铺信息缓存
 */
@Component
@Slf4j
@Data
public class SyncVStoreCache {

    @Autowired
    VStoreRpcService vStoreRpcService;

    /**
     * 店铺信息对象Map
     * key：店铺所属UUID，value：店铺对象
     */
    private Map<Long,WorksShopDto> worksShopMap = new HashMap<>();

    /**
     * 获取店铺信息数据ById
     * @param id （大V/企业定制号ID）
     * @return
     */
    public WorksShopDto getWorksShopById(Long id){
        if(id == null){
            return null;
        }
        return worksShopMap.get(id);
    }

    @PostConstruct
    public void init() {
        log.info("[任务报告]同步梦工厂店铺数据，系统启动时初始化到本地缓存--》start");
        refreshCache();
        log.info("[任务报告]同步梦工厂店铺数据，系统启动时初始化到本地缓存--》end");
    }

    public void refreshCache() {

        Map<Long,WorksShopDto> tempMap = vStoreRpcService.getAllWorksShopMap();
        //更新缓存数据
        if(tempMap.size() > 0){
            worksShopMap = tempMap;
        }
    }

}
