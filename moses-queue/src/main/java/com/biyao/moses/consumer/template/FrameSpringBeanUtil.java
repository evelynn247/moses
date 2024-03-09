package com.biyao.moses.consumer.template;

import java.util.List;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class FrameSpringBeanUtil implements ApplicationContextAware {
    private static ApplicationContext applicationContext;
  
    @Autowired
    private List<KafkaConsumerService> KakfaConsumerServices;
 
    @Override
    public void setApplicationContext(ApplicationContext act) throws BeansException {
        applicationContext = act;
        //kafka消费者初始化
        if(CollectionUtils.isNotEmpty(KakfaConsumerServices)) {
        	for (KafkaConsumerService kakfaConsumerService : KakfaConsumerServices) {
        		kakfaConsumerService.init();
			}
        }
    }
 
    @SuppressWarnings("unchecked")
    public static <T> T getBean(String name) {
        return (T) applicationContext.getBean(name);
    }
 
    public static <T> T getBean(Class<T> cls) {
        return applicationContext.getBean(cls);
    }
}
