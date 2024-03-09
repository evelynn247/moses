package com.biyao.moses.params;

import com.biyao.experiment.ExperimentRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * @author zhaiweixi@idstaff.com
 * @date 2019/10/10
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BaseRequest2 extends ExperimentRequest {
    /**
     * uuid
     */
    protected String uuid;
    /**
     * uid
     */
    protected Integer uid;
    /**
     * app版本号
     */
    protected String av;
    /**
     * app数字版本号
     */
    protected String avn;
    /**
     * 机型
     */
    protected String device;
    /**
     * 站点标识
     */
    protected Integer siteId;
    /**
     * 用户类型  1:老客  2:新访客  3:老访客
     */
    protected Integer upcUserType;
    /**
     * 用户性别  0:男 1:女 -1:中性
     */
    protected Integer userSex;
    /**
     * 服务端生成的唯一标志
     */
    protected String sid;

    /**
     * 用户最近深度浏览的时间
     */
    protected Long latestViewTime;

    /**
     * 轮播图置顶商品
     */
    protected String priorityProductId;

    /**
     * 日志debug开关，scm白名单中的用户打开该日志开关
     */
    protected Boolean debug = false;

    /**
     * 是否展示活动入口，1：展示，其他：不展示
     */
    protected String showAdvert = "0";

    /**
     * 网关传入的可以展示的活动信息列表
     */
    protected List<AdvertInfo> advertInfoList;

    /**
     * 当前页面的前端页面ID
     */
    protected String frontPageId;

    /**
     * 当前页面的页面标示
     */
    protected String pagePositionId;

    @Override
    public String getExperimentUuid() {
        return this.uuid;
    }

    @Override
    public Date getExperimentRequestTime() {
        return new Date();
    }
}
