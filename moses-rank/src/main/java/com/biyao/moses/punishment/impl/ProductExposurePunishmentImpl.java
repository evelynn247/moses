package com.biyao.moses.punishment.impl;

import com.biyao.moses.common.constant.CacheRedisKeyConstant;
import com.biyao.moses.model.match2.MatchItem2;
import com.biyao.moses.params.rank2.RankRequest2;
import com.biyao.moses.punishment.PunishmentService;
import com.biyao.moses.util.CacheRedisUtil;
import com.uc.domain.bean.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * 商品曝光惩罚因子计算
 */
@Slf4j
@Component
public class ProductExposurePunishmentImpl implements PunishmentService {

    //24小时的毫秒数
    private final long oneDayTime = 86400000; //1000 * 3600 * 24

    @Autowired
    private CacheRedisUtil cacheRedisUtil;

    @Override
    public Map<Long, Double> getPunishment(String uuid, List<MatchItem2> matchItemList, User user) {

        if (CollectionUtils.isEmpty(matchItemList)) {
            return new HashMap<>();
        }
        //获取曝光商品 格式pid:time
        List<String> fakeExpPids = cacheRedisUtil.lrange(CacheRedisKeyConstant.NEW_HOME_FEED_EXPOSURE + uuid, 0, -1);

        List<String> realExpPids = null;
        if (user != null){
            realExpPids = user.getExpPids();
        }

        return calculateExposurePunishmentScore(matchItemList, fakeExpPids, realExpPids, uuid);
    }

    @Override
    public Map<Long, Double> getPunishment(RankRequest2 rankRequest2, User user) {
        return getPunishment(rankRequest2.getUuid(),rankRequest2.getMatchItemList(),user);
    }


    /**
     * 根据假曝光和真实曝光计算商品曝光惩罚分
     * @param matchItem2List
     * @param fakeExpPids
     * @param realExpPids
     * @param uuid
     * @return
     */
    public Map<Long, Double> calculateExposurePunishmentScore(List<MatchItem2> matchItem2List, List<String> fakeExpPids, List<String> realExpPids, String uuid){
        Map<Long, Double> punishmentMap = new HashMap<>();
        if (CollectionUtils.isEmpty(fakeExpPids) && CollectionUtils.isEmpty(realExpPids)) {
            return punishmentMap;
        }
        try {
            long currentTime = System.currentTimeMillis();
            //获取当日0点毫秒数
            long zeroTime = currentTime - (currentTime + TimeZone.getDefault().getRawOffset()) % (oneDayTime);

            Map<Long, Integer> fakeExposureTodayMap = new HashMap<>();
            getTodayFakeExp(fakeExpPids, zeroTime, fakeExposureTodayMap, uuid);

            Map<Long, Integer> exposure2DaysMap = new HashMap<>();//昨日0点至当前时间的商品曝光集合
            Map<Long, Integer> exposure4DaysMap = new HashMap<>();//3天前0点至当前时间的商品曝光集合
            Map<Long, Integer> exposure8DaysMap = new HashMap<>();//7天前0点至当前时间的商品曝光集合

            getRealExp(realExpPids, zeroTime, exposure2DaysMap, exposure4DaysMap, exposure8DaysMap, uuid);

            aggrTodayFakeExp(exposure2DaysMap, exposure4DaysMap, exposure8DaysMap, fakeExposureTodayMap);

            for (MatchItem2 item : matchItem2List) {
                int punishmentNum2days = 3 * exposure2DaysMap.getOrDefault(item.getProductId(), 0);
                int punishmentNum4days = 2 * exposure4DaysMap.getOrDefault(item.getProductId(), 0);
                int punishmentNum8days = exposure8DaysMap.getOrDefault(item.getProductId(), 0);

                //计算三种曝光惩罚因子指数最大的，也就是曝光因子数值最小的
                int mixNum = punishmentNum2days;
                if (punishmentNum4days > mixNum) {
                    mixNum = punishmentNum4days;
                }
                if (punishmentNum8days > mixNum) {
                    mixNum = punishmentNum8days;
                }

                try {
                    double minPunishmentNum = Math.pow(0.5, mixNum);
                    punishmentMap.put(item.getProductId(), minPunishmentNum);
                } catch (Exception e) {
                    log.error("[严重异常]对比2天内、4天内、8天内曝光惩罚因子最低值出错，uuid {}", uuid, e);
                    continue;
                }
            }
        }catch (Exception e){
            log.error("[严重异常]计算商品曝光惩罚分时出现异常，uuid {} ", uuid, e);
        }
        return punishmentMap;
    }

    /**
     * 从假曝光库中获取今日假曝光商品信息
     * @param fakeExpPids
     * @param zeroTime
     * @param fakeExposureTodayMap
     * @param uuid
     */
    private void getTodayFakeExp(List<String> fakeExpPids, long zeroTime, Map<Long, Integer> fakeExposureTodayMap, String uuid){
        if(CollectionUtils.isEmpty(fakeExpPids)){
            return;
        }
        long currentTime = System.currentTimeMillis();
        for (String str : fakeExpPids) {
            if (StringUtils.isEmpty(str)) {
                continue;
            }
            try {
                String[] pidTimeStr = str.split(":");
                long pid = Long.parseLong(pidTimeStr[0]);
                long time = Long.parseLong(pidTimeStr[1]);//时间戳
                //如果时间大于当前时间，则认为是无效数据
                if(time > currentTime){
                    continue;
                }
                //今日0点至当前时间
                if (time >= zeroTime) {
                    if (fakeExposureTodayMap.containsKey(pid)) {
                        Integer expNum = fakeExposureTodayMap.get(pid);
                        fakeExposureTodayMap.put(pid, expNum + 1);
                    } else {
                        fakeExposureTodayMap.put(pid, 1);
                    }
                }
            } catch (Exception e) {
                log.error("[严重异常]使用假曝光数据统计各个时段商品曝光数量出现异常 uuid {}", uuid, e);
            }
        }
    }

    /**
     * 获取真实曝光商品信息
     * @param realExpPids
     * @param zeroTime
     * @param realExposureOneDayBeforeMap  昨天真实曝光信息
     * @param realExposureThreeDaysBeforeMap 前3天真实曝光信息
     * @param realExposureSevenDaysBeforeMap 前7天真实曝光信息
     * @param uuid
     */
    private void getRealExp(List<String> realExpPids, long zeroTime,
                            Map<Long, Integer> realExposureOneDayBeforeMap,
                            Map<Long, Integer> realExposureThreeDaysBeforeMap,
                            Map<Long, Integer> realExposureSevenDaysBeforeMap,
                            String uuid){
        if(CollectionUtils.isEmpty(realExpPids)){
            return;
        }
        long currentTime = System.currentTimeMillis();

        for (String str : realExpPids) {
            if (StringUtils.isEmpty(str)) {
                continue;
            }
            try {
                String[] pidTimeStr = str.split(":");
                long pid = Long.parseLong(pidTimeStr[0]);
                long time = Long.parseLong(pidTimeStr[1]);//时间戳
                //如果时间大于当前时间，则认为是无效数据
                if(time > currentTime){
                    continue;
                }
                //昨日真实曝光
                if (time < zeroTime && time >= zeroTime - oneDayTime) {
                    if (realExposureOneDayBeforeMap.containsKey(pid)) {
                        Integer expNum = realExposureOneDayBeforeMap.get(pid);
                        realExposureOneDayBeforeMap.put(pid, expNum + 1);
                    } else {
                        realExposureOneDayBeforeMap.put(pid, 1);
                    }
                }
                //前3天真实曝光
                if (time < zeroTime && time >= zeroTime - 3 * oneDayTime) {
                    if (realExposureThreeDaysBeforeMap.containsKey(pid)) {
                        Integer expNum = realExposureThreeDaysBeforeMap.get(pid);
                        realExposureThreeDaysBeforeMap.put(pid, expNum + 1);
                    } else {
                        realExposureThreeDaysBeforeMap.put(pid, 1);
                    }
                }

                //前7天真实曝光
                if (time < zeroTime && time >= zeroTime - 7 * oneDayTime) {
                    if (realExposureSevenDaysBeforeMap.containsKey(pid)) {
                        Integer expNum = realExposureSevenDaysBeforeMap.get(pid);
                        realExposureSevenDaysBeforeMap.put(pid, expNum + 1);
                    } else {
                        realExposureSevenDaysBeforeMap.put(pid, 1);
                    }
                }
            } catch (Exception e) {
                log.error("[严重异常]使用UC商品曝光数据统计各个时段商品真实曝光数量出现异常 uuid {}", uuid, e);
            }
        }
    }

    /**
     * 聚合真实曝光和假曝光
     * @param exposure2DaysMap 昨天真实曝光
     * @param exposure4DaysMap 前3天真实曝光
     * @param exposure8DaysMap 前7天真实曝光
     * @param fakeExposureTodayMap 今天假曝光
     */
    private void aggrTodayFakeExp(Map<Long, Integer> exposure2DaysMap, Map<Long, Integer> exposure4DaysMap,
                                  Map<Long, Integer> exposure8DaysMap, Map<Long, Integer> fakeExposureTodayMap){
        if(fakeExposureTodayMap == null || fakeExposureTodayMap.size() == 0){
            return;
        }
        for(Map.Entry<Long, Integer> entry : fakeExposureTodayMap.entrySet()){
            Long pid = entry.getKey();
            Integer count = entry.getValue();
            if(exposure2DaysMap.containsKey(pid)){
                exposure2DaysMap.put(pid, exposure2DaysMap.get(pid) + count);
            }else{
                exposure2DaysMap.put(pid, count);
            }

            if(exposure4DaysMap.containsKey(pid)){
                exposure4DaysMap.put(pid, exposure4DaysMap.get(pid) + count);
            }else{
                exposure4DaysMap.put(pid, count);
            }

            if(exposure8DaysMap.containsKey(pid)){
                exposure8DaysMap.put(pid, exposure8DaysMap.get(pid) + count);
            }else{
                exposure8DaysMap.put(pid, count);
            }
        }
    }
}
