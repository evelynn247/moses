package com.biyao.search.ui.model;

import lombok.Data;

import java.io.Serializable;

/**
 * @author zj
 * @version 1.0
 * @date 2020/1/22 10:54
 * @description
 */
@Data
public class SaleAgent implements Serializable {

    private static final long serialVersionUID = -5626473066375916012L;
    /**
     * 头像
     */
    private String portrait;

    /**
     * 用户身份 1：普通用户；2：大V；3：平台号；4：商家号；5：赠送店铺号；6：企业定制号
     */
    private String identityType;

    /**
     * 昵称
     */
    private String nickName;
}
