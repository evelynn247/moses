package com.biyao.moses.util;

import com.alibaba.fastjson.JSON;
import com.biyao.moses.StartApplication;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;

import com.biyao.moses.params.match.MatchRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;

import java.util.ArrayList;
import java.util.List;


@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(locations = {"classpath:dubbo-consumer.xml", "classpath:log4j2-spring.xml", "classpath:application.properties"})
//@SpringBootTest
@SpringBootTest(classes={StartApplication.class})
public class FilterTest {

    @Resource
    FilterUtil filterUtil;
    @Resource
    MatchFilterUtil matchFilterUtil;

    String uuid = "123456789";
    String uid = "144709006";
    String lat = "38.083328", lng = "114.603058";

    @Test
    public void testInit(){
        List<TotalTemplateInfo> totalTemplateList = new ArrayList<>();
        String expKey = "moses:10300128_DefaultMatch_1234";
        MatchRequest matchRequest = new MatchRequest();
        matchRequest.setUuId(uuid);
        matchRequest.setUid(uid);
        matchRequest.setLat(lat);
        matchRequest.setLng(lng);
        List<TotalTemplateInfo> result =  matchFilterUtil.gpWhitelistFilter(expKey, totalTemplateList, matchRequest);
        System.out.println(JSON.toJSONString(result));
    } 
}
