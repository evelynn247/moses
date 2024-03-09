package com.biyao.moses.cache;

import com.biyao.moses.common.constant.MatchRedisKeyConstant;
import com.biyao.moses.common.enums.SeasonEnum;
import com.biyao.moses.util.MatchRedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @ClassName ProductSeasonCacheNoCron
 * @Description 商品季节缓存
 * @Author xiaojiankai
 * @Date 2019/10/23 15:05
 * @Version 1.0
 **/
@Slf4j
public class ProductSeasonCacheNoCron {
    //value 为春、夏、秋、冬对应的枚举id值之和，当包含四季时，值为四季对应枚举的id值
    private Map<String, Integer> productSeasonMap = new HashMap<>();

    @Autowired
    private MatchRedisUtil redisUtil;
    protected void init(){
      refreshProductSeasonCache();
    }

    public void refreshProductSeasonCache(){
        log.info("[任务进度][商品季节]获取商品季节信息开始");
        long start = System.currentTimeMillis();
        String redisKey = MatchRedisKeyConstant.PRODUCT_SEASON;
        String productSeasonStr = redisUtil.getString(redisKey);
        Map<String, Integer> productSeasonMapTmp = parseProductSeasonStr(productSeasonStr);
        if(CollectionUtils.isEmpty(productSeasonMapTmp)){
            log.error("[严重异常][商品季节]获取商品季节信息为空，不刷新缓存");
        }else{
            productSeasonMap = productSeasonMapTmp;
            log.info("[任务进度][商品季节]获取商品季节信息结束，耗时{}ms，商品个数 {}",  System.currentTimeMillis()-start, productSeasonMapTmp.size());
        }
    }

    /**
     * 通过商品id获取商品季节信息
     * 如果没有该商品的季节信息，则返回0
     * @param pid
     * @return
     */
    public int getProductSeasonValue(String pid){
        int result = 0;
        if(StringUtils.isBlank(pid)){
            return result;
        }
        if(productSeasonMap.containsKey(pid)){
            Integer seasonValue = productSeasonMap.get(pid);
            if(seasonValue != null){
                result = seasonValue.intValue();
            }
        }
        return result;
    }

    /**
     * 格式为：pid:season1|season2,pid:season1,...,pid:season1:season2
     * @param productSeasonStr
     * @return
     */
    private Map<String, Integer> parseProductSeasonStr(String productSeasonStr){
        Map<String, Integer> result = new HashMap<>();
        if(StringUtils.isBlank(productSeasonStr)){
            return result;
        }
        String[] productSeasonArray = productSeasonStr.split(",");
        for(String productSeason : productSeasonArray){
            String[] pidSeasons = productSeason.split(":");
            //如果长度不为2，则过滤掉该数据
            if(pidSeasons.length != 2){
                continue;
            }
            String pid = pidSeasons[0];
            String seasons = pidSeasons[1];
            String[] seasonArray = seasons.split("\\|");
            int seasonValue = 0;
            for(String season : seasonArray){
                if(SeasonEnum.COMMON.getName().equals(season)){
                    //如果商品季节包括四季，则忽略其他季节
                    seasonValue = SeasonEnum.COMMON.getId();
                    break;
                }

                if(SeasonEnum.SPRING.getName().equals(season)){
                    seasonValue += SeasonEnum.SPRING.getId();
                }else if(SeasonEnum.SUMMER.getName().equals(season)){
                    seasonValue += SeasonEnum.SUMMER.getId();
                }else if(SeasonEnum.AUTUMN.getName().equals(season)){
                    seasonValue += SeasonEnum.AUTUMN.getId();
                }else if(SeasonEnum.WINTER.getName().equals(season)){
                    seasonValue += SeasonEnum.WINTER.getId();
                }
            }
            if(seasonValue != 0){
                result.put(pid,seasonValue);
            }
        }
        return result;
    }
}
