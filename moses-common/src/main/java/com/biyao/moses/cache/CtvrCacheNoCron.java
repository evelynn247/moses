package com.biyao.moses.cache;

import com.biyao.moses.common.constant.MatchRedisKeyConstant;
import com.biyao.moses.util.MatchRedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

/**
 * Ctvr商品数据缓存
 */
@Slf4j
public class CtvrCacheNoCron {


    @Autowired
    private MatchRedisUtil redisUtil;

    private Map<Long, Double> ctvrMap = new HashMap<>();

    protected void init() {
        refresh();
    }

    public void refresh() {
        log.info("刷新Ctvr信息开始");
        Map<Long, Double> tmpCtvrMap = null;
        try {
            String ctvrStr = redisUtil.getString(MatchRedisKeyConstant.PRODUCT_CTVR);
            if (StringUtils.isBlank(ctvrStr)) {
                return;
            }
            tmpCtvrMap = new HashMap<>();
            String[] split = ctvrStr.split(",");
            for (int a = 0; a < split.length; a++) {
                String[] productAndScore = split[a].split(":");
                tmpCtvrMap.put(Long.parseLong(productAndScore[0]), Double.parseDouble(productAndScore[1]));
            }
        } catch (Exception e) {
            log.error("[严重异常]缓存Ctvr数据异常", e);
            return;
        }
        ctvrMap = tmpCtvrMap;

        log.info("刷新Ctvr信息结束");
    }

    public Double getCtvrByPid(Long pid) {

        if(ctvrMap.size()==0){
            return 1.0;
        }

        Double score = ctvrMap.get(pid);
        if (score == null) {
            score = ctvrMap.getOrDefault(-1l, 0.0);
        }
        return score;
    }

}
