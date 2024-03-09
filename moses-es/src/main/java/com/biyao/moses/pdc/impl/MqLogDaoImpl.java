package com.biyao.moses.pdc.impl;

import com.biyao.moses.pdc.IMqLogDao;
import com.biyao.moses.pdc.domain.MqLogDomain;
import com.biyao.moses.pdc.mapper.MqLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

/**
 * @program: moses-parent-online
 * @description:
 * @author: changxiaowei
 * @Date: 2021-12-02 19:54
 **/
@Repository
public class MqLogDaoImpl implements IMqLogDao {

    @Autowired
    private MqLogMapper mqLogMapper;

    @Override
    public MqLogDomain selectByUuid(String uuid) {
        return this.mqLogMapper.selectByUuid(uuid);
    }

    @Override
    public MqLogDomain selectByUuidAndMsgType(String uuid,byte msgType) {
        return this.mqLogMapper.selectByUuidAndMsgType(uuid,msgType);
    }

    @Override
    public List<MqLogDomain> selectByUuid(List<String> uuids) {
        return this.mqLogMapper.selectByUuids(uuids);
    }

    @Override
    public String insert(MqLogDomain MqLogDomain) {
        if(StringUtils.isEmpty(MqLogDomain.getUuid())				) {
            throw new NullPointerException("uuid");
        }

        mqLogMapper.insert(MqLogDomain);
        return MqLogDomain.getUuid();
    }

    @Override
    public int updateSuccess(MqLogDomain mqErrorDomain) {
        return this.mqLogMapper.updateSuccess(mqErrorDomain);
    }

    @Override
    public int updateFailure(MqLogDomain mqErrorDomain) {
        return this.mqLogMapper.updateFailure(mqErrorDomain);
    }

    @Override
    public List<MqLogDomain> selectBySendMsgTime(String mqTopic, Date beginTime, Date endTime) {
        return this.mqLogMapper.selectBySendMsgTimeExMqTag(mqTopic, beginTime, endTime);
    }

    @Override
    public List<MqLogDomain> selectBySendMsgTime(String mqTopic, String mqTag, Date beginTime, Date endTime) {
        return this.mqLogMapper.selectBySendMsgTime(mqTopic, mqTag,beginTime, endTime);
    }

    @Override
    public List<MqLogDomain> selectSuccessMsgByTime(String mqTopic, String mqTag, Date beginTime, Date endTime) {
        return this.mqLogMapper.selectSuccessMsgByTime(mqTopic, mqTag,beginTime, endTime);
    }

    @Override
    public int countBySendMsgTime(String mqTopic, Date beginTime, Date endTime, List<String> tags) {
        return this.mqLogMapper.countBySendMsgTime(mqTopic, beginTime, endTime, tags);
    }

}
