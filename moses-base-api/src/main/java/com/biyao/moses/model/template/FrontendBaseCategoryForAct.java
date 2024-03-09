package com.biyao.moses.model.template;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @program: moses-parent
 * @description: 活动页面类目基本信息
 * @author: changxiaowei
 * @Date: 2022-01-26 17:50
 **/
@Data
public class FrontendBaseCategoryForAct implements Serializable {

    private static final long serialVersionUID = -7197976612512411916L;
    /**
     * 前台类目ID
     */
    protected Integer categoryId;
    /**
     * 前台类目名称
     */
    protected String categoryName;
    /**
     * 前台类目级别
     */
    protected Integer categoryLevel;
    /**
     * 类目入口spu商品
     */
    protected Long priorityProductId;
    // 后台三级类目id
    protected List<Integer> thirdCategoryId;
    // tagid
    protected List<Long> tagId;
}
