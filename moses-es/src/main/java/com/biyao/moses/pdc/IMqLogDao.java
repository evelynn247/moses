package com.biyao.moses.pdc;

import com.biyao.moses.pdc.domain.MqLogDomain;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * @program: moses-parent-online
 * @description: mqlog mapper接口
 * @author: changxiaowei
 * @Date: 2021-12-02 16:30
 **/
public interface IMqLogDao {

    /**
     * 查询日志（by uuid）
     *
     * @param uuid
     * @return
     */
    public MqLogDomain selectByUuid(String uuid);

    /**
     * 查询日志（by uuid && msgType）
     * @param uuid
     * @return
     */
    MqLogDomain selectByUuidAndMsgType(@Param("uuid") String uuid, @Param("msgType") byte msgType);
    /**
     * 查询日志（by uuids）
     * @param uuids
     * @return
     */
    public List<MqLogDomain> selectByUuid(List<String> uuids);

    /**
     * 查询日志（by 发送时间）
     *
     * @param beginTime
     * @param mqTopic
     * @param endTime
     * @return
     */
    public List<MqLogDomain> selectBySendMsgTime(String mqTopic, Date beginTime, Date endTime);

    /**
     * 查询日志
     * @param mqTopic
     * @param mqTag
     * @param beginTime
     * @param endTime
     * @return
     */
    public List<MqLogDomain> selectBySendMsgTime(String mqTopic, String mqTag, Date beginTime, Date endTime);

    /**
     * 查询消费成功的日志
     * @param mqTopic
     * @param mqTag
     * @param beginTime
     * @param endTime
     * @return
     */
    public List<MqLogDomain> selectSuccessMsgByTime(String mqTopic, String mqTag, Date beginTime, Date endTime);


    /**
     * 查询日志（by 发送时间）
     *
     * @param beginTime
     * @param mqTopic
     * @param endTime
     * @param tags
     * @return
     */
    public int countBySendMsgTime(String mqTopic, Date beginTime, Date endTime, List<String> tags);


    /**
     * 插入数据
     *
     * @param mqLogDomain
     * @return
     */
    public String insert(MqLogDomain mqLogDomain);

    /**
     * 更新消费成功
     *
     * @param mqLogDomain
     * @return
     */
    public int updateSuccess(MqLogDomain mqLogDomain);

    /**
     * 更新消费失败
     *
     * @param mqLogDomain
     * @return
     */
    public int updateFailure(MqLogDomain mqLogDomain);
}
