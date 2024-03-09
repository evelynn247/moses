package com.biyao.moses.service.imp;

import com.biyao.moses.StartApplication;
import com.biyao.moses.match2.param.MatchParam;
import com.biyao.moses.match2.service.impl.IbcfMatchImpl;
import com.biyao.moses.model.match2.MatchItem2;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author zhaiweixi@idstaff.com
 * @date 2019/10/19
 **/
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes={StartApplication.class})
public class TestIbcfMatch {

    @Resource
    IbcfMatchImpl ibcfMatch;

    @Test
    public void test(){
        MatchParam matchParam = MatchParam.builder()
                .uid(144711526)
                .uuid("919101111300970c009a9ebd415bf0000000")
                .upcUserType(1)
                .userSex(0)
                .build();

        List<MatchItem2> matchItem2List = ibcfMatch.match(matchParam);
    }
}
