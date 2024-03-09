package com.biyao.moses.mq;

/**
 * @program: moses-parent-online
 * @description:
 * @author: changxiaowei
 * @Date: 2021-12-03 14:52
 **/
public class BodyInfo {

    public BodyInfo() {
        super();
    }

    /**
     * body信息构造
     *
     * @param businessId 业务ID
     * @param createTime 消息创建时间
     */
    public BodyInfo(String businessId, Long createTime) {
        super();
        this.businessId = businessId;
        this.createTime = createTime;
    }

    /**
     * 业务ID
     */
    private String businessId;

    /**
     * 消息创建时间
     */
    private Long createTime;


    public String getBusinessId() {
        return businessId;
    }

    public void setBusinessId(String businessId) {
        this.businessId = businessId;
    }

    public Long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }
}
