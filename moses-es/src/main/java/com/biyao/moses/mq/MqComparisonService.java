package com.biyao.moses.mq;

import com.biyao.moses.pdc.IMqLogDao;
import com.biyao.moses.util.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * @program: moses-parent-online
 * @description: Mq 对账
 * @author: changxiaowei
 * @Date: 2021-12-06 14:41
 **/
@Slf4j
@Service
public class MqComparisonService {
    /**
     * 统计多少分钟之前的消息
     */
    private static final int LAST_MINUTE = 10;
    /**
     * 统计区间20分钟内的消息
     */
    private static final int FIRST_MINUTE = 20;


    @Resource
    private IMqLogDao mqLogDao;

    /**
     * 获取mq发送数量接口
     */
    @FunctionalInterface
    public interface IMqSendCountRpcService{

        /**
         * 查询发送数量
         * @param beginTime
         * @param endTime
         * @return
         */
        int getCount(Date beginTime, Date endTime, String topic, List<String> tags);
    }

    /**
     * 对账异常自动补偿接口
     */
    public interface IMqAutoCompensateService{

        /**
         * 自动补偿
         * @param beginTime 开始时间
         * @param endTime 结束时间
         */
        void compensate(Date beginTime, Date endTime);
    }

    /**
     * 通用对账方法
     * @param rpc RPC接口用于返回发送MQ消息总数
     * @param topic 用于对比的topic
     */
    public void comparison(String topic, IMqSendCountRpcService rpc){
        this.comparison(topic, null, rpc, (beginTime, endTime) -> {});
    }

    /**
     * 通用对账方法, 指定tags
     * @param rpc RPC接口用于返回发送MQ消息总数
     * @param topic 用于对比的topic
     */
    public void comparison(String topic,List<String> tags, IMqSendCountRpcService rpc){
        this.comparison(topic, tags, rpc, (beginTime, endTime) -> {});
    }

    /**
     * 通用对账方法
     * @param rpc RPC接口用于返回发送MQ消息总数
     * @param topic 用于对比的topic
     */
    public void comparison(String topic, IMqSendCountRpcService rpc, IMqAutoCompensateService autoCompensateService){
        this.comparison(topic, null, rpc, autoCompensateService);
    }
    /**
     * 通用对账方法
     * @param rpc RPC接口用于返回发送MQ消息总数
     * @param topic 用于对比的topic
     */
    public void comparison(String topic, List<String> tags, IMqSendCountRpcService rpc, IMqAutoCompensateService autoCompensateService){

        Date nowDate = new Date();
        /**
         * 实际是统计10分钟之前，10分钟之内数据
         */
        Date endTime = DateUtil.addMinute(nowDate, -LAST_MINUTE);
        Date beginTime = DateUtil.addMinute(nowDate, -FIRST_MINUTE);

        try {
            //通过rpc获取对方发送mq消息数量
            int sendCount = rpc.getCount(beginTime, endTime, topic, tags);
            //通过日志查询消费mq数量
            int consumeCount = this.mqLogDao.countBySendMsgTime(topic, beginTime, endTime, tags);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            //记录检查报告
            log.info("[检查报告]MQ消费数量对账，topic:{}，对账时间范围:{} ~ {} , 发送条数:{}, 消费条数:{}", topic, sdf.format(beginTime), sdf.format(endTime), sendCount, consumeCount);

            //如果数量不一致，邮件报警
            if(sendCount != consumeCount){
                log.error("[严重异常]MQ消息消费数量定时对账检查异常，topic:{},beginTime:{},endTime:{},sendCount:{}，consumeCount:{}",
                        topic,sdf.format(beginTime),sdf.format(endTime),sendCount,consumeCount);
                // 数量不一致时，触发自动补偿逻辑
                autoCompensateService.compensate(beginTime, endTime);
            }
        } catch (Exception e) {
            /*
             * 记录日志并发送异常邮件
             */
            log.error("[严重异常]执行消费MQ消息定时对账任务时发生异常,topic:{}," +
                    "无严重影响，本次错过的检查，将在下次定时任务执行时覆盖到,请管理员检查异常原因，及时排除。防止下一次执行定时任务时依然出现异常情况",
                    topic);
        }
    }
}
