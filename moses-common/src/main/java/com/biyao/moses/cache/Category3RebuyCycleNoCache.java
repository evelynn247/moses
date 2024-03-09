package com.biyao.moses.cache;

import com.biyao.moses.common.constant.MatchRedisKeyConstant;
import com.biyao.moses.util.MatchRedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

/**
 * @ClassName Category3RebuyCycleNoCache
 * @Description 后台三级类目对应的复购周期区间的最小值缓存
 * @Author xiaojiankai
 * @Date 2020/1/17 11:52
 * @Version 1.0
 **/
@Slf4j
public class Category3RebuyCycleNoCache {
    @Autowired
    private MatchRedisUtil matchRedisUtil;

    //key 为后台三级类目ID，value 为相似三级类目ID
    private Map<Long, Long> cate3RebuyCycleMap = new HashMap<>();


    private final long day1ToMs = 86400000; // 24 * 3600*1000

    protected void init(){
        refresh();
    }

    public void refresh(){
        log.info("[任务进度][复购周期]获取后台三级类目复购周期开始");
        long start = System.currentTimeMillis();
        String redisKey = MatchRedisKeyConstant.MOSES_CATE3_REBUY_CYCLE;
        Map<Long, Long> cate3RebuyCycleMapTmp = new HashMap<>();
        try{
            String cate3RebuyCycleStr = matchRedisUtil.getString(redisKey);
            if(StringUtils.isBlank(cate3RebuyCycleStr)){
                log.error("[严重异常][复购周期]获取后台三级类目复购周期为空，match redis key {}", redisKey);
                return;
            }
            cate3RebuyCycleMapTmp = parse(cate3RebuyCycleStr);
        }catch (Exception e){
            log.error("[严重异常][复购周期]获取后台三级类目复购周期失败，e ", e);
        }

        if(cate3RebuyCycleMapTmp != null && cate3RebuyCycleMapTmp.size() > 0){
            cate3RebuyCycleMap = cate3RebuyCycleMapTmp;
            log.info("[任务进度][复购周期]获取后台三级类目复购周期结束，个数 {}，耗时{}ms",
                    cate3RebuyCycleMapTmp.size(),System.currentTimeMillis()-start);
        }
    }

    /**
     * 通过后台三级类目获取复购周期，如果不存在复购周期，则返回null
     * @param cate3
     * @return
     */
    public Long getRebuyCycleMs(Long cate3){
        return cate3RebuyCycleMap.get(cate3);
    }

    /**
     * 字符串解析为Map
     * 格式为：cate3Id:rebuyDayNum,...cate3Id:rebuyDayNum
     * @param cate3RebuyCycleStr
     * @return Map， key为后台三级类目Id；value 为rebuyDay对应的毫秒数
     */
    private Map<Long, Long> parse(String cate3RebuyCycleStr){
        Map<Long, Long> result = new HashMap<>();
        if(StringUtils.isBlank(cate3RebuyCycleStr)){
            return result;
        }
        boolean isCheckError = false;
        String[] cate3RebuyCycleArray = cate3RebuyCycleStr.trim().split(",");
        for(String cate3RebuyCycle : cate3RebuyCycleArray){
            try {
                if (StringUtils.isBlank(cate3RebuyCycle)) {
                    isCheckError = true;
                    continue;
                }
                String[] cate3AndRebuyCycle = cate3RebuyCycle.trim().split(":");
                if (cate3AndRebuyCycle.length != 2) {
                    isCheckError = true;
                    continue;
                }

                Long cate3Id = Long.valueOf(cate3AndRebuyCycle[0].trim());
                Long rebuyCycle = Long.valueOf(cate3AndRebuyCycle[1].trim()) * day1ToMs;
                result.put(cate3Id, rebuyCycle);
            }catch (Exception e){
                isCheckError = false;
                log.error("[严重异常][复购周期]解析后台三级类目复购周期时出现异常，复购周期 {}", cate3RebuyCycleStr, e);
            }
        }
        if(isCheckError){
            log.error("[严重异常][复购周期]解析后台三级类目复购周期时发生错误，复购周期 {}", cate3RebuyCycleStr);
        }
        return result;
    }
}
