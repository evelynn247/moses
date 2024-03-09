package com.biyao.moses.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * @program: moses-parent
 * @description: spring反射获取bean
 * @author: changxiaowei
 * @create: 2021-03-26 10:00
 **/
@Component
public class ApplicationContextUtil  implements ApplicationContextAware {

    private static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    public static <T> T getBean(Class<T> classz) {
        return context.getBean(classz);
    }

    public static Object getBean(String paramString) {
        return context.getBean(paramString);
    }
}