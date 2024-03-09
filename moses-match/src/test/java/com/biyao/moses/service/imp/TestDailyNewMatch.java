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
public class TestDailyNewMatch {

    @Resource
    DailyNewProductMatch dailyNewProductMatch;

    String dataKey = "10300162_SPM_0000";
    String uuid = "2190603173405xpp63p8s7wc6fr3d0000000";

    @Test
    public void test1(){
        MatchDataSourceTypeConf matchDataSourceTypeConf = new MatchDataSourceTypeConf();
        matchDataSourceTypeConf.setLat("");
        matchDataSourceTypeConf.setDataSourceType("");
        Map<String, List<TotalTemplateInfo>> result = dailyNewProductMatch.executeRecommendMatch(dataKey, matchDataSourceTypeConf, uuid);
        System.out.println(JSON.toJSONString(result));

    }
}
