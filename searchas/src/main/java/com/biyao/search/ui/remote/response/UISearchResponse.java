package com.biyao.search.ui.remote.response;

import com.biyao.search.ui.model.VModel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 搜索结果response
 *
 * @author biyao
 * @date
 */
@Data
public class UISearchResponse {
    /**
     * 当前搜索词
     */
    private String query;
    /**
     * 大V模块数据，大运河V1.2之前有数据正常返回，没有数据赋值null;大运河V1.2及以后版本该字段废弃，赋值null
     */
    private VModel vmodel;

    /**
     * 大V和企业定制用户,大运河V1.2及以后版本使用该字段出大V和企业定制用户卡片信息
     */
    private List<VModel> vmodelList = new ArrayList<>();

    /**
     * 当前搜索ID
     */
    private String sid;

    /**
     * 返回结果是否显示浮层
     * 1 - 显示
     * 0 - 不显示
     */
    private Integer bottomHanging = 0;

    /**
     * 当前排序方式
     */
    private String orderBy;

    /**
     * 搜索页顶部提示文本（目前是未完全匹配时使用）
     * 外层的list代表多行文本
     * 内层的list代表一行文本里的多个文本块
     */
    private List<SearchTitle> tips = new ArrayList<>();

    /**
     * 顶部面板相关内容
     */
    private TopPanel topPanel = new TopPanel();

    /**
     * 区块列表
     */
    private List<BlockData> blockData = new ArrayList<>();
    /**
     * 图片筛选项列表
     */
    @Setter
    @Getter
    private List<FilterItem> imageFilters = new ArrayList<>();

    /**
     * 商家店铺搜索预置页搜本店没有结果时使用，无搜索结果赋值为1，默认0表示有搜索结果
     */
    private String isEmpty="0";

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public String getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(String orderBy) {
        this.orderBy = orderBy;
    }

    public List<SearchTitle> getTips() {
        return tips;
    }

    public void setTips(List<SearchTitle> tips) {
        this.tips = tips;
    }

    public TopPanel getTopPanel() {
        return topPanel;
    }

    public void setTopPanel(TopPanel topPanel) {
        this.topPanel = topPanel;
    }

    public Integer getBottomHanging() {
        return bottomHanging;
    }

    public void setBottomHanging(Integer bottomHanging) {
        this.bottomHanging = bottomHanging;
    }

    public List<BlockData> getBlockData() {
        return blockData;
    }

    public void setBlockData(List<BlockData> blockData) {
        this.blockData = blockData;
    }


}
