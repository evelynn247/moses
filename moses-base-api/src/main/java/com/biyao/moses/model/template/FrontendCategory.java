package com.biyao.moses.model.template;

import java.io.Serializable;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * 前台类目
 */
@Setter
@Getter
public class FrontendCategory implements Serializable {

    private static final long serialVersionUID = -5952025436484065686L;

    /**
     * 前台类目ID
     */
    private Integer categoryId;
    /**
     * 前台类目名称
     */
    private String categoryName;
    /**
     * 前台类目级别
     */
    private Integer categoryLevel;
    /**
     * 类目图片
     */
    private String imageUrl;
    /**
     * 子类目列表
     */
    private List<FrontendCategory> subCategoryList;
    // 以下仿照CMS
    /**
     * 前台类目类型
     * 0:普通类目
     * 1：定制类目
     */
    private Integer categoryType;
    /**
     * 后台类目Id集合
     */
    private List<Integer> backendCategoryIdList;
    /**
     * 标签Id集合
     */
    private List<Long> tagIdList;

    /**
     * 当前类目的所有前台三级类目
     */
    private List<FrontendCategory> thirdCategoryDtoList;

   // 以下是分类页属性
    /**
     * 跳转类型
     */
    private Integer jumpType;

    /**
     * 跳转内容
     */
    private JumpContent jumpContent;

    /**
     * webp类型的图片
     */
    private String webpImageUrl;
    
    // 这个用来排序
    private Double score;

    /**
     * 置顶商品ID（平台核心转化V2.1置顶商品ID）
     */
    private String priorityProductId;
}
