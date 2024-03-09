package com.biyao.moses.cache;

import com.biyao.moses.SyncTask;
import com.biyao.moses.config.MatchRedisUtil;
import com.biyao.moses.service.ProductEsServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.biyao.moses.constant.RedisKeyConstant.MOSES_NEWUSER_SPECIAL_PRODUCTS;

/**
 * @program: moses-parent-online
 * @description: 新访客新手专享商品缓存   （运营配置的500个商品）
 * @author: changxiaowei
 * @Date: 2022-02-19 15:59
 **/
@Slf4j
@Service
@EnableScheduling
public class NewVProductCache {

    private Set<Long> newVProductSet = new HashSet<>();

    @Autowired
    MatchRedisUtil matchRedisUtil;
    @Autowired
    ProductEsServiceImpl productEsService;
    /**
     * 每10分钟刷新一次 新手专享数据
     */
    @Scheduled(cron = "0 0/10 * * * ?")
    protected void init(){
        refreshNewProductCache();
    }
/**
 * @Des  刷新新访客新手专享商品缓存（cms配置的商品）
 * @Param []
 * @return void
 * @Author changxiaowei
 * @Date  2022/2/19
 */
   public void refreshNewProductCache(){
       log.info("[任务进度]获取cms配置新访客新手专享商品开始");
       String newProducts = matchRedisUtil.getString(MOSES_NEWUSER_SPECIAL_PRODUCTS);
       if(StringUtils.isEmpty(newProducts)){
           log.error("[严重异常]查询redis获取cms配置新手专享商品结果为空");
           return;
       }
       Set<Long> newVProductSetnew = new HashSet<>();
       String[] newProductArr = newProducts.split(",");
       // 本次需要更新的商品
       List<Long> updatePidList = new ArrayList<>();
       for (String newProduct : newProductArr) {
            try {
                Long pid = Long.valueOf(newProduct);
                newVProductSetnew.add(pid);
                // 如果新集合中有 但是原内存中没有 表示该商品为新增的
                if(!CollectionUtils.isEmpty(newVProductSet) && !newVProductSet.contains(pid)){
                    updatePidList.add(pid);
                }
            }catch (Exception e){
                log.error("[严重异常]cms配置的新手专享格式错误.newProducts:{}",newProducts);
            }
       }
       for (Long pid : newVProductSet) {
            // 如果内存中 有 但是新集合中没有 则表示删除的
           if(!CollectionUtils.isEmpty(newVProductSetnew) && !newVProductSetnew.contains(pid)){
               updatePidList.add(pid);
           }
       }
       if(!CollectionUtils.isEmpty(newVProductSetnew)){
           newVProductSet = newVProductSetnew;
       }
       // 异步更新es中的商品
       if(!CollectionUtils.isEmpty(updatePidList)){
           productEsService.updateIndexByPids(updatePidList);
       }
       log.info("[任务进度]获取cms配置新访客新手专享商品开始");
   }

    /**
     * 判断商品是否为新访客新手专享商品
     * @param pid
     * @return
     */
   public boolean isNewVProduct(Long pid){
       return !CollectionUtils.isEmpty(newVProductSet) && newVProductSet.contains(pid);
   }
    public boolean isNewVProductCacheNull(){
       return CollectionUtils.isEmpty(newVProductSet);
    }
}
