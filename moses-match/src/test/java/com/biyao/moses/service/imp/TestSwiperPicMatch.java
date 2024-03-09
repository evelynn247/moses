package com.biyao.moses.service.imp;

import com.alibaba.fastjson.JSON;
import com.biyao.moses.StartApplication;
import com.biyao.moses.model.exp.MatchDataSourceTypeConf;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes={StartApplication.class})
public class TestSwiperPicMatch {

    @Resource
    SwiperPicMatch swiperPicMatch;

    String dataKey = "10300162_SPM_0000";
    String uuid = "71709271026183a0af4910832b3530000000";

    @Test
    public void test1(){
        MatchDataSourceTypeConf matchDataSourceTypeConf = new MatchDataSourceTypeConf();
        matchDataSourceTypeConf.setLat("38.083328");
        matchDataSourceTypeConf.setLng("114.603058");
        matchDataSourceTypeConf.setDataSourceType("");
        matchDataSourceTypeConf.setUpcUserType(3);
        Map<String, List<TotalTemplateInfo>> result = swiperPicMatch.executeRecommendMatch(dataKey, matchDataSourceTypeConf, uuid);
        System.out.println(JSON.toJSONString(result));

    }
}
