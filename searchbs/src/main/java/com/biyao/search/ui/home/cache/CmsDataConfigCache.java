package com.biyao.search.ui.home.cache;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.biyao.search.ui.cache.guava.BaseGuavaCache;
import com.biyao.search.ui.home.constant.HomeConsts;
import com.biyao.search.ui.model.CmsTopic;
import com.biyao.search.ui.util.HttpClientUtil;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component(value = "cmsSpecialConfigCache")
public class CmsDataConfigCache extends BaseGuavaCache<String, List<CmsTopic>> {

    private Logger logger = LoggerFactory.getLogger(CmsDataConfigCache.class);

    private final static String KEY = "search:cms_special";

    private final static int TIMEOUT = 8000;

    private final static String GET_CMS_SPECIAL_KEY = "http://cmsapi.biyao.com/topic/getTopicList";
    @Override
    public void loadValueWhenStarted() {
        this.setRefreshDuration(1);
        this.setRefreshTimeUnit(TimeUnit.HOURS);
    }

    @Override
    protected List<CmsTopic> getValueWhenExpired(String key)  {
        String json = "";
        List<CmsTopic> returnList = new ArrayList<CmsTopic>();
        try{
           json = HttpClientUtil.sendGetRequest(GET_CMS_SPECIAL_KEY+"?pageSize=20&pageNum=1",TIMEOUT);
        }catch (Exception e){
            logger.error("[严重异常] get cms topic list error,url={} , error message is {}", GET_CMS_SPECIAL_KEY, e);
            return returnList;
        }
        if(StringUtils.isBlank(json)){
            logger.error("[严重异常] refresh cms special,url={},get json is null", GET_CMS_SPECIAL_KEY);
            return returnList;
        }
        try {
            JSONObject result = JSONObject.parseObject(json);
            if(result.getInteger("success")==1){
                JSONObject data = result.getJSONObject("data");
                if (MapUtils.isEmpty(data)) {
                    logger.error("[严重异常]get cms topic list error , data is null");
                    return returnList;
                }
                returnList = data.getJSONArray("list").toJavaList(CmsTopic.class);
            }else{
                logger.error("[严重异常]get cms topic list error, return message is error , json is {}",json);
                return returnList;
            }
        }catch (Exception e){
            logger.error("paser json data error , error message is {}",e);
        }
        return returnList;
    }

    public void refreshValue(){
        this.refreshValue(KEY);
    }

    public List<CmsTopic> getCmsData(){
        return this.getValueOrDefault(KEY, new ArrayList<>());
    }

}
