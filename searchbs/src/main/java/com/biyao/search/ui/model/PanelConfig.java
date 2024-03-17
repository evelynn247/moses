package com.biyao.search.ui.model;

import com.biyao.search.ui.rest.response.SearchOrderBy;
import com.biyao.search.ui.rest.response.SearchOrderByConsts;
import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author zhaiweixi@idstaff.com
 * @date 2019/10/17
 * 搜索结果页面板配置
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PanelConfig {

    /**
     * 面板是否显示
     * 0:不显示 1:显示，默认不显示
     */
    private Integer onOff = 0;
    /**
     * 底部悬浮框是否显示
     * 0:不显示 1:显示，默认不显示
     */
    private Integer bottomHanging = 0;
    /**
     * 排序选项
     * 默认 热门、销量、价格
     */
    private List<SearchOrderBy> searchOrderByList = Lists.newArrayList(SearchOrderByConsts.HOT, SearchOrderByConsts.SALE,SearchOrderByConsts.NEW, SearchOrderByConsts.PRICE);
    /**
     * 搜索结果显示类型
     * double: 双列显示
     */
    private String showStyle = "double";
}
