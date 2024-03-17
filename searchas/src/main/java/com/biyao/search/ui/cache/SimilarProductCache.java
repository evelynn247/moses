package com.biyao.search.ui.cache;

import com.biyao.search.ui.constant.CommonConstant;
import com.biyao.search.ui.util.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * @author zj
 * @version 1.0
 * @date 2020/9/16 14:19
 * @description query相似商品缓存
 */
@Component
@Slf4j
public class SimilarProductCache {

    private Map<String, List<String>> similarProductMap = new HashMap<>();

    @PostConstruct
    public void init(){
        log.info("[操作日志]加载query相似商品缓存开始...");
        refresh();
        log.info("[操作日志]加载query相似商品缓存结束");
    }

    public void refresh(){
        Map<String, List<String>> temp = new HashMap<>();
        try{
            //读取文件
            //文件格式为 query:pid1,pid2,pid3,...
            List<String> lines = FileUtil.getRemoteFile(CommonConstant.SIMILAR_PRODUCT_URL);
            if(lines !=null){
                lines.forEach(line->{
                    if(!line.startsWith("#")){
                        String[] arrayTemp = line.split(":");
                        if(arrayTemp.length == 2){
                            String key = arrayTemp[0].trim();
                            String value = arrayTemp[1].trim();
                            if(StringUtils.isNotBlank(key) && StringUtils.isNotBlank(value)){
                                String[] pids = value.split(",");
                                if(pids.length > 0){
                                    temp.put(key, Arrays.asList(pids));
                                }
                            }
                        }
                    }
                });
                similarProductMap = temp;
                log.info("[操作日志]刷新query相似商品缓存结束，共更新数据：{}条",similarProductMap.size());
            }

        }catch(Exception e){
            log.error("[严重异常]加载query相似商品缓存异常：", e);
        }

    }

    /**
     * 返回query词对应的相似商品集合
     * @param query
     * @return
     */
    public List<String> getSimilarProduct(String query){
        return similarProductMap.getOrDefault(query,null);
    }
}
