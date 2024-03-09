package com.biyao.moses.model.template.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 新手专享返回一起拼一级类目实体
 */
@Setter
@Getter
@Builder
public class FirstCategory implements Serializable {

    private static final long serialVersionUID = 1L;
    //一级类目ID
    private String firstCategoryId;
    //一级类目名称
    private String firstCategoryName;

}
