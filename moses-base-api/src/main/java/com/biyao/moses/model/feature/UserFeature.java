package com.biyao.moses.model.feature;

import lombok.*;

import java.util.List;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserFeature {

    /**
     * uuid
     */
    private String uuid;
    /**
     * uid
     */
    private Integer uid;
    /**
     * 性别
     */
    private String sex;
    /**
     * 类目偏好
     */
    private List<String> preferCategoryList;
}
