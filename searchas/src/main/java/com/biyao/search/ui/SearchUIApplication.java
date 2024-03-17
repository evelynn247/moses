package com.biyao.search.ui;

import com.alibaba.fastjson.JSON;
import com.biyao.search.common.enums.PlatformEnum;
import com.biyao.search.ui.remote.UISearchService;
import com.biyao.search.ui.remote.request.UISearchRequest;

import com.biyao.search.ui.remote.response.HttpResult2;
import com.biyao.search.ui.remote.response.UISearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.math.BigDecimal;


public class SearchUIApplication {

    static public Logger logger = LoggerFactory.getLogger(SearchUIApplication.class);

    public static void main(String[] args) throws Exception {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"spring/search-ui.xml"});
        context.start();
        //test(context);
        System.in.read(); // 按任意键退出
    }

    public static void test(ClassPathXmlApplicationContext context){
        UISearchService uiSearchService = (UISearchService) context.getBean("uiNewSearchService");
        UISearchRequest request = new UISearchRequest();
        request.setUuid("92012081812472b17d80bb2f9b3090000000");
        request.setPlatform(PlatformEnum.IOS);
        request.setPlatformStr("ios");
        request.setAppVersionNum(1000);
        request.setAppVersionNumStr("1000");
        //request.setPvid("ddddd");
        request.setSearchParam("q=经典飞行员造型");
        //request.setCtp("{\"pvid\":\"asdafaaas\",\"stid\":7,\"did\":\"asdafasfaa\"}");
       // request.setQuery("q=眼镜");
        //request.setUid(143730291);
        //request.setSupplierId(130043);
        request.setToActivity(0);
        Byte a = 1;
        System.out.println(a==1);
        //request.setFacetStr("活动:一起拼");
        HttpResult2<UISearchResponse> result = uiSearchService.search(request);
        //System.out.println(JSON.toJSONString(uiSearchService.search(request)));

    }
}
