package com.biyao.moses.consumer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.biyao.moses.bean.KafkaConfigure;
import com.biyao.moses.consumer.template.KafkaConsumerService;
import com.biyao.moses.consumer.template.KafkaConsumerTemplate;
import com.biyao.moses.util.StringUtil;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import com.uc.domain.constant.UserFieldConstants;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author 邹立强 (zouliqiang@idstaff.com)
 * @version V1.0
 * @Description 曝光商品
 * @date 2019年7月12日下午5:53:37
 */
@Component
public class ExposureProductConsumer extends KafkaConsumerTemplate implements KafkaConsumerService {

    private Logger logger = LoggerFactory.getLogger(ExposureProductConsumer.class);

    @Value("${kafka.bootstrapServers}")
    private String bootstrapServers;
    @Value("${kafka.consumer.exposure.groupId}")
    private String groupId;
    @Value("${kafka.consumer.exposure.topic}")
    private String topic;
    @Value("${kafka.consumer.exposure.threadNum}")
    private Integer threadNum;
    private ExecutorService executorService;
    @Value("${kafka.producer.acks}")
    private String acks;
    @Value("${kafka.producer.retries}")
    private Integer retries;
    @Value("${kafka.producer.batch.size}")
    private Integer batchSize;
    @Value("${kafka.producer.linger.ms}")
    private Integer lingerMs;
    @Value("${kafka.producer.buffer.memory}")
    private Long bufferMemory;
    @Value("${kafka.producer.compression.type}")
    private String compressionType;
    @Value("${kafka.producer.uc.topic}")
    private String kafkaTopic;
    private static String producerTopic;
    private static KafkaProducer<String, String> producer;

    /**
     * mosesqueue消费者是否处理数据开关缓存（用于redis内存紧张、读写过度）
     */
    public static boolean consumerOnOff = true;

    //private RedisUtil redisUtil;
    //限制缓存个数
    //private static final int NUMBER_RECORDS_KEEPING = 500;
    //30天有效期
    //private static final int expireTime = 2592000;//60 * 60 * 24 * 30

    public void init() {
        try {
            executorService = Executors.newFixedThreadPool(threadNum);
            for (int i = 0; i < threadNum; i++) {
                executorService.execute(new ExposureProductConsumer().setBootstrapServers(bootstrapServers)
                        .setGroupId(groupId).setTopic(topic));
            }
            producer = initProducerInstance(KafkaConfigure.builder().acks(acks).batchSize(batchSize)
                    .bootstrapServers(bootstrapServers).bufferMemory(bufferMemory).compressionType(compressionType)
                    .lingerMs(lingerMs).retries(retries).build());
            producerTopic = kafkaTopic;
            //redisUtil = FrameSpringBeanUtil.getBean("redisUtil");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    @BProfiler(key = "com.biyao.moses.consumer.ExposureProductConsumer.handleMessage", monitorType = {MonitorType.TP,
            MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR})
    public void handleMessage(ConsumerRecords<String, String> records) {
        for (ConsumerRecord<String, String> record : records) {
            try {
                //开关关闭，不处理数据
                if (!consumerOnOff) {
                    return;
                }
                String message = record.value();
                if (StringUtils.isBlank(message)) {
                    continue;
                }
                /** message
                 * lt=exp_log        st=2021-07-06 15:10:03        u=23417959        uu=72107060843050071fea2f8c682cf0000000        stid=7        spm=        pcate=100000        p=500001        did=0071fea2f8c682cf        d=iPhone%2011%20Pro%20Max        avn=180        av=5.55.1        utm=        ssid=0071fea2f8c682cf-1625555162863        pvid=0071fea2-1625555162721-QWNMYZWIHE        rpvid=        ip=221.222.21.8        os=iOS13.6        exp_url=biyao://product/togetherGroup/goodsDetail        exp_param={"ct":"1625555385355","suId":"1304345671010400001","stp":"{\"spm\":\"7.500001.home_category_products.0\",\"rpvid\":\"0071fea2-1625555162721-QWNMYZWIHE\",\"scm\":\"moses.old.1607.\",\"aid\":\"{\\\"rcd\\\":\\\"a6b42374cb6bfad4.1625555327828\\\"}\"}"}        exp_stp={"spm":"7.500001.home_category_products.0","rpvid":"0071fea2-1625555162721-QWNMYZWIHE","scm":"moses.old.1607.","aid":"{\"rcd\":\"a6b42374cb6bfad4.1625555327828\"}"}        exp_spm=7.500001.home_category_products.0
                 */
                // 曝光日志kafka中一条消息会存在多条曝光日志使用\n分隔
                String[] msgItems = message.split("\n");
                StringBuffer msgStr = new StringBuffer();
                int count = 0;
                for (String msg : msgItems) {
                    try {
                        HashMap<String, String> mapByLogStr = getMapByLogStr(msg);
                        String expParam = mapByLogStr.get("exp_param");
                        if (StringUtils.isBlank(expParam)) {
                            continue;
                        }
                        @SuppressWarnings("unchecked")
                        Map<String, String> map = JSONObject.parseObject(expParam, Map.class);
                        String goodsId = map.get("goodsID");
                        String suId = map.get("suId");
                        String ct = map.get("ct");
                        // 获取曝光日志中的数据源信息
                        String stp = map.get("stp");
                        //mosesqueue上报曝光日志中 统一格式为pid:time:source
                        String source="";
                        if(!StringUtil.isBlank(stp)){
                            @SuppressWarnings("unchecked")
                            Map<String, String> stpMap = JSONObject.parseObject(stp, Map.class);
                            String scm = stpMap.get("scm");
                            if (!StringUtil.isBlank(scm)) {
                                String[] split = scm.split("\\.");
                                source = split[1];
                            }
                        }
                        String uuid = mapByLogStr.get("uu");
                        suId = StringUtils.isNotBlank(suId) ? suId : goodsId;
                        if (StringUtils.isNotBlank(suId) && StringUtils.isNotBlank(uuid)) {
                            String pid = suId.substring(0, 10);
                            long time;
                            if (StringUtils.isNotBlank(ct)) {
                                time = Long.valueOf(ct);
                            } else {
                                time = System.currentTimeMillis();
                            }
                            if (count == 0) {
                                msgStr.append("uuid=").append(uuid).append("\t").append(UserFieldConstants.EXPPIDS).append("=");
                                count++;
                            }
                            // 埋点中有数据源信息 则需要拼接数据源
                            if(StringUtil.isBlank(source)){
                                msgStr.append(pid).append(":").append(time).append(",");
                            }else {
                                msgStr.append(pid).append(":").append(time).append(":").append(source).append(",");
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (msgStr.length() > 0) {
                    sendKafka(producer, producerTopic, msgStr.substring(0, msgStr.length() - 1));
                }
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("消费商品曝光数据异常：{}，数据内容：{}", e, JSON.toJSONString(record));
            }
        }
    }
}
