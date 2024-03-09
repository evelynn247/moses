package com.biyao.moses.cache;

import com.biyao.moses.Enum.SeasonEnum;
import com.biyao.moses.config.AlgorithmRedisUtil;
import com.biyao.moses.config.MatchRedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.*;

import static com.biyao.moses.constant.RedisKeyConstant.PRODUCT_SEASON;

/**
 * @program: moses-parent-online
 * @description:
 * @author: changxiaowei
 * @Date: 2021-12-21 15:50
 **/
@Slf4j
@Service
@EnableScheduling
public class ProductSeasonCache {
        private  Map<Long, List<Byte>> productSeasonMap = new HashMap<>();

        @Autowired
        private MatchRedisUtil redisUtil;
        @PostConstruct
        @Scheduled(cron = "0 0 0/1 * * ?")
        protected void init(){
            refreshProductSeasonCache();
        }

        public void refreshProductSeasonCache(){
            log.info("[任务进度][商品季节]获取商品季节信息开始");
            long start = System.currentTimeMillis();
            String productSeasonStr = redisUtil.getString(PRODUCT_SEASON);
            Map<Long, List<Byte>> productSeasonMapTmp = parseProductSeasonStr(productSeasonStr);
            if(CollectionUtils.isEmpty(productSeasonMapTmp)){
                log.error("[严重异常][商品季节]获取商品季节信息为空，不刷新缓存");
            }else{
                productSeasonMap = productSeasonMapTmp;
                log.info("[任务进度][商品季节]获取商品季节信息结束，耗时{}ms，商品个数 {}",  System.currentTimeMillis()-start, productSeasonMapTmp.size());
            }
        }

        /**
         * 通过商品id获取商品季节信息
         * @param pid
         * @return
         */
        public List<Byte> getProductSeasonValue(Long pid){

            List<Byte> defaultResult =  new ArrayList<>();
            defaultResult.add((byte)0);
            return productSeasonMap.getOrDefault(pid,defaultResult);
        }

        /**
         * 格式为：pid:season1|season2,pid:season1,...,pid:season1:season2
         * @param productSeasonStr
         * @return
         */
        private Map<Long, List<Byte>> parseProductSeasonStr(String productSeasonStr){
            Map<Long, List<Byte>> result = new HashMap<>();
            if(StringUtils.isBlank(productSeasonStr)){
                return result;
            }
            String[] productSeasonArray = productSeasonStr.split(",");
            for(String productSeason : productSeasonArray){
                try {
                    String[] pidSeasons = productSeason.split(":");
                    //如果长度不为2，则过滤掉该数据
                    if(pidSeasons.length != 2){
                        continue;
                    }
                    String seasons = pidSeasons[1];
                    String[] seasonArray = seasons.split("\\|");
                    List<Byte> seasonValue = new ArrayList<>();
                    for(String season : seasonArray){
                        seasonValue.add((byte)SeasonEnum.getSeasonIdByName(season));
                    }
                    result.put(Long.valueOf(pidSeasons[0]),seasonValue);
                }catch (Exception e){
                    log.error("[严重异常]商品季节格式错误，跳过该商品。productSeason:{}",productSeason);
                }
            }
            return result;
        }
}
