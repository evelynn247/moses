package com.biyao.moses.consumer;

import com.alibaba.fastjson.JSON;
import com.biyao.moses.common.constant.MatchRedisKeyConstant;
import com.biyao.moses.common.constant.RedisKeyConstant;
import com.biyao.moses.consumer.template.MqConsumerTemplate;
import com.biyao.moses.schedule.NewUserSpecialTagIdCache;
import com.biyao.moses.util.MatchRedisUtil;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

/**
 * @Description 新手专享商品变更消费
 * @date 2019年6月19日上午11:22:53
 * @version V1.0
 * @author xiaojiankai
 */
@Component
@Slf4j
public class NewUserSpecialProductConsumer extends MqConsumerTemplate {
	
	private Logger logger = LoggerFactory.getLogger(NewUserSpecialProductConsumer.class);

    @Autowired
    private MatchRedisUtil matchRedisUtil;
    @Autowired
    private NewUserSpecialTagIdCache newUserSpecialTagIdCache;
    @Value("${rocketmq.server.namesrvAddr}")
    private String namesrvAddr;
    @Value("${rocketmq.newuser.special.product.consumer.group}")
    private String customerGroup;
    @Value("${rocketmq.newuser.special.product.topic}")
    private String topic;
    private String tags = "*";
    //过期时间为30天
    private static final int expireTime =2592000; // 30 * 24 * 3600

    @PostConstruct
    public void init(){
        initConsumer(namesrvAddr,customerGroup,topic,tags);
    }

    @Override
    @BProfiler(key = "NewUserSpecialProductConsumer.handleMessage", monitorType = { MonitorType.TP,
			MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR })
    public void handleMessage(List<MessageExt> messageExtList) {
        for(MessageExt messageExt : messageExtList){
            String msgBody = null;
            Long tagId = null;
            try {
                msgBody = new String(messageExt.getBody(), "UTF-8");
                /**
                 * msgbody数据格式：{"tagId":244,"execTime":1561172404000,"changed":[1300476288,1300475269,1300476293,1300475274]}
                 */
                Map bodyMap = JSON.parseObject(msgBody, Map.class);
                if(!bodyMap.containsKey("tagId") || bodyMap.get("tagId") == null || StringUtils.isEmpty(bodyMap.get("tagId").toString())){
                    log.error("tag标签日志消息格式错误,tag {}, magBody{}", tagId, JSON.toJSONString(msgBody));
                    return;
                }
                tagId = Long.valueOf(bodyMap.get("tagId").toString());
                List<Long> newUserSpecialProducts = null;
                //如果tagId为新手专享的tagId，则更新新手专享商品的redis缓存
                if(tagId != null && newUserSpecialTagIdCache != null &&
                    newUserSpecialTagIdCache.getNewUserSpecialTagId() != null &&
                    tagId.intValue() == newUserSpecialTagIdCache.getNewUserSpecialTagId().intValue()){
                    newUserSpecialProducts = newUserSpecialTagIdCache.queryProductTagMarked(tagId);
                    if(newUserSpecialProducts != null){
                        StringBuilder value = new StringBuilder();
                        for(Long product : newUserSpecialProducts){
                            value.append(product+",");
                        }
                        //删除最后一个逗号
                        if(value.length() >0) {
                            value.deleteCharAt(value.length() - 1);
                        }
                        matchRedisUtil.setString(MatchRedisKeyConstant.MOSES_NEWUSER_SPECIAL_PRODUCTS, value.toString(),expireTime);
                    }
                }
                //log.error("刷新新手专享tag {}, newUserSpecialProducts {}",tagId, JSON.toJSONString(newUserSpecialProducts));

            }catch(Exception e){
                log.error("处理tag日志消息异常 {},tag {}, magBody{}", e, tagId, JSON.toJSONString(msgBody));
            }

        }
    }
}
