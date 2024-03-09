package com.biyao.moses.model.drools;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @program: moses-parent
 * @description:
 * @author: changxiaowei
 * @create: 2021-04-12 15:55
 **/
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BuildBaseFactParam {
    private  String sceneId;
    private String topicId;
    private String biz;
    private String uuid;
    private String uid;
    private String utype;
    private String siteId;
}
