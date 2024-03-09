package com.biyao.moses.pdc.domain;

import lombok.Data;

import java.util.Date;

/**
 * @program: moses-parent-online
 * @description:
 * @author: changxiaowei
 * @Date: 2021-12-02 16:35
 **/
@Data
public class MqLogDomain {
    private String uuid;
    private Integer type;
    private String mqContext;
    private String mqTag;
    private Date createTime;
    private Date consumeSuccTime;
    private Integer reconsumeTimes;
    private String remark;
    private Integer status;
    private Date sendMsgTime;
    private String mqTopic;
    private String businessId;
}
