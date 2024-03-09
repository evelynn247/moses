package com.biyao.moses.cache;

import com.biyao.moses.common.constant.CommonConstants;
import com.biyao.moses.common.constant.RedisKeyConstant;
import com.biyao.moses.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@EnableScheduling
public class CategorySortCache {

    @Autowired
    RedisUtil redisUtil;

    /**
     * 类目id排序集合（男、女、通用）
     */
    private Map<Integer,Double> categoryScore_male = new ConcurrentHashMap<>();
    private Map<Integer,Double> categoryScore_female = new ConcurrentHashMap<>();
    private Map<Integer,Double> categoryScore_common = new ConcurrentHashMap<>();


    /**
     * 一小时刷新一次
     */
    @Scheduled(cron = "0 0 0/1 1/1 * ?")
    protected void refresh() {
        log.info("[任务进度][类目排序分]获取类目排序分开始");
        long start = System.currentTimeMillis();
        Map<Integer,Double> maleTemp = getResult(RedisKeyConstant.CATEGORYSCOREMALE);
        Map<Integer,Double> femaleTemp = getResult(RedisKeyConstant.CATEGORYSCOREFEMALE);
        Map<Integer,Double> commonTemp = getResult(RedisKeyConstant.CATEGORYSCORECOMMON);

        if(maleTemp != null){
            categoryScore_male = maleTemp;
        }
        if(femaleTemp != null){
            categoryScore_female =femaleTemp;
        }
        if(commonTemp != null){
            categoryScore_common = commonTemp;
        }

        log.info("[任务进度][类目排序分]获取类目排序分结束，耗时{}ms，男性类目分个数 {}，女性类目分个数 {}，未知性别类目分个数 {}",
                System.currentTimeMillis()-start, categoryScore_male.size(),
                categoryScore_female.size(), categoryScore_common.size());
    }

    @PostConstruct
    protected void init(){
        refresh();
    }

    public Map<Integer,Double> getCategorySortMap(String sex) {
        if(CommonConstants.MALE_SEX.equals(sex)){
            return categoryScore_male;
        }else if(CommonConstants.FEMALE_SEX.equals(sex)){
            return categoryScore_female;
        }else{
            return categoryScore_common;
        }
    }

    private Map<Integer, Double> getResult(String key) {
        Map<Integer, Double> result = new HashMap<>();
        try{
            Map<String,String> redisResult = redisUtil.hgetAll(key);
            if(redisResult != null && redisResult.size() !=0){
                for (Map.Entry<String, String> item:redisResult.entrySet()) {
                    result.put(Integer.valueOf(item.getKey()),Double.valueOf(item.getValue()));
                }
            }
        }catch(Exception e){
            log.error("[严重异常][类目排序分]获取类目排序分出现异常，moses redis key {}，", key, e);
            return null;
        }
        return result;
    }


}
