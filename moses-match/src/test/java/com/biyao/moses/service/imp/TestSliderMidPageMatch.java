package com.biyao.moses.service.imp;

import com.biyao.moses.StartApplication;
import com.biyao.moses.model.exp.MatchDataSourceTypeConf;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes={StartApplication.class})
public class TestSliderMidPageMatch {

    @Resource
    SliderMidPageMatch sliderMidPageMatch;

    String dataKey = "10300171_sliderMidPageMatch_0000";
    String uuid = "123456789";

    @Test
    public void test1(){
        MatchDataSourceTypeConf matchDataSourceTypeConf = new MatchDataSourceTypeConf();
        matchDataSourceTypeConf.setLat("38.083328");
        matchDataSourceTypeConf.setLng("114.603058");
        matchDataSourceTypeConf.setDataSourceType("");
        matchDataSourceTypeConf.setUpcUserType(1);
//        matchDataSourceTypeConf.setPriorityProductId("1300435011");
        System.out.println(sliderMidPageMatch.executeRecommendMatch(dataKey, matchDataSourceTypeConf, uuid).get(dataKey));
    }
}
