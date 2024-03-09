package com.biyao.moses.pdc.mapper;

import com.biyao.moses.pdc.domain.MqLogDomain;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * @program: moses-parent-online
 * @description:
 * @author: changxiaowei
 * @Date: 2021-12-02 19:58
 **/
public interface MqLogMapper {

    /**
     * 查询日志（by uuid）
     *
     * @param uuid
     * @return
     */
    MqLogDomain selectByUuid(String uuid);

    /**
     * 查询日志（by uuids）
     *
     * @param uuids
     * @return
     */
    List<MqLogDomain> selectByUuids(List<String> uuids);
    /**
     * 查询日志（by uuid && msgType）
     * @param uuid
     * @return
     */
    MqLogDomain selectByUuidAndMsgType(@Param("uuid") String uuid, @Param("msgType") Byte msgType);

    /**
     * 查询日志（by 发送时间）
     *
     * @param beginTime
     * @param endTime
     * @return
     */
    List<MqLogDomain> selectBySendMsgTimeExMqTag(@Param("mqTopic") String mqTopic, @Param("beginTime") Date beginTime, @Param("endTime") Date endTime);

    /**
     * 查询日志
     * @param mqTopic
     * @param mqTag
     * @param beginTime
     * @param endTime
     * @return
     */
    List<MqLogDomain> selectBySendMsgTime(@Param("mqTopic") String mqTopic, @Param("mqTag") String mqTag, @Param("beginTime") Date beginTime, @Param("endTime") Date endTime);

    /**
     * 查询消费成功的日志
     * @param mqTopic
     * @param mqTag
     * @param beginTime
     * @param endTime
     * @return
     */
    List<MqLogDomain> selectSuccessMsgByTime(@Param("mqTopic") String mqTopic, @Param("mqTag") String mqTag, @Param("beginTime") Date beginTime, @Param("endTime") Date endTime);

    /**
     * 查询日志（by 发送时间）
     *
     * @param beginTime
     * @param mqTopic
     * @param endTime
     * @return
     */
    int countBySendMsgTime(@Param("mqTopic") String mqTopic, @Param("beginTime") Date beginTime, @Param("endTime") Date endTime, @Param("tags") List<String> tags);

    /**
     * 插入数据
     *
     * @param mqErrorDomain
     * @return
     */
    int insert(MqLogDomain mqErrorDomain);

    /**
     * 更新消费成功
     *
     * @param mqErrorDomain
     * @return
     */
    int updateSuccess(MqLogDomain mqErrorDomain);

    /**
     * 更新消费失败
     *
     * @param mqErrorDomain
     * @return
     */
    int updateFailure(MqLogDomain mqErrorDomain);
}
