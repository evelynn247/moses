package com.biyao.moses.model.template;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 跳转类型 (分类页使用)
 */
@Getter
@Setter
public class JumpContent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 跳转参数
     */
    private String jumpParam;

    /**
     * 目标页面的名称
     */
    private String jumpPageTitle;
}
