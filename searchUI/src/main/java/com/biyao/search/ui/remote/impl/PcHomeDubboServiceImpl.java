package com.biyao.search.ui.remote.impl;

import com.biyao.search.ui.cache.guava.PcHomeTopicGuavaCache;
import com.biyao.search.ui.home.dubbo.IPcHomeDubboService;
import com.biyao.search.ui.home.model.HomeResponse;
import com.biyao.search.ui.home.model.pc.PcFloor;
import com.biyao.search.ui.home.model.pc.PcTopic;
import com.biyao.search.ui.model.PcTopicVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by IntelliJ IDEA.
 * pc首页服务
 *
 * @author maping
 * @date 2018/8/29 20:02
 * To change this template use File | Settings | File and Code Templates.
 */
public class PcHomeDubboServiceImpl implements IPcHomeDubboService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PcHomeDubboServiceImpl.class);
    @Autowired
    private PcHomeTopicGuavaCache pcHomeTopicGuavaCache;

    /**
     * 提供pc首页topic数据
     *
     * @return HomeResponse
     * code:0 成功,其他失败
     * data:List<PcFloor>
     * message
     */
    @Override
    public HomeResponse<List<PcFloor>> getPcFloor() {
        ArrayList<PcTopicVo> pcTopicVoList;
        try {
            pcTopicVoList = pcHomeTopicGuavaCache.getPcHomeFloorData();
        } catch (Exception e) {
            LOGGER.error("[严重异常]从缓存获取floor数据出错", e);
            return new HomeResponse<>(1, "从缓存获取数据出错");
        }
        if (pcTopicVoList == null || pcTopicVoList.isEmpty()) {
            LOGGER.error("[一般异常]从缓存获取floor---暂无数据");
            return new HomeResponse<>(1, "暂无数据");
        }
        Map<Integer, List<PcTopicVo>> pcTopicVoMap = pcTopicVoList.stream().collect(Collectors.groupingBy(PcTopicVo::getFloorId));
        //构造返回数据
        ArrayList<PcFloor> pcFloorList = new ArrayList<>();
        for (Map.Entry<Integer, List<PcTopicVo>> entry : pcTopicVoMap.entrySet()) {
            List<PcTopicVo> pcTopicVos = entry.getValue();
            if (pcTopicVos.isEmpty()) {
                continue;
            }
            //构造pcTopic
            List<PcTopic> pcTopicList = new ArrayList<>();
            for (int i = 0; i < pcTopicVos.size(); i++) {
                PcTopicVo pcTopicVo = pcTopicVos.get(i);
                PcTopic pcTopic = new PcTopic();
                pcTopic.setTopicTitle(pcTopicVo.getTopicTitle());
                pcTopic.setTopicViceTitle(pcTopicVo.getTopicViceTitle());
                pcTopic.setLittleImages(pcTopicVo.getLittleImages());
                pcTopic.setLargeImages(pcTopicVo.getLargeImages());
                pcTopic.setSupplierId(pcTopicVo.getSupplierId());

                pcTopicList.add(pcTopic);
            }
            //生成pcFloor
            PcFloor pcFloor = new PcFloor();
            pcFloor.setFloorId(pcTopicVos.get(0).getFloorId());
            pcFloor.setFloorTitle(pcTopicVos.get(0).getFloorTitle());
            pcFloor.setFloorViceTitle(pcTopicVos.get(0).getFloorViceTitle());
            pcFloor.setFloorType(pcTopicVos.get(0).getFloorType());
            pcFloor.setPcTopics(pcTopicList);

            pcFloorList.add(pcFloor);
        }
        //根据floorId排序
        if (!pcFloorList.isEmpty()) {
            Collections.sort(pcFloorList, Comparator.comparing(PcFloor::getFloorId));
        }
        return new HomeResponse<>(pcFloorList);
    }
}
