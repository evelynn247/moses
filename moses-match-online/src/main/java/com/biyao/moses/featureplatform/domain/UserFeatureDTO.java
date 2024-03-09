package com.biyao.moses.featureplatform.domain;


import lombok.Data;

/**
 * @author zhangzhimin
 * @date 2022-03-25
 */
@Data
public class UserFeatureDTO {

    /**
     *基于用户设备的唯一标识
     */
    private String uuid;

    /**
     *注册用户唯一标识
     */
    private Long uid;
}
