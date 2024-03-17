package com.biyao.search.ui.remote;


import com.alibaba.fastjson.JSON;
import com.biyao.search.common.enums.PlatformEnum;
import com.biyao.search.ui.remote.request.UISearchRequest;
import com.biyao.search.ui.remote.response.HttpResult2;
import com.biyao.search.ui.remote.response.UISearchResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.CollectionUtils;

import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/spring/search-ui.xml")
public class UISearchServiceTest {

    @Autowired
    private UISearchService uiSearchService;


    @Test
    public void testSearch() {
        UISearchRequest request = new UISearchRequest();
        String str = "眼镜";
        request.setUuid("92012081812472b17d80bb2f9b3090000000");
        request.setPlatform(PlatformEnum.IOS);
        request.setPlatformStr("ios");
        request.setAppVersionNum(141);
        request.setAppVersionNumStr("141");
        request.setPvid("ddddd");
        request.setSearchParam("q=环保智能称重");
        request.setCtp("{\"pvid\":\"asdafaaas\",\"stid\":7,\"did\":\"asdafasfaa\"}");
        request.setQuery("q=".concat(str));
        request.setUid(143730291);
        request.setToActivity(0);
        HttpResult2<UISearchResponse> result = uiSearchService.search(request);
        System.out.println(JSON.toJSONString(uiSearchService.search(request)));

        boolean isok = !CollectionUtils.isEmpty(result.getData().getBlockData().get(0).getTemplates());

        assertTrue("查询"+str+"数据为空",  isok);

    }

    @Test
    public void testSearch01() {
        UISearchRequest request = new UISearchRequest();
        String str = "眼镜";
        request.setUuid("92012081812472b17d80bb2f9b3090000000");
        request.setPlatform(PlatformEnum.IOS);
        request.setPlatformStr("ios");
        request.setAppVersionNum(2141);
        request.setAppVersionNumStr("2141");
        //request.setPvid("ddddd");
        request.setSearchParam("q=经典飞行员造型");
        //request.setCtp("{\"pvid\":\"asdafaaas\",\"stid\":7,\"did\":\"asdafasfaa\"}");
        //request.setQuery("q=眼镜");
        //request.setUid(143730291);
        //request.setSupplierId(130043);
        request.setToActivity(0);
        //request.setFacetStr("活动:一起拼");
        HttpResult2<UISearchResponse> result = uiSearchService.search(request);
        System.out.println(JSON.toJSONString(uiSearchService.search(request)));

        boolean isok = !CollectionUtils.isEmpty(result.getData().getBlockData().get(0).getTemplates());

        assertTrue("查询"+str+"数据为空",  isok);

    }


}
