package com.biyao.search.ui.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @description: 序列化工具
 * @author: luozhuo
 * @date: 2017年2月16日 上午11:52:06 
 */
public class SerializeUtil {
	private static Logger logger = LoggerFactory.getLogger(SerializeUtil.class);
    /**
     * @description: 序列化
     * @param object
     * @return
     * @author: luozhuo
     * @date: 2017年2月16日 上午11:53:20
     */
    public static byte[] serialize(Object object) {
        ObjectOutputStream oos = null;
        ByteArrayOutputStream baos = null;
        try {
            baos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(baos);
            oos.writeObject(object);
            byte[] bytes = baos.toByteArray();
            return bytes;
        } catch (Exception e) {
        	logger.error("[严重异常]序列化工具出现异常, object = {}", object,  e);
        }
        return null;
    }

    /**
     * @description: 反序列化
     * @param bytes
     * @return
     * @author: Administrator
     * @date: 2017年2月16日 上午11:54:07
     */
    public static Object unserialize(byte[] bytes) {
        ByteArrayInputStream bais = null;
        try {
            bais = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = new ObjectInputStream(bais);
            return ois.readObject();
        } catch (Exception e) {
        	logger.error("[严重异常]序列化工具出现异常",e);
        }
        return null;
    }

}