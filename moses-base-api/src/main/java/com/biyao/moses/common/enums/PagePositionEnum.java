package com.biyao.moses.common.enums;

/**
 * @program: moses-parent
 * @description: 页面标示
 * @author: changxiaowei
 * http://wiki.biyao.com/pages/viewpage.action?pageId=60162054
 * @create: 2021-07-05 11:56
 **/
public enum PagePositionEnum {
    HOMEPAGE("首页feeds流","0"),
    PERSONALPAFE("个人中心","1"),
    SHOPPAGE("购物车","2"),
    ORDER("订单页","3"),
    HOMETABCAR("首页tab分类页","4"),
    SINGLECAR("单类目中间页/三级类目中间页","5")
    ;
    private String pageName;
    private String pagePositionId;

    PagePositionEnum(String pageName, String pagePositionId) {
        this.pageName = pageName;
        this.pagePositionId = pagePositionId;
    }
    public String getPagePositionId() {
        return pagePositionId;
    }
    public String getPageName() {
        return pageName;
    }
}
