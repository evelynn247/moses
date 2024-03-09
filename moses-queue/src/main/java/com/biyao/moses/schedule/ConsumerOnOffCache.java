package com.biyao.moses.schedule;

import com.biyao.moses.common.constant.MatchRedisKeyConstant;
import com.biyao.moses.consumer.ExposureProductConsumer;
import com.biyao.moses.util.MatchRedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * mosesqueue消费者是否处理数据开关缓存（用于redis内存紧张、读写过度）
 */
@Component
@Slf4j
@EnableScheduling
public class ConsumerOnOffCache {

    @Autowired
    private MatchRedisUtil matchRedisUtil;


    /**
     * 检查开关 每5分钟一次
     */
    @Scheduled(cron = "0 0/3 * * * ?")
    private void refreshOnOff() {
        String result = matchRedisUtil.getString(MatchRedisKeyConstant.MOSESQUEUE_CONSUMER_ONOFF);
        log.error("@@@消费者开关状态为  {}",result);
        //开关关闭 不处理数据
        if (StringUtils.isNotBlank(result) && "0".equals(result)) {
            ExposureProductConsumer.consumerOnOff = false ;
        } else {
            ExposureProductConsumer.consumerOnOff = true ;
        }
    }
}
