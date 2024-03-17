package com.biyao.search.ui.cache.guava;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.biyao.search.ui.model.PcTopicVo;
import com.biyao.search.ui.util.FileUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Created by IntelliJ IDEA.
 * 从振威提供的pc首页文件中缓存topic数据
 *
 * @author maping
 * @date 2018/8/30 10:49
 * To change this template use File | Settings | File and Code Templates.
 */
@Component
public class PcHomeTopicGuavaCache extends BaseGuavaCache<String, ArrayList<PcTopicVo>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PcHomeTopicGuavaCache.class);
    private static final String key = "pc_home_topic";
    private static final String PC_HOME_TOPIC_URL = "http://conf.nova.biyao.com/search/ui/conf/pc_topic_supplier.json";

    @Override
    @PostConstruct
    public void loadValueWhenStarted() {
        this.setMaxSize(5000);
        this.setRefreshDuration(12);
        this.setRefreshTimeUnit(TimeUnit.HOURS);
    }

    /**
     * 获取缓存topic数据
     *
     * @param key
     * @return ArrayList<PcTopicVo>
     */
    @Override
    protected ArrayList<PcTopicVo> getValueWhenExpired(String key) throws Exception {
        ArrayList<PcTopicVo> PcTopicVoList = new ArrayList<>();
        String topicStr;
        try {
            topicStr = FileUtil.remoteRead(PC_HOME_TOPIC_URL);
        } catch (Exception e) {
            LOGGER.error("[严重异常]读取pc首页topic文件出错:{}", e);
            return PcTopicVoList;
        }
        if (StringUtils.isBlank(topicStr)) {
            LOGGER.info("[一般异常]读取pc首页topic文件-----暂无数据");
            return PcTopicVoList;
        }
        JSONObject jo = JSONObject.parseObject(topicStr);
        JSONArray jsonArray = jo.getJSONArray("topic");
        if (jsonArray == null || jsonArray.isEmpty()) {
            LOGGER.info("[一般异常]读取pc首页topic文件-----暂无topic数据");
            return PcTopicVoList;
        }
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            //floor相关
            Integer floorId = jsonObject.getInteger("floorId");
            String floorTitle = jsonObject.getString("floorTitle");
            String floorViceTitle = jsonObject.getString("floorViceTitle");
            Integer floorType = jsonObject.getInteger("floorType");
            //topic相关
            String topicTitle = jsonObject.getString("topicTitle");
            String topicViceTitle = jsonObject.getString("topicViceTitle");
            String littleImages = jsonObject.getString("littleImages");
            String largeImages = jsonObject.getString("largeImages");
            Integer supplierId = jsonObject.getInteger("supplierId");

            //生成一个PcTopicVo
            PcTopicVo pcTopicVo = new PcTopicVo();
            pcTopicVo.setFloorId(floorId);
            pcTopicVo.setFloorTitle(floorTitle);
            pcTopicVo.setFloorViceTitle(floorViceTitle);
            pcTopicVo.setFloorType(floorType);
            pcTopicVo.setTopicTitle(topicTitle);
            pcTopicVo.setTopicViceTitle(topicViceTitle);
            if (!StringUtils.isBlank(littleImages)) {
                pcTopicVo.setLittleImages(Arrays.asList(littleImages.split(",")));
            }
            if (!StringUtils.isBlank(largeImages)) {
                pcTopicVo.setLargeImages(Arrays.asList(largeImages.split(",")));
            }
            pcTopicVo.setSupplierId(supplierId);

            //添加到集合
            PcTopicVoList.add(pcTopicVo);
        }
        return PcTopicVoList;
    }

    /**
     * 暴露对外获取缓存的数据
     *
     * @return ArrayList<PcTopicVo>
     */
    public ArrayList<PcTopicVo> getPcHomeFloorData() {
        return this.getValueOrDefault(key, new ArrayList<>());
    }
}
