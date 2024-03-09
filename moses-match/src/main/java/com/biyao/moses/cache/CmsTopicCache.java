package com.biyao.moses.cache;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.biyao.moses.model.cms.CmsTopic;
import com.biyao.moses.util.HttpClientUtil;

@Slf4j
@Component
@EnableScheduling
public class CmsTopicCache {

    private final static int TIMEOUT = 8000;

    private final static String GET_CMS_SPECIAL_KEY = "http://cmsapi.biyao.com/topic/getTopicList";
    
    private CmsTopic cmsTopic = new CmsTopic();
	public CmsTopic getCmsTopic() {
		return cmsTopic;
	}

	@PostConstruct
	private void init() {

        String json = "";
        List<CmsTopic> returnList = new ArrayList<CmsTopic>();
        try{
           json = HttpClientUtil.sendGetRequest(GET_CMS_SPECIAL_KEY+"?pageSize=20&pageNum=1",TIMEOUT);
        }catch (Exception e){
            log.error("get cms topic list error , error message is {}",e);
            return;
        }
        if(StringUtils.isBlank(json)){
            log.error("refresh cms special,get json is null");
            return;
        }
        try {
            JSONObject result = JSONObject.parseObject(json);
            if(result.getInteger("success")==1){
                JSONObject data = result.getJSONObject("data");
                if (MapUtils.isEmpty(data)) {
                    log.error("get cms topic list error , data is null");
                    return;
                }
                returnList = data.getJSONArray("list").toJavaList(CmsTopic.class);
            }else{
                log.error("get cms topic list error, return message is error , json is {}",json);
                return;
            }
        }catch (Exception e){
            log.error("paser json data error , error message is {}",e);
            return;
        }
        if (returnList!=null && returnList.size()>0) {
        	cmsTopic = returnList.get(0);
		}
	}
	
	@Scheduled(cron = "0 0/10 * * * ?")
	private void refreshProductDetailCache() {
		init();
	}
	
}