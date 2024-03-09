package com.biyao.moses.rank;

import com.alibaba.fastjson.JSONObject;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.rank.impl.SameShopShuffleRank;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.List;

/**
 * @Auther: ws
 * @Date: 2019/5/28 18:03
 * @Description:
 */
@RunWith(SpringJUnit4ClassRunner.class)
public class SameShopShuffleRankTest  {

    @Test
    public void testReOrder(){
        List<TotalTemplateInfo> executeRecommend = new ArrayList<>();
        TotalTemplateInfo totalTemplateInfo = new TotalTemplateInfo();
        totalTemplateInfo.setSupplierId("1");
        totalTemplateInfo.setMainTitle("1A");
        executeRecommend.add(totalTemplateInfo);

         totalTemplateInfo = new TotalTemplateInfo();
        totalTemplateInfo.setSupplierId("1");
        totalTemplateInfo.setMainTitle("1B");
        executeRecommend.add(totalTemplateInfo);

         totalTemplateInfo = new TotalTemplateInfo();
        totalTemplateInfo.setSupplierId("2");
        totalTemplateInfo.setMainTitle("2A");
        executeRecommend.add(totalTemplateInfo);

        totalTemplateInfo = new TotalTemplateInfo();
        totalTemplateInfo.setSupplierId("2");
        totalTemplateInfo.setMainTitle("2B");
        executeRecommend.add(totalTemplateInfo);

        totalTemplateInfo = new TotalTemplateInfo();
        totalTemplateInfo.setSupplierId("2");
        totalTemplateInfo.setMainTitle("3A");
        executeRecommend.add(totalTemplateInfo);

        SameShopShuffleRank rank = new SameShopShuffleRank();
        List<TotalTemplateInfo> temProductList = new ArrayList<>();
        temProductList.addAll(executeRecommend);
        List<TotalTemplateInfo> resultList = new ArrayList<>();
        rank.reOrderProductList(resultList,temProductList,0,executeRecommend.size());
        System.out.println(JSONObject.toJSONString(executeRecommend));
        System.out.println(JSONObject.toJSONString(resultList));
    }



}
