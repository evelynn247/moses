package com.biyao.moses.service.http;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.biyao.moses.util.HttpClientUtil;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;

@Service
@Slf4j
public class LocalServiceImpl  {

	private static String locUrl = "http://dcapi.biyao.com/location/queryCityNameByLngLatUuid";
	
	
    @BProfiler(key = "com.biyao.moses.service.http.LocalServiceImpl.getCityNameByLoc",
            monitorType = {MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR})
	public String getCityNameByLoc(String uuid,String lat,String lng) {
		
		String result = "";
		
		try {
			String url = buildUrl(locUrl,uuid,lat,lng);
			String sendGetRequest = HttpClientUtil.sendGetRequest(url, 200);
			JSONObject parseObject = JSONObject.parseObject(sendGetRequest);
			Integer code = parseObject.getInteger("code");
			if(code != null&& code == 1){ //请求成功
				String data = parseObject.getString("data");
				
				JSONObject dataObject = JSONObject.parseObject(data);
				result = dataObject.getString("cityName");
				
			}else{
				log.error("调用queryCityNameByLngLatUuid错误,返回{}",sendGetRequest);
			}
			
		} catch (Exception e) {
			log.error("请求match出错", e);
		}
		log.info("地理位置参数uuid={},lat={},lng={},result={}",uuid,lat,lng,result);
		return result;

	}
	
	private String buildUrl (String url,String uuid,String lat,String lng){
		
		if (StringUtils.isEmpty(uuid)||StringUtils.isEmpty(lat)||StringUtils.isEmpty(lng)) {
			return "";
		}
		String result = url + "?uuid="+uuid+"&lat="+lat+"&lng="+lng;
		return result;
	}
}