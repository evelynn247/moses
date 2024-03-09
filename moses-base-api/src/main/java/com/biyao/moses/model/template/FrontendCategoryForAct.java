package com.biyao.moses.model.template;

import lombok.Data;

import java.util.List;

/**
 * @program: moses-parent
 * @description:
 * @author: changxiaowei
 * @Date: 2022-01-26 17:51
 **/
@Data
public class FrontendCategoryForAct extends FrontendBaseCategoryForAct{

    private static final long serialVersionUID = -8127968181261516759L;
    // 子级类目
    private List<FrontendCategoryForAct> subFrontendCategoryList;
}
