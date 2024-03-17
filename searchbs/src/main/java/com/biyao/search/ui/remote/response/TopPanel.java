package com.biyao.search.ui.remote.response;

import com.biyao.search.ui.rest.response.SearchOrderBy;
import com.biyao.search.ui.rest.response.SearchOrderByConsts;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;

public class TopPanel {
    /**
     * 面板开关
     * 1 - 展示
     * 0 - 关闭
     */
    private Integer onOff = 0;

    /**
     * 排序项列表
     */
    private List<SearchOrderBy> orderByList = Lists.newArrayList(SearchOrderByConsts.NORMAL, SearchOrderByConsts.SALE,SearchOrderByConsts.NEW, SearchOrderByConsts.PRICE);

    /**
     * 筛选项列表
     */
    private List<UIFacet> uIFacet = new ArrayList<UIFacet>();

    /**
     * 是否展示上新小红点
     * 1-展示；0-不展示（默认不展示）
     */
    private Byte isShowRedDot = 0;

    public Integer getOnOff() {
        return onOff;
    }

    public void setOnOff(Integer onOff) {
        this.onOff = onOff;
    }

    public List<SearchOrderBy> getOrderByList() {
        return orderByList;
    }

    public void setOrderByList(List<SearchOrderBy> orderByList) {
        this.orderByList = orderByList;
    }

    public List<UIFacet> getFacet() {
        return uIFacet;
    }

    public void setFacet(List<UIFacet> uIFacet) {
        this.uIFacet = uIFacet;
    }


    public Byte getIsShowRedDot() {
        return isShowRedDot;
    }

    public void setIsShowRedDot(Byte showRedDot) {
        isShowRedDot = showRedDot;
    }
}
