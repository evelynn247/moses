package com.biyao.search.ui.model;

import lombok.Data;

import java.io.Serializable;

/**
 * @program: search-ui
 * @description: 大V模块数据模型
 * @author: xiafang
 * @create: 2019-10-14 15:39
 **/
@Data
public class VModel implements Serializable {

    private static final long serialVersionUID = 7434841926824341149L;

    /**
     * 大V头像Url
     */
    private String vdisplayPhotoUrl;
    /**
     * 大V昵称
     */
    private String vnickName;
    /**
     * 大V简介
     */
    private String vdesc;
    /**
     * 大V主页Url
     */
    private String vhomePageUrl;
    /**
     * 大V身份
     */
    private String videntity;
    /**
     * 大V背景图Url
     */
    private String vbackgroundImageUrl;
    /**
     * 卡片类型，用于区分大V和企业定制用户卡片信息
     */
    private String vtype;
}
